#GWT Eclipse Plugin
The GWT Plugin for Eclipse repository for version 3. 
This plugin does not contain the cloud tools as they have been split into 
[Google Plugin for Eclipse](https://github.com/GoogleCloudPlatform/google-plugin-for-eclipse).  

# RoadMap

* V3 release EA is scheduled for November 2016 @ GWTCon (In the meantime use the GPE fork version)

## Reference

* [Documentation](http://gwt-plugins.github.io/documentation/)
* [GPE Fork V2 GWT Eclipse Plugin](https://github.com/gwt-plugins/gwt-eclipse-plugin/tree/gpe-fork) * Recommended use

## V2 Repository (GPE-Fork)

* [GPE Fork V2 GWT Eclipse Plugin](https://github.com/gwt-plugins/gwt-eclipse-plugin/tree/gpe-fork)


## V3 Repository
The Eclipse repositories for this plugin. 

### Snapshot

* [http://storage.googleapis.com/gwt-eclipse-plugin/v2/snapshot](http://storage.googleapis.com/gwt-eclipse-plugin/v2/snapshot)

### Production

* [http://storage.googleapis.com/gwt-eclipse-plugin/v2/release](http://storage.googleapis.com/gwt-eclipse-plugin/v2/release)


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