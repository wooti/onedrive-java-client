package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.TaskQueue;
import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.fs.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class UploadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());
    private final OneDriveItem parent;
    private final File file;
    private final boolean replace;

    public UploadTask(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem, OneDriveItem parent, File file, boolean replace) {

        super(queue, api, fileSystem);

        this.parent = Preconditions.checkNotNull(parent);
        this.file = Preconditions.checkNotNull(file);
        this.replace = replace;

        if (!parent.isFolder()) {
            throw new IllegalArgumentException("Specified parent is not a folder");
        }
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Upload " + parent.getFullName() + "/" + file.getName();
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {

        if (file.isDirectory()) {
            OneDriveItem newParent = api.createFolder(parent, file.getName());

            //noinspection ConstantConditions
            for (File f : file.listFiles()) {
                queue.add(new UploadTask(queue, api, fileSystem, newParent, f, false));
            }
        } else {

            if (isSizeInvalid(file)) {
                return;
            }

            long startTime = System.currentTimeMillis();
            OneDriveItem response = replace ? api.replaceFile(parent, file) : api.uploadFile(parent, file);

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info(String.format("Uploaded %d KB in %dms (%.2f KB/s) to %s file %s",
                    file.length() / 1024,
                    elapsedTime,
                    elapsedTime > 0 ? ((file.length() / 1024d) / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    response.getFullName()));
        }
    }
}

