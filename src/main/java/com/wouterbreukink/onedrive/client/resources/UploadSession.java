package com.wouterbreukink.onedrive.client.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadSession {

    private String uploadUrl;
    private String[] nextExpectedRanges;

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String[] getNextExpectedRanges() {
        return nextExpectedRanges;
    }
}
