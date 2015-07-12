package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;

public class CheckFolderTask extends Task {

    private static final Logger log = LogManager.getLogger(CheckFolderTask.class.getName());
    private final OneDriveAPI client;
    private final Item remoteFolder;
    private final File localFolder;

    public CheckFolderTask(TaskQueue queue, OneDriveAPI client, Item remoteFolder, File localFolder) {

        super(queue);

        this.client = Preconditions.checkNotNull(client);
        this.remoteFolder = Preconditions.checkNotNull(remoteFolder);
        this.localFolder = Preconditions.checkNotNull(localFolder);

        if (!remoteFolder.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }

        if (!localFolder.isDirectory()) {
            throw new IllegalArgumentException("Specified localFolder is not a folder");
        }
    }

    public int priority() {
        return 10;
    }

    @Override
    public String toString() {
        return "Check folder " + remoteFolder.getFullName();
    }

    @Override
    protected void taskBody() throws OneDriveAPIException {

        // Fetch the remote files
        log.info("Syncing folder " + remoteFolder.getFullName());
        Item[] remoteFiles = client.getChildren(remoteFolder);

        // Index the local files
        Map<String, File> localFiles = Maps.newHashMap();
        for (File file : localFolder.listFiles()) {
            localFiles.put(file.getName(), file);
        }

        for (Item remoteFile : remoteFiles) {

            File localFile = localFiles.get(remoteFile.getName());

            if (localFile != null) {

                if (remoteFile.isFolder() != localFile.isDirectory()) {
                    log.warn(String.format(
                            "Conflict detected in item '%s'. Local is %s, Remote is %s",
                            remoteFile.getFullName(),
                            remoteFile.isFolder() ? "directory" : "file",
                            localFile.isDirectory() ? "directory" : "file"));

                    continue;
                }

                if (remoteFile.isFolder()) {
                    queue.add(new CheckFolderTask(queue, client, remoteFile, localFile));
                } else {
                    queue.add(new CheckFileTask(queue, client, remoteFile, localFile));
                }

                localFiles.remove(remoteFile.getName());
            } else {
                log.info("TODO Item is extra - Would delete item?: " + remoteFile.getFullName());
            }
        }

        // Anything left does not exist on OneDrive

        // Filter stuff we don't want to upload
        // TODO config this out
        localFiles.remove("Thumbs.db");
        localFiles.remove(".picasa.ini");
        localFiles.remove("._.DS_Store");
        localFiles.remove(".DS_Store");

        for (File localFile : localFiles.values()) {
            if (localFile.isDirectory()) {
                Item createdItem = client.createFolder(remoteFolder, localFile.getName());
                log.info("Created new folder " + createdItem.getFullName());
                queue.add(new CheckFolderTask(queue, client, createdItem, localFile));
            } else {

                // TODO: Skip big files (for now)
                if (localFile.length() > 10 * 1024 * 1024) {
                    log.warn("TODO Skipping big file");
                    return;
                }

                queue.add(new UploadFileTask(queue, client, remoteFolder, localFile, false));
            }
        }

    }
}
