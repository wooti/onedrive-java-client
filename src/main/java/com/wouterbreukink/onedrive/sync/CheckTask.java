package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.io.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

public class CheckTask extends Task {

    private static final Logger log = LogManager.getLogger(CheckTask.class.getName());
    private final Item remoteFile;
    private final File localFile;


    public CheckTask(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem, Item remoteFile, File localFile) {
        super(queue, api, fileSystem);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.localFile = Preconditions.checkNotNull(localFile);
    }

    public int priority() {
        return 10;
    }

    @Override
    public String toString() {
        return "Checking file " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {

        if (localFile.isDirectory() && remoteFile.isFolder()) { // If we are syncing folders

            Item[] remoteFiles = remoteFile.getChildren() != null ? remoteFile.getChildren() : api.getChildren(remoteFile);

            // Index the local files
            Map<String, File> localFileCache = Maps.newHashMap();
            //noinspection ConstantConditions
            for (File file : localFile.listFiles()) {
                localFileCache.put(file.getName(), file);
            }

            // Iterate over all the remote files
            for (Item remoteFile : remoteFiles) {
                File localFile = localFileCache.remove(remoteFile.getName());
                processChild(remoteFile, localFile);
            }

            // Iterate over any local files we've not matched yet
            for (File localFile : localFileCache.values()) {
                processChild(null, localFile);
            }
        } else if (localFile.isFile() && !remoteFile.isFolder()) { // If we are syncing files
            if (isSizeInvalid(localFile, remoteFile)) {
                return;
            }

            if (isIgnored(localFile, remoteFile)) {
                return;
            }

            if (!filesMatch(remoteFile, localFile)) {
                switch (getCommandLineOpts().getDirection()) {
                    case UP:
                        queue.add(new UploadTask(queue, api, fileSystem, remoteFile.getParentReference(), localFile, true));
                        break;
                    case DOWN:
                        queue.add(new DownloadTask(queue, api, fileSystem, localFile.getParentFile(), remoteFile, true));
                        break;
                    default:
                        throw new IllegalStateException("Unsupported direction " + getCommandLineOpts().getDirection());
                }
            }
        } else { // // Resolve cases where remote and local disagree over whether the item is a file or folder
            switch (getCommandLineOpts().getDirection()) {
                case UP:
                    new DeleteTask(queue, api, fileSystem, remoteFile).taskBody(); // Execute immediately
                    queue.add(new UploadTask(queue, api, fileSystem, remoteFile.getParentReference(), localFile, true));
                    break;
                case DOWN:
                    new DeleteTask(queue, api, fileSystem, localFile).taskBody(); // Execute immediately
                    queue.add(new DownloadTask(queue, api, fileSystem, localFile.getParentFile(), remoteFile, true));
                    break;
                default:
                    throw new IllegalStateException("Unsupported direction " + getCommandLineOpts().getDirection());
            }
        }
    }

    private boolean filesMatch(Item remoteFile, File localFile) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);

        // Timestamp rounded to the nearest second
        Date localCreatedDate = new Date(attr.creationTime().to(TimeUnit.SECONDS) * 1000);
        Date localModifiedDate = new Date(attr.lastModifiedTime().to(TimeUnit.SECONDS) * 1000);

        boolean sizeMatches = remoteFile.getSize() == localFile.length();
        boolean createdMatches = remoteFile.getFileSystemInfo().getCreatedDateTime().equals(localCreatedDate);
        boolean modifiedMatches = remoteFile.getFileSystemInfo().getLastModifiedDateTime().equals(localModifiedDate);

        if (!getCommandLineOpts().useHash() && sizeMatches && createdMatches && modifiedMatches) {
            // Close enough!
            return true;
        }

        long remoteCrc = remoteFile.getFile().getHashes().getCrc32();
        long localCrc = fileSystem.getChecksum(localFile);
        boolean crcMatches = remoteCrc == localCrc;

        // If the crc matches but the timestamps do not we won't upload the content again
        if (crcMatches && !(modifiedMatches || !createdMatches)) {
            queue.add(new UpdateFileDatesTask(queue, api, fileSystem, remoteFile, localFile));
        }

        return remoteCrc == localCrc;
    }

    private void processChild(Item remoteFile, File localFile) {

        if (remoteFile == null && localFile == null) {
            throw new IllegalArgumentException("Must specify at least one file");
        }

        boolean remoteOnly = localFile == null;
        boolean localOnly = remoteFile == null;

        // Skip if ignored
        if (isIgnored(localFile, remoteFile)) {
            return;
        }

        // Case 1: We only have the file remotely
        if (remoteOnly) {
            switch (getCommandLineOpts().getDirection()) {
                case UP:
                    queue.add(new DeleteTask(queue, api, fileSystem, remoteFile));
                    break;
                case DOWN:
                    queue.add(new DownloadTask(queue, api, fileSystem, this.localFile, remoteFile, false));
                    break;
                default:
                    throw new IllegalStateException("Unsupported direction " + getCommandLineOpts().getDirection());
            }
        }

        // Case 2: We only have the file locally
        else if (localOnly) {
            switch (getCommandLineOpts().getDirection()) {
                case UP:
                    queue.add(new UploadTask(queue, api, fileSystem, this.remoteFile, localFile, false));
                    break;
                case DOWN:
                    queue.add(new DeleteTask(queue, api, fileSystem, localFile));
                    break;
                default:
                    throw new IllegalStateException("Unsupported direction " + getCommandLineOpts().getDirection());
            }
        }

        // Case 3: We have the file in both locations
        else {
            queue.add(new CheckTask(queue, api, fileSystem, remoteFile, localFile));
        }
    }

    private boolean isSizeInvalid(File localFile, Item remoteFile) {
        return false;
    }


    private boolean isIgnored(File localFile, Item remoteFile) {

        // TODO: Implement and Config this out
        String filename = "";

        HashSet<String> ignored = Sets.newHashSet("Thumbs.db", ".picasa.ini", "._.DS_Store", ".DS_Store");
        return ignored.contains(filename);

    }
}
