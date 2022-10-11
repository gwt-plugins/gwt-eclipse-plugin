# GWT Eclipse Plugin

This repository houses the source for the GWT Eclipse Plugin build with nobuto. 

This version has been created, because there is no official version any more.
  
* See Issues here: https://github.com/gwt-plugins/gwt-eclipse-plugin/issues

The current version installs on Windows and Linux with Eclipse 2021-12 up to Eclipse 2022-09 (see Eclipse-2022-06 and newer if you have problems with installing). Other versions aren't testet.

## Quality
* This plugin is Beta Quality. 

## Features
* GWT SDK Selection
* GWT Compiler Launcher
* GWT Development Mode with Jetty Launcher (a.k.a DevMode Super Dev Mode)
* GWT Development Mode Launcher (a.k.a CodeServer Super Dev Mode)
* GWT Legacy Development Mode Launcher (a.k.a Classic Dev Mode or OOPHM Dev Mode)
* GWT Java Editor
* GWT UIBinder Editor
* OOPHM for Legacy Dev Mode
* CSS Resources Editor
* Maven integration

## Install
To install just download the zip file from the release, and add it to the Eclipse software sites.
### Eclipse-2022-06 and newer
You need to have Mylyn before installing this gwt-eclipse-plugin, because it is no longer included Eclipse. Mylyn can be installed from the Marketplace.
Or you can simply add "https://download.eclipse.org/mylyn/releases/latest/" to your available Software Sites.

## Development 
To compile from source, you will only need this command:
* `sh nobuto.sh`
No eclipse, maven, ant, gradle or any other build tool is required. Just a plain JDK is needed.
