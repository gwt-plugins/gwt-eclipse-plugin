#!/bin/sh
# upload release repo

cd repository

# upload snapshot
# http://storage.googleapis.com/gwt-eclipse-plugin/release
gsutil cp -r . gs://gwt-eclipse-plugin/release