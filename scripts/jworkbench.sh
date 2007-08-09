#!/bin/sh 
# Start SQL Workbench/J
scriptpath=`dirname $0`
java -Xmx256m -jar $scriptpath/Workbench.jar $@
