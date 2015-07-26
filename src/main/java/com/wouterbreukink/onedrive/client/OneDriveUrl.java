package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

class OneDriveUrl extends GenericUrl {

    private static final String rootUrl = "https://api.onedrive.com/v1.0";
    @Key("$skiptoken")
    private String token;

    public OneDriveUrl(String encodedUrl) {
        super(encodedUrl);
    }

    public static OneDriveUrl defaultDrive() {
        return new OneDriveUrl(rootUrl + "/drive");
    }

    public static OneDriveUrl driveRoot() {
        return new OneDriveUrl(rootUrl + "/drive/root");
    }

    public static OneDriveUrl children(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + "/children");
    }

    public static OneDriveUrl putContent(String id, String name) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + ":/" + name + ":/content");
    }

    public static OneDriveUrl postMultiPart(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + "/children");
    }

    public static OneDriveUrl createUploadSession(String id, String name) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + ":/" + name + ":/upload.createSession");
    }

    public static OneDriveUrl getPath(String path) {
        return new OneDriveUrl(rootUrl + "/drive/root:/" + path);
    }

    public static GenericUrl item(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id);
    }

    public static GenericUrl content(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + "/content");
    }

    public void setToken(String token) {
        this.token = token;
    }
}

