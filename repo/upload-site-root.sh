#!/bin/sh
# upload release repo

cd repository

# upload snapshot
# http://storage.googleapis.com/gwt-eclipse-plugin
gsutil cp index.html gs://gwt-eclipse-plugin