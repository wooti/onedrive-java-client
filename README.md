onedrive-java-client
==========

A Microsoft OneDrive command line client written in Java 7.

## Usage
```bash
usage: onedrive-java-syncer
 -c,--hash-compare               always compare files by hash
    --direction <up|down>        direction of synchronisation.
 -h,--help                       print this message
 -i,--ignore <ignore_file>       ignore entry file
 -k,--keyfile <file>             key file to use
 -L,--log-level <level (1-7)>    controls the verbosity of logging
    --local <path>               the local path
    --logfile <file>             log to file
 -M,--max-size <size_in_KB>      only process files smaller than <size> KB
 -n,--dry-run                    only do a dry run without making changes
 -r,--recursive                  recurse into directories
    --remote <path>              the remote path on OneDrive
 -s,--split-after <size_in_MB>   use multi-part upload for big files
 -t,--threads <count>            number of threads to use
 -v,--version                    print the version information and exit
 -y,--tries <count>              try each service request <count> times
```

## Setup

(1) Ensure you have a Java Runtime installed

(2) Download the latest release

(3) Authorise the application

## Synchronisation Modes

The application currently supports one-way mirroring of data, so it will create, update and delete files as necessary to make the target side match the source side.
* UP - Mirrors a local folder to a OneDrive folder
* DOWN - Mirrors a OneDrive folder to a local folder

### Moving / Renaming Files

The client does not currently maintain a local record of changes, so renamed and moved files are detected as brand new files and uploaded. The old file will be deleted.

### Large Files

For files larger than 5MB (configurable with ``--split-after``), onedrive-java-client will split the upload into blocks of 5MB. This reduces the cost of a temporary network failure, as at most the last 5MB of any upload will need to be re-sent.

### Data Integrity

By default files are compared by looking at the size, created date and last modified date. For additional safety the ``--hash-compare`` flag can be specified which forces a CRC32 hash check for each file.

The ``--dry-run`` option can be used to test the synchronisation operation, this executes the operation without applying any changes.

## Libraries Used

The following libraries have been used
* [Jersey](https://jersey.java.net/) - HTTP REST Client
* [Jackson](https://github.com/FasterXML/jackson) - JSON Parser
* [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) - Command line arguments parser
* [Apache Log4j2](http://logging.apache.org/log4j/2.x/) - Logging framework
* [Guava](https://github.com/google/guava) - Google core libraries

## License

See [license](LICENSE.md) license.