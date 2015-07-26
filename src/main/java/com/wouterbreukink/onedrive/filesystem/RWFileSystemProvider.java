package com.wouterbreukink.onedrive.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

class RWFileSystemProvider extends ROFileSystemProvider implements FileSystemProvider {

    private static void removeRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // try to delete the file anyway, even if its attributes
                // could not be read, since delete-only access is
                // theoretically possible
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed; propagate exception
                    throw exc;
                }
            }
        });
    }

    @Override
    public void delete(File file) throws IOException {
        removeRecursive(file.toPath());
    }

    @Override
    public File createFolder(File file, String name) throws IOException {
        File newFolder = new File(file, name);

        if (!newFolder.mkdir()) {
            throw new IOException(String.format("Unable to create local directory '%s' in '%s'", name, file.getName()));
        }

        return newFolder;
    }

    @Override
    public File createFile(File file, String name) throws IOException {
        return new File(file, name);
    }

    @Override
    public void replaceFile(File original, File replacement) throws IOException {
        if (original.exists() && !original.delete()) {
            throw new IOException("Unable to replace local file" + original.getPath());
        }

        if (!replacement.renameTo(original)) {
            throw new IOException("Unable to replace local file" + original.getPath());
        }
    }

    @Override
    public void setAttributes(File downloadFile, Date created, Date lastModified) throws IOException {
        BasicFileAttributeView attributes = Files.getFileAttributeView(downloadFile.toPath(), BasicFileAttributeView.class);
        FileTime createdFt = FileTime.fromMillis(created.getTime());
        FileTime lastModifiedFt = FileTime.fromMillis(lastModified.getTime());
        attributes.setTimes(lastModifiedFt, lastModifiedFt, createdFt);
    }

    @Override
    public boolean verifyCrc(File file, long crc) throws IOException {
        return getChecksum(file) == crc;
    }
}
