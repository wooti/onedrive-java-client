package com.wouterbreukink.onedrive.client;

import java.io.File;
import java.io.IOException;

public abstract class OneDriveUploadSessionBase 
{
    // Upload in chunks of 5MB as per MS recommendation
    protected static final int CHUNK_SIZE = 5 * 1024 * 1024;
    protected final File file;
    protected final String remoteFilename;
    protected final String uploadUrl;
    protected OneDriveItem parent;
    protected Range[] ranges;
    protected long totalUploaded;
    protected long lastUploaded;
    protected OneDriveItem item;
    
    protected OneDriveUploadSessionBase(OneDriveItem parent, File file, String remoteFilename, String uploadUrl, String[] ranges) 
    {
        this.parent = parent;
        this.file = file;
        this.remoteFilename = remoteFilename;
        this.uploadUrl = uploadUrl;
        setRanges(ranges);
    }
    
    public void setRanges(String[] stringRanges) 
    {
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

    public void setComplete(OneDriveItem item) 
    {
        this.item = item;
        lastUploaded = file.length() - totalUploaded;
        totalUploaded = file.length();
    }
    
    public OneDriveItem getItem() {
        return item;
    }
    
	protected abstract byte[] getChunk() throws IOException;
    
	public abstract long getRemoteFileLength();
	
    private static class Range 
    {
        public long start;
        @SuppressWarnings("unused")
		public long end;

        private Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

}