package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;
import com.wouterbreukink.onedrive.client.facets.QuotaFacet;

public class Drive {

    @Key
    private String id;

    @Key
    private String driveType;

    @Key
    private IdentitySet owner;

    @Key
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
