#!/bin/sh
java -Djava.net.useSystemProxies=true -cp build/classes:build/nobuto.jar de.exware.nobuto.Main $1 $2 $3 $4

