#!/bin/sh 
# Start the SQL Workbench/J DataPumper directly
scriptpath=`dirname $0`
$scriptpath/sqlworkbench.sh -datapumper $@
