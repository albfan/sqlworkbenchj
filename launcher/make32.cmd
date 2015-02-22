@echo off

pushd "%~dp0"

echo main.class=workbench.WbStarter>t.ini
echo vm.heapsize.preferred=1024>>t.ini

echo [ErrorMessages]>err.ini
echo java.not.found=No suitable Java version could found. Please make sure you have a 32-bit Java 7 installed>>err.ini
echo java.failed=Java failed to startup successfully. Please make sure you have 32-bit Java 7 installed>>err.ini

copy /b t.ini+workbench.ini+err.ini sqlworkbench.ini

rcedit /C SQLWorkbench.exe
rcedit /I SQLWorkbench.exe workbench.ico
rcedit /N SQLWorkbench.exe sqlworkbench.ini

echo main.class=workbench.console.SQLConsole>t.ini
copy t.ini+workbench.ini+err.ini sqlwbconsole.ini

rcedit /C sqlwbconsole.exe
rcedit /I sqlwbconsole.exe console.ico
rcedit /N sqlwbconsole.exe sqlwbconsole.ini

del t.ini > nul
del err.ini > nul
del sqlworkbench.ini > nul
del sqlwbconsole.ini > nul

