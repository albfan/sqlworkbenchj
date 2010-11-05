copy ..\src\workbench\resource\images\workbench.ico .
copy ..\src\workbench\resource\images\console.ico .

rcedit64 /C SQLWorkbench64.exe
rcedit64 /I SQLWorkbench64.exe workbench.ico
rcedit64 /N SQLWorkbench64.exe sqlworkbench.ini

rcedit /I sqlwbconsole64.exe console.ico
rcedit /N sqlwbconsole64.exe sqlwbconsole.ini