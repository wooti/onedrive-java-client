package com.wouterbreukink.onedrive.client.resources;

/**
 * Copyright Wouter Breukink 2015
 */
public class ItemReference {

    private String driveId;
    private String id;
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

    public String getReadablePath() {

        if (path == null) return path;

        int index = path.indexOf(':');
        return index > 0 ? path.substring(index + 1) : path;
    }
}
