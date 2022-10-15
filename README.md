# GWT Eclipse Plugin

This repository houses the source for the GWT Eclipse Plugin build with nobuto. 

This version has been created, because there is no official version any more.
  
* See Issues here: https://github.com/gwt-plugins/gwt-eclipse-plugin/issues

The current version installs on Windows and Linux with Eclipse 2021-12 up to Eclipse 2022-09. Other versions have not been testet.

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
To install just add this to your available software sites:
  http://keeitsi.com/software/eclipse-plugins/ 

## Development 
To compile from source, you will only need this command:
* `sh nobuto.sh`
No eclipse, maven, ant, gradle or any other build tool is required. Just a plain JDK is needed.
