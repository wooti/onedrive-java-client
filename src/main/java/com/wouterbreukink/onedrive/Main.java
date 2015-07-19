package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAPI;
import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.ROOneDriveAPI;
import com.wouterbreukink.onedrive.client.RWOneDriveAPI;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.fs.FileSystemProvider;
import com.wouterbreukink.onedrive.fs.ROFileSystemProvider;
import com.wouterbreukink.onedrive.fs.RWFileSystemProvider;
import com.wouterbreukink.onedrive.tasks.CheckTask;
import com.wouterbreukink.onedrive.tasks.Task;
import com.wouterbreukink.onedrive.tasks.TaskReporter;
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

/***
 * OneDrive Java Client
 * Copyright (C) 2015 Wouter Breukink
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
public class Main {

    private static final Logger log = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        // Parse command line args
        try {
            CommandLineOpts.initialise(args);
        } catch (ParseException ex) {
            log.error("Unable to parse command line arguments - " + ex.getMessage());
            CommandLineOpts.printHelp();
            return;
        }

        if (getCommandLineOpts().help()) {
            CommandLineOpts.printHelp();
            return;
        }

        if (getCommandLineOpts().version()) {
            String version = getCommandLineOpts().getClass().getPackage().getImplementationVersion();
            log.info("onedrive-java-client version " + (version != null ? version : "DEVELOPMENT"));
            return;
        }

        // Initialise a log file (if set)
        if (getCommandLineOpts().getLogFile() != null) {
            String logFileName = LogUtils.addFileLogger(getCommandLineOpts().getLogFile());
            log.info(String.format("Writing log output to %s", logFileName));
        }

        // Initialise the client
        Client client = ClientBuilder
                .newClient()
                        //.register(new LoggingFilter(Logger.getLogger(LoggingFilter.class.getName()), false))
                .register(MultiPartFeature.class)
                .register(JacksonFeature.class);

        // Workaround to be able to submit PATCH requests
        client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        // Initialise the OneDrive Authoriser
        OneDriveAuth authoriser = new OneDriveAuth(client);
        if (!authoriser.initialise(getCommandLineOpts().getKeyFile())) {
            authoriser.printAuthInstructions();
            return;
        }

        // Initialise the OneDrive API
        OneDriveAPI api;
        FileSystemProvider fileSystem;
        if (getCommandLineOpts().isDryRun()) {
            log.warn("This is a dry run - no changes will be made");
            api = new ROOneDriveAPI(client, authoriser);
            fileSystem = new ROFileSystemProvider();
        } else {
            api = new RWOneDriveAPI(client, authoriser);
            fileSystem = new RWFileSystemProvider();
        }

        // Report on progress
        TaskReporter reporter = new TaskReporter();

        // Check the given root folder
        Item rootFolder = api.getPath(getCommandLineOpts().getRemotePath());

        if (!rootFolder.isFolder()) {
            log.error(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
            return;
        }

        log.info(String.format("Fetched root folder '%s' - found %d items", rootFolder.getFullName(), rootFolder.getFolder().getChildCount()));

        // Start synchronisation operation at the root
        final TaskQueue queue = new TaskQueue();
        queue.add(new CheckTask(new Task.TaskOptions(queue, api, fileSystem, reporter), rootFolder, new File(getCommandLineOpts().getLocalPath())));

        // Get a bunch of threads going
        ExecutorService executorService = Executors.newFixedThreadPool(getCommandLineOpts().getThreads());

        for (int i = 0; i < getCommandLineOpts().getThreads(); i++) {
            executorService.submit(new Runnable() {
                @Override
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
        log.info("Synchronisation complete - summary:");
        reporter.report();

        System.exit(0);
    }
}
