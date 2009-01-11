@echo off
title SQL Workbench/J

setlocal 

if not "%WORKBENCH_JDK%"=="" set JAVA_HOME=%WORKBENCH_JDK%
if not "%JAVA_HOME%"=="" set JAVA_BINPATH=%JAVA_HOME%\bin\

set wbdir=%~dp0
set cp=%wbdir%sqlworkbench.jar;%wbdir%poi.jar

%JAVA_BINPATH%java -Xmx256m -cp %cp% workbench.console.SQLConsole %*
