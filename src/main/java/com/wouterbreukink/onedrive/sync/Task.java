package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import jersey.repackaged.com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Task implements Runnable, Comparable<Task> {

    private static final Logger log = Logger.getLogger(CheckFileTask.class.getName());
    private static AtomicInteger taskIdCounter = new AtomicInteger(1);

    protected final TaskQueue queue;
    private final int id;
    private int attempt;

    public Task(TaskQueue queue) {
        this.id = taskIdCounter.getAndIncrement();
        this.attempt = 0;
        this.queue = Preconditions.checkNotNull(queue);
    }

    protected abstract int priority();

    protected abstract void taskBody() throws OneDriveAPIException;

    public void run() {
        attempt++;
        try {
            log.finest(String.format("Starting task %d:%d - %s", id, attempt, this.toString()));
            taskBody();
            return;
        } catch (OneDriveAPIException ex) {

            switch (ex.getCode()) {
                case 401:
                    log.warning(String.format("Task %d:%d encountered 401 (Unauthorised) response", id, attempt));
                case 503:
                    log.warning(String.format("Task %d:%d encountered 503 (Temporarily Unavailable) - sleeping 10 seconds", id, attempt));
                    queue.suspend(10);
                case 509:
                    log.warning(String.format("Task %d:%d encountered error 509 (Bandwidth Limit Exceeded) - sleeping 60 seconds", id, attempt));
                    queue.suspend(60);
                default:
                    log.warning(String.format("Task %d:%d encountered %s", id, attempt, ex.getMessage()));
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, String.format("Task %d:%d encountered exception", id, attempt), ex);
        }

        if (attempt < 3) {
            queue.add(this);
        } else {
            log.severe(String.format("Task %d did not complete - %d", id, this.toString()));
        }
    }

    public int compareTo(Task o) {
        return o.priority() - priority();
    }
}
