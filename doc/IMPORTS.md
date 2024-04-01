# Imports
This application supports inclusion of other (HTML) files into an HTML files

## Usage
General syntax:
```html
<!--@import-html file.html-->
```
Comments are used to specify imports in a straightforward way.
Paths work the same as with CSS or JS files, you can use absolute paths and relative ones; URLs can also be used to import files from external sources.
<b>Please note that you can only import stuff below the level of ```<head>``` or ```<body>``` </b>