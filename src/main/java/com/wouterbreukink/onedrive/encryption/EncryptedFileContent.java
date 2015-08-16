package com.wouterbreukink.onedrive.encryption;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpContent;

public class EncryptedFileContent extends AbstractInputStreamContent {

	private File thePlainTextFile;
	private EncryptionProvider theEncryptionProvider;
	private byte[] theCipherText;	
	
	public EncryptedFileContent(String type, File plainTextFile) throws IOException
	{
		super(type);
		thePlainTextFile = plainTextFile;
		theEncryptionProvider = new EncryptionProvider(getCommandLineOpts().getEncryptionKey());
		theCipherText = theEncryptionProvider.encryptFile(thePlainTextFile);		
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
