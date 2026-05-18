@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -Dfile.encoding=UTF-8 -jar "%SCRIPT_DIR%target\j-bridge-1.0.17.jar" %*
endlocal
