package com.wouterbreukink.onedrive.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.encryption.EncryptionProvider;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

class ROFileSystemProvider implements FileSystemProvider {

    public void delete(File file) throws IOException {
        // Do nothing
    }

    @SuppressWarnings("serial")
	public File createFolder(File file, String name) throws IOException {

        return new File(file, name) {
            @Override
            public boolean isDirectory() {
                return true;
            }
        };
    }

    public File createFile(File file, String name) throws IOException {
        return new File(file, name);
    }

    public void replaceFile(File original, File replacement) throws IOException {
        // Do nothing
    }
    
	@Override
	public void replaceAndDecryptFile(File original, File replacement) throws IOException {
		// Do nothing		
	}

    public void setAttributes(File downloadFile, Date created, Date lastModified) throws IOException {
        // Do nothing
    }

    public boolean verifyCrc(File file, long crc) throws IOException {
        return true;
    }

    @Override
    public FileMatch verifyMatch(File localFile, OneDriveItem remoteFile) throws IOException 
    {
        // Round to nearest second
        Date remoteCreatedDate = new Date((remoteFile.getCreatedDateTime().getTime() / 1000) * 1000);
        Date remoteLastModifiedDate = new Date((remoteFile.getLastModifiedDateTime().getTime() / 1000) * 1000);

        BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);

        // Timestamp rounded to the nearest second
        Date localCreatedDate = new Date(attr.creationTime().to(TimeUnit.SECONDS) * 1000);
        Date localLastModifiedDate = new Date(attr.lastModifiedTime().to(TimeUnit.SECONDS) * 1000);
        
        long localFileLength = getCommandLineOpts().isEncryptionEnabled() ? 
        		EncryptionProvider.computeEncryptedLength(localFile.length()) : 
        		localFile.length();
        
        boolean sizeMatches = remoteFile.getSize() == localFileLength;
        boolean createdMatches = remoteCreatedDate.equals(localCreatedDate);
        boolean modifiedMatches = remoteLastModifiedDate.equals(localLastModifiedDate);
       
        if (!getCommandLineOpts().useHash() && sizeMatches && createdMatches && modifiedMatches) {
            // Close enough!
            return FileMatch.YES;
        }

        long localCrc = getChecksum(localFile);
        boolean crcMatches = remoteFile.getCrc32() == localCrc;

        // If the crc matches but the timestamps do not we won't upload the content again
        if (crcMatches && !(modifiedMatches && createdMatches)) {
            return FileMatch.CRC;
        } else if (crcMatches) {
            return FileMatch.YES;
        } else {
            return FileMatch.NO;
        }
    }

    public long getChecksum(File file) throws IOException {

        // Compute CRC32 checksum
        CheckedInputStream cis = null;

        try {
            cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
            byte[] buf = new byte[1024];

            //noinspection StatementWithEmptyBody
            while (cis.read(buf) >= 0) {
            }

            return cis.getChecksum().getValue();
        } finally {
            if (cis != null) {
                cis.close();
            }
        }
    }
}
