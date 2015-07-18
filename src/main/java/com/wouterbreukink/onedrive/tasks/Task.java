package com.wouterbreukink.onedrive.tasks;

import com.wouterbreukink.onedrive.TaskQueue;
import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.fs.FileSystemProvider;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

public abstract class Task implements Runnable, Comparable<Task> {

    private static final Logger log = LogManager.getLogger(Task.class.getName());
    private static AtomicInteger taskIdCounter = new AtomicInteger(1);

    protected final TaskQueue queue;
    protected final OneDriveAPI api;
    protected final FileSystemProvider fileSystem;

    private final int id;
    private int attempt;

    public Task(TaskQueue queue, OneDriveAPI api, FileSystemProvider fileSystem) {
        this.queue = Preconditions.checkNotNull(queue);
        this.api = Preconditions.checkNotNull(api);
        this.fileSystem = Preconditions.checkNotNull(fileSystem);
        this.id = taskIdCounter.getAndIncrement();
        this.attempt = 0;
    }

    protected abstract int priority();

    protected abstract void taskBody() throws IOException, OneDriveAPIException;

    public void run() {
        attempt++;
        try {
            log.debug(String.format("Starting task %d:%d - %s", id, attempt, this.toString()));
            taskBody();
            return;
        } catch (OneDriveAPIException ex) {

            switch (ex.getCode()) {
                case 401:
                    log.warn(String.format("Task %d:%d encountered 401 (Unauthorised) response", id, attempt));
                case 503:
                    log.warn(String.format("Task %d:%d encountered 503 (Temporarily Unavailable) - sleeping 10 seconds", id, attempt));
                    queue.suspend(10);
                case 509:
                    log.warn(String.format("Task %d:%d encountered error 509 (Bandwidth Limit Exceeded) - sleeping 60 seconds", id, attempt));
                    queue.suspend(60);
                default:
                    log.warn(String.format("Task %d:%d encountered %s", id, attempt, ex.getMessage()));
            }
        } catch (Exception ex) {
            log.error(String.format("Task %d:%d encountered exception", id, attempt), ex);
        }

        if (attempt < getCommandLineOpts().getTries()) {
            queue.add(this);
        } else {
            log.error(String.format("Task %d did not complete - %s", id, this.toString()));
        }
    }

    protected boolean isSizeInvalid(File localFile) {
        return isSizeInvalid(localFile.getPath(), localFile.length());
    }

    protected boolean isSizeInvalid(Item remoteFile) {
        return isSizeInvalid(remoteFile.getFullName(), remoteFile.getSize());
    }

    private boolean isSizeInvalid(String filename, long size) {
        int maxSizeKb = getCommandLineOpts().getMaxSizeKb();
        if (maxSizeKb > 0 && size > maxSizeKb * 1024) {
            log.info(String.format("Skipping file %s - size is %dKB (bigger than maximum of %dKB)",
                    filename,
                    size / 1024,
                    maxSizeKb));
            return true;
        }

        return false;
    }

    @SuppressWarnings("NullableProblems")
    public int compareTo(Task o) {
        return o.priority() - priority();
    }
}
