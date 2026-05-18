@echo off
rem ============================================================
rem  ARS Suite - unified Windows installer (thin wrapper).
rem
rem  All logic lives in install.ps1. This wrapper just runs it
rem  with an execution-policy bypass so a fresh box needs zero
rem  PowerShell setup. Arguments pass straight through:
rem
rem      install.bat                (full install)
rem      install.bat -SkipDeps      (toolchain already present)
rem      install.bat -SkipBuild     (jars already built)
rem
rem  Re-runnable as an upgrade. Never touches logs, the j-vault
rem  inventory DB, or station credentials.
rem ============================================================
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1" %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
