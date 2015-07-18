package com.wouterbreukink.onedrive.client;

import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.UploadSession;
import com.wouterbreukink.onedrive.client.resources.WriteFolder;
import com.wouterbreukink.onedrive.client.resources.WriteItem;
import com.wouterbreukink.onedrive.client.resources.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.resources.facets.MultiPartItem;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
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
                .payloadBinary(Files.readAllBytes(file.toPath()))
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
                .payloadBinary(Files.readAllBytes(file.toPath()))
                .method("POST");

        return request.getResponse(Item.class);
    }

    @Override
    public OneDriveItem uploadFileInChunks(OneDriveItem parent, File file, int chunkSize) throws OneDriveAPIException, IOException {

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + ":/" + file.getName() + ":/upload.createSession")
                .payloadJson(MultiPartItem.create(file.getName()))
                .header("Content-Length", file.length())
                .method("POST");

        UploadSession session = request.getResponse(UploadSession.class);

        int uploaded = 0;
        String uploadUrl = session.getUploadUrl();
        byte[] chunk = new byte[chunkSize];
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        Item item = null;

        while (session.getNextExpectedRanges() != null && session.getNextExpectedRanges().length > 0) {

            // Send the next chunk the server has asked for
            String nextRange = session.getNextExpectedRanges()[0];
            long nextChunk = Long.parseLong(nextRange.substring(0, nextRange.indexOf('-')));

            raf.seek(nextChunk);
            int read = raf.read(chunk);

            if (read < chunkSize) {
                chunk = Arrays.copyOf(chunk, read);
            }

            OneDriveRequest uploadPart = getDefaultRequest()
                    .target(uploadUrl)
                    .payloadBinary(chunk)
                    .header("Content-Range", String.format("bytes %d-%d/%d", uploaded, uploaded + chunk.length - 1, file.length()))
                    .method("PUT");

            if (read == chunkSize && uploaded + read < file.length()) {
                session = uploadPart.getResponse(UploadSession.class);
            } else {
                item = uploadPart.getResponse(Item.class);
                break;
            }

            uploaded += read;
        }

        // Now update the item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
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
