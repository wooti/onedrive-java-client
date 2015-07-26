package com.wouterbreukink.onedrive.client.api;

import com.wouterbreukink.onedrive.client.*;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.UploadSession;
import com.wouterbreukink.onedrive.client.resources.WriteFolder;
import com.wouterbreukink.onedrive.client.resources.WriteItem;
import com.wouterbreukink.onedrive.client.resources.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.resources.facets.MultiPartItem;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

class RWOneDriveProvider extends ROOneDriveProvider implements OneDriveProvider {

    public RWOneDriveProvider(Client client, OneDriveAuth authoriser) {
        super(client, authoriser);
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + ":/" + file.getName() + ":/content")
                .payloadBinary(Files.readAllBytes(file.toPath()))
                .method("PUT");

        Item item = request.getResponse(Item.class);

        // Now update the item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
    }

    public Item uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        // Generate the update item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        FileSystemInfoFacet fsi = new FileSystemInfoFacet();
        fsi.setLastModifiedDateTime(new Date(attr.lastModifiedTime().toMillis()));
        fsi.setCreatedDateTime(new Date(attr.creationTime().toMillis()));
        WriteItem itemToWrite = new WriteItem(file.getName(), fsi, true);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + "/children")
                .payloadJson(itemToWrite)
                .payloadBinary(Files.readAllBytes(file.toPath()))
                .method("POST");

        return request.getResponse(Item.class);
    }

    @Override
    public OneDriveUploadSession startUploadSession(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {
        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + ":/" + file.getName() + ":/upload.createSession")
                .payloadJson(MultiPartItem.create(file.getName()))
                .header("Content-Length", file.length())
                .method("POST");

        UploadSession session = request.getResponse(UploadSession.class);

        return new OneDriveUploadSession(parent, file, session.getUploadUrl(), session.getNextExpectedRanges());
    }

    @Override
    public void uploadChunk(OneDriveUploadSession session) throws OneDriveAPIException, IOException {

        byte[] bytesToUpload = session.getChunk();
        long length = session.getFile().length();
        Item item;

        OneDriveRequest uploadPart = getDefaultRequest()
                .target(session.getUploadUrl())
                .payloadBinary(bytesToUpload)
                .header("Content-Range", String.format("bytes %d-%d/%d", session.getTotalUploaded(), session.getTotalUploaded() + bytesToUpload.length - 1, length))
                .method("PUT");

        if (session.getTotalUploaded() + bytesToUpload.length < length) {
            UploadSession response = uploadPart.getResponse(UploadSession.class);
            session.setRanges(response.getNextExpectedRanges());
            return;
        } else {
            item = uploadPart.getResponse(Item.class);
        }

        // If this is the final chunk then set the properties
        BasicFileAttributes attr = Files.readAttributes(session.getFile().toPath(), BasicFileAttributes.class);
        item = updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));

        // Upload session is now complete
        session.setComplete(item);
    }

    public Item updateFile(Item item, Date createdDate, Date modifiedDate) throws OneDriveAPIException {

        WriteItem updateItem = new WriteItem(item.getName(), new FileSystemInfoFacet(), false);

        updateItem.getFileSystemInfo().setCreatedDateTime(createdDate);
        updateItem.getFileSystemInfo().setLastModifiedDateTime(modifiedDate);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + item.getId())
                .payloadJson(updateItem)
                .method("PATCH");

        return request.getResponse(Item.class);
    }

    public Item createFolder(OneDriveItem parent, String name) throws OneDriveAPIException {

        WriteFolder newFolder = new WriteFolder(name);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + "/children")
                .payloadJson(newFolder)
                .method("POST");

        return request.getResponse(Item.class);
    }

    public void download(Item item, File target) throws OneDriveAPIException {
        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + item.getId() + "/content")
                .method("GET");

        request.getFile(target);
    }

    public void delete(Item remoteFile) throws OneDriveAPIException {
        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + remoteFile.getId())
                .method("DELETE");

        request.getResponse(null);
    }
}
