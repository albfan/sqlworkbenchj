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
  JAVACMD=${JAVA_BIN}/bin/java
fi

cp=$scriptpath/sqlworkbench.jar
cp=$cp:$scriptpath/dom4j-1.6.1.jar
cp=$cp:$scriptpath/poi-ooxml-schemas.jar
cp=$cp:$scriptpath/poi-ooxml.jar
cp=$cp:$scriptpath/poi.jar
cp=$cp:$scriptpath/stax-api-1.0.1.jar
cp=$cp:$scriptpath/xmlbeans-2.3.0.jar

$JAVACMD -Djava.awt.headless=true \
         -Xmx256m \
         -Dvisualvm.display.name=SQLWorkbench \
         -cp $cp workbench.console.SQLConsole $@ 
