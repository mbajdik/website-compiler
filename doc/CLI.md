# Using the Command Line Interface

## Main entrypoint
```
Usage: wmake [make|build|new|create|compile] [SUBCOMMAND OPTIONS]

DEFAULT SUBCOMMAND: make (automatically executed if in a project)

COMMANDS
    make, build:    Builds a wmake project
    new, create:    Dumps the default configuration - usage: "wmake new [location?]"
    compile:        Compiles a single file
    help:           Displays this message
```

### Compile (```compile```)
```
usage: wmake compile [OPTIONS...] [FILE]
 -h,--help             Display this message
 -l,--loglevel <arg>   Specify the log level (e.g.: for errors only: 0)
    --logfile <arg>    The file to save the log to
 -n,--no-minify        Don't minify the output of the HTML compiler
    --no-minify-css    Don't minify the CSS in the compiled HTML
    --no-minify-js     Don't minify the JavaScript in the compiled HTML
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
    --no-log           Fully disables logging
 -o,--save-dir <arg>   The directory to save the output to, if not in
                       wmake.json (otherwise in [root]/.target/)
 -q,--quiet            Will not output anything unless an error occurs
 -r,--root <arg>       The project root, wmake.json is used by default (or
                       the CWD if it's not supplied)
 -z,--zip <arg>        The zip file to save the output to, otherwise saved
                       in a directory
```