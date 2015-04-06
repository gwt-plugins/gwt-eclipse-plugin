#!/bin/sh
# upload snapshot repo

cd repository

# upload snapshot
gsutil cp -r . gs://gwt-eclipse-plugin/snapshot