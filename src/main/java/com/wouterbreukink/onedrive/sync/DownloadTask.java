package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.io.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class DownloadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());
    private final File parent;
    private final Item file;
    private final boolean replace;

    public DownloadTask(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem, File parent, Item file, boolean replace) {

        super(queue, api, fileSystem);

        this.parent = Preconditions.checkNotNull(parent);
        this.file = Preconditions.checkNotNull(file);
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
        return "Download " + file.getFullName();
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {

        if (file.isFolder()) {

            File newParent = fileSystem.createFolder(parent, file.getName());

            for (Item item : api.getChildren(file)) {
                queue.add(new DownloadTask(queue, api, fileSystem, newParent, item, false));
            }

        } else {
            long startTime = System.currentTimeMillis();

            File downloadFile = fileSystem.createFile(parent, file.getName() + ".tmp");

            api.download(file, downloadFile);

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info(String.format("Downloaded %d KB in %dms (%.2f KB/s) to %s file %s",
                    file.getSize() / 1024,
                    elapsedTime,
                    elapsedTime > 0 ? ((file.getSize() / 1024d) / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    file.getFullName()));

            // Do a CRC check on the downloaded file
            if (!fileSystem.verifyCrc(downloadFile, file.getFile().getHashes().getCrc32())) {
                throw new IOException(String.format("Download of file '%s' failed", file.getFullName()));
            }

            fileSystem.setAttributes(
                    downloadFile,
                    file.getFileSystemInfo().getCreatedDateTime(),
                    file.getFileSystemInfo().getLastModifiedDateTime());

            fileSystem.replaceFile(new File(parent, file.getName()), downloadFile);
        }
    }
}

