@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -Dfile.encoding=UTF-8 -jar "%SCRIPT_DIR%target\morse-trainer-1.0.10.jar" %*
endlocal
