package com.wouterbreukink.onedrive.encryption;

import java.io.IOException;

public class EncryptionException extends IOException {
	
	private static final long serialVersionUID = 3137198448336932472L;

	public EncryptionException(String message) {
        super(message);        
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

}
