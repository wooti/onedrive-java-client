package com.wouterbreukink.onedrive.client;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import com.wouterbreukink.onedrive.encryption.EncryptionProvider;

public class OneDriveEncryptedUploadSession implements OneDriveUploadSessionInterface
{
    // Upload in chunks of 5MB as per MS recommendation
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;
    private final File file;
    private final String remoteFilename;
    private final String uploadUrl;
    private OneDriveItem parent;
    private Range[] ranges;
    private long totalUploaded;
    private long lastUploaded;
    private OneDriveItem item;
    private long currentStreamPosition;
    private EncryptionProvider encryptionProvider;
    private DataInputStream encryptedInputStream = null;

    public OneDriveEncryptedUploadSession(OneDriveItem parent, File file, String remoteFilename, String uploadUrl, String[] ranges) throws IOException 
    {
        this.parent = parent;
        this.file = file;
        this.remoteFilename = remoteFilename;
        this.uploadUrl = uploadUrl;        
        this.currentStreamPosition = 0;
        this.encryptedInputStream = EncryptionProvider.getEncryptionProvider()
        		.encryptFileToStream(file);
        setRanges(ranges);
    }

    public void setRanges(String[] stringRanges) {

        this.ranges = new Range[stringRanges.length];
        for (int i = 0; i < stringRanges.length; i++) {
            long start = Long.parseLong(stringRanges[i].substring(0, stringRanges[i].indexOf('-')));

            String s = stringRanges[i].substring(stringRanges[i].indexOf('-') + 1);

            long end = 0;
            if (!s.isEmpty()) {
                end = Long.parseLong(s);
            }

            ranges[i] = new Range(start, end);
        }

        if (ranges.length > 0) {
            lastUploaded = ranges[0].start - totalUploaded;
            totalUploaded = ranges[0].start;
        }
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

    public long getTotalUploaded() {
        return totalUploaded;
    }

    public long getLastUploaded() {
        return lastUploaded;
    }

    public OneDriveItem getParent() {
        return parent;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public File getFile() {
        return file;
    }
    
    public String getRemoteFilename() {
        return remoteFilename;
    }

    public boolean isComplete() {
        return item != null;
    }

    public void setComplete(OneDriveItem item) {
        this.item = item;
        lastUploaded = file.length() - totalUploaded;
        totalUploaded = file.length();
        if (encryptedInputStream != null)
        {
        	try { encryptedInputStream.close(); } 
        	catch (IOException e) {}	
        }
        
    }

    public OneDriveItem getItem() {
        return item;
    }

    private static class Range {
        public long start;
        @SuppressWarnings("unused")
		public long end;

        private Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

	@Override
	public long getRemoteFileLength() 
	{
		return encryptionProvider.computeEncryptedLength(file.length());
	}
}
