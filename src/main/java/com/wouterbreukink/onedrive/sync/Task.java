package com.wouterbreukink.onedrive.sync;

import com.sun.istack.internal.NotNull;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;
import jersey.repackaged.com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Task implements Runnable, Comparable<Task> {

    private static final Logger log = LogManager.getLogger(Task.class.getName());
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

        if (attempt < 3) {
            queue.add(this);
        } else {
            log.error(String.format("Task %d did not complete - %s", id, this.toString()));
        }
    }

    public int compareTo(@NotNull Task o) {
        return o.priority() - priority();
    }
}
