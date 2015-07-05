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
import javax.ws.rs.core.Response;
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

        serviceTarget = client.target("https://api.onedrive.com/v1.0")
                .queryParam("access_token", authoriser.getAuthorisation().getAccessToken());
    }

    private <T> T getResponse(Invocation invoke, Class<T> entityType) {

        for (int retries = 10; retries > 0; retries--) {
            Response response = invoke.invoke();

            if (response.getStatus() == 401) {
                serviceTarget.queryParam("access_token", authoriser.getAuthorisation().getAccessToken());
                continue;
            }

            if (response.getStatus() == 503) {
                try {
                    log.warning("Server returned 503 - sleeping 10 seconds");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    log.warning(e.toString());
                }
                continue;
            }

            if (response.getStatus() == 509) {
                try {
                    log.warning("Server returned 509 - sleeping 60 seconds");
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    log.warning(e.toString());
                }
                continue;
            }

            verifyResponse(response);
            return response.readEntity(entityType);
        }

        throw new Error("Unable to complete request");
    }

    public Drive getDefaultDrive() {

        Invocation invocation =
                serviceTarget.path("drive")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .buildGet();

        return getResponse(invocation, Drive.class);
    }

    public Item getRoot() {
        Invocation invocation =
                serviceTarget.path("drive/root")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .buildGet();

        return getResponse(invocation, Item.class);
    }

    public Item[] getChildren(Item parent) {

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        List<Item> itemsToReturn = Lists.newArrayList();

        Invocation invocation =
                serviceTarget.path("/drive/items/" + parent.getId() + "/children")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .buildGet();

        ItemSet items = getResponse(invocation, ItemSet.class);

        Collections.addAll(itemsToReturn, items.getValue());

        while (items.getNextPage() != null) {

            log.finer(String.format("Got %d items, fetching next page", itemsToReturn.size()));

            invocation =
                    serviceTarget.path("/drive/items/" + parent.getId() + "/children")
                            .queryParam("$skiptoken", items.getNextToken())
                            .request(MediaType.TEXT_PLAIN_TYPE)
                            .buildGet();

            items = getResponse(invocation, ItemSet.class);

            Collections.addAll(itemsToReturn, items.getValue());
        }

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

        Invocation invocation =
                serviceTarget.path("/drive/items/" + parent.getId() + "/children")
                        .request()
                        .buildPost(Entity.entity(multiPart, multiPart.getMediaType()));


        Item response = getResponse(invocation, Item.class);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.fine(String.format("Upload complete - %d KB in %dms - %.2f KB/s",
                file.length() / 1024,
                elapsedTime,
                elapsedTime > 0 ? ((file.length() / 1024d) / (elapsedTime / 1000d)) : 0));

        return response;
    }

    private void verifyResponse(Response response) {

        int status = response.getStatus();
        if (status != 200 && status != 201) {
            ErrorFacet error = response.readEntity(ErrorSet.class).getError();
            throw new Error(String.format("Error Code %d: %s (%s)", response.getStatus(), error.getCode(), error.getMessage()));
        }
    }

    public Item getPath(String path) {
        Invocation invocation =
                serviceTarget.path("drive/root:/" + path)
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .buildGet();

        return getResponse(invocation, Item.class);
    }

    public Item updateFile(Item item, Date createdDate, Date modifiedDate) {

        WebTarget updateTarget = serviceTarget.path("/drive/items/" + item.getId());
        Invocation.Builder updateBuilder = updateTarget.request(MediaType.TEXT_PLAIN_TYPE);

        WriteItem updateItem = new WriteItem(item.getName(), new FileSystemInfoFacet(), false);

        updateItem.getFileSystemInfo().setCreatedDateTime(createdDate);
        updateItem.getFileSystemInfo().setLastModifiedDateTime(modifiedDate);

        Invocation invocation =
                serviceTarget.path("/drive/items/" + item.getId())
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .build("PATCH", Entity.json(updateItem));

        return getResponse(invocation, Item.class);
    }

    public Item createFolder(Item parent, String name) {

        WriteFolder newFolder = new WriteFolder(name);

        Invocation invocation =
                serviceTarget.path("/drive/items/" + parent.getId() + "/children")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .buildPost(Entity.json(newFolder));

        return getResponse(invocation, Item.class);
    }
}
