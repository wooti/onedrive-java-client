package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.resources.*;
import com.wouterbreukink.onedrive.resources.facets.FileSystemInfoFacet;
import jersey.repackaged.com.google.common.collect.Lists;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class OneDriveClient {

    private static final Logger log = Logger.getLogger(OneDriveClient.class.getName());
    private final Client client;
    private final OneDriveAuth authoriser;

    public OneDriveClient(Client client, OneDriveAuth authoriser) {

        log.setLevel(Main.logLevel);

        this.authoriser = authoriser;
        this.client = client;
    }

    public Drive getDefaultDrive() {

        OneDriveRequest request = getDefaultRequest()
                .path("drive")
                .method("GET");

        return request.getResponse(Drive.class);
    }

    public Item getRoot() {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root")
                .method("GET");

        return request.getResponse(Item.class);
    }

    public Item[] getChildren(Item parent) {

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

            if (token != null) {
                log.finer(String.format("Got %d items, fetching next page", itemsToReturn.size()));
            }
        } while (token != null); // If we have a token for the next page we need to keep going

        return itemsToReturn.toArray(new Item[itemsToReturn.size()]);
    }

    public Item replaceFile(ItemReference parent, File file) throws IOException {
        return replaceFile(parent.getId(), file);
    }

    public Item replaceFile(Item parent, File file) throws IOException {
        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        return replaceFile(parent.getId(), file);
    }

    private Item replaceFile(String parentId, File file) throws IOException {

        FileInputStream stream = new FileInputStream(file);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parentId + ":/" + file.getName() + ":/content")
                .entity(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM))
                .method("PUT");

        Item item = request.getResponse(Item.class);

        // Now update the item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
    }

    public Item uploadFile(ItemReference parent, File file) throws IOException {
        return uploadFile(parent.getId(), file);
    }

    public Item uploadFile(Item parent, File file) throws IOException {
        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        return uploadFile(parent.getId(), file);
    }

    private Item uploadFile(String parentId, File file) throws IOException {

        FileInputStream stream = new FileInputStream(file);

        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        FileSystemInfoFacet fsi = new FileSystemInfoFacet();
        fsi.setLastModifiedDateTime(new Date(attr.lastModifiedTime().toMillis()));
        fsi.setCreatedDateTime(new Date(attr.creationTime().toMillis()));

        WriteItem itemToWrite = new WriteItem(file.getName(), fsi, true);

        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(Boundary.addBoundary(new MediaType("multipart", "related")));

        BodyPart p0 = new BodyPart(itemToWrite, MediaType.APPLICATION_JSON_TYPE);
        BodyPart p1 = new BodyPart(stream, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        p0.getHeaders().putSingle("Content-ID", "<metadata>");
        p1.getHeaders().putSingle("Content-ID", "<content>");

        multiPart.bodyPart(p0);
        multiPart.bodyPart(p1);

        log.fine("Starting upload of file: " + file.getPath());
        long startTime = System.currentTimeMillis();

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parentId + "/children")
                .entity(Entity.entity(multiPart, multiPart.getMediaType()))
                .method("POST");

        Item response = request.getResponse(Item.class);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.fine(String.format("Upload complete - %d KB in %dms - %.2f KB/s",
                file.length() / 1024,
                elapsedTime,
                elapsedTime > 0 ? ((file.length() / 1024d) / (elapsedTime / 1000d)) : 0));

        return response;
    }

    public Item getPath(String path) {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root:/" + path)
                .method("GET");

        return request.getResponse(Item.class);
    }

    private OneDriveRequest getDefaultRequest() {
        return new OneDriveRequest(client, authoriser);
    }

    public Item updateFile(Item item, Date createdDate, Date modifiedDate) {

        WriteItem updateItem = new WriteItem(item.getName(), new FileSystemInfoFacet(), false);

        updateItem.getFileSystemInfo().setCreatedDateTime(createdDate);
        updateItem.getFileSystemInfo().setLastModifiedDateTime(modifiedDate);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + item.getId())
                .entity(Entity.json(updateItem))
                .method("PATCH");

        return request.getResponse(Item.class);
    }

    public Item createFolder(Item parent, String name) {

        WriteFolder newFolder = new WriteFolder(name);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + "/children")
                .entity(Entity.json(newFolder))
                .method("POST");

        return request.getResponse(Item.class);
    }
}
