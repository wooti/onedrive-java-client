package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class DeleteTask extends Task {

    private static final Logger log = LogManager.getLogger(DeleteTask.class.getName());
    private final Item remoteFile;
    private final File localFile;

    public DeleteTask(TaskOptions options, Item remoteFile) {

        super(options);

        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.localFile = null;
    }

    public DeleteTask(TaskOptions options, File localFile) {

        super(options);

        this.localFile = Preconditions.checkNotNull(localFile);
        this.remoteFile = null;
    }

    public int priority() {
        return 25;
    }

    @Override
    public String toString() {
        if (localFile != null) {
            return "Delete local file " + localFile.getPath();
        } else {
            return "Delete remote file " + remoteFile.getFullName();
        }
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {
        if (localFile != null) {
            fileSystem.delete(localFile);
            reporter.localDeleted();
            log.info("Deleted local file " + localFile.getPath());
        } else {
            api.delete(remoteFile);
            reporter.remoteDeleted();
            log.info("Deleted remote file " + remoteFile.getFullName());
        }
    }
}
