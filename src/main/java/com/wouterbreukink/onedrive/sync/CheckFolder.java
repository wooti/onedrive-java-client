package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.Main;
import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Maps;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Copyright Wouter Breukink 2015
 */
public class CheckFolder implements Task {

    private static final Logger log = Logger.getLogger(CheckFolder.class.getName());

    private final OneDriveClient client;
    private final Item remoteFolder;
    private final File localFolder;

    public CheckFolder(OneDriveClient client, Item remoteFolder, File localFolder) {

        Preconditions.checkNotNull(client);
        Preconditions.checkNotNull(remoteFolder);
        Preconditions.checkNotNull(localFolder);

        if (!remoteFolder.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }

        if (!localFolder.isDirectory()) {
            throw new IllegalArgumentException("Specified localFolder is not a folder");
        }

        this.client = client;
        this.remoteFolder = remoteFolder;
        this.localFolder = localFolder;
    }

    public int priority() {
        return 10;
    }

    public int compareTo(Task o) {
        return o.priority() - priority();
    }

    public void run() {

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
                    log.warning(String.format(
                            "Conflict detected in item '%s'. Local is %s, Remote is %s",
                            remoteFile.getFullName(),
                            remoteFile.isFolder() ? "directory" : "file",
                            localFile.isDirectory() ? "directory" : "file"));

                    continue;
                }

                if (remoteFile.isFolder()) {
                    Main.queue.add(new CheckFolder(client, remoteFile, localFile));
                } else {
                    Main.queue.add(new CheckFile(client, remoteFile, localFile));
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
                Main.queue.add(new CheckFolder(client, createdItem, localFile));
            } else {
                Main.queue.add(new UploadFile(client, remoteFolder, localFile, false));
            }
        }

    }
}
