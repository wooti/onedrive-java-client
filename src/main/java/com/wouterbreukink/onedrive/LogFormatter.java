package com.wouterbreukink.onedrive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        return new StringBuilder(1000)
                .append(df.format(new Date(record.getMillis())))
                .append(" - [")
                .append(record.getLevel())
                .append("] - ")
                        //.append(toClassNameOnly(record.getSourceClassName()))
                        //.append(" - ")
                .append(formatMessage(record))
                .append('\n')
                .toString();
    }

    private String toClassNameOnly(String sourceClassName) {

        if (sourceClassName == null || sourceClassName.isEmpty())
            return null;

        int index = sourceClassName.lastIndexOf('.');
        return index > 0 ? sourceClassName.substring(index + 1) : sourceClassName;
    }
}
