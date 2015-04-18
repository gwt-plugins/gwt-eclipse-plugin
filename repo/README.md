#GWT Plugin Repo

##Upload

* TODO possibly download repo before it's built
* `cd ./target/repository` then upload
* `gsutil cp -r . gs://gwt-eclipse-plugin/snapshot` - uploading snapshot to bucket

###Bucket Permissions

* `gsutil defacl set public-read gs://bucket` - public permssions 


##Download

* http://storage.googleapis.com/gwt-eclipse-plugin/snapshot - snapshot