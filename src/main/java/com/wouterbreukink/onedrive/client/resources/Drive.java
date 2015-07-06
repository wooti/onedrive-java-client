package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wouterbreukink.onedrive.client.resources.facets.Quota;

@JsonIgnoreProperties(value = "@odata.context")
public class Drive {

    private String id;
    private String driveType;
    private IdentitySet owner;
    private Quota quota;

    public String getId() {
        return id;
    }

    public String getDriveType() {
        return driveType;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public IdentitySet getOwner() {
        return owner;
    }

    public Quota getQuota() {
        return quota;
    }
}
