@echo off
title SQL Workbench/J
set wbdir=%~dp0

java -Xmx256m ^
     -cp %wbdir%sqlworkbench.jar;%wbdir%poi.jar ^
     workbench.console.SQLConsole %*
