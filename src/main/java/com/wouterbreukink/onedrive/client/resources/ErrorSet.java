package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;
import com.wouterbreukink.onedrive.client.facets.ErrorFacet;

public class ErrorSet {

    @Key
    private ErrorFacet error;

    public ErrorFacet getError() {
        return error;
    }
}
