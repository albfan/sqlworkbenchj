#!/bin/sh
# Start SQL Workbench/J in GUI mode

SCRIPT_PATH="$(dirname "$(readlink -f "$0")")"

JAVACMD="java"

if [ -x "$SCRIPT_PATH/jre/bin/java" ]
then
  JAVACMD="$SCRIPT_PATH/jre/bin/java"
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

cp=$SCRIPT_PATH/sqlworkbench.jar
cp=$cp:$SCRIPT_PATH/dom4j-1.6.1.jar
cp=$cp:$SCRIPT_PATH/poi-ooxml-schemas.jar
cp=$cp:$SCRIPT_PATH/poi-ooxml.jar
cp=$cp:$SCRIPT_PATH/poi.jar
cp=$cp:$SCRIPT_PATH/stax-api-1.0.1.jar
cp=$cp:$SCRIPT_PATH/resolver.jar
cp=$cp:$SCRIPT_PATH/serializer.jar
cp=$cp:$SCRIPT_PATH/simple-odf.jar
cp=$cp:$SCRIPT_PATH/ext/*

# When running in batch mode on a system with no X11 installed, the option
#   -Djava.awt.headless=true
# might be needed for some combinations of OS and JDK

exec $JAVACMD -Dvisualvm.display.name=SQLWorkbench -Dawt.useSystemAAFontSettings=on -cp $cp workbench.WbStarter "$@"
