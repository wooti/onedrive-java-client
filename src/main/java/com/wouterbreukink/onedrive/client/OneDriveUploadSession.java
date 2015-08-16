package com.wouterbreukink.onedrive.client;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class OneDriveUploadSession implements OneDriveUploadSessionInterface {

    // Upload in chunks of 5MB as per MS recommendation
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;
    private final File file;
    private final String remoteFilename;
    private final String uploadUrl;
    private final RandomAccessFile raf;
    private OneDriveItem parent;
    private Range[] ranges;
    private long totalUploaded;
    private long lastUploaded;
    private OneDriveItem item;

    public OneDriveUploadSession(OneDriveItem parent, File file, String remoteFilename, String uploadUrl, String[] ranges) throws IOException {
        this.parent = parent;
        this.file = file;
        this.remoteFilename = remoteFilename;
        this.uploadUrl = uploadUrl;
        this.raf = new RandomAccessFile(file, "r");
        setRanges(ranges);
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#setRanges(java.lang.String[])
	 */
    @Override
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

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getChunk()
	 */
    @Override
	public byte[] getChunk() throws IOException {

        byte[] bytes = new byte[CHUNK_SIZE];

        raf.seek(totalUploaded);
        int read = raf.read(bytes);

        if (read < CHUNK_SIZE) {
            bytes = Arrays.copyOf(bytes, read);
        }

        return bytes;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getTotalUploaded()
	 */
    @Override
	public long getTotalUploaded() {
        return totalUploaded;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getLastUploaded()
	 */
    @Override
	public long getLastUploaded() {
        return lastUploaded;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getParent()
	 */
    @Override
	public OneDriveItem getParent() {
        return parent;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getUploadUrl()
	 */
    @Override
	public String getUploadUrl() {
        return uploadUrl;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getFile()
	 */
    @Override
	public File getFile() {
        return file;
    }
    
    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getRemoteFilename()
	 */
    @Override
	public String getRemoteFilename() {
        return remoteFilename;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#isComplete()
	 */
    @Override
	public boolean isComplete() {
        return item != null;
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#setComplete(com.wouterbreukink.onedrive.client.OneDriveItem)
	 */
    @Override
	public void setComplete(OneDriveItem item) {
        this.item = item;
        lastUploaded = file.length() - totalUploaded;
        totalUploaded = file.length();
    }

    /* (non-Javadoc)
	 * @see com.wouterbreukink.onedrive.client.OneDriveUploadSessionInterface2#getItem()
	 */
    @Override
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
	public long getRemoteFileLength() {
		return file.length();
	}
}
