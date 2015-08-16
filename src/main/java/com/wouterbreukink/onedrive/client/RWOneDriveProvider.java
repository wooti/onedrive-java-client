package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.*;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.util.Key;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.facets.FileFacet;
import com.wouterbreukink.onedrive.client.facets.FileSystemInfoFacet;
import com.wouterbreukink.onedrive.client.facets.FolderFacet;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.UploadSession;
import com.wouterbreukink.onedrive.client.serialization.JsonDateSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

class RWOneDriveProvider extends ROOneDriveProvider implements OneDriveProvider {

    public RWOneDriveProvider(AuthorisationProvider authoriser) {
        super(authoriser);
    }

    @Override
    public OneDriveItem replaceFile(OneDriveItem parent, File file, String remoteFilename) throws IOException {

        FileContent fileContent = new FileContent(null, file);
        FileSystemInfoFacet fsi = new FileSystemInfoFacet(file); 
        return replaceEncryptedFile(parent, fileContent, fsi, remoteFilename);
    }
    
    @Override
    public OneDriveItem replaceEncryptedFile(OneDriveItem parent, HttpContent httpContent, FileSystemInfoFacet fsi, String remoteFilename) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        HttpRequest request = requestFactory.buildPutRequest(
                OneDriveUrl.putContent(parent.getId(), remoteFilename),
                httpContent);

        Item response = request.execute().parseAs(Item.class);
        OneDriveItem item = OneDriveItem.FACTORY.create(response);

        // Now update the item
        return updateFile(item, fsi);
    }

    @Override
    public OneDriveItem uploadFile(OneDriveItem parent, File file, String remoteFilename) throws IOException {

    	FileContent fileContent = new FileContent(null, file);
        FileSystemInfoFacet fsi = new FileSystemInfoFacet(file);
    	return uploadEncryptedFile(parent, fileContent, fsi, remoteFilename);    	
    }
    
    @Override
    public OneDriveItem uploadEncryptedFile(OneDriveItem parent, HttpContent httpContent, FileSystemInfoFacet fsi, String remoteFilename) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        // Generate the update item        
        WriteItemFacet itemToWrite = new WriteItemFacet(remoteFilename, fsi, true);

        MultipartContent content = new MultipartContent()
                .addPart(new MultipartContent.Part(
                        new HttpHeaders()
                                .set("Content-ID", "<metadata>")
                                .setAcceptEncoding(null),
                        new JsonHttpContent(JSON_FACTORY, itemToWrite)))
                .addPart(new MultipartContent.Part(
                        new HttpHeaders()
                                .set("Content-ID", "<content>")
                                .setAcceptEncoding(null),
                        httpContent));

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.postMultiPart(parent.getId()), content);

        request.setLoggingEnabled(true);

        return OneDriveItem.FACTORY.create(request.execute().parseAs(Item.class));
    }

    @Override
    public OneDriveUploadSessionInterface startUploadSession(OneDriveItem parent, File file, String remoteFilename) throws IOException {

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.createUploadSession(parent.getId(), remoteFilename),
                new JsonHttpContent(JSON_FACTORY, new UploadSessionFacet(remoteFilename)));

        UploadSession session = request.execute().parseAs(UploadSession.class);

        return new OneDriveUploadSession(parent, file, remoteFilename, session.getUploadUrl(), session.getNextExpectedRanges());
    }
    
    @Override
    public OneDriveUploadSessionInterface startEncryptedUploadSession(OneDriveItem parent, File file, String remoteFilename) throws IOException {

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.createUploadSession(parent.getId(), remoteFilename),
                new JsonHttpContent(JSON_FACTORY, new UploadSessionFacet(remoteFilename)));

        UploadSession session = request.execute().parseAs(UploadSession.class);

        return new OneDriveEncryptedUploadSession(parent, file, remoteFilename, session.getUploadUrl(), session.getNextExpectedRanges());
    }

    @Override
    public void uploadChunk(OneDriveUploadSessionInterface session) throws IOException {

        byte[] bytesToUpload = session.getChunk();
        OneDriveItem item;

        HttpRequest request = requestFactory.buildPutRequest(
                new GenericUrl(session.getUploadUrl()),
                new ByteArrayContent(null, bytesToUpload));

        request.getHeaders().setContentRange(String.format("bytes %d-%d/%d", session.getTotalUploaded(), session.getTotalUploaded() + bytesToUpload.length - 1, session.getRemoteFileLength()));

        if (session.getTotalUploaded() + bytesToUpload.length < session.getFile().length()) {
            UploadSession response = request.execute().parseAs(UploadSession.class);
            session.setRanges(response.getNextExpectedRanges());
            return;
        } else {
            item = OneDriveItem.FACTORY.create(request.execute().parseAs(Item.class));
        }

        // If this is the final chunk then set the properties
        BasicFileAttributes attr = Files.readAttributes(session.getFile().toPath(), BasicFileAttributes.class);
        item = updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));

        // Upload session is now complete
        session.setComplete(item);
    }

    @Override
    public OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws IOException {

        FileSystemInfoFacet fileSystem = new FileSystemInfoFacet(createdDate, modifiedDate);        

        return updateFile(item, fileSystem);
    }
    
    @Override
    public OneDriveItem updateFile(OneDriveItem item, FileSystemInfoFacet fsi) throws IOException {

        WriteItemFacet updateItem = new WriteItemFacet(item.getName(), fsi, false);

        HttpRequest request = requestFactory.buildPatchRequest(
                OneDriveUrl.item(item.getId()),
                new JsonHttpContent(JSON_FACTORY, updateItem));

        Item response = request.execute().parseAs(Item.class);
        return OneDriveItem.FACTORY.create(response);
    }

    @Override
    public OneDriveItem createFolder(OneDriveItem parent, String name) throws IOException {

        WriteFolderFacet newFolder = new WriteFolderFacet(name);

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.children(parent.getId()),
                new JsonHttpContent(JSON_FACTORY, newFolder));

        Item response = request.execute().parseAs(Item.class);
        return OneDriveItem.FACTORY.create(response);
    }

    @Override
    public void download(OneDriveItem item, File target) throws IOException {

        HttpRequest request = requestFactory.buildGetRequest(OneDriveUrl.content(item.getId()));

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(target);
            request.execute().download(fos);
        } catch (IOException e) {
            throw new OneDriveAPIException(0, "Unable to download file", e);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }
    
    @Override
    public void delete(OneDriveItem remoteFile) throws IOException {
        HttpRequest request = requestFactory.buildDeleteRequest(OneDriveUrl.item(remoteFile.getId()));
        request.execute();
    }

    static class WriteFolderFacet {
        @Key
        private String name;
        @Key
        private FolderFacet folder;

        public WriteFolderFacet(String name) {
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

    static class WriteItemFacet {
        @Key
        private final FileSystemInfoFacet fileSystemInfo;
        @Key
        private String name;
        @Key
        private FileFacet file = new FileFacet();
        @Key("@content.sourceUrl")
        private String multipart;

        public WriteItemFacet(String name, FileSystemInfoFacet fileSystemInfo, boolean multipart) {
            this.name = name;
            this.fileSystemInfo = fileSystemInfo;
            this.multipart = multipart ? "cid:content" : null;
        }
    }

    static class UploadSessionFacet {

        @Key
        private FileDetail item;

        private UploadSessionFacet(String name) {
            this.item = new FileDetail(name);
        }

        public FileDetail getItem() {
            return item;
        }

        public class FileDetail {

            @Key
            private String name;

            @Key("@name.conflictBehavior")
            private String conflictBehavior = "replace";

            public FileDetail(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }
    }
}
