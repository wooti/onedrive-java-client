package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = "@odata.context")
public class ErrorSet {

    private ErrorFacet error;


    public ErrorFacet getError() {
        return error;
    }
}
