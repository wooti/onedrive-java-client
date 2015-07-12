package com.wouterbreukink.onedrive;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandLineOpts {

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
    private Path keyFile = Paths.get("keyFile.txt");

    public static CommandLineOpts getCommandLineOpts() {
        if (!opts.isInitialised) {
            throw new IllegalStateException("The command line options have not been initialised");
        }
        return opts;
    }

    public static void initialise(String[] args) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(optionsToParse, args);

        opts.help = line.hasOption("help");
        opts.useHash = line.hasOption("hash-compare");
        opts.version = line.hasOption("version");
        opts.recursive = line.hasOption("recursive");

        if (line.hasOption("local")) {
            opts.localPath = line.getOptionValue("local");
        }

        if (line.hasOption("remote")) {
            opts.remotePath = line.getOptionValue("remote");
        }

        if (line.hasOption("direction")) {
            String chosen = line.getOptionValue("direction").toLowerCase();
            if (!chosen.equals("up") && !chosen.equals("down") && !chosen.equals("sync")) {
                throw new ParseException("Direction must be one of up, down or sync");
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

        // TODO: LogLevel
        // TODO: LogFile
        // TODO: Dry run
        // TODO: Conflict

        opts.isInitialised = true;
    }

    private static Options buildOptions() {
        Option hash = Option.builder("c")
                .longOpt("hash-compare")
                .desc("always compare files by hash")
                .build();

        Option direction = Option.builder()
                .longOpt("direction")
                .hasArg()
                .argName("up|down|sync")
                .desc("direction of synchronisation.")
                .required()
                .build();

        Option help = Option.builder("h")
                .longOpt("help")
                .desc("print this message")
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
                .required()
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
                .required()
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

        Option conflict = Option.builder("x")
                .longOpt("conflict")
                .hasArg()
                .argName("L|R|B|S")
                .desc("conflict resolution by Local file, Remote file, Both files or Skipping the file.")
                .build();

        Option retries = Option.builder("y")
                .longOpt("tries")
                .hasArg()
                .argName("count")
                .desc("try each service request <count> times")
                .build();

        return new Options()
                .addOption(hash)
                .addOption(direction)
                .addOption(help)
                .addOption(keyFile)
                .addOption(logLevel)
                .addOption(localPath)
                .addOption(logFile)
                .addOption(maxSize)
                .addOption(dryRun)
                .addOption(recursive)
                .addOption(remotePath)
                .addOption(threads)
                .addOption(version)
                .addOption(conflict)
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

    public enum Direction {
        UP,
        DOWN,
        BOTH
    }

}
