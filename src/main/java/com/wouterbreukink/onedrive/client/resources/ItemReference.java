package com.wouterbreukink.onedrive.client.resources;

import com.wouterbreukink.onedrive.client.OneDriveItem;
import jersey.repackaged.com.google.common.base.Throwables;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class ItemReference implements OneDriveItem {

    private String driveId;
    private String id;
    private String path;

    public String getDriveId() {
        return driveId;
    }

    public String getId() {
        return id;
    }

    public boolean isDirectory() {
        return true;
    }

    public String getPath() {
        return path;
    }

    public String getFullName() {

        if (path == null) {
            return null;
        }

        int index = path.indexOf(':');

        try {
            return URLDecoder.decode(index > 0 ? path.substring(index + 1) : path, "UTF-8") + (isDirectory() ? "/" : "");
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }
}
