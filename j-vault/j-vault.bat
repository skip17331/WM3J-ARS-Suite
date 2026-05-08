@echo off
rem Windows wrapper for J-Vault.
setlocal
set SCRIPT_DIR=%~dp0

java ^
    --module-path "%SCRIPT_DIR%lib\javafx" ^
    --add-modules javafx.controls,javafx.fxml ^
    -Dfile.encoding=UTF-8 ^
    -jar "%SCRIPT_DIR%target\j-vault-1.0.0.jar" ^
    %*
