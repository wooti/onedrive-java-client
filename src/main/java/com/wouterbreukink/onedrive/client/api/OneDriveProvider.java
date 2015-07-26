package com.wouterbreukink.onedrive.client.api;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.OneDriveUploadSession;
import com.wouterbreukink.onedrive.client.resources.Drive;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public interface OneDriveProvider {

    // Read only operations

    Drive getDefaultDrive() throws OneDriveAPIException;

    OneDriveItem getRoot() throws OneDriveAPIException;

    OneDriveItem[] getChildren(OneDriveItem parent) throws OneDriveAPIException;

    OneDriveItem getPath(String path) throws OneDriveAPIException;

    // Write operations

    OneDriveItem replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException;

    OneDriveItem uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException;

    OneDriveUploadSession startUploadSession(OneDriveItem parent, File file) throws OneDriveAPIException, IOException;

    void uploadChunk(OneDriveUploadSession session) throws OneDriveAPIException, IOException;

    OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws OneDriveAPIException;

    OneDriveItem createFolder(OneDriveItem parent, String name) throws OneDriveAPIException;

    void download(OneDriveItem item, File target) throws OneDriveAPIException;

    void delete(OneDriveItem remoteFile) throws OneDriveAPIException;

    class FACTORY {

        public static OneDriveProvider readOnlyApi(Client client, OneDriveAuth authoriser) {
            return new ROOneDriveProvider(client, authoriser);
        }

        public static OneDriveProvider readWriteApi(Client client, OneDriveAuth authoriser) {
            return new RWOneDriveProvider(client, authoriser);
        }
    }
}
