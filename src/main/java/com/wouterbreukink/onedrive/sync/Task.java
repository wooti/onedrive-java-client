package com.wouterbreukink.onedrive.sync;

public interface Task extends Runnable, Comparable<Task> {
    int priority();
}
