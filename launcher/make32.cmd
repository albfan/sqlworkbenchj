@echo off

pushd "%~dp0"

echo main.class=workbench.WbStarter>t.ini
copy /b t.ini+workbench.ini sqlworkbench.ini

rcedit /C SQLWorkbench.exe
rcedit /I SQLWorkbench.exe workbench.ico
rcedit /N SQLWorkbench.exe sqlworkbench.ini

echo main.class=workbench.console.SQLConsole>t.ini
copy t.ini+workbench.ini sqlwbconsole.ini

rcedit /C sqlwbconsole.exe
rcedit /I sqlwbconsole.exe console.ico
rcedit /N sqlwbconsole.exe sqlwbconsole.ini

del t.ini > nul

