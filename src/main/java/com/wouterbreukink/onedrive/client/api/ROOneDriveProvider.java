package com.wouterbreukink.onedrive.client.api;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.OneDriveRequest;
import com.wouterbreukink.onedrive.client.OneDriveUploadSession;
import com.wouterbreukink.onedrive.client.resources.Drive;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.ItemSet;
import jersey.repackaged.com.google.common.collect.Lists;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

class ROOneDriveProvider implements OneDriveProvider {

    protected final Client client;
    protected final OneDriveAuth authoriser;

    public ROOneDriveProvider(Client client, OneDriveAuth authoriser) {
        this.authoriser = authoriser;
        this.client = client;
    }

    public Drive getDefaultDrive() throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive")
                .method("GET");

        return request.getResponse(Drive.class);
    }

    public OneDriveItem getRoot() throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root")
                .method("GET");

        return OneDriveItem.FACTORY.create(request.getResponse(Item.class));
    }

    public OneDriveItem[] getChildren(OneDriveItem parent) throws OneDriveAPIException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Specified Item is not a folder");
        }

        List<OneDriveItem> itemsToReturn = Lists.newArrayList();

        String token = null;

        do {
            OneDriveRequest request = getDefaultRequest()
                    .path("/drive/items/" + parent.getId() + "/children")
                    .skipToken(token)
                    .method("GET");

            ItemSet items = request.getResponse(ItemSet.class);

            for (Item i : items.getValue()) {
                itemsToReturn.add(OneDriveItem.FACTORY.create(i));
            }

            token = items.getNextToken();

        } while (token != null); // If we have a token for the next page we need to keep going

        return itemsToReturn.toArray(new OneDriveItem[itemsToReturn.size()]);
    }

    public OneDriveItem getPath(String path) throws OneDriveAPIException {

        OneDriveRequest request = getDefaultRequest()
                .path("drive/root:/" + path)
                .withChildren()
                .method("GET");

        return OneDriveItem.FACTORY.create(request.getResponse(Item.class));
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, file.getName(), file.isDirectory());
    }

    public OneDriveItem uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        return OneDriveItem.FACTORY.create(parent, file.getName(), file.isDirectory());
    }

    @Override
    public OneDriveUploadSession startUploadSession(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {
        return new OneDriveUploadSession(parent, file, null, new String[0]);
    }

    @Override
    public void uploadChunk(OneDriveUploadSession session) throws OneDriveAPIException, IOException {
        session.setComplete(OneDriveItem.FACTORY.create(session.getParent(), session.getFile().getName(), session.getFile().isDirectory()));
    }

    public OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws OneDriveAPIException {
        // Do nothing, just return the unmodified item
        return item;
    }

    public OneDriveItem createFolder(OneDriveItem parent, String name) throws OneDriveAPIException {
        // Return a dummy folder
        return OneDriveItem.FACTORY.create(parent, name, true);
    }

    public void download(OneDriveItem item, File target) throws OneDriveAPIException {
        // Do nothing
    }

    public void delete(OneDriveItem remoteFile) throws OneDriveAPIException {
        // Do nothing
    }

    protected OneDriveRequest getDefaultRequest() {
        return new OneDriveRequest(client, authoriser, "https://api.onedrive.com/v1.0");
    }
}
