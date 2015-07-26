package com.wouterbreukink.onedrive.client.facets;

import com.google.api.client.util.Key;

public class ErrorFacet {

    @Key
    private String code;
    @Key
    private String message;
    @Key
    private ErrorFacet innerError;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ErrorFacet getInnerError() {
        return innerError;
    }
}
