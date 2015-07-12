package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;

import java.util.Date;
import java.util.logging.Logger;

public class UpdateFileTask extends Task {

    private static final Logger log = Logger.getLogger(CheckFileTask.class.getName());

    private final OneDriveAPI client;
    private final Date created;
    private final Date modified;
    private final Item remoteFile;

    public UpdateFileTask(TaskQueue queue, OneDriveAPI client, Item remoteFile, Date created, Date modified) {

        super(queue);

        this.client = Preconditions.checkNotNull(client);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.created = Preconditions.checkNotNull(created);
        this.modified = Preconditions.checkNotNull(modified);
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Update properties for " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() throws OneDriveAPIException {
        log.fine("Updating properties on item: " + remoteFile.getFullName());
        client.updateFile(remoteFile, created, modified);
    }
}

