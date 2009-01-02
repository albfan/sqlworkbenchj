#!/bin/sh 
# Start SQL Workbench/J in console mode
scriptpath=`dirname $0`

JAVA_BIN=""

if [ -n "$WORKBENCH_JDK" ]
then
  JAVA_BIN=$WORKBENCH_JDK
fi

if [ -z "$JAVA_BIN" ]
then
  JAVA_BIN=$JAVA_HOME
fi

JAVACMD="java"

if [ -n "$JAVA_BIN" ]
then
  JAVACMD=${JAVA_BIN%/}/bin/java
fi

echo $JAVACMD

$JAVACMD -Djava.awt.headless=true -Xmx256m \
         -cp $scriptpath/sqlworkbench.jar:$scriptpath/poi.jar workbench.console.SQLConsole $@ 