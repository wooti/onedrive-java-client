package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.ErrorFacet;
import com.wouterbreukink.onedrive.client.resources.ErrorSet;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class OneDriveRequest {

    private static final Logger log = Logger.getLogger(OneDriveRequest.class.getName());

    private String path;
    private String method;
    private OneDriveAuth authoriser;
    private String skipToken;
    private Client client;
    private Entity<?> entity;

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

    public OneDriveRequest entity(Entity<?> entity) {
        this.entity = entity;
        return this;
    }

    public <T> T getResponse(Class<T> entityType) {

        // TODO config retries
        for (int retries = 5; retries > 0; retries--) {
            Response response = null;
            try {
                response = getResponse();

                if (response.getStatus() == 401) {
                    log.warning("Received 401 (Unauthorised) response");
                    authoriser.getTokenFromRefreshToken(authoriser.getAuthorisation().getRefreshToken());
                    continue;
                }

                if (response.getStatus() == 503) {
                    try {
                        log.warning("Server returned 503 - sleeping 10 seconds");
                        response.close();
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        log.warning(e.toString());
                    }
                    continue;
                }

                if (response.getStatus() == 509) {
                    try {
                        log.warning("Server returned 509 - sleeping 60 seconds");
                        response.close();
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        log.warning(e.toString());
                    }
                    continue;
                }

                verifyResponse(response);
                return response.readEntity(entityType);
            } catch (Throwable ex) {
                log.severe("Caught exception: " + ex);
            } finally {

                if (response != null) {
                    response.close();
                }
            }
        }

        throw new Error("Unable to complete request");
    }

    private Response getResponse() {

        WebTarget requestTarget = client
                .target("https://api.onedrive.com/v1.0")
                .path(path)
                .queryParam("access_token", authoriser.getAuthorisation().getAccessToken());

        if (skipToken != null) {
            requestTarget = requestTarget.queryParam("$skiptoken", skipToken);
        }

        Invocation.Builder builder = requestTarget
                .request(MediaType.TEXT_PLAIN_TYPE);

        if (entity != null) {
            return builder.method(method, entity);
        } else {
            return builder.method(method);
        }
    }

    private void verifyResponse(Response response) {

        int status = response.getStatus();
        if (status != 200 && status != 201) {
            ErrorFacet error = response.readEntity(ErrorSet.class).getError();
            throw new Error(String.format("Error Code %d: %s (%s)", response.getStatus(), error.getCode(), error.getMessage()));
        }
    }
}
