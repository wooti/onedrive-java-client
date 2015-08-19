package com.wouterbreukink.onedrive.client;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import com.wouterbreukink.onedrive.encryption.EncryptionProvider;

public class OneDriveEncryptedUploadSession extends OneDriveUploadSessionBase
{
    private long currentStreamPosition;
    private DataInputStream encryptedInputStream = null;

    public OneDriveEncryptedUploadSession(OneDriveItem parent, File file, String remoteFilename, String uploadUrl, String[] ranges) throws IOException 
    {
    	super(parent, file, remoteFilename, uploadUrl, ranges);
        this.currentStreamPosition = 0;
        this.encryptedInputStream = EncryptionProvider.getEncryptionProvider()
        		.encryptFileToStream(file);        
    }
    
    public byte[] getChunk() throws IOException {

    	if (currentStreamPosition != totalUploaded)
        	throw new IOException("Unexpected encrypted stream position");
    	
    	long totalLength = getRemoteFileLength();
    	long leftInStream = totalLength - currentStreamPosition;
    	if (leftInStream <= 0)
    	{
    		throw new IOException("End of stream reached");
    	}
    	
    	long toRead = Math.min(leftInStream, CHUNK_SIZE);
    	
        byte[] bytes = new byte[(int)toRead];

        encryptedInputStream.readFully(bytes);
        currentStreamPosition += toRead;
        
        return bytes;
    }

    @Override
    public void setComplete(OneDriveItem item) 
    {
    	super.setComplete(item);
        
    	if (encryptedInputStream != null)
        {
        	try { encryptedInputStream.close(); } 
        	catch (IOException e) {}	
        }
        
    }

	@Override
	public long getRemoteFileLength() 
	{
		return EncryptionProvider.computeEncryptedLength(file.length());
	}
}
