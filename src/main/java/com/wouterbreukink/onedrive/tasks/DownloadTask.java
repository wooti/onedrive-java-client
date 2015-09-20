package com.wouterbreukink.onedrive.tasks;

import com.google.api.client.util.Preconditions;
import com.wouterbreukink.onedrive.Utils;
import com.wouterbreukink.onedrive.client.OneDriveItem;
import com.wouterbreukink.onedrive.encryption.EncryptionException;
import com.wouterbreukink.onedrive.encryption.EncryptionProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;
import static com.wouterbreukink.onedrive.LogUtils.readableFileSize;
import static com.wouterbreukink.onedrive.LogUtils.readableTime;

public class DownloadTask extends Task {

    private static final Logger log = LogManager.getLogger(UploadTask.class.getName());
    private final File parent;
    private final OneDriveItem remoteFile;
    private final String remoteFilename;
    private final boolean replace;

    public DownloadTask(TaskOptions options, File parent, OneDriveItem remoteFile, boolean replace) throws IOException {

        super(options);

        this.parent = Preconditions.checkNotNull(parent);
        this.remoteFile = Preconditions.checkNotNull(remoteFile);
        this.replace = Preconditions.checkNotNull(replace);
        
        if (getCommandLineOpts().isEncryptionEnabled())
        {
        	
        	try {
				remoteFilename = EncryptionProvider.getEncryptionProvider()
						.decryptFilename(remoteFile.getName());
			} catch (EncryptionException e) {
				throw new IOException(String.format("Download of file '%s' failed (Cannot decrypt filename)", remoteFile.getFullName()));
			}
        	
        	
        }
        else
        	remoteFilename = remoteFile.getName(); 


        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Specified parent is not a folder");
        }
    }

    public int priority() {
        return 50;
    }

    @Override
    public String toString() {
        return "Download " + remoteFile.getFullName();
    }

    @Override
    protected void taskBody() throws IOException {

        if (isIgnored(remoteFile)) {
            reporter.skipped();
            return;
        }

        if (remoteFile.isDirectory()) {

            File newParent = fileSystem.createFolder(parent, remoteFilename);

            for (OneDriveItem item : api.getChildren(remoteFile)) {
                queue.add(new DownloadTask(getTaskOptions(), newParent, item, false));
            }

        } else {

            if (isSizeInvalid(remoteFile)) {
                reporter.skipped();
                return;
            }

            long startTime = System.currentTimeMillis();

            File downloadFile = null;

            try {
                downloadFile = fileSystem.createFile(parent, remoteFilename + ".tmp");

                api.download(remoteFile, downloadFile);

                long elapsedTime = System.currentTimeMillis() - startTime;

                // Do a CRC check on the downloaded file
                if (!fileSystem.verifyCrc(downloadFile, remoteFile.getCrc32())) {
                    throw new IOException(String.format("Download of file '%s' failed", remoteFile.getFullName()));
                }

                fileSystem.setAttributes(
                        downloadFile,
                        remoteFile.getCreatedDateTime(),
                        remoteFile.getLastModifiedDateTime());
                
                if (getCommandLineOpts().isEncryptionEnabled())
                	fileSystem.replaceAndDecryptFile(new File(parent, remoteFilename), downloadFile);
                else
                	fileSystem.replaceFile(new File(parent, remoteFilename), downloadFile);
                
                log.info(String.format("Downloaded %s in %s (%s/s) to %s file <local>/%s from file <onedrive>%s",
            		readableFileSize(remoteFile.getSize()),
                    readableTime(elapsedTime),
                    elapsedTime > 0 ? readableFileSize(remoteFile.getSize() / (elapsedTime / 1000d)) : 0,
                    replace ? "replace" : "new",
                    Utils.getLocalRelativePath(downloadFile),
                    remoteFile.getFullName()));
                
                reporter.fileDownloaded(replace, remoteFile.getSize());
            } catch (Throwable e) {
                if (downloadFile != null) {
                    if (!downloadFile.delete()) {
                        log.warn("Unable to remove temporary file " + downloadFile.getPath());
                    }
                }

                throw e;
            }
        }
    }
}

