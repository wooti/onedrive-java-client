package com.wouterbreukink.onedrive;

import com.google.api.client.util.Sets;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class CommandLineOpts {

    private static final Logger log = LogManager.getLogger(Main.class.getName());

    private static final Options optionsToParse = buildOptions();
    private static final CommandLineOpts opts = new CommandLineOpts();
    private boolean isInitialised;

    // Mandatory arguments
    private Direction direction;
    private String localPath;
    private String remotePath;

    // Optional arguments
    private boolean help = false;
    private boolean useHash = false;
    private int threads = 5;
    private int tries = 3;
    private boolean version = false;
    private boolean recursive = false;
    private int maxSizeKb = 0;
    private Path keyFile = Paths.get("onedrive.key");
    private boolean dryRun = false;
    private String logFile = null;
    private int splitAfter = 5;
    private Set<String> ignored = null;
    private boolean authorise = false;

    public static CommandLineOpts getCommandLineOpts() {
        if (!opts.isInitialised) {
            throw new IllegalStateException("The command line options have not been initialised");
        }
        return opts;
    }

    public static void initialise(String[] args) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(optionsToParse, args);

        for (Option opt : line.getOptions()) {
            log.debug(String.format("Parsing command line option -%s, value = %s ",
                    opt.getLongOpt() != null ? "-" + opt.getLongOpt() : opt.getOpt(),
                    opt.getValue()));
        }

        opts.help = line.hasOption("help");
        opts.useHash = line.hasOption("hash-compare");
        opts.version = line.hasOption("version");
        opts.recursive = line.hasOption("recursive");
        opts.dryRun = line.hasOption("dry-run");
        opts.authorise = line.hasOption("authorise");

        if (line.hasOption("local")) {
            opts.localPath = line.getOptionValue("local");
        }

        if (line.hasOption("remote")) {
            opts.remotePath = line.getOptionValue("remote");
        }

        if (line.hasOption("direction")) {
            String chosen = line.getOptionValue("direction").toLowerCase();
            if (!chosen.equals("up") && !chosen.equals("down")) {
                throw new ParseException("Direction must be one of up or down");
            }
            opts.direction = Direction.valueOf(chosen.toUpperCase());
        }

        if (line.hasOption("threads")) {
            opts.threads = Integer.parseInt(line.getOptionValue("threads"));
        }

        if (line.hasOption("tries")) {
            opts.tries = Integer.parseInt(line.getOptionValue("tries"));
        }

        if (line.hasOption("max-size")) {
            opts.maxSizeKb = Integer.parseInt(line.getOptionValue("max-size"));
        }

        if (line.hasOption("keyfile")) {
            opts.keyFile = Paths.get(line.getOptionValue("keyfile"));
        }

        if (line.hasOption("logfile")) {
            opts.logFile = line.getOptionValue("logfile");
        }

        if (line.hasOption("split-after")) {
            opts.splitAfter = Integer.parseInt(line.getOptionValue("split-after"));

            if (opts.splitAfter > 60) {
                throw new ParseException("maximum permissible value for split-after is 60");
            }
        }

        if (line.hasOption("ignore")) {
            Path ignoreFile = Paths.get(line.getOptionValue("ignore"));
            if (!Files.exists(ignoreFile)) {
                throw new ParseException("specified ignore file does not exist");
            }

            try {
                opts.ignored = Sets.newHashSet();
                opts.ignored.addAll(Files.readAllLines(ignoreFile, Charset.defaultCharset()));
            } catch (IOException e) {
                throw new ParseException(e.getMessage());
            }
        }

        opts.isInitialised = true;
    }

    private static Options buildOptions() {
        Option authorise = Option.builder("a")
                .longOpt("authorise")
                .desc("generate authorisation url")
                .build();

        Option hash = Option.builder("c")
                .longOpt("hash-compare")
                .desc("always compare files by hash")
                .build();

        Option direction = Option.builder()
                .longOpt("direction")
                .hasArg()
                .argName("up|down")
                .desc("direction of synchronisation.")
                .build();

        Option help = Option.builder("h")
                .longOpt("help")
                .desc("print this message")
                .build();

        Option ignore = Option.builder("i")
                .longOpt("ignore")
                .hasArg()
                .argName("ignore_file")
                .desc("ignore entry file")
                .build();

        Option keyFile = Option.builder("k")
                .longOpt("keyfile")
                .hasArg()
                .argName("file")
                .desc("key file to use")
                .build();

        Option logLevel = Option.builder("L")
                .longOpt("log-level")
                .hasArg()
                .argName("level (1-7)")
                .desc("controls the verbosity of logging")
                .build();

        Option localPath = Option.builder()
                .longOpt("local")
                .hasArg()
                .argName("path")
                .desc("the local path")
                .build();

        Option logFile = Option.builder()
                .longOpt("logfile")
                .hasArg()
                .argName("file")
                .desc("log to file")
                .build();

        Option maxSize = Option.builder("M")
                .longOpt("max-size")
                .hasArg()
                .argName("size_in_KB")
                .desc("only process files smaller than <size> KB")
                .build();

        Option dryRun = Option.builder("n")
                .longOpt("dry-run")
                .desc("only do a dry run without making changes")
                .build();

        Option recursive = Option.builder("r")
                .longOpt("recursive")
                .desc("recurse into directories")
                .build();

        Option remotePath = Option.builder()
                .longOpt("remote")
                .hasArg()
                .argName("path")
                .desc("the remote path on OneDrive")
                .build();

        Option splitAfter = Option.builder("s")
                .longOpt("split-after")
                .hasArg()
                .argName("size_in_MB")
                .desc("use multi-part upload for big files")
                .build();

        Option threads = Option.builder("t")
                .longOpt("threads")
                .hasArg()
                .argName("count")
                .desc("number of threads to use")
                .build();

        Option version = Option.builder("v")
                .longOpt("version")
                .desc("print the version information and exit")
                .build();

        Option retries = Option.builder("y")
                .longOpt("tries")
                .hasArg()
                .argName("count")
                .desc("try each service request <count> times")
                .build();

        return new Options()
                .addOption(authorise)
                .addOption(hash)
                .addOption(direction)
                .addOption(help)
                .addOption(ignore)
                .addOption(keyFile)
                .addOption(logLevel)
                .addOption(localPath)
                .addOption(logFile)
                .addOption(maxSize)
                .addOption(dryRun)
                .addOption(recursive)
                .addOption(remotePath)
                .addOption(splitAfter)
                .addOption(threads)
                .addOption(version)
                .addOption(retries);
    }

    public static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("onedrive-java-syncer", optionsToParse);
    }

    public Direction getDirection() {
        return direction;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public boolean help() {
        return help;
    }

    public int getThreads() {
        return threads;
    }

    public boolean useHash() {
        return useHash;
    }

    public int getTries() {
        return tries;
    }

    public boolean version() {
        return version;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public int getMaxSizeKb() {
        return maxSizeKb;
    }

    public Path getKeyFile() {
        return keyFile;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public String getLogFile() {
        return logFile;
    }

    public int getSplitAfter() {
        return splitAfter;
    }

    public Set<String> getIgnored() {
        return ignored;
    }

    public boolean isAuthorise() {
        return authorise;
    }

    public enum Direction {
        UP,
        DOWN
    }
}
