@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -Dfile.encoding=UTF-8 -jar "%SCRIPT_DIR%target\j-log-1.0.52.jar" %*
endlocal
