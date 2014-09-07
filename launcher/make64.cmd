@echo off

echo main.class=workbench.WbStarter>t.ini
echo vm.heapsize.max.percent=50>>t.ini
copy /b t.ini+workbench.ini sqlworkbench.ini > nul

rcedit64 /C SQLWorkbench64.exe
rcedit64 /I SQLWorkbench64.exe workbench.ico
rcedit64 /N SQLWorkbench64.exe sqlworkbench.ini

echo main.class=workbench.console.SQLConsole>t.ini
copy t.ini+workbench.ini sqlwbconsole.ini > nul

rcedit64 /C sqlwbconsole64.exe
rcedit64 /I sqlwbconsole64.exe console.ico
rcedit64 /N sqlwbconsole64.exe sqlwbconsole.ini

del t.ini > nul
del sqlworkbench.ini > nul
del sqlwbconsole.ini > nul
