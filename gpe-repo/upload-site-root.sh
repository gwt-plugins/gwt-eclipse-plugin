#!/bin/sh
# upload release repo

cd repository

# upload snapshot
gsutil cp index.html gs://gwt-eclipse-plugin