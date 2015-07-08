package com.wouterbreukink.onedrive.client.resources;

public interface OneDriveItem {
    String getId();

    boolean isFolder();

    String getPath();
}
