package com.wouterbreukink.onedrive;

import org.apache.commons.cli.*;

public class CommandLineOpts {

    private static final Options optionsToParse = buildOptions();
    private static final CommandLineOpts opts = new CommandLineOpts();
    private boolean isInitialised;
    private String direction;
    private String localPath;
    private String remotePath;
    private boolean help = false;
    private int threads = 5;
    private boolean useHash;

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

        if (line.hasOption("local")) {
            opts.localPath = line.getOptionValue("local");
        }

        if (line.hasOption("remote")) {
            opts.remotePath = line.getOptionValue("remote");
        }

        if (line.hasOption("direction")) {
            opts.direction = line.getOptionValue("direction");
        }

        if (line.hasOption("threads")) {
            opts.threads = Integer.parseInt(line.getOptionValue("threads"));
        }

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
                .argName("size")
                .desc("only process files smaller than <size>")
                .build();

        Option minSize = Option.builder("m")
                .longOpt("max-size")
                .hasArg()
                .argName("size")
                .desc("only process files larger than <size>")
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
                .longOpt("retries")
                .hasArg()
                .argName("count")
                .desc("retry each service request <count> times")
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
                .addOption(minSize)
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

    public String getDirection() {
        return direction;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public boolean isHelp() {
        return help;
    }

    public int getThreads() {
        return threads;
    }

    public boolean useHash() {
        return useHash;
    }
}
