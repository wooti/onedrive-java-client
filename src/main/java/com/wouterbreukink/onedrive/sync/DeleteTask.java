package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.fs.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class DeleteTask extends Task {

    private static final Logger log = LogManager.getLogger(DeleteTask.class.getName());
    private final Item remoteFile;
    private final File localFile;

    public DeleteTask(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem, Item remoteFile) {

        super(queue, api, fileSystem);

        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.localFile = null;
    }

    public DeleteTask(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem, File localFile) {

        super(queue, api, fileSystem);

        this.localFile = Preconditions.checkNotNull(localFile);
        this.remoteFile = null;
    }

    public int priority() {
        return 25;
    }

    @Override
    public String toString() {
        if (localFile != null) {
            return "Delete local file " + localFile.getName();
        } else {
            return "Delete remote file " + remoteFile.getFullName();
        }
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {
        if (localFile != null) {
            fileSystem.delete(localFile);
        } else {
            api.delete(remoteFile);
        }
    }
}
