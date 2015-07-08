package com.wouterbreukink.onedrive.client;

public class OneDriveAPIException extends Exception {

    private final int code;
    private final String message;

    public OneDriveAPIException(int code, String message) {

        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
