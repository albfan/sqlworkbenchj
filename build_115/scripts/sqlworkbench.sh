#!/bin/sh 

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
 
# Start SQL Workbench/J
scriptpath=`dirname "$PRG"`

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

# When running in batch mode on a system with no X11
# installed the option -Djava.awt.headless=true might
# be needed for some combinations of OS and JDK

exec $JAVACMD -Dvisualvm.display.name=SQLWorkbench -Xmx256m -jar $scriptpath/sqlworkbench.jar $@
