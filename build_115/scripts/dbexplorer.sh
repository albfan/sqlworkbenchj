#!/bin/sh 
# Start the SQL Workbench/J Database Explorer directly
scriptpath=`dirname $0`
$scriptpath/sqlworkbench.sh -dbexplorer $@
