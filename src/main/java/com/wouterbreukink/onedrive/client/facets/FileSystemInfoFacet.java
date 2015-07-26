package com.wouterbreukink.onedrive.client.facets;

import com.google.api.client.util.Key;

public class FileSystemInfoFacet {

    @Key
    private String createdDateTime;
    @Key
    private String lastModifiedDateTime;

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(String lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }
}
