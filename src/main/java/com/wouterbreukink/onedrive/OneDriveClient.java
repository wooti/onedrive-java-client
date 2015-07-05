package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.resources.*;
import com.wouterbreukink.onedrive.resources.facets.FileSystemInfoFacet;
import jersey.repackaged.com.google.common.collect.Lists;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
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
    private final WebTarget serviceTarget;
    private final OneDriveAuth authoriser;

    public OneDriveClient(Client client, OneDriveAuth authoriser) {

        log.setLevel(Main.logLevel);

        this.authoriser = authoriser;

        serviceTarget = client.target("https://api.onedrive.com/v1.0");
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

    /*
    public Item uploadFileNoMeta(Item parent, File file) throws FileNotFoundException, JsonProcessingException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        WebTarget uploadTarget = serviceTarget.path("/drive/items/" + parent.getId() + ":/" + file.getName() + ":/content");

        Invocation.Builder putBuilder = uploadTarget.request(MediaType.TEXT_PLAIN_TYPE);

        FileInputStream stream = null;
        stream = new FileInputStream(file);

        Response response = putBuilder.put(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM));

        Item item = response.readEntity(Item.class);

        // Now update the item
        WebTarget updateTarget = serviceTarget.path("/drive/items/" + item.getId());
        Invocation.Builder updateBuilder = updateTarget.request(MediaType.TEXT_PLAIN_TYPE);

        item.getFileSystemInfo().setCreatedDateTime(new Date(file.lastModified()));
        item.getFileSystemInfo().setLastModifiedDateTime(new Date(file.lastModified()));

        Entity<WriteItem> json = Entity.json(item.toWrite());

        response = updateBuilder.method("PATCH", json);

        verifyResponse(response);
        return response.readEntity(Item.class);
    }
    */

    public Item uploadFile(Item parent, File file) throws IOException {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

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
                .path("/drive/items/" + parent.getId() + "/children")
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
        return OneDriveRequest.newRequest(serviceTarget, authoriser);
    }

    public Item updateFile(Item item, Date createdDate, Date modifiedDate) {

        WebTarget updateTarget = serviceTarget.path("/drive/items/" + item.getId());
        Invocation.Builder updateBuilder = updateTarget.request(MediaType.TEXT_PLAIN_TYPE);

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
