@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -Dfile.encoding=UTF-8 -jar "%SCRIPT_DIR%target\j-map-1.0.31.jar" %*
endlocal
