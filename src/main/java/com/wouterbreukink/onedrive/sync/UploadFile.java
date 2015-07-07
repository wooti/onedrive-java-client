package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadFile implements Task {

    private static final Logger log = Logger.getLogger(CheckFile.class.getName());

    private final OneDriveClient client;
    private final OneDriveItem parent;
    private final File file;
    private final boolean replace;

    public UploadFile(OneDriveClient client, OneDriveItem parent, File file, boolean replace) {

        Preconditions.checkNotNull(client);
        Preconditions.checkNotNull(parent);
        Preconditions.checkNotNull(file);

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }

        this.client = client;
        this.parent = parent;
        this.file = file;
        this.replace = replace;
    }

    public int priority() {
        return 50;
    }

    public int compareTo(Task o) {
        return o.priority() - priority();
    }

    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            Item response;
            if (replace) {
                response = client.replaceFile(parent, file);
            } else {
                response = client.uploadFile(parent, file);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.fine(String.format("Uploaded %d KB in %dms (%.2f KB/s) to %s file %s",
                    file.length() / 1024,
                    elapsedTime,
                    elapsedTime > 0 ? ((file.length() / 1024d) / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    response.getFullName()));

        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to upload file " + file.getName(), e);
        }
    }
}

