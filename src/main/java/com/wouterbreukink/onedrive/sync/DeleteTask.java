package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DeleteTask extends Task {

    private static final Logger log = LogManager.getLogger(DeleteTask.class.getName());

    private final OneDriveAPI api;
    private final Item remoteFile;
    private final File localFile;

    public DeleteTask(TaskQueue queue, OneDriveAPI api, Item remoteFile) {

        super(queue);

        this.api = Preconditions.checkNotNull(api);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.localFile = null;
    }

    public DeleteTask(TaskQueue queue, OneDriveAPI api, File localFile) {

        super(queue);

        this.api = Preconditions.checkNotNull(api);
        this.localFile = Preconditions.checkNotNull(localFile);
        this.remoteFile = null;
    }

    public static void removeRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // try to delete the file anyway, even if its attributes
                // could not be read, since delete-only access is
                // theoretically possible
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed; propagate exception
                    throw exc;
                }
            }
        });
    }

    public int priority() {
        return 60;
    }

    @Override
    public String toString() {
        if (localFile != null) {
            return "Delete local file " + localFile.getName();
        } else {
            return "Delete remote file " + remoteFile.getFullName();
        }
    }

    @Override
    protected void taskBody() throws IOException, OneDriveAPIException {
        if (localFile != null) {
            removeRecursive(localFile.toPath());
        } else {
            api.delete(remoteFile);
        }
    }
}
