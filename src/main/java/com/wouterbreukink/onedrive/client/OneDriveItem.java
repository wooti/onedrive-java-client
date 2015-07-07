package com.wouterbreukink.onedrive.client;

/**
 * Copyright Wouter Breukink 2015
 */
public interface OneDriveItem {
    String getId();

    boolean isFolder();

    String getPath();
}
