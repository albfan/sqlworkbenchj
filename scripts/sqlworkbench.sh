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
cp=$cp:$scriptpath/resolver.jar
cp=$cp:$scriptpath/serializer.jar
cp=$cp:$scriptpath/simple-odf.jar
cp=$cp:$scriptpath/ext/*

# When running in batch mode on a system with no X11 installed, the option
#   -Djava.awt.headless=true
# might be needed for some combinations of OS and JDK

exec $JAVACMD -Dvisualvm.display.name=SQLWorkbench -Xmx512m -cp $cp workbench.WbStarter $@
