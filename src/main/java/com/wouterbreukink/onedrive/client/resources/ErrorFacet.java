package com.wouterbreukink.onedrive.client.resources;

/**
 * Copyright Wouter Breukink 2015
 */

public class ErrorFacet {

    private String code;
    private String message;
    private ErrorFacet innererror;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ErrorFacet getInnererror() {
        return innererror;
    }
}