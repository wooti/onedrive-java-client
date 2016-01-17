package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.*;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.util.Key;
import com.wouterbreukink.onedrive.client.authoriser.AuthorisationProvider;
import com.wouterbreukink.onedrive.client.downloader.ResumableDownloader;
import com.wouterbreukink.onedrive.client.downloader.ResumableDownloaderProgressListener;
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

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

class RWOneDriveProvider extends ROOneDriveProvider implements OneDriveProvider {

    public RWOneDriveProvider(AuthorisationProvider authoriser) {
        super(authoriser);
    }

    public OneDriveItem replaceFile(OneDriveItem parent, File file) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        HttpRequest request = requestFactory.buildPutRequest(
                OneDriveUrl.putContent(parent.getId(), file.getName()),
                new FileContent(null, file));

        Item response = request.execute().parseAs(Item.class);
        OneDriveItem item = OneDriveItem.FACTORY.create(response);

        // Now update the item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
    }

    public OneDriveItem uploadFile(OneDriveItem parent, File file) throws IOException {

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent is not a folder");
        }

        // Generate the update item
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        FileSystemInfoFacet fsi = new FileSystemInfoFacet();
        fsi.setLastModifiedDateTime(JsonDateSerializer.INSTANCE.serialize(new Date(attr.lastModifiedTime().toMillis())));
        fsi.setCreatedDateTime(JsonDateSerializer.INSTANCE.serialize(new Date(attr.creationTime().toMillis())));
        WriteItemFacet itemToWrite = new WriteItemFacet(file.getName(), fsi, true, false);

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
                        new FileContent(null, file)));

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.postMultiPart(parent.getId()), content);

        request.setLoggingEnabled(true);

        return OneDriveItem.FACTORY.create(request.execute().parseAs(Item.class));
    }

    @Override
    public OneDriveUploadSession startUploadSession(OneDriveItem parent, File file) throws IOException {

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.createUploadSession(parent.getId(), file.getName()),
                new JsonHttpContent(JSON_FACTORY, new UploadSessionFacet(file.getName())));

        UploadSession session = request.execute().parseAs(UploadSession.class);

        return new OneDriveUploadSession(parent, file, session.getUploadUrl(), session.getNextExpectedRanges());
    }

    @Override
    public void uploadChunk(OneDriveUploadSession session) throws IOException {

        byte[] bytesToUpload = session.getChunk();
        OneDriveItem item;

        HttpRequest request = requestFactory.buildPutRequest(
                new GenericUrl(session.getUploadUrl()),
                new ByteArrayContent(null, bytesToUpload));

        request.getHeaders().setContentRange(String.format("bytes %d-%d/%d", session.getTotalUploaded(), session.getTotalUploaded() + bytesToUpload.length - 1, session.getFile().length()));

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

    public OneDriveItem updateFile(OneDriveItem item, Date createdDate, Date modifiedDate) throws IOException {

        FileSystemInfoFacet fileSystem = new FileSystemInfoFacet();
        fileSystem.setCreatedDateTime(JsonDateSerializer.INSTANCE.serialize(createdDate));
        fileSystem.setLastModifiedDateTime(JsonDateSerializer.INSTANCE.serialize(modifiedDate));

        WriteItemFacet updateItem = new WriteItemFacet(item.getName(), fileSystem, false, item.isDirectory());

        HttpRequest request = requestFactory.buildPatchRequest(
                OneDriveUrl.item(item.getId()),
                new JsonHttpContent(JSON_FACTORY, updateItem));

        Item response = request.execute().parseAs(Item.class);
        return OneDriveItem.FACTORY.create(response);
    }

    public OneDriveItem createFolder(OneDriveItem parent, File target) throws IOException {

        WriteFolderFacet newFolder = new WriteFolderFacet(target.getName());

        HttpRequest request = requestFactory.buildPostRequest(
                OneDriveUrl.children(parent.getId()),
                new JsonHttpContent(JSON_FACTORY, newFolder));

        Item response = request.execute().parseAs(Item.class);
        OneDriveItem item = OneDriveItem.FACTORY.create(response);

        // Set the remote timestamps
        BasicFileAttributes attr = Files.readAttributes(target.toPath(), BasicFileAttributes.class);
        item = updateFile(item, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));

        return item;
    }

    public void download(OneDriveItem item, File target, ResumableDownloaderProgressListener progressListener) throws IOException {

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(target);
            ResumableDownloader downloader = new ResumableDownloader(HTTP_TRANSPORT, requestFactory.getInitializer());
            downloader.setProgressListener(progressListener);
            downloader.setChunkSize(getCommandLineOpts().getSplitAfter() * 1024 * 1024);

            downloader.download(OneDriveUrl.content(item.getId()), fos);
        } catch (IOException e) {
            throw new OneDriveAPIException(0, "Unable to download file", e);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

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
        private FolderFacet folder;
        @Key
        private FileFacet file;
        @Key("@content.sourceUrl")
        private String multipart;

        public WriteItemFacet(String name, FileSystemInfoFacet fileSystemInfo, boolean multipart, boolean isDirectory) {
            this.name = name;
            this.fileSystemInfo = fileSystemInfo;
            this.multipart = multipart ? "cid:content" : null;

            if (isDirectory) {
                this.folder = new FolderFacet();
            } else {
                this.file = new FileFacet();
            }
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
