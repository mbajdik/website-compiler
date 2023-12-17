# Using the Command Line Interface

## Main entrypoint
```
Usage: wmake [make|build|compile] [SUBCOMMAND OPTIONS]

DEFAULT SUBCOMMAND: make

COMMANDS
    make, build:    Builds a wmake project
    compile:        Compiles a single file
    help:           Displays this message
```

### Compile (```compile```)
```
usage: wmake compile [OPTIONS...] [FILE]
 -h,--help             Display this message
 -l,--loglevel <arg>   Specify the log level (e.g.: for errors only: 0)
    --logfile <arg>    The file to save the log to
 -m,--options <arg>    Options to pass over to the minifier
 -n,--no-minify        Don't minify the output of the HTML compiler
 -o,--save <arg>       Where to save the output, STDOUT is used by default
 -r,--root <arg>       The project root, CWD is used by default
```

### Make (```make```, ```build```)
```
usage: wmake make [OPTIONS...] [LOCATION]
 -h,--help             Display this message
 -l,--loglevel <arg>   Specify the log level (e.g.: for errors only: 0)
    --logfile <arg>    The file to save the log to, otherwise
                       ([root]/.wmake/logs/[date].log
 -o,--save-dir <arg>   The directory to save the output to, if not in
                       wmake.json (otherwise in [root]/.target/)
 -r,--root <arg>       The project root, wmake.json is used by default (or
                       the CWD if it's not supplied)
 -z,--zip <arg>        The zip file to save the output to, otherwise saved
                       in a directory
```