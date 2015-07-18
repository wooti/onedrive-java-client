package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.TaskQueue;
import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.fs.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

            // Skip if the file size is too big
            switch (getCommandLineOpts().getDirection()) {
                case UP:
                    if (isSizeInvalid(localFile) || isIgnored(localFile)) {
                        return;
                    }
                    break;
                case DOWN:
                    if (isSizeInvalid(remoteFile) || isIgnored(remoteFile)) {
                        return;
                    }
                    break;

            }

            // Check if the remote file matches the local file
            FileSystemProvider.FileMatch match = fileSystem.verifyMatch(
                    localFile, remoteFile.getFile().getHashes().getCrc32(),
                    remoteFile.getSize(),
                    remoteFile.getFileSystemInfo().getCreatedDateTime(),
                    remoteFile.getFileSystemInfo().getLastModifiedDateTime());

            switch (match) {
                case NO:
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
                    break;
                case CRC:
                    queue.add(new UpdatePropertiesTask(queue, api, fileSystem, remoteFile, localFile));
                    break;
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

    private void processChild(Item remoteFile, File localFile) {

        if (remoteFile == null && localFile == null) {
            throw new IllegalArgumentException("Must specify at least one file");
        }

        boolean remoteOnly = localFile == null;
        boolean localOnly = remoteFile == null;

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
}
