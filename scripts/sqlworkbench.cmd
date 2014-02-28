@echo off

setlocal 

if exist "%~dp0jre\bin\java.exe" set JAVA_BINPATH=%~dp0jre\bin
if exist "%WORKBENCH_JDK%\bin\java.exe" set JAVA_BINPATH=%WORKBENCH_JDK%\bin
if exist "%JAVA_HOME%\bin\java.exe" set JAVA_BINPATH=%JAVA_HOME%\bin

set wbdir=%~dp0

if "%1"=="console" goto console_mode

:gui
start "SQL Workbench/J" "%JAVA_BINPATH%\javaw.exe"^
      -Xmx512m ^
      -Dvisualvm.display.name=SQLWorkbench ^
      -Dsun.awt.keepWorkingSetOnMinimize=true ^
      -jar %wbdir%sqlworkbench.jar %*
goto :eof

:console_mode
title SQL Workbench/J
set cp=%wbdir%sqlworkbench.jar;%wbdir%poi.jar
"%JAVA_BINPATH%java.exe" -Dvisualvm.display.name=SQLWorkbench ^
                         -Xmx512m -cp %cp% workbench.console.SQLConsole %*

goto :eof

