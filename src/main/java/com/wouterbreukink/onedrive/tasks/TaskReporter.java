package com.wouterbreukink.onedrive.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;
import static com.wouterbreukink.onedrive.LogUtils.readableTime;

public class TaskReporter {

    private static final Logger log = LogManager.getLogger(TaskReporter.class.getName());

    private int same;
    private int remoteDeleted;
    private int localDeleted;
    private int skipped;
    private int propsUpdated;
    private int errors;

    private int newUploaded;
    private long newUploadedSize;
    private int replaceUploaded;
    private long replaceUploadedSize;

    private int newDownloaded;
    private long newDownloadedSize;
    private int replaceDownloaded;
    private long replaceDownloadedSize;

    private long startTime;

    public TaskReporter() {
        startTime = System.currentTimeMillis();
    }

    public synchronized void same() {
        same++;
    }

    public synchronized void remoteDeleted() {
        remoteDeleted++;
    }

    public synchronized void localDeleted() {
        localDeleted++;
    }

    public synchronized void skipped() {
        skipped++;
    }

    public synchronized void error() {
        errors++;
    }

    public synchronized void fileUploaded(boolean replace, long size) {
        if (replace) {
            replaceUploaded++;
            replaceUploadedSize += size;
        } else {
            newUploaded++;
            newUploadedSize += size;
        }
    }

    public synchronized void fileDownloaded(boolean replace, long size) {
        if (replace) {
            replaceDownloaded++;
            replaceDownloadedSize += size;
        } else {
            newDownloaded++;
            newDownloadedSize += size;
        }
    }

    public synchronized void propertiesUpdated() {
        propsUpdated++;
    }

    public synchronized void report() {

        if (errors > 0) {
            log.error(String.format("%d tasks failed - see log for details", errors));
        }

        if (same > 0) {
            log.info(String.format("Skipped %d unchanged file%s", same, plural(same)));
        }

        if (skipped > 0) {
            log.info(String.format("Skipped %d ignored file%s", skipped, plural(skipped)));
        }

        if (localDeleted > 0) {
            log.info(String.format("Deleted %d local file%s", localDeleted, plural(skipped)));
        }

        if (remoteDeleted > 0) {
            log.info(String.format("Deleted %d remote file%s", remoteDeleted, plural(skipped)));
        }

        if (propsUpdated > 0) {
            log.info(String.format("Updated timestamps on %d file%s", propsUpdated, plural(skipped)));
        }

        if (newUploaded > 0 || replaceUploaded > 0) {
            log.info(String.format("Uploaded %d file%s (%s) - %d new file%s (%s), %d replaced file%s (%s)",
                    newUploaded + replaceUploaded,
                    plural(newUploaded + replaceUploaded),
                    readableFileSize(newUploadedSize + replaceUploadedSize),
                    newUploaded,
                    plural(newUploaded),
                    readableFileSize(newUploadedSize),
                    replaceUploaded,
                    plural(replaceUploaded),
                    readableFileSize(replaceUploadedSize)));
        }

        if (newDownloaded > 0 || replaceDownloaded > 0) {
            log.info(String.format("Uploaded %d file%s (%s) - %d new file%s (%s), %d replaced file%s (%s)",
                    newDownloaded + replaceDownloaded,
                    plural(newDownloaded + replaceDownloaded),
                    readableFileSize(newDownloadedSize + replaceDownloadedSize),
                    newDownloaded,
                    plural(newDownloaded),
                    readableFileSize(newDownloadedSize),
                    replaceDownloaded,
                    plural(replaceDownloaded),
                    readableFileSize(replaceDownloadedSize)));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info(String.format("Elapsed time: %s", readableTime(elapsed)));
    }

    private String plural(long same) {
        return same != 1 ? "s" : "";
    }
}
