package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wouterbreukink.onedrive.client.resources.facets.QuotaFacet;

@JsonIgnoreProperties(value = "@odata.context")
public class Drive {

    private String id;
    private String driveType;
    private IdentitySet owner;
    private QuotaFacet quota;

    public String getId() {
        return id;
    }

    public String getDriveType() {
        return driveType;
    }

    public IdentitySet getOwner() {
        return owner;
    }

    public QuotaFacet getQuota() {
        return quota;
    }
}
