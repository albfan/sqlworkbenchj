#!/bin/sh 
# Start the SQL Workbench/J Database Explorer directly
scriptpath=`dirname $0`
java -Xmx256m -jar $scriptpath/Workbench.jar -dbexplorer $@