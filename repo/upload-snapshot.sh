#!/bin/sh
# upload snapshot repo

# install utils
# https://cloud.google.com/storage/docs/gsutil_install

# Note: allow the public to read the bucket
# gsutil defacl set public-read gs://gwt-plugin-snapshot

cd repo/target/repository

# upload snapshot
# url locaiton: http://storage.googleapis.com/gwt-eclipse-plugin/snapshot
gsutil cp -r . gs://gwt-eclipse-plugin/snapshot