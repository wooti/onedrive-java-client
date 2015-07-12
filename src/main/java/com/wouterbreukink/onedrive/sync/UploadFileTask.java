package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.client.resources.OneDriveItem;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

public class UploadFileTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadFileTask.class.getName());
    private final OneDriveAPI api;
    private final OneDriveItem parent;
    private final File file;
    private final boolean replace;

    public UploadFileTask(TaskQueue queue, OneDriveAPI api, OneDriveItem parent, File file, boolean replace) {

        super(queue);

        this.api = Preconditions.checkNotNull(api);
        this.parent = Preconditions.checkNotNull(parent);
        this.file = Preconditions.checkNotNull(file);
        this.replace = replace;

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Upload file " + parent.getFullName() + "/" + file.getName();
    }

    @Override
    protected void taskBody() throws OneDriveAPIException {
        try {
            long startTime = System.currentTimeMillis();
            Item response;

            if (getCommandLineOpts().isDryRun()) {
                if (replace) {
                    log.info(String.format("Would replace remote file: %s/%s", parent.getFullName(), file.getName()));
                } else {
                    log.info(String.format("Would upload new remote file: %s/%s", parent.getFullName(), file.getName()));
                }
            } else {
                if (replace) {
                    response = api.replaceFile(parent, file);
                } else {
                    response = api.uploadFile(parent, file);
                }

                long elapsedTime = System.currentTimeMillis() - startTime;

                log.info(String.format("Uploaded %d KB in %dms (%.2f KB/s) to %s file %s",
                        file.length() / 1024,
                        elapsedTime,
                        elapsedTime > 0 ? ((file.length() / 1024d) / (elapsedTime / 1000d)) : 0,
                        replace ? "replace" : "new",
                        response.getFullName()));
            }
        } catch (IOException e) {
            log.error("Unable to upload file " + file.getName(), e);
        }
    }
}

