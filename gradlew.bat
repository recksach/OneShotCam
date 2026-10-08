@echo off
set DIR=%~dp0
if '%DIR:~-1%'=='\' set DIR=%DIR:~0,-1%
java -jar %DIR%\gradle\wrapper\gradle-wrapper.jar %*
