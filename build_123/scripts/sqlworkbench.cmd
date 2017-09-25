@echo off

setlocal

set JAVA_BINPATH=

if exist "%~dp0jre\bin\java.exe" (
   set JAVA_BINPATH=%~dp0jre\bin\
) else (
   if exist "%WORKBENCH_JDK%\bin\java.exe" (
     set JAVA_BINPATH=%WORKBENCH_JDK%\bin\
   ) else (
     if exist "%JAVA_HOME%\jre\bin\java.exe" (
        set JAVA_BINPATH=%JAVA_HOME%\jre\bin\
     ) else (
       if exist "%JAVA_HOME%\bin\java.exe" set JAVA_BINPATH=%JAVA_HOME%\bin\
     )
   )
)

set wbdir=%~dp0

set cp=%wbdir%sqlworkbench.jar
set cp=%cp%;%wbdir%poi*.jar
set cp=%cp%;%wbdir%dom4j*.jar
set cp=%cp%;%wbdir%stax*.jar
set cp=%cp%;%wbdir%*odf*.jar
set cp=%cp%;%wbdir%resolver*.jar
set cp=%cp%;%wbdir%serializer*.jar
set cp=%cp%;%wbdir%xerces*.jar
set cp=%cp%;%wbdir%log4j.jar
set cp=%cp%;%wbdir%mail.jar
set cp=%cp%;%wbdir%ext\*

call :get_memory
set /a max_mem=%free_memory% / 2

if "%1"=="console" goto console_mode

:gui
start "SQL Workbench/J" "%JAVA_BINPATH%javaw.exe"^
      -Xmx%max_mem%m ^
      -Dvisualvm.display.name=SQLWorkbench ^
      -Dsun.awt.keepWorkingSetOnMinimize=true ^
      -Dsun.java2d.dpiaware=true ^
      -Dsun.java2d.noddraw=true ^
      -cp %cp% workbench.WbStarter %*
goto :eof

:console_mode
title SQL Workbench/J

"%JAVA_BINPATH%java.exe" -Dvisualvm.display.name=SQLWorkbench ^
                         -cp %cp% workbench.console.SQLConsole %*

goto :eof

:get_memory

  for /f "skip=1" %%p in ('wmic os get FreePhysicalMemory') do ( 
    set /a free_memory=%%p/1024
    goto :eof
  )
  goto :eof

