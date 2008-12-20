#!/bin/sh 
# Start SQL Workbench/J in console mode
scriptpath=`dirname $0`

java -Djava.awt.headless=true \
     -Xmx256m \
     -cp $scriptpath/sqlworkbench.jar:$scriptpath/poi.jar workbench.console.SQLConsole $@ 
