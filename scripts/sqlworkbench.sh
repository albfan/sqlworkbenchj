#!/bin/sh 
# Start SQL Workbench/J
scriptpath=`dirname $0`

# When running in batch mode on a system with no X11
# installed the option -Djava.awt.headless=true might
# be needed for some combinations of OS and JDK
java -Xmx256m -jar $scriptpath/sqlworkbench.jar $@
