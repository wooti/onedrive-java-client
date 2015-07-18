package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

public class DownloadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());
    private final OneDriveAPI api;
    private final File parent;
    private final Item file;
    private final boolean replace;

    public DownloadTask(TaskQueue queue, OneDriveAPI api, File parent, Item file, boolean replace) {

        super(queue);

        this.api = Preconditions.checkNotNull(api);
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
            File newParent = new File(parent, file.getName());

            if (newParent.mkdir()) {
                for (Item item : api.getChildren(file)) {
                    queue.add(new DownloadTask(queue, api, newParent, item, false));
                }
            } else {
                log.error("Unable to download folder - could not create local directory");
            }
        } else {
            long startTime = System.currentTimeMillis();

            File targetFile = new File(parent, file.getName() + (replace ? ".tmp" : ""));
            targetFile.deleteOnExit();

            api.download(file, targetFile);

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info(String.format("Downloaded %d KB in %dms (%.2f KB/s) to %s file %s",
                    file.getSize() / 1024,
                    elapsedTime,
                    elapsedTime > 0 ? ((file.getSize() / 1024d) / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    file.getFullName()));

            // Do a CRC check on the downloaded file
            long remoteCrc = file.getFile().getHashes().getCrc32();
            long localCrc = Utils.getChecksum(targetFile);
            boolean crcMatches = remoteCrc == localCrc;

            if (!crcMatches) {
                throw new IllegalStateException("Download failed");
            }

            // Set the attributes
            BasicFileAttributeView attributes = Files.getFileAttributeView(targetFile.toPath(), BasicFileAttributeView.class);
            FileTime lastModified = FileTime.fromMillis(file.getFileSystemInfo().getLastModifiedDateTime().getTime());
            FileTime created = FileTime.fromMillis(file.getFileSystemInfo().getCreatedDateTime().getTime());
            attributes.setTimes(lastModified, lastModified, created);

            if (replace) {
                File originalFile = new File(parent, file.getName());

                if (!originalFile.delete()) {
                    throw new IOException("Unable to replace local file" + file.getFullName());
                }

                if (!targetFile.renameTo(originalFile)) {
                    throw new IOException("Unable to replace local file" + file.getFullName());
                }
            }
        }
    }
}

