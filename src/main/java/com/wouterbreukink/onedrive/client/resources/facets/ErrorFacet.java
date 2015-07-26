package com.wouterbreukink.onedrive.client.resources.facets;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorFacet {

    private String code;
    private String message;
    private ErrorFacet innerError;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @JsonProperty("innererror")
    public ErrorFacet getInnerError() {
        return innerError;
    }
}
