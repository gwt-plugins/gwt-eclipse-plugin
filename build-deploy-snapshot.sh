#!/bin/sh

# build plugin
mvn clean install

#build mirror
cd mirrors
mvn clean install
cd ..


# upload
sh ./repo/upload-snapshot.sh

