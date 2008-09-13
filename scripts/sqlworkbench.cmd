@echo off
set wbdir=%~dp0
java -Xmx256m -jar %wbdir%sqlworkbench.jar %*
