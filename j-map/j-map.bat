@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAVAFX=%SCRIPT_DIR%lib\javafx"
java --module-path "%JAVAFX%" --add-modules javafx.controls,javafx.fxml -Dfile.encoding=UTF-8 -jar "%SCRIPT_DIR%target\j-map-1.0.28.jar" %*
endlocal
