package com.wouterbreukink.onedrive.encryption;

import java.io.IOException;

public class EncryptionException extends IOException {
	
    public EncryptionException(String message) {
        super(message);        
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

}
