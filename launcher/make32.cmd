copy ..\src\workbench\resource\images\workbench.ico .
copy ..\src\workbench\resource\images\console.ico .

rcedit /C SQLWorkbench.exe
rcedit /I SQLWorkbench.exe workbench.ico
rcedit /N SQLWorkbench.exe sqlworkbench.ini

rcedit /C sqlwbconsole.exe
rcedit /I sqlwbconsole.exe console.ico
rcedit /N sqlwbconsole.exe sqlwbconsole.ini