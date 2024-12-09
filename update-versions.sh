#!/bin/sh
# https://eclipse.org/tycho/sitedocs/tycho-release/tycho-versions-plugin/set-version-mojo.html
VERSION=$1
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=${VERSION}-SNAPSHOT

