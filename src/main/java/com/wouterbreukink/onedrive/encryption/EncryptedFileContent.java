package com.wouterbreukink.onedrive.encryption;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.google.api.client.http.AbstractInputStreamContent;

public class EncryptedFileContent extends AbstractInputStreamContent {

	private File thePlainTextFile;
	private byte[] theCipherText;	
	
	public EncryptedFileContent(String type, File plainTextFile) throws IOException
	{
		super(type);
		thePlainTextFile = plainTextFile;
		theCipherText = EncryptionProvider.getEncryptionProvider()
				.encryptFile(thePlainTextFile);		
	}
	
	@Override
	public long getLength() throws IOException 
	{
		return theCipherText.length;		
	}

	@Override
	public boolean retrySupported() 
	{
		return true;
	}

	@Override
	public InputStream getInputStream() 
	{
		return new ByteArrayInputStream(theCipherText, 0, theCipherText.length);
	}

	@Override
	public EncryptedFileContent setType(String type) 
	{
		return (EncryptedFileContent) super.setType(type);
	}

	@Override
	public EncryptedFileContent setCloseInputStream(boolean closeInputStream) 
	{
		return (EncryptedFileContent) super.setCloseInputStream(closeInputStream);
	}	
}
