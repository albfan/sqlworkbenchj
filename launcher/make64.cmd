@echo off

echo [ErrorMessages]>err.ini
echo java.not.found=No suitable Java version could found. Please make sure you have a 64-bit Java 7 installed>>err.ini
echo java.failed=Java failed to startup successfully. Please make sure you have 64-bit Java 7 installed>>err.ini

echo main.class=workbench.WbStarter>t.ini
echo vm.heapsize.max.percent=50>>t.ini

copy /b t.ini+workbench.ini+err.ini sqlworkbench.ini > nul

rcedit64 /C SQLWorkbench64.exe
rcedit64 /I SQLWorkbench64.exe workbench.ico
rcedit64 /N SQLWorkbench64.exe sqlworkbench.ini

echo main.class=workbench.console.SQLConsole>t.ini

echo show.popup=false>>err.ini
copy t.ini+workbench.ini+err.ini sqlwbconsole.ini > nul

rcedit64 /C sqlwbconsole64.exe
rcedit64 /I sqlwbconsole64.exe console.ico
rcedit64 /N sqlwbconsole64.exe sqlwbconsole.ini

del t.ini > nul
del err.ini > nul
del sqlworkbench.ini > nul
del sqlwbconsole.ini > nul
