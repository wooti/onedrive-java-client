package com.wouterbreukink.onedrive.client.resources;

public class ItemReference implements OneDriveItem {

    private String driveId;
    private String id;
    private String path;

    public String getDriveId() {
        return driveId;
    }

    public String getId() {
        return id;
    }

    public boolean isFolder() {
        return true;
    }

    public String getPath() {
        return path;
    }

    public String getReadablePath() {

        if (path == null) {
            return path;
        }

        int index = path.indexOf(':');
        return index > 0 ? path.substring(index + 1) : path;
    }
}
