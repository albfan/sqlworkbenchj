#!/bin/sh 
# Start the SQL Workbench/J DataPumper directly
scriptpath=`dirname $0`
java -Xmx256m -jar $scriptpath/Workbench.jar -datapumper $@