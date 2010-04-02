' *******************************************************************************************
' This script will create a shortcut in the current directory that will start SQL Workbench/J 
' The shortcut will point to the javaw.exe indicated by the JAVA_HOME variable
' *******************************************************************************************

set WshShell = WScript.CreateObject("WScript.Shell" )
javaHome = WshShell.ExpandEnvironmentStrings("%JAVA_HOME%")

javaPath = javaHome & "\bin\javaw.exe"
' WScript.Echo javaPath

wbpath = Left(WScript.ScriptFullName, Len(WScript.ScriptFullName) - Len(WScript.ScriptName))
exepath = wbpath & "sqlworkbench.exe"
jarpath = wbpath & "sqlworkbench.jar"

' WScript.Echo jarpath

set oShellLink = WshShell.CreateShortcut(wbPath & "SQLWorkbench.lnk")
oShellLink.TargetPath = javapath 
oShellLink.Arguments = " -Xmx256m -jar " & chr(34) & jarpath & chr(34)
oShellLink.IconLocation = chr(34) & exepath & ",1"
oShellLink.WindowStyle = 1
oShellLink.Save
