package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.logging.LogFormatter;
import jersey.repackaged.com.google.common.base.Preconditions;
import jersey.repackaged.com.google.common.collect.Maps;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    private static final Properties props = new Properties();
    private static final Logger log = Logger.getLogger(Main.class.getPackage().getName());

    public static void main(String[] args) throws Exception {

        // Remove existing handlers
        LogManager.getLogManager().reset();

        // Initialise logger
        log.setLevel(Level.ALL);

        // Set custom handler
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        log.addHandler(handler);

        log.fine("Initialised logging");

        // Load configuration
        props.loadFromXML(new FileInputStream("onedrive.xml"));
        log.fine("Loaded configuration");

        // Init client
        Client client = ClientBuilder
                .newClient()
                        //.register(new LoggingFilter(Logger.getLogger(LoggingFilter.class.getName()), false))
                .register(MultiPartFeature.class)
                .register(JacksonFeature.class);

        // Workaround to be able to submit PATCH requests
        client.property("jersey.config.client.httpUrlConnection.setMethodWorkaround", true);

        OneDriveAuth authoriser = new OneDriveAuth(client, props);

        if (authoriser.getAuthorisation() == null) {
            return;
        }

        OneDriveClient oneDrive = new OneDriveClient(client, authoriser);

        Item rootFolder = oneDrive.getPath("Pictures");

        if (!rootFolder.isFolder()) {
            log.severe(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
            return;
        }

        log.fine(String.format("Fetched root folder '%s' - found %d items", rootFolder.getFullName(), rootFolder.getFolder().getChildCount()));

        compareFolders(oneDrive, rootFolder, new File("P:\\"));
    }

    public static void compareFolders(OneDriveClient oneDrive, Item remoteFolder, File localFolder) {

        Preconditions.checkNotNull(oneDrive);
        Preconditions.checkNotNull(remoteFolder);
        Preconditions.checkNotNull(localFolder);

        if (!remoteFolder.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }

        if (!localFolder.isDirectory()) {
            throw new IllegalArgumentException("Specified localFolder is not a folder");
        }

        // Fetch the remote files
        log.info("Syncing path " + remoteFolder.getFullName());
        Item[] remoteFiles = oneDrive.getChildren(remoteFolder);

        // Index the local files
        Map<String, File> localFiles = Maps.newHashMap();
        for (File file : localFolder.listFiles()) {
            localFiles.put(file.getName(), file);
        }

        for (Item remoteFile : remoteFiles) {

            File localFile = localFiles.get(remoteFile.getName());

            if (localFile != null) {

                if (remoteFile.isFolder() != localFile.isDirectory()) {
                    log.warning(String.format(
                            "Conflict detected in item '%s'. Local is %s, Remote is %s",
                            remoteFile.getFullName(),
                            remoteFile.isFolder() ? "directory" : "file",
                            localFile.isDirectory() ? "directory" : "file"));

                    continue;
                }

                if (remoteFile.isFolder()) {
                    compareFolders(oneDrive, remoteFile, localFile);
                } else {
                    compareFile(oneDrive, remoteFile, localFile);
                }

                localFiles.remove(remoteFile.getName());

            } else {
                log.info("TODO Item is extra - Would delete item?: " + remoteFile.getFullName());
            }
        }

        // Anything left does not exist on OneDrive

        // Filter stuff we don't want to upload
        // TODO config this out
        localFiles.remove("Thumbs.db");
        localFiles.remove(".picasa.ini");
        localFiles.remove("._.DS_Store");
        localFiles.remove(".DS_Store");

        for (File file : localFiles.values()) {
            if (file.isDirectory()) {
                Item createdItem = oneDrive.createFolder(remoteFolder, file.getName());
                log.info("Created new folder " + createdItem.getFullName());
                compareFolders(oneDrive, createdItem, file);
            } else {
                try {
                    oneDrive.uploadFile(remoteFolder, file);
                } catch (IOException e) {
                    log.log(Level.WARNING, "Unable to upload new file", e);
                }
            }
        }
    }

    private static void compareFile(OneDriveClient oneDrive, Item remoteFile, File localFile) {
        try {
            BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);

            boolean sizeMatches = remoteFile.getSize() == localFile.length();
            boolean createdMatches = remoteFile.getFileSystemInfo().getCreatedDateTime().getTime() == attr.creationTime().toMillis();
            boolean modifiedMatches = remoteFile.getFileSystemInfo().getLastModifiedDateTime().getTime() == attr.lastModifiedTime().toMillis();
            if (sizeMatches && createdMatches && modifiedMatches) {
                // Close enough!
                return;
            }

            // TODO: Skip big files (for now)
            if (localFile.length() > 10 * 1024 * 2014) {
                log.warning("TODO Skipping big file");
                return;
            }

            if (!createdMatches) {
                log.info("Item on OneDrive has different creation time: " + remoteFile.getFullName());
            }

            if (!modifiedMatches) {
                log.info("Item on OneDrive has different modified time: " + remoteFile.getFullName());
            }

            long remoteCrc = remoteFile.getFile().getHashes().getCrc32();
            long localCrc = Utils.getChecksum(localFile);

            // If the content is different
            if (remoteCrc != localCrc) {
                log.fine("Uploading new copy of file: " + remoteFile.getFullName());
                oneDrive.replaceFile(remoteFile.getParentReference(), localFile);
            } else if (!createdMatches || !modifiedMatches) {
                log.fine("Updating properties on item: " + remoteFile.getFullName());
                oneDrive.updateFile(remoteFile, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
            }

        } catch (IOException e) {
            e.printStackTrace();
            log.log(Level.WARNING, "Unable to compare file", e);
        }
    }
}
