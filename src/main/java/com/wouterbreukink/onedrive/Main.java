package com.wouterbreukink.onedrive;

import com.wouterbreukink.onedrive.client.OneDriveAuth;
import com.wouterbreukink.onedrive.client.OneDriveClient;
import com.wouterbreukink.onedrive.client.resources.Item;
import com.wouterbreukink.onedrive.logging.LogFormatter;
import com.wouterbreukink.onedrive.sync.SyncFolderTask;
import com.wouterbreukink.onedrive.sync.Task;
import jersey.repackaged.com.google.common.collect.Maps;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
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

    public static final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<Task>();
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

        // Process command line args
        CommandLineOpts opts = new CommandLineOpts(args);

        if (!opts.parse() || opts.isHelp()) {
            opts.printHelp();
            return;
        }

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
        client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

        OneDriveAuth authoriser = new OneDriveAuth(client, props);

        if (authoriser.getAuthorisation() == null) {
            return;
        }

        OneDriveClient oneDrive = new OneDriveClient(client, authoriser);

        Item rootFolder = oneDrive.getPath(opts.getRemotePath());

        if (!rootFolder.isFolder()) {
            log.severe(String.format("Specified root '%s' is not a folder", rootFolder.getFullName()));
            return;
        }

        log.fine(String.format("Fetched root folder '%s' - found %d items", rootFolder.getFullName(), rootFolder.getFolder().getChildCount()));

        // Start the queue
        Main.queue.add(new SyncFolderTask(oneDrive, rootFolder, new File(opts.getLocalPath())));

        // Get a bunch of threads going
        ExecutorService executorService = Executors.newFixedThreadPool(opts.getThreads());

        for (int i = 0; i < opts.getThreads(); i++) {
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        Task nextTask;
                        while ((nextTask = queue.poll(30, TimeUnit.SECONDS)) != null) {
                            nextTask.run();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        while (true) {
            Thread.sleep(60000);
            Task[] tasks = queue.toArray(new Task[1]);
            Map<String, Integer> map = Maps.newHashMap();

            for (Task task : tasks) {
                Integer value = map.get(task.name());
                if (value == null) {
                    map.put(task.name(), 1);
                } else {
                    map.put(task.name(), value + 1);
                }
            }

            log.info(String.format("Queue contains %d tasks:", tasks.length));
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                log.info(String.format("%d tasks of type %s", entry.getValue(), entry.getKey()));
            }

            if (tasks.length == 0) {
                return;
            }
        }
    }
}
