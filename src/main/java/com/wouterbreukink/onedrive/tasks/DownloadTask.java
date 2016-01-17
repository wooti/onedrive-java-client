package com.wouterbreukink.onedrive.tasks;

import com.google.api.client.util.Preconditions;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.client.downloader.ResumableDownloader;
import com.wouterbreukink.onedrive.client.downloader.ResumableDownloaderProgressListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;
import static com.wouterbreukink.onedrive.LogUtils.readableTime;

public class DownloadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());
    private final File parent;
    private final OneDriveItem remoteFile;
    private final boolean replace;

    public DownloadTask(TaskOptions options, File parent, OneDriveItem remoteFile, boolean replace) {

        super(options);

        this.parent = Preconditions.checkNotNull(parent);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.replace = Preconditions.checkNotNull(replace);

        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Specified parent is not a folder");
        }
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Download " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() throws IOException {

        if (isIgnored(remoteFile)) {
            reporter.skipped();
            return;
        }

        if (remoteFile.isDirectory()) {

            File newParent = fileSystem.createFolder(parent, remoteFile.getName());
            queue.add(new UpdatePropertiesTask(getTaskOptions(), remoteFile, newParent));

            for (OneDriveItem item : api.getChildren(remoteFile)) {
                queue.add(new DownloadTask(getTaskOptions(), newParent, item, false));
            }

        } else {

            if (isSizeInvalid(remoteFile)) {
                reporter.skipped();
                return;
            }

            final long startTime = System.currentTimeMillis();

            File downloadFile = null;

            try {
                downloadFile = fileSystem.createFile(parent, remoteFile.getName() + ".tmp");

                // The progress reporter
                ResumableDownloaderProgressListener progressListener = new ResumableDownloaderProgressListener() {

                    private long startTimeInner = System.currentTimeMillis();

                    @Override
                    public void progressChanged(ResumableDownloader downloader) throws IOException {

                        switch (downloader.getDownloadState()) {
                            case MEDIA_IN_PROGRESS:
                                long elapsedTimeInner = System.currentTimeMillis() - startTimeInner;

                                log.info(String.format("Downloaded chunk (progress %.1f%%) of %s (%s/s) for file %s",
                                        downloader.getProgress() * 100,
                                        readableFileSize(downloader.getChunkSize()),
                                        elapsedTimeInner > 0 ? readableFileSize(downloader.getChunkSize() / (elapsedTimeInner / 1000d)) : 0,
                                        remoteFile.getFullName()));

                                startTimeInner = System.currentTimeMillis();
                                break;
                            case MEDIA_COMPLETE:
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                log.info(String.format("Downloaded %s in %s (%s/s) of %s file %s",
                                        readableFileSize(remoteFile.getSize()),
                                        readableTime(elapsedTime),
                                        elapsedTime > 0 ? readableFileSize(remoteFile.getSize() / (elapsedTime / 1000d)) : 0,
                                        replace ? "replaced" : "new",
                                        remoteFile.getFullName()));
                        }
                    }
                };

                api.download(remoteFile, downloadFile, progressListener);

                // Do a CRC check on the downloaded file
                if (!fileSystem.verifyCrc(downloadFile, remoteFile.getCrc32())) {
                    throw new IOException(String.format("Download of file '%s' failed", remoteFile.getFullName()));
                }

                fileSystem.setAttributes(
                        downloadFile,
                        remoteFile.getCreatedDateTime(),
                        remoteFile.getLastModifiedDateTime());

                fileSystem.replaceFile(new File(parent, remoteFile.getName()), downloadFile);
                reporter.fileDownloaded(replace, remoteFile.getSize());
            } catch (Throwable e) {
                if (downloadFile != null) {
                    if (!downloadFile.delete()) {
                        log.warn("Unable to remove temporary file " + downloadFile.getPath());
                    }
                }

                throw e;
            }
        }
    }
}

