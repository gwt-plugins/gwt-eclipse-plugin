#!/bin/sh
# upload release repo

cd repository

# upload snapshot
gsutil cp -r . gs://gwt-eclipse-plugin/release/kepler