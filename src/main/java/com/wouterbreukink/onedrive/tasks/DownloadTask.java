package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;

public class DownloadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());
    private final File parent;
    private final Item remoteFile;
    private final boolean replace;

    public DownloadTask(TaskOptions options, File parent, Item remoteFile, boolean replace) {

        super(options);

        this.parent = Preconditions.checkNotNull(parent);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
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
        return "Download " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {

        if (remoteFile.isDirectory()) {

            File newParent = fileSystem.createFolder(parent, remoteFile.getName());

            for (Item item : api.getChildren(remoteFile)) {
                queue.add(new DownloadTask(getTaskOptions(), newParent, item, false));
            }

        } else {

            if (isSizeInvalid(remoteFile)) {
                reporter.skipped();
                return;
            }

            // Skip if ignored
            if (isIgnored(remoteFile)) {
                reporter.skipped();
                return;
            }

            long startTime = System.currentTimeMillis();

            File downloadFile = fileSystem.createFile(parent, remoteFile.getName() + ".tmp");

            api.download(remoteFile, downloadFile);

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info(String.format("Downloaded %s in %dms (%s/s) to %s file %s",
                    readableFileSize(remoteFile.getSize()),
                    elapsedTime,
                    elapsedTime > 0 ? readableFileSize(remoteFile.getSize() / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    remoteFile.getFullName()));

            // Do a CRC check on the downloaded file
            if (!fileSystem.verifyCrc(downloadFile, remoteFile.getFile().getHashes().getCrc32())) {
                throw new IOException(String.format("Download of file '%s' failed", remoteFile.getFullName()));
            }

            fileSystem.setAttributes(
                    downloadFile,
                    remoteFile.getFileSystemInfo().getCreatedDateTime(),
                    remoteFile.getFileSystemInfo().getLastModifiedDateTime());

            fileSystem.replaceFile(new File(parent, remoteFile.getName()), downloadFile);
            reporter.fileDownloaded(replace, remoteFile.getSize());
        }
    }
}

