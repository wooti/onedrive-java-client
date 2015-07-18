package com.wouterbreukink.onedrive.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class ROFileSystemProvider implements FileSystemProvider {

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

    public long getChecksum(File file) throws IOException {

        // Compute CRC32 checksum
        CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
        byte[] buf = new byte[1024];

        //noinspection StatementWithEmptyBody
        while (cis.read(buf) >= 0) {
        }

        return cis.getChecksum().getValue();
    }
}
