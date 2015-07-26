package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.OneDriveUploadSession;
import com.wouterbreukink.onedrive.client.api.OneDriveItem;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;
import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;
import static com.wouterbreukink.onedrive.LogUtils.readableTime;

public class UploadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());

    private final OneDriveItem parent;
    private final File localFile;
    private final boolean replace;

    public UploadTask(TaskOptions options, OneDriveItem parent, File localFile, boolean replace) {

        super(options);

        this.parent = Preconditions.checkNotNull(parent);
        this.localFile = Preconditions.checkNotNull(localFile);
        this.replace = replace;

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Specified parent is not a folder");
        }
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Upload " + parent.getFullName() + localFile.getName();
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {

        if (isIgnored(localFile)) {
            reporter.skipped();
            return;
        }

        if (localFile.isDirectory()) {
            OneDriveItem newParent = api.createFolder(parent, localFile.getName());

            //noinspection ConstantConditions
            for (File f : localFile.listFiles()) {
                queue.add(new UploadTask(getTaskOptions(), newParent, f, false));
            }
        } else {

            if (isSizeInvalid(localFile)) {
                reporter.skipped();
                return;
            }

            long startTime = System.currentTimeMillis();

            OneDriveItem response;
            if (localFile.length() > getCommandLineOpts().getSplitAfter() * 1024 * 1024) {

                OneDriveUploadSession session = api.startUploadSession(parent, localFile);

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

                    log.info(String.format("Uploaded chunk (progress %.1f%%) of %s (%s/s) for file %s",
                            ((double) session.getTotalUploaded() / session.getFile().length()) * 100,
                            readableFileSize(session.getLastUploaded()),
                            elapsedTimeInner > 0 ? readableFileSize(session.getLastUploaded() / (elapsedTimeInner / 1000d)) : 0,
                            parent.getFullName() + localFile.getName()));
                }

                response = session.getItem();

            } else {
                response = replace ? api.replaceFile(parent, localFile) : api.uploadFile(parent, localFile);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info(String.format("Uploaded %s in %s (%s/s) to %s file %s",
                    readableFileSize(localFile.length()),
                    readableTime(elapsedTime),
                    elapsedTime > 0 ? readableFileSize(localFile.length() / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    response.getFullName()));

            reporter.fileUploaded(replace, localFile.length());
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

