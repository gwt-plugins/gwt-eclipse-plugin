# GWT Eclipse Plugin

[![Join the chat at https://gitter.im/gwt-plugins/gwt-eclipse-plugin](https://badges.gitter.im/gwt-plugins/gwt-eclipse-plugin.svg)](https://gitter.im/gwt-plugins/gwt-eclipse-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This repository houses the source for the GWT Eclipse Plugin. 

## Reference
* [Documentation](http://gwt-plugins.github.io/documentation/)
* [GWT Eclipse Plugin Youtube Playlist](https://www.youtube.com/watch?v=DU7ZQVLR5Zo&list=PLBbgqtDgdc_TqzA-qXrjgTFMC_6DKAQyT)

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

### Maven
Supports the two GWT Maven plugins.

* [Mojo GWT Maven Plugin](https://gwt-maven-plugin.github.io/gwt-maven-plugin/)
* [TBroyer GWT Maven Plugin](https://tbroyer.github.io/gwt-maven-plugin/)

## MarketPlace
Install from the Eclipse marketplace.

<a href="https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=5576850" class="drag" title="Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client"><img style="width:80px;" typeof="foaf:Image" class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.svg" alt="Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client" /></a>

* [https://marketplace.eclipse.org/content/gwt-plugin](https://marketplace.eclipse.org/content/gwt-plugin)


## Repository
The Eclipse repositories for this plugin.

### Production
Release update site.

* [Eclipse Marketplace entry for the GWT Plugin](https://marketplace.eclipse.org/content/gwt-plugin)
* [Update sites for the GWT Plugin](https://plugins.gwtproject.org/eclipse/site/latest)

### Production Zip
Download the repo in a zip file from the latest release at the
[releases page](https://github.com/gwt-plugins/gwt-eclipse-plugin/releases/).)

## Development

### Importing
Simply use Maven to import all the plugins and modules.

* Use Eclipse Import and choose import with Existing Maven projects. Don't forget to select recursive import.  
* Select all the projects and import them. This will create the .project, .classpath and .settings files. If some exist, they will be overwritten.  

### Target Definition
The target defintion build is based off of the Google Cloud Tools. Follow their instructions to build.

* [Install the target.tbd plugin](https://github.com/mbarbero/fr.obeo.releng.targetplatform) - Install the targets plugin. 
* Once the plugin is installed, right click on the target.tbd file and set the target. 
* Note: The targets will have to be updated by setting the targets. This will regenerate the Eclipse target files.  

### Build
To build, Apache Maven and Java 17 are required. Invoke `mvn verify` to build and test. The resulting
update site can be found in `repo/target/repository` for local deployment and testing.

### Release
Creating a release requires signing artifacts. Set the following environment variables before running
`mvn verify`:
 * `SIGN_KEYSTORE` - Path to a pkcs12 keystore that contains a key to use to sign this release
 * `SIGN_STOREPASS` - Passphrase for the keystore
 * `SIGN_ALIAS` - Alias of the key to use to sign the release
 * `SIGN_KEYPASS` - Passphrase for the key
 * `SIGN_TSA` - URL of a Time stamp authority to use to sign this release
 
 At this time, releases are performed manually. The releases deployed to the marketplace will be signed
 with the certificate for `plugins.gwtproject.org`.

### Deploy
Releases are uploaded as zips to the [release](https://github.com/gwt-plugins/gwt-eclipse-plugin/releases/)
part of the Github project page, and also deployed at
https://gwt-plugins.github.io/documentation/repo/site/current as Eclipse update sites. Releases will be
added to the [GWT-Plugin](https://marketplace.eclipse.org/content/gwt-plugin) page on the Eclipse
Marketplace.

### Testing
There are a couple of archetypes that are used to test. 

* [Single Module Mojo Archetype](https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-test-gwt27)
* [Single Module TBroyer Archetype](https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-basic) ([TBroyer GWT Maven Plugin](https://github.com/tbroyer/gwt-maven-plugin))

## Thanks to our Sponsors
Sponsors that provide man power and equipment to help get the job done. 

### Google
The Google Cloud Tools team has put a significant amount of effort in helping bring the GWT Eclipse Plugin to market. 

* [Google Cloud Tools](https://cloud.google.com/)

## Not Included
* This plugin does not include the [Google Cloud Tools Eclipse](https://github.com/GoogleCloudPlatform/google-cloud-eclipse) features.


