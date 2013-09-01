@echo off
set JAVA_HOME=c:\Programme\Java\JDK6
set ANT_OPTS=-Dlog4j.configuration=cobertura-log4j.xml
call ant coverage
