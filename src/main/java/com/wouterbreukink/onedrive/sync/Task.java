package com.wouterbreukink.onedrive.sync;

public abstract class Task implements Runnable, Comparable<Task> {

    abstract int priority();

    public String name() {
        return this.getClass().getName();
    }

    public int compareTo(Task o) {
        return o.priority() - priority();
    }
}
