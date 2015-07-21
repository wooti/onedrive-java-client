package com.wouterbreukink.onedrive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.text.DecimalFormat;

public class LogUtils {

    private LogUtils() {
    }

    public static String readableFileSize(double size) {
        return readableFileSize((long) size);
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String readableTime(long ms) {

        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000d);
        } else {
            long seconds = ms / 1000;
            long s = seconds % 60;
            long m = (seconds / 60) % 60;
            long h = (seconds / (60 * 60)) % 24;
            return String.format("%02d:%02d:%02d", h, m, s);
        }
    }

    public static String addFileLogger(String logFile) {

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        // Default log layout
        Layout<? extends Serializable> layout =
                PatternLayout.createLayout("%d %p [%t] %m%n", null, null, null, true, true, null, null);

        // Create a new file appender for the given filename
        FileAppender appender = FileAppender.createAppender(
                logFile,
                "false",
                "false",
                "FileAppender",
                "false",
                "true",
                "true",
                null,
                layout,
                null,
                null,
                null,
                config);

        appender.start();
        ((Logger) LogManager.getRootLogger()).addAppender(appender);

        return appender.getFileName();
    }
}
