' ********************************************************************
' This script will start SQL Workbench/J 
'
' It is intended for situations where the .exe launcher does not work
' ********************************************************************

set WshShell = WScript.CreateObject("WScript.Shell" )
set FSO = WScript.CreateObject("Scripting.FileSystemObject")

javaPath = "javaw.exe"
javaHome = WshShell.ExpandEnvironmentStrings("%JAVA_HOME%")
wbJava = WshShell.ExpandEnvironmentStrings("%WORKBENCH_JDK%")
wbpath = Left(WScript.ScriptFullName, Len(WScript.ScriptFullName) - Len(WScript.ScriptName))

set args = WScript.Arguments

localJRE = wbpath & "jre\bin\javaw.exe"

if (FSO.FileExists(localJRE)) then 
  javaPath = localJRE
elseif FSO.FileExists(wbJava & "\bin\javaw.exe") then 
  javaPath = wbJava & "\bin\javaw.exe"
elseif FSO.FileExists(javaHome & "\bin\javaw.exe") then 
  javaPath = javaHome & "\bin\javaw.exe"
end if

jarpath = wbpath & "sqlworkbench.jar"

javaCmd = chr(34) & javaPath & chr(34) & " " & chr(34) & jarpath & chr(34)

if (args.length > 0) then
  for each arg in args
    javaCmd = javaCmd & " " & arg
  next
end if

retValue = wshShell.Run(javaCmd, 0, false)
