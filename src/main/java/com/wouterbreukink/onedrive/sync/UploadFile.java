package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.OneDriveItem;
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
            if (replace) {
                log.fine("Uploading new copy of file: " + parent.getPath() + "/" + file.getName());
                client.replaceFile(parent, file);
            } else {
                log.fine("Uploading file: " + parent.getPath() + "/" + file.getName());
                client.uploadFile(parent, file);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to upload file " + file.getName(), e);
        }
    }
}

