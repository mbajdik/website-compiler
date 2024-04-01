# website-compiler
### A really simple Kotlin application to compile and build static web-applications

## Usage
See the specific files for [imports](doc/IMPORTS.md), [configuration](doc/CONFIGURATION.md) and [CLI usage](doc/CLI.md)

## Installation, dependencies
### If you don't want to compile
You can use the pre-packaged ```.jar``` files found in the Releases page, or you can use the Linux compatible sh-banged (```#!/bin/java -jar```) executable
#### <b>Please note that this project uses the Java version specified in the Maven config: ```17```</b>
#### For your information, this project was only tested on Linux
### To compile
The Maven build system is used to compile this project into ```.jar``` files with or without dependencies
### Dependencies
Since the distributed executables are compiled with dependencies, you won't need to download any libraries
<br><br>
<i>But</i> in order to use the minify feature of this application you will need the ```html-minifier``` NodeJS package to be in your PATH

## Contributing
I made this project to ease my work, the time I uploaded it I deemed it mostly feature complete. Saying this I'm sorry to tell You, but I don't need any help in this project