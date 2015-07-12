package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.Utils;
import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckFileTask extends Task {

    private static final Logger log = Logger.getLogger(CheckFileTask.class.getName());

    private final OneDriveAPI client;
    private final Item remoteFile;
    private final File localFile;

    public CheckFileTask(TaskQueue queue, OneDriveAPI client, Item remoteFile, File localFile) {

        super(queue);

        this.client = Preconditions.checkNotNull(client);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.localFile = Preconditions.checkNotNull(localFile);
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Check file " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() {

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
            if (localFile.length() > 10 * 1024 * 1024) {
                log.warning("TODO Skipping big file");
                return;
            }

            long remoteCrc = remoteFile.getFile().getHashes().getCrc32();
            long localCrc = Utils.getChecksum(localFile);

            // If the content is different
            if (remoteCrc != localCrc) {
                queue.add(new UploadFileTask(queue, client, remoteFile.getParentReference(), localFile, true));
            } else if (!createdMatches || !modifiedMatches) {
                queue.add(new UpdateFileTask(queue, client, remoteFile, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis())));
            }

        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to compare file", e);
        }
    }
}
