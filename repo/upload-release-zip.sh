#!/bin/sh
# upload release zip

# install utils
# https://cloud.google.com/storage/docs/gsutil_install

# Note: allow the public to read the bucket
# gsutil defacl ch -u AllUsers:R gs://gwt-eclipse-plugin
# gsutil defacl set public-read gs://gwt-eclipse-plugin
# gsutil -m acl ch -g AllUsers:R -r gs://gwt-eclipse-plugin

cd repo/target

mkdir downloads

# rename artifact
cp repo-3.0.0-SNAPSHOT.zip ./downloads/repo-3.0.0.zip

# upload snapshot
# url locaiton: http://storage.googleapis.com/gwt-eclipse-plugin/v2/release
gsutil cp -r ./downloads gs://gwt-eclipse-plugin/v3/downloads


# go back to original location
cd ../..
