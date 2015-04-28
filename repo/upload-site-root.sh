#!/bin/sh
# upload release repo

# install utils
# https://cloud.google.com/storage/docs/gsutil_install

# Note: allow the public to read the bucket
# gsutil defacl set public-read gs://gwt-plugin-snapshot

cd repo/target/repository

# upload snapshot
# http://storage.googleapis.com/gwt-eclipse-plugin
gsutil cp index.html gs://gwt-eclipse-plugin