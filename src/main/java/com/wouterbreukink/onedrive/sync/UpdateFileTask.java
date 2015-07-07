package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;

import java.util.Date;
import java.util.logging.Logger;

public class UpdateFileTask extends Task {

    private static final Logger log = Logger.getLogger(SyncFileTask.class.getName());

    private final OneDriveClient client;
    private final Date created;
    private final Date modified;
    private final Item remoteFile;

    public UpdateFileTask(OneDriveClient client, Item remoteFile, Date created, Date modified) {

        Preconditions.checkNotNull(client);
        Preconditions.checkNotNull(remoteFile);

        this.client = client;
        this.remoteFile = remoteFile;
        this.created = created;
        this.modified = modified;
    }

    public int priority() {
        return 50;
    }

    public void run() {
        log.fine("Updating properties on item: " + remoteFile.getFullName());
        client.updateFile(remoteFile, created, modified);
    }
}

