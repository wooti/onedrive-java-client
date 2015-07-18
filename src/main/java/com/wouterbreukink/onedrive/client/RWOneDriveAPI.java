package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.WriteFolder;
import com.wouterbreukink.onedrive.client.resources.WriteItem;
import com.wouterbreukink.onedrive.client.resources.facets.FileSystemInfoFacet;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

public class RWOneDriveAPI extends ROOneDriveAPI implements OneDriveAPI {

    public RWOneDriveAPI(Client client, OneDriveAuth authoriser) {
        super(client, authoriser);
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + ":/" + file.getName() + ":/content")
                .payloadFile(file)
                .method("PUT");

        Item item = request.getResponse(Item.class);

        // Now update the item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
    }

    public Item uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isFolder()) {
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
                .payloadFile(file)
                .method("POST");

        return request.getResponse(Item.class);
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

    private OneDriveRequest getDefaultRequest() {
        return new OneDriveRequest(client, authoriser);
    }
}
