package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.Main;
import com.wouterbreukink.onedrive.Utils;
import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright Wouter Breukink 2015
 */
public class CheckFile implements Task {

    private static final Logger log = Logger.getLogger(CheckFile.class.getName());

    private final OneDriveClient client;
    private final Item remoteFile;
    private final File localFile;

    public CheckFile(OneDriveClient client, Item remoteFile, File localFile) {

        Preconditions.checkNotNull(client);
        Preconditions.checkNotNull(remoteFile);
        Preconditions.checkNotNull(localFile);

        this.client = client;
        this.remoteFile = remoteFile;
        this.localFile = localFile;
    }

    public int priority() {
        return 50;
    }

    public int compareTo(Task o) {
        return o.priority() - priority();
    }

    public void run() {

        try {
            BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);

            boolean sizeMatches = remoteFile.getSize() == localFile.length();
            boolean createdMatches = remoteFile.getFileSystemInfo().getCreatedDateTime().getTime() == attr.creationTime().toMillis();
            boolean modifiedMatches = remoteFile.getFileSystemInfo().getLastModifiedDateTime().getTime() == attr.lastModifiedTime().toMillis();
            if (sizeMatches && createdMatches && modifiedMatches) {
                // Close enough!
                return;
            }

            // TODO: Skip big files (for now)
            if (localFile.length() > 10 * 1024 * 2014) {
                log.warning("TODO Skipping big file");
                return;
            }

            long remoteCrc = remoteFile.getFile().getHashes().getCrc32();
            long localCrc = Utils.getChecksum(localFile);

            // If the content is different
            if (remoteCrc != localCrc) {
                Main.queue.add(new UploadFile(client, remoteFile.getParentReference(), localFile, true));
            } else if (!createdMatches || !modifiedMatches) {
                Main.queue.add(new UpdateFile(client, remoteFile, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis())));
            }

        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Unable to compare file", e);
        }
    }
}
