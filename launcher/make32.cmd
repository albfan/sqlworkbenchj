@echo off

pushd "%~dp0"

call :patch_ini

rcedit /C SQLWorkbench.exe
rcedit /I SQLWorkbench.exe workbench.ico
rcedit /N SQLWorkbench.exe sqlworkbench32.ini

rcedit /C sqlwbconsole.exe
rcedit /I sqlwbconsole.exe console.ico
rcedit /N sqlwbconsole.exe sqlwbconsole.ini

rem del sqlworkbench32.ini > nul

goto :eof

:patch_ini

rem The vm.heapsize.max.percent property doesn't work correctly with 32bit JVMs if the computer has more than 3GB of memora
rem (e.g. on a 64bit Windows but running 32bit Java)

set file=sqlworkbench.ini
set outfile=sqlworkbench32.ini
echo.>%outfile%

for /f " usebackq eol=# tokens=1 delims=" %%i in ("%file%") do (
  if "%%i"=="vm.heapsize.max.percent=50" (
     echo vm.heapsize.preferred=1024>>%outfile%
  ) else (
    @echo %%i>>%outfile%
  )
)
