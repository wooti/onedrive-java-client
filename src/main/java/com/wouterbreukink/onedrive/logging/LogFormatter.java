package com.wouterbreukink.onedrive.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000)
                .append(df.format(new Date(record.getMillis())))
                .append(" - [")
                .append(record.getLevel())
                .append("] - ")
                        //.append(toClassNameOnly(record.getSourceClassName()))
                        //.append(" - ")
                .append(formatMessage(record));


        if (record.getThrown() != null) {
            builder.append('\n').append(getStackTrace(record.getThrown()));
        }

        builder.append('\n');

        return builder.toString();
    }

    private String toClassNameOnly(String sourceClassName) {

        if (sourceClassName == null || sourceClassName.isEmpty())
            return null;

        int index = sourceClassName.lastIndexOf('.');
        return index > 0 ? sourceClassName.substring(index + 1) : sourceClassName;
    }
}
