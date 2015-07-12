package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.sync.CheckFolderTask;
import com.wouterbreukink.onedrive.sync.Task;
import com.wouterbreukink.onedrive.sync.TaskQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final Properties props = new Properties();
    private static final Logger log = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        // Process command line args
        CommandLineOpts opts = new CommandLineOpts(args);

        if (!opts.parse() || opts.isHelp()) {
            opts.printHelp();
            return;
        }

        // Load configuration
        props.loadFromXML(new FileInputStream("onedrive.xml"));
        log.debug("Loaded configuration");

        // Init client
        Client client = ClientBuilder
                .newClient()
                        //.register(new LoggingFilter(Logger.getLogger(LoggingFilter.class.getName()), false))
                .register(MultiPartFeature.class)
                .register(JacksonFeature.class);

        // Workaround to be able to submit PATCH requests
        client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

        OneDriveAuth authoriser = new OneDriveAuth(client, props);

        if (authoriser.getAuthorisation() == null) {
            return;
        }

        OneDriveAPI oneDrive = new OneDriveAPI(client, authoriser);

        Item rootFolder = oneDrive.getPath(opts.getRemotePath());

        if (!rootFolder.isFolder()) {
            log.error(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
            return;
        }

        log.info(String.format("Fetched root folder '%s' - found %d items", rootFolder.getFullName(), rootFolder.getFolder().getChildCount()));

        // Start the queue
        final TaskQueue queue = new TaskQueue();
        queue.add(new CheckFolderTask(queue, oneDrive, rootFolder, new File(opts.getLocalPath())));

        // Get a bunch of threads going
        ExecutorService executorService = Executors.newFixedThreadPool(opts.getThreads());

        for (int i = 0; i < opts.getThreads(); i++) {
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            Task taskToRun = null;
                            try {
                                taskToRun = queue.take();
                                taskToRun.run();
                            } finally {
                                if (taskToRun != null) {
                                    queue.done(taskToRun);
                                }
                            }

                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        queue.waitForCompletion();
        log.info("Finished.");

        System.exit(0);
    }
}
