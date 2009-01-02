#!/bin/sh 
# Start SQL Workbench/J
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

# When running in batch mode on a system with no X11
# installed the option -Djava.awt.headless=true might
# be needed for some combinations of OS and JDK

$JAVACMD -Xmx256m -jar $scriptpath/sqlworkbench.jar $@