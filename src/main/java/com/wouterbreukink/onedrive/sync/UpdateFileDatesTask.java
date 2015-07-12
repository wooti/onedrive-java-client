package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class UpdateFileDatesTask extends Task {

    private static final Logger log = LogManager.getLogger(UpdateFileDatesTask.class.getName());
    private final OneDriveAPI api;
    private final Date created;
    private final Date modified;
    private final Item remoteFile;

    public UpdateFileDatesTask(TaskQueue queue, OneDriveAPI api, Item remoteFile, Date created, Date modified) {

        super(queue);

        this.api = Preconditions.checkNotNull(api);
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
        log.info("Updating timestamps on item: " + remoteFile.getFullName());
        api.updateFile(remoteFile, created, modified);
    }
}

