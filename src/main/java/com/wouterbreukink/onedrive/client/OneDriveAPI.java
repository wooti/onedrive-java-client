package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.Drive;
import com.wouterbreukink.onedrive.client.resources.Item;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public interface OneDriveAPI {

    // Read only operations

    Drive getDefaultDrive() throws OneDriveAPIException;

    Item getRoot() throws OneDriveAPIException;

    Item[] getChildren(OneDriveItem parent) throws OneDriveAPIException;

    Item getPath(String path) throws OneDriveAPIException;

    // Write operations

    OneDriveItem replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException;

    OneDriveItem uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException;

    OneDriveItem uploadFileInChunks(OneDriveItem parent, File file, int chunkSize) throws OneDriveAPIException, IOException;

    OneDriveItem updateFile(Item item, Date createdDate, Date modifiedDate) throws OneDriveAPIException;

    OneDriveItem createFolder(OneDriveItem parent, String name) throws OneDriveAPIException;

    void download(Item item, File target) throws OneDriveAPIException;

    void delete(Item remoteFile) throws OneDriveAPIException;
}
