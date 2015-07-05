package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.resources.Authorisation;
import com.wouterbreukink.onedrive.resources.Item;
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
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Main {

    public static final Level logLevel = Level.ALL;

    private static final Properties props = new Properties();
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        // Remove existing handlers
        LogManager.getLogManager().reset();

        // Initialise logger
        log.setLevel(logLevel);

        // Set custom handler
        Logger globalLogger = Logger.getLogger("");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        globalLogger.addHandler(handler);

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
        Authorisation auth = authoriser.getAuthorisation();

        if (auth == null) {
            authoriser.printAuthInstructions();
            return;
        }

        log.fine("Fetched authorisation token for user " + auth.getUserId());

        OneDriveClient oneDrive = new OneDriveClient(client, authoriser);

        Item rootFolder = oneDrive.getPath("Pictures");

        if (!rootFolder.isFolder()) {
            log.severe(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
            return;
        }

        log.fine(String.format("Fetched root folder '%s' - found %d items", rootFolder.getFullName(), rootFolder.getFolder().getChildCount()));

        compareFolder(oneDrive, rootFolder, new File("P:\\"));
    }

    public static void compareFolder(OneDriveClient oneDrive, Item remoteFolder, File localFolder) {

        Preconditions.checkNotNull(oneDrive);
        Preconditions.checkNotNull(remoteFolder);
        Preconditions.checkNotNull(localFolder);

        if (!remoteFolder.isFolder()) {
            throw new IllegalArgumentException("Specified folder is not a folder");
        }

        if (!localFolder.isDirectory()) {
            throw new IllegalArgumentException("Specified localFolder is not a folder");
        }

        // Fetch the children
        log.info("Scanning path " + remoteFolder.getFullName());
        Item[] children = oneDrive.getChildren(remoteFolder);

        // Index the local files
        Map<String, File> localFiles = Maps.newHashMap();
        for (File file : localFolder.listFiles()) {
            localFiles.put(file.getName(), file);
        }

        for (Item child : children) {

            File localFile = localFiles.get(child.getName());

            if (localFile != null) {

                if (child.isFolder() != localFile.isDirectory()) {
                    log.warning("CONFLICT!!" + child.getFullName());
                    continue;
                }

                if (child.isFolder()) {
                    compareFolder(oneDrive, child, localFile);
                } else {
                    compareFile(oneDrive, child, localFile);
                }

                localFiles.remove(child.getName());

            } else {
                log.info("TODO Item is extra - Would delete item?: " + child.getFullName());
            }
        }

        // Anything left does not exist on OneDrive

        // Filter stuff we don't want to upload
        // TODO config this out
        localFiles.remove("Thumbs.db");
        localFiles.remove(".picasa.ini");
        localFiles.remove("._.DS_Store");
        localFiles.remove(".DS_Store");

        if (localFiles.size() > 0) {
            log.info("Extra files: " + localFiles.size());
        }

        for (File file : localFiles.values()) {
            if (file.isDirectory()) {
                Item createdItem = oneDrive.createFolder(remoteFolder, file.getName());
                log.info("Created new folder " + createdItem.getFullName());
                compareFolder(oneDrive, createdItem, file);
            } else {
                try {
                    oneDrive.uploadFile(remoteFolder, file);
                } catch (IOException e) {
                    log.warning("Unable to upload file: " + file);
                }
            }
        }
    }

    private static void compareFile(OneDriveClient oneDrive, Item child, File localFile) {
        try {
            BasicFileAttributes attr = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);

            long createdDate = attr.creationTime().toMillis();
            long modifiedDate = attr.lastModifiedTime().toMillis();

            if (child.getSize() == attr.size()
                    && child.getFileSystemInfo().getCreatedDateTime().getTime() == createdDate
                    && child.getFileSystemInfo().getLastModifiedDateTime().getTime() == modifiedDate) {
                // Close enough!
                return;
            }

            // TODO: Skip big files (for now)
            if (localFile.length() > 10 * 1024 * 2014) {
                log.warning("TODO Skipping big file");
                return;
            }

            long remoteCrc = child.getFile().getHashes().getCrc32();
            long localCrc = getChecksum(localFile);

            if (child.getSize() != attr.size()) {
                log.info("Item on OneDrive has different size: " + child.getFullName());
            }

            if (remoteCrc != localCrc) {
                log.info("Item on OneDrive has different CRC32: " + child.getFullName());
            }

            if (child.getFileSystemInfo().getCreatedDateTime().getTime() != attr.creationTime().toMillis()) {
                log.info("Item on OneDrive has different creation time: " + child.getFullName());
            }

            if (child.getFileSystemInfo().getLastModifiedDateTime().getTime() != attr.lastModifiedTime().toMillis()) {
                log.info("Item on OneDrive has different modified time: " + child.getFullName());
            }

            // If the content is different
            if (child.getSize() != attr.size() || remoteCrc != localCrc) {
                log.warning("TODO WOULD UPLOAD NEW FILE HERE");
            } else {
                log.fine("Updating properties on item: " + child.getFullName());
                oneDrive.updateFile(child, new Date(createdDate), new Date(modifiedDate));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long getChecksum(File file) throws IOException {

        // Compute CRC32 checksum
        CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
        byte[] buf = new byte[128];
        while (cis.read(buf) >= 0) {
        }

        return cis.getChecksum().getValue();
    }
}
