package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.ErrorFacet;
import com.wouterbreukink.onedrive.client.resources.ErrorSet;
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
import java.util.logging.Level;
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

    public <T> T getResponse(Class<T> entityType) {

        // TODO config retries
        for (int retries = 5; retries > 0; retries--) {
            Response response = null;
            try {

                response = getResponse();
                if (!responseValid(response)) {
                    continue;
                }

                return response.readEntity(entityType);

            } catch (Throwable ex) {
                log.log(Level.SEVERE, "Unable to process request", ex);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        throw new Error("Unable to complete request");
    }

    private Response getResponse() throws FileNotFoundException {

        WebTarget requestTarget = client
                .target("https://api.onedrive.com/v1.0")
                .path(path)
                .queryParam("access_token", authoriser.getAuthorisation().getAccessToken());

        if (skipToken != null) {
            requestTarget = requestTarget.queryParam("$skiptoken", skipToken);
        }

        Invocation.Builder builder = requestTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Entity<?> entity = null;
        if (payloadFile != null && payloadJson != null) {
            entity = generateMultipartEntity();
        } else if (payloadFile != null) {
            Entity.entity(new FileInputStream(payloadFile), MediaType.APPLICATION_OCTET_STREAM);
        } else if (payloadJson != null) {
            entity = Entity.json(payloadJson);
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

    private boolean responseValid(Response response) {

        try {
            switch (response.getStatus()) {
                case 200:
                case 201:
                case 202:
                    return true;
                case 401:
                    log.warning("Received 401 (Unauthorised) response");
                    authoriser.getTokenFromRefreshToken(authoriser.getAuthorisation().getRefreshToken());
                    return false;
                case 503:
                    log.warning("Server returned 503 (Temporarily Unavailable) - sleeping 10 seconds");
                    Thread.sleep(10000);
                    return false;
                case 509:
                    log.warning("Server returned 509 (Bandwidth Limit Exceeded) - sleeping 60 seconds");
                    Thread.sleep(60000);
                    return false;
                default:
                    ErrorFacet error = response.readEntity(ErrorSet.class).getError();
                    throw new Error(String.format("Error Code %d: %s (%s)", response.getStatus(), error.getCode(), error.getMessage()));

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
