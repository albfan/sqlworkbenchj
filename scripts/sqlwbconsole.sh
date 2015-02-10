#!/bin/sh
# Start SQL Workbench/J in console mode

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

scriptpath=`dirname "$PRG"`

JAVACMD="java"

if [ -x "$scriptpath/jre/bin/java" ]
then
  JAVACMD="$scriptpath/jre/bin/java"
elif [ -x "$WORKBENCH_JDK/bin/java" ]
then
  JAVACMD="$WORKBENCH_JDK/bin/java"
elif [ -x "$JAVA_HOME/jre/bin/java" ]
then
  JAVACMD="$JAVA_HOME/jre/bin/java"
elif [ -x "$JAVA_HOME/bin/java" ]
then
  JAVACMD="$JAVA_HOME/bin/java"
fi

cp=$scriptpath/sqlworkbench.jar
cp=$cp:$scriptpath/dom4j-1.6.1.jar
cp=$cp:$scriptpath/poi-ooxml-schemas.jar
cp=$cp:$scriptpath/poi-ooxml.jar
cp=$cp:$scriptpath/poi.jar
cp=$cp:$scriptpath/stax-api-1.0.1.jar
cp=$cp:$scriptpath/xmlbeans-2.3.0.jar
cp=$cp:$scriptpath/ext/*

$JAVACMD -Djava.awt.headless=true \
         -Xmx512m \
         -Dvisualvm.display.name=SQLWorkbench \
         -cp $cp workbench.console.SQLConsole $@

