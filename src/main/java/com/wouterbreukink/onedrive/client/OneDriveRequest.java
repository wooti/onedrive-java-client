package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.ErrorSet;
import jersey.repackaged.com.google.common.base.Throwables;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

public class OneDriveRequest {

    private static final Logger log = Logger.getLogger(OneDriveRequest.class.getName());
    private final OneDriveAuth authoriser;
    private final Client client;

    private String path;
    private String method;
    private String skipToken;

    private Object payloadJson;
    private File payloadFile;

    public OneDriveRequest(Client client, OneDriveAuth authoriser) {
        this.client = client;
        this.authoriser = authoriser;
    }

    public OneDriveRequest path(String path) {
        this.path = path;
        return this;
    }

    public OneDriveRequest method(String method) {
        this.method = method;
        return this;
    }

    public OneDriveRequest skipToken(String skipToken) {
        this.skipToken = skipToken;
        return this;
    }

    public OneDriveRequest payloadFile(File payloadFile) {
        this.payloadFile = payloadFile;
        return this;
    }

    public OneDriveRequest payloadJson(Object payloadJson) {
        this.payloadJson = payloadJson;
        return this;
    }

    public <T> T getResponse(Class<T> entityType) throws OneDriveAPIException {

        Response response = getResponse();

        switch (response.getStatus()) {
            case 200:
            case 201:
            case 202:
                return response.readEntity(entityType);
            case 401:
                authoriser.getTokenFromRefreshToken(authoriser.getAuthorisation().getRefreshToken());
            default:
                throw new OneDriveAPIException(response.getStatus(), getMessage(response));
        }
    }

    private Response getResponse() {

        WebTarget requestTarget = client
                .target("https://api.onedrive.com/v1.0")
                .path(path)
                .queryParam("access_token", authoriser.getAuthorisation().getAccessToken());

        if (skipToken != null) {
            requestTarget = requestTarget.queryParam("$skiptoken", skipToken);
        }

        Invocation.Builder builder = requestTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Entity<?> entity = null;
        try {
            if (payloadFile != null && payloadJson != null) {
                entity = generateMultipartEntity();
            } else if (payloadFile != null) {
                Entity.entity(new FileInputStream(payloadFile), MediaType.APPLICATION_OCTET_STREAM);
            } else if (payloadJson != null) {
                entity = Entity.json(payloadJson);
            }
        } catch (FileNotFoundException e) {
            Throwables.propagate(e);
        }

        return entity != null ? builder.method(method, entity) : builder.method(method);
    }

    private Entity<?> generateMultipartEntity() throws FileNotFoundException {
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(Boundary.addBoundary(new MediaType("multipart", "related")));

        BodyPart jsonPart = new BodyPart(payloadJson, MediaType.APPLICATION_JSON_TYPE);
        jsonPart.getHeaders().putSingle("Content-ID", "<metadata>");
        multiPart.bodyPart(jsonPart);

        BodyPart filePart = new BodyPart(new FileInputStream(payloadFile), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        filePart.getHeaders().putSingle("Content-ID", "<content>");
        multiPart.bodyPart(filePart);

        return Entity.entity(multiPart, multiPart.getMediaType());
    }

    private String getMessage(Response response) {

        if (response == null) {
            return null;
        }

        StringBuilder msgBuilder = new StringBuilder().append("Error Code ").append(response.getStatus());

        ErrorSet error;

        if ((error = response.readEntity(ErrorSet.class)) != null) {
            msgBuilder.append(": ").append(error.getError().getCode());
            msgBuilder.append(" (").append(error.getError().getMessage()).append(")");
        } else {
            msgBuilder.append(": unknown error");
        }

        return msgBuilder.toString();
    }
}
