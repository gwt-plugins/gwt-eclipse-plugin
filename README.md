#GWT Eclipse Plugin
The GWT Plugin for Eclipse repository for version 3. 
This plugin does not contain the cloud tools as they have been split into
[Google Cloud Platform plugin for Eclipse](https://github.com/GoogleCloudPlatform/google-cloud-eclipse).

# RoadMap

* V3 release EA is scheduled for November 2016 @ GWTCon (In the meantime use the GPE fork version).

## Reference

* [Documentation](http://gwt-plugins.github.io/documentation/)

## V2 Repository (GPE-Fork)

* [GPE Fork V2 GWT Eclipse Plugin](https://github.com/gwt-plugins/gwt-eclipse-plugin/tree/gpe-fork) * Recommended use


## V3 Repository
The Eclipse repositories for this plugin. 

### MarketPlace

* [https://marketplace.eclipse.org/content/gwt-eclipse-plugin](https://marketplace.eclipse.org/content/gwt-eclipse-plugin)

### Production

* [http://storage.googleapis.com/gwt-eclipse-plugin/v3/release](http://storage.googleapis.com/gwt-eclipse-plugin/v3/release)

### Snapshot

* [http://storage.googleapis.com/gwt-eclipse-plugin/v3/snapshot](http://storage.googleapis.com/gwt-eclipse-plugin/v3/snapshot)

## Build
Sencha has provided an internal build agent to build. 
[Sencha Eclipse Build](https://teamcity.sencha.com/viewType.html?buildTypeId=Gxt3_Gwt_GwtEclipsePlugin)

* `mvn clean package`

### Deploy
Google storage write permissions are needed to deploy. 

* `sh ./build-deploy-release.sh` - deploy production version
* `sh ./build-deploy-snapshot.sh` - deploy snapshot version


## Project

### Importing
Simply use Maven to import all the plugins and modules. 

* Use Eclipse Maven Import and import with recursion. 
* Select all the projects and import. 


## Thanks Sponsors
Sponsors that provide man power and equipment to help get the job done. 

### Google
The Google Cloud Tools team has put a significant amount of effort in helping bring the GWT Eclipse Plugin to market. 

* [Google Cloud Tools](https://cloud.google.com/)

### Sencha
Sencha provides man power to help update the plugin and the build server which automates the build. 

* [Sencha.com](http://sencha.com) 
