package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.io.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

public class UpdatePropertiesTask extends Task {

    private static final Logger log = LogManager.getLogger(UpdatePropertiesTask.class.getName());
    private final Item remoteFile;
    private final File localFile;

    public UpdatePropertiesTask(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem, Item remoteFile, File localFile) {

        super(queue, api, fileSystem);

        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.localFile = Preconditions.checkNotNull(localFile);
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Update properties for " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {

        switch (getCommandLineOpts().getDirection()) {
            case UP:
                BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);
                // Timestamp rounded to the nearest second
                Date localCreatedDate = new Date(attr.creationTime().to(TimeUnit.SECONDS) * 1000);
                Date localModifiedDate = new Date(attr.lastModifiedTime().to(TimeUnit.SECONDS) * 1000);
                api.updateFile(remoteFile, localCreatedDate, localModifiedDate);

            case DOWN:
                fileSystem.setAttributes(localFile, remoteFile.getFileSystemInfo().getCreatedDateTime(), remoteFile.getFileSystemInfo().getLastModifiedDateTime());
                break;
            default:
                throw new IllegalStateException("Unsupported direction " + getCommandLineOpts().getDirection());
        }
    }
}

