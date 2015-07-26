package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;

public class ItemReference {

    @Key
    private String driveId;
    @Key
    private String id;
    @Key
    private String path;

    public String getDriveId() {
        return driveId;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }
}
