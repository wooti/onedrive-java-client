package com.wouterbreukink.onedrive.client.resources;

import com.google.api.client.util.Key;

public class UploadSession {

    @Key
    private String uploadUrl;
    @Key
    private String[] nextExpectedRanges;

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String[] getNextExpectedRanges() {
        return nextExpectedRanges;
    }
}
