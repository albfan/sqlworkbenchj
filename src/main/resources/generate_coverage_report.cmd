@echo off
set JAVA_HOME=c:\jdk8
set ANT_OPTS=-Dlog4j.configuration=cobertura-log4j.xml
call ant coverage
