package com.wouterbreukink.onedrive.client.resources.facets;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wouterbreukink.onedrive.client.serialization.JsonDateSerializer;

import java.util.Date;

public class FileSystemInfoFacet {

    private Date createdDateTime;
    private Date lastModifiedDateTime;

    @JsonSerialize(using = JsonDateSerializer.class)
    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @JsonSerialize(using = JsonDateSerializer.class)
    public Date getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(Date lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }
}
