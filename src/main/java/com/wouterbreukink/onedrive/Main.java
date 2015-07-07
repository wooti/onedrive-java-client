package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.logging.LogFormatter;
import com.wouterbreukink.onedrive.sync.CheckFolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    public static final PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>();
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

        // Start the queue
        Main.queue.add(new CheckFolder(oneDrive, rootFolder, new File("P:\\")));

        // Get a bunch of threads going
        int threads = 6;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            queue.poll(10, TimeUnit.MINUTES).run();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        while (true) {
            Thread.sleep(15000);
            log.info("Queue is of size: " + queue.size());
            if (queue.isEmpty()) {
                return;
            }
        }
    }
}
