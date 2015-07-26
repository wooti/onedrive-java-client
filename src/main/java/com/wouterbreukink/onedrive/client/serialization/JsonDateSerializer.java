package com.wouterbreukink.onedrive.client.serialization;

import com.google.api.client.repackaged.com.google.common.base.Throwables;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class JsonDateSerializer {

    public static final JsonDateSerializer INSTANCE = new JsonDateSerializer();
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        df2.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public synchronized String serialize(Date value) {
        return df.format(value);
    }

    public synchronized Date deserialize(String value) {
        try {
            return df.parse(value);
        } catch (ParseException e) {
            try {
                return df2.parse(value);
            } catch (ParseException e1) {
                throw Throwables.propagate(e);
            }

        }
    }
}
