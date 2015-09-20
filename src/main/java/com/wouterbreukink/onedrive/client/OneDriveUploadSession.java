package com.wouterbreukink.onedrive.client;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class OneDriveUploadSession extends OneDriveUploadSessionBase {

    private final RandomAccessFile raf;
    
    public OneDriveUploadSession(OneDriveItem parent, File file, String remoteFilename, String uploadUrl, String[] ranges) throws IOException 
    {
        super(parent, file, remoteFilename, uploadUrl, ranges);
        this.raf = new RandomAccessFile(file, "r");        
    }

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

	@Override
	public long getRemoteFileLength() {
		return file.length();
	}
}
