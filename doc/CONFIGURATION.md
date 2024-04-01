# Specification of configuration
To be able to run this tool in a directory and compile it, you need to add a configuration ```wmake.json``` file

### Example (not defaults)
```json
{
  "minifier": {
    "minify_js": false,
    "node_path": "/bin/node"
  },
  "compiler": {
    "add_css": ["/style.css"]
  },
  "make": {
    "root": "src",
    "output_type": "dir",
    "output": "build",
    
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

## ```minifier``` Options related to running ```html-minifier```
#### ```minify_js``` Whether to minify JavaScript files
#### ```minify_css``` Whether to minify CSS files
#### ```node_path``` Absolute path to the NodeJS binary
#### ```minifier_path``` Absolute path to the html-minifier script
These paths might include a ```~``` which will be substituted with the ```user.home``` property

## ```compiler``` HTML compiler settings
#### ```add_js``` JavaScript sources to add to the header in every HTML file (header is made if it doesn't exist)
#### ```add_css``` Same, but with CSS files
It's a good practice to add the ```/``` at the beginning of the paths, since that would indicate an absolute path
#### ```footer_html``` (nullable) Text to add at the end of the first ```<body>``` tag in a ```<footer>``` tag
#### ```auto_title``` Whether to add an ```<h1>``` tag to the body containing the contents of the first occurring ```<title>``` tag
 - ```none``` completely off
 - ```h1``` insert an ```h1``` tag if needed
 - ```title``` insert a ```title``` tag if needed
 - ```both``` automatically detect the presence of each, and insert the other if it doesn't exist

## ```make```
The make configuration is responsible for specifying the files to compile or include in the built project
#### ```root``` The project root (e.g.: ```src```)
#### ```output_type``` The way the built project is outputted
 - ```dir```
 - ```zip```
#### ```output```
#### ```mode``` The way HTML files are chosen
 - ```recurse```
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