# Specification of configuration
To be able to run this tool in a directory and compile it, you need to add a configuration ```wmake.json``` file

### Example (not defaults)
```json
{
  "minifier": {
    "options": [
      "--minify-js",
      "--minify-css",
      "--collapse-whitespace"
    ]
  },
  "make": {
    "mode": "manual",
    "manual": ["index.html"],
    "ignore_hidden": true,
    "threads": 3,
    
    "hooks": {
      "pre_build": [],
      "post_build": []
    },
    "hook_runner": ["/bin/bash", "-c"],
    
    "other": {
      "mode": "manual",
      "manual": ["data", "favicon.ico"],
      "manual_mode": "ALL",
      "minify_json": true
    }
  }
}
```

## ```minifier```
The minifier object of the configuration requires only one node: ```options```
#### ```options``` The JSON array that contains the parameters to pass over to the minifier

## ```make```
The make configuration is responsible for specifying the files to compile or include in the built project
#### ```mode``` The way HTML files are chosen
 - ```traverse```
 - ```root_only```
 - ```manual```
#### ```manual``` (optional) The HTML files/dirs (traversed) to be compiled
#### ```ignore_hidden``` Whether to ignore files that have the hidden status in the filesystem or start with a dot
#### ```threads``` The total number of threads usable while compiling

<br>

#### ```hooks```
 - ```pre_build``` Ran before starting the build
 - ```post_build``` Ran after finishing the build
#### ```hook_runner``` The command to prepend all hook commands with (e.g.: bash, zsh)

<br>

### ```other``` The configuration for including other files
#### ```mode``` The way other files are chosen
 - ```all```
 - ```no_site```
 - ```assets```
 - ```manual```
 - ```none```
#### ```manual``` (optional) The other files/dirs to be included
#### ```manual_mode``` The way manual files are chosen
 - ```all```
 - ```no_site```
 - ```assets```
#### ```minify_json``` Whether to simply minify all included JSON files