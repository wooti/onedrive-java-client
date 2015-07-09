package com.wouterbreukink.onedrive.sync;

import com.wouterbreukink.onedrive.Main;
import com.wouterbreukink.onedrive.client.OneDriveAPIException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Task implements Runnable, Comparable<Task> {

    private static final Logger log = Logger.getLogger(CheckFileTask.class.getName());
    private static AtomicInteger taskIdCounter = new AtomicInteger(1);

    private final int id;
    private int attempt;

    public Task() {
        this.id = taskIdCounter.getAndIncrement();
        this.attempt = 0;
    }

    abstract int priority();

    abstract void taskBody() throws OneDriveAPIException;

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
                    sleep(10);
                case 509:
                    log.warning(String.format("Task %d:%d encountered error 509 (Bandwidth Limit Exceeded) - sleeping 60 seconds", id, attempt));
                    sleep(60);
                default:
                    log.warning(String.format("Task %d:%d encountered %s", id, attempt, ex.getMessage()));
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, String.format("Task %d:%d encountered exception", id, attempt), ex);
        }

        if (attempt < 3) {
            sleep(1);
            Main.queue.add(this);
        } else {
            log.severe(String.format("Task %d did not complete", id, attempt));
        }
    }

    public String name() {
        return this.getClass().getName();
    }

    public int compareTo(Task o) {
        return o.priority() - priority();
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
