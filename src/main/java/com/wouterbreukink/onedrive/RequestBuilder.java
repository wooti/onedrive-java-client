package com.wouterbreukink.onedrive;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class RequestBuilder {

    private String path;
    private String method;
    private OneDriveAuth authoriser;
    private String skipToken;
    private WebTarget target;
    private Entity<?> entity;

    public RequestBuilder(WebTarget target) {
        this.target = target;
    }

    public RequestBuilder path(String path) {
        this.path = path;
        return this;
    }

    public RequestBuilder method(String method) {
        this.method = method;
        return this;
    }

    public RequestBuilder path(OneDriveAuth authoriser) {
        this.authoriser = authoriser;
        return this;
    }

    public RequestBuilder skipToken(String skipToken) {
        this.skipToken = skipToken;
        return this;
    }

    public RequestBuilder entity(Entity<?> entity) {
        this.entity = entity;
        return this;
    }

    public Invocation Build() {
        Invocation.Builder builder =
                target.path(path)
                        .request(MediaType.TEXT_PLAIN_TYPE);

        if (skipToken != null) {
            builder.header("$skiptoken", skipToken);
        }

        //if (entity != null) {
        return builder.build(method, entity);
        //} else {
        //    return builder.build(method);
        //}

    }
}
