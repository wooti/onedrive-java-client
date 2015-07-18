package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.TaskQueue;
import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.client.OneDriveUploadSession;
import com.wouterbreukink.onedrive.fs.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

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

            OneDriveItem response;
            if (file.length() > getCommandLineOpts().getSplitAfter() * 1024 * 1024) {

                OneDriveUploadSession session = api.startUploadSession(parent, file);

                while (!session.isComplete()) {
                    long startTimeInner = System.currentTimeMillis();

                    // Attempt each chunk 10x
                    for (int i = 0; i < 10; i++) {
                        try {
                            startTimeInner = System.currentTimeMillis();
                            api.uploadChunk(session);
                            break;
                        } catch (OneDriveAPIException ex) {
                            switch (ex.getCode()) {
                                case 401:
                                case 500:
                                case 502:
                                case 503:
                                case 504:
                                    log.warn(String.format("Multipart Upload Task %s encountered %s - sleeping 10 seconds", getId(), ex.getMessage()));
                                    sleep(10);
                                    break;
                                case 429:
                                case 509:
                                    log.warn(String.format("Multipart Upload Task %s encountered %s - sleeping 60 seconds", getId(), ex.getMessage()));
                                    sleep(60);
                                    break;
                                default:
                                    throw ex;
                            }
                        }
                    }

                    long elapsedTimeInner = System.currentTimeMillis() - startTimeInner;

                    log.info(String.format("Uploaded chunk of %d KB (%.1f%%) in %dms (%.2f KB/s) for file %s",
                            session.getLastUploaded() / 1024,
                            ((double) session.getTotalUploaded() / session.getFile().length()) * 100,
                            elapsedTimeInner,
                            elapsedTimeInner > 0 ? ((session.getLastUploaded() / 1024d) / (elapsedTimeInner / 1000d)) : 0,
                            parent.getFullName() + "/" + file.getName()));
                }

                response = session.getItem();

            } else {
                response = replace ? api.replaceFile(parent, file) : api.uploadFile(parent, file);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info(String.format("Uploaded %d KB in %dms (%.2f KB/s) to %s file %s",
                    file.length() / 1024,
                    elapsedTime,
                    elapsedTime > 0 ? ((file.length() / 1024d) / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    response.getFullName()));
        }
    }

    private void sleep(int i) {
        try {
            Thread.sleep(i * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

