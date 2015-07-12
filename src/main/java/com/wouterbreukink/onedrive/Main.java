package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.sync.CheckFolderTask;
import com.wouterbreukink.onedrive.sync.Task;
import com.wouterbreukink.onedrive.sync.TaskQueue;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        // Parse command line args
        try {
            CommandLineOpts.initialise(args);
        } catch (ParseException ex) {
            log.error("Unable to parse command line arguments - " + ex.getMessage());
            return;
        }

        if (getCommandLineOpts().help()) {
            CommandLineOpts.printHelp();
            return;
        }

        if (getCommandLineOpts().version()) {
            log.info("onedrive-java-client version ALPHA");
        }

        // Initialise the client
        Client client = ClientBuilder
                .newClient()
                        //.register(new LoggingFilter(Logger.getLogger(LoggingFilter.class.getName()), false))
                .register(MultiPartFeature.class)
                .register(JacksonFeature.class);

        // Workaround to be able to submit PATCH requests
        client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

        // Initialise the OneDrive Authoriser
        OneDriveAuth authoriser = new OneDriveAuth(client);
        if (!authoriser.initialise(getCommandLineOpts().getKeyFile())) {
            authoriser.printAuthInstructions();
            return;
        }

        // Initialise the OneDrive API
        OneDriveAPI api = new OneDriveAPI(client, authoriser);

        // Warning for dry Run
        if (getCommandLineOpts().isDryRun()) {
            log.warn("This is a dry run - no changes will be made");
        }

        // Check the given root folder
        Item rootFolder = api.getPath(getCommandLineOpts().getRemotePath());

        if (!rootFolder.isFolder()) {
            log.error(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
            return;
        }

        log.info(String.format("Fetched root folder '%s' - found %d items", rootFolder.getFullName(), rootFolder.getFolder().getChildCount()));

        // Start synchronisation operation at the root
        final TaskQueue queue = new TaskQueue();
        queue.add(new CheckFolderTask(queue, api, rootFolder, new File(getCommandLineOpts().getLocalPath())));

        // Get a bunch of threads going
        ExecutorService executorService = Executors.newFixedThreadPool(getCommandLineOpts().getThreads());

        for (int i = 0; i < getCommandLineOpts().getThreads(); i++) {
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        //noinspection InfiniteLoopStatement
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
