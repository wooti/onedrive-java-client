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

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

class ROFileSystemProvider implements FileSystemProvider {

    public void delete(File file) throws IOException {
        // Do nothing
    }

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

    public void setAttributes(File downloadFile, Date created, Date lastModified) throws IOException {
        // Do nothing
    }

    public boolean verifyCrc(File file, long crc) throws IOException {
        return true;
    }

    public FileMatch verifyMatch(File file, long crc, long fileSize, Date created, Date lastModified) throws IOException {

        // Round to nearest second
        created = new Date((created.getTime() / 1000) * 1000);
        lastModified = new Date((lastModified.getTime() / 1000) * 1000);

        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        // Timestamp rounded to the nearest second
        Date localCreatedDate = new Date(attr.creationTime().to(TimeUnit.SECONDS) * 1000);
        Date localModifiedDate = new Date(attr.lastModifiedTime().to(TimeUnit.SECONDS) * 1000);

        boolean sizeMatches = fileSize == file.length();
        boolean createdMatches = created.equals(localCreatedDate);
        boolean modifiedMatches = lastModified.equals(localModifiedDate);

        if (!getCommandLineOpts().useHash() && sizeMatches && createdMatches && modifiedMatches) {
            // Close enough!
            return FileMatch.YES;
        }

        long localCrc = getChecksum(file);
        boolean crcMatches = crc == localCrc;

        // If the crc matches but the timestamps do not we won't upload the content again
        if (crcMatches && !(modifiedMatches && createdMatches)) {
            return FileMatch.CRC;
        } else if (crcMatches) {
            return FileMatch.YES;
        } else {
            return FileMatch.NO;
        }
    }

    public FileMatch verifyMatch(File file, Date created, Date lastModified) throws IOException {

        // Round to nearest second
        created = new Date((created.getTime() / 1000) * 1000);
        lastModified = new Date((lastModified.getTime() / 1000) * 1000);

        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        // Timestamp rounded to the nearest second
        Date localCreatedDate = new Date(attr.creationTime().to(TimeUnit.SECONDS) * 1000);
        Date localModifiedDate = new Date(attr.lastModifiedTime().to(TimeUnit.SECONDS) * 1000);

        boolean createdMatches = created.equals(localCreatedDate);
        boolean modifiedMatches = lastModified.equals(localModifiedDate);

        if (createdMatches && modifiedMatches) {
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
