package com.wouterbreukink.onedrive.client;

import java.io.IOException;

public class OneDriveAPIException extends IOException {

    private final int code;

    public OneDriveAPIException(int code, String message) {
        super(message);
        this.code = code;
    }

    public OneDriveAPIException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
