package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.tasks.Task;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskQueue {

    private final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<Task>();
    private final Object suspendedMonitor = new Object();
    private final Object doneMonitor = new Object();
    private AtomicInteger tasksInProgress = new AtomicInteger(0);
    private volatile boolean suspended = false;

    public void add(Task t) {
        tasksInProgress.incrementAndGet();
        queue.add(t);
    }

    public Task take() throws InterruptedException {

        // Wait for the queue to be active
        synchronized (suspendedMonitor) {
            while (suspended) {
                suspendedMonitor.wait();
            }
        }

        return queue.take();
    }

    public void done(Task t) {
        if (tasksInProgress.decrementAndGet() == 0) {
            synchronized (doneMonitor) {
                doneMonitor.notifyAll();
            }
        }
    }

    public void waitForCompletion() throws InterruptedException {
        while (tasksInProgress.get() > 0) {
            synchronized (doneMonitor) {
                doneMonitor.wait();
            }
        }
    }

    public void suspend(int seconds) {

        synchronized (suspendedMonitor) {

            if (suspended) {
                return;
            }

            suspended = true;
        }

        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (suspendedMonitor) {
                suspended = false;
                suspendedMonitor.notifyAll();
            }
        }
    }
}
