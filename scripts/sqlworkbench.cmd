@echo off

set wbdir=%~dp0

if "%1"=="console" goto console_mode

:gui
start javaw -Xmx256m -jar %wbdir%sqlworkbench.jar %*
goto :eof

:console_mode
java -Xmx256m -cp %wbdir%sqlworkbench.jar;%wbdir%poi.jar workbench.console.SQLConsole %*  
goto :eof

