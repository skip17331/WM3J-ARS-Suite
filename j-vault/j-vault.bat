@echo off
rem Windows wrapper for J-Vault.
setlocal
set SCRIPT_DIR=%~dp0

java -Dfile.encoding=UTF-8 ^
     -jar "%SCRIPT_DIR%target\j-vault-1.0.20.jar" ^
     %*
