package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.*;
import com.wouterbreukink.onedrive.client.resources.facets.FileSystemInfoFacet;
import jersey.repackaged.com.google.common.collect.Lists;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class OneDriveAPI {

    private final Client client;
    private final OneDriveAuth authoriser;

    public OneDriveAPI(Client client, OneDriveAuth authoriser) {
        this.authoriser = authoriser;
        this.client = client;
    }

    public Drive getDefaultDrive() throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive")
                .method("GET");

        return request.getResponse(Drive.class);
    }

    public Item getRoot() throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root")
                .method("GET");

        return request.getResponse(Item.class);
    }

    public Item[] getChildren(OneDriveItem parent) throws OneDriveAPIException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        List<Item> itemsToReturn = Lists.newArrayList();

        String token = null;

        do {
            OneDriveRequest request = getDefaultRequest()
                    .path("/drive/items/" + parent.getId() + "/children")
                    .skipToken(token)
                    .method("GET");

            ItemSet items = request.getResponse(ItemSet.class);
            Collections.addAll(itemsToReturn, items.getValue());
            token = items.getNextToken();

        } while (token != null); // If we have a token for the next page we need to keep going

        return itemsToReturn.toArray(new Item[itemsToReturn.size()]);
    }

    public Item replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

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

    public Item getPath(String path) throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root:/" + path)
                .method("GET");

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

    private OneDriveRequest getDefaultRequest() {
        return new OneDriveRequest(client, authoriser);
    }
}
