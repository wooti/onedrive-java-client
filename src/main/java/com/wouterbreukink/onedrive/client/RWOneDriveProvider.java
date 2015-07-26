package com.wouterbreukink.onedrive.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.facets.FileFacet;
import com.wouterbreukink.onedrive.client.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.facets.FolderFacet;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.UploadSession;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

class RWOneDriveProvider extends ROOneDriveProvider implements OneDriveProvider {

    public RWOneDriveProvider(Client client, AuthorisationProvider authoriser) {
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

        OneDriveItem item = OneDriveItem.FACTORY.create(request.getResponse(Item.class));

        // Now update the item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
    }

    public OneDriveItem uploadFile(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        // Generate the update item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        FileSystemInfoFacet fsi = new FileSystemInfoFacet();
        fsi.setLastModifiedDateTime(new Date(attr.lastModifiedTime().toMillis()));
        fsi.setCreatedDateTime(new Date(attr.creationTime().toMillis()));
        WriteableItem itemToWrite = new WriteableItem(file.getName(), fsi, true);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + "/children")
                .payloadJson(itemToWrite)
                .payloadBinary(Files.readAllBytes(file.toPath()))
                .method("POST");

        return OneDriveItem.FACTORY.create(request.getResponse(Item.class));
    }

    @Override
    public OneDriveUploadSession startUploadSession(OneDriveItem parent, File file) throws OneDriveAPIException, IOException {
        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + ":/" + file.getName() + ":/upload.createSession")
                .payloadJson(new MultiPartItem(file.getName()))
                .header("Content-Length", file.length())
                .method("POST");

        UploadSession session = request.getResponse(UploadSession.class);

        return new OneDriveUploadSession(parent, file, session.getUploadUrl(), session.getNextExpectedRanges());
    }

    @Override
    public void uploadChunk(OneDriveUploadSession session) throws OneDriveAPIException, IOException {

        byte[] bytesToUpload = session.getChunk();
        long length = session.getFile().length();
        OneDriveItem item;

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
            item = OneDriveItem.FACTORY.create(uploadPart.getResponse(Item.class));
        }

        // If this is the final chunk then set the properties
        BasicFileAttributes attr = Files.readAttributes(session.getFile().toPath(), BasicFileAttributes.class);
        item = updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));

        // Upload session is now complete
        session.setComplete(item);
    }

    public OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws OneDriveAPIException {

        WriteableItem updateItem = new WriteableItem(item.getName(), new FileSystemInfoFacet(), false);

        updateItem.getFileSystemInfo().setCreatedDateTime(createdDate);
        updateItem.getFileSystemInfo().setLastModifiedDateTime(modifiedDate);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + item.getId())
                .payloadJson(updateItem)
                .method("PATCH");

        return OneDriveItem.FACTORY.create(request.getResponse(Item.class));
    }

    public OneDriveItem createFolder(OneDriveItem parent, String name) throws OneDriveAPIException {

        WriteableFolder newFolder = new WriteableFolder(name);

        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + parent.getId() + "/children")
                .payloadJson(newFolder)
                .method("POST");

        return OneDriveItem.FACTORY.create(request.getResponse(Item.class));
    }

    public void download(OneDriveItem item, File target) throws OneDriveAPIException {
        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + item.getId() + "/content")
                .method("GET");

        request.getFile(target);
    }

    public void delete(OneDriveItem remoteFile) throws OneDriveAPIException {
        OneDriveRequest request = getDefaultRequest()
                .path("/drive/items/" + remoteFile.getId())
                .method("DELETE");

        request.getResponse(null);
    }

    private class WriteableFolder {
        private String name;
        private FolderFacet folder;

        public WriteableFolder(String name) {
            this.name = name;
            this.folder = new FolderFacet();
        }

        public String getName() {
            return name;
        }

        public FolderFacet getFolder() {
            return folder;
        }
    }

    private class WriteableItem {
        private final FileSystemInfoFacet fileSystemInfo;
        private String name;
        private FileFacet file = new FileFacet();
        private boolean multipart;

        public WriteableItem(String name, FileSystemInfoFacet fileSystemInfo, boolean multipart) {
            this.name = name;
            this.fileSystemInfo = fileSystemInfo;
            this.multipart = multipart;
        }

        public FileSystemInfoFacet getFileSystemInfo() {
            return fileSystemInfo;
        }

        public String getName() {
            return name;
        }

        public FileFacet getFile() {
            return file;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("@content.sourceUrl")
        public String getSourceUrl() {
            return multipart ? "cid:content" : null;
        }

    }

    public class MultiPartItem {

        private FileDetail item;
        private String name;

        private MultiPartItem(String name) {
            this.name = name;
            this.item = new FileDetail();
        }

        public FileDetail getItem() {
            return item;
        }

        public class FileDetail {

            private String conflictBehavior = "replace";

            public String getName() {
                return name;
            }

            @JsonProperty("@name.conflictBehavior")
            public String getConflictBehavior() {
                return conflictBehavior;
            }
        }
    }
}
