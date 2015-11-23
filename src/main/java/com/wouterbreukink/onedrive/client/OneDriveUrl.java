package com.wouterbreukink.onedrive.client;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + ":/" + encode(name) + ":/content");
    }

    public static OneDriveUrl postMultiPart(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + "/children");
    }

    public static OneDriveUrl createUploadSession(String id, String name) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + ":/" + encode(name) + ":/upload.createSession");
    }

    public static OneDriveUrl getPath(String path) {
        return new OneDriveUrl(rootUrl + "/drive/root:/" + encode(path).replace("%5C", "/"));
    }

    public static GenericUrl item(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id);
    }

    public static GenericUrl content(String id) {
        return new OneDriveUrl(rootUrl + "/drive/items/" + id + "/content");
    }

    private static String encode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public void setToken(String token) {
        this.token = token;
    }
}

