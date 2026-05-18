@echo off
rem ============================================================
rem  ARS Suite - the ONE command to install on Windows.
rem
rem  Just double-click this file (or run  .\install.bat ).
rem  Nothing else to type. It installs Java + Maven if needed,
rem  builds everything, and adds Start-Menu shortcuts.
rem
rem  First run takes about 5-10 minutes. Re-running later just
rem  upgrades. It never touches your logs, the J-Vault
rem  inventory, or saved passwords.
rem
rem  Power users:
rem      install.bat -SkipDeps      (Java/Maven already present)
rem      install.bat -SkipBuild     (jars already built)
rem
rem  A full transcript is saved to  install-log.txt  next to
rem  this file - attach that if you ever need to report a problem.
rem ============================================================
setlocal

echo.
echo   Installing the WM3J ARS Suite. This can take 5-10 minutes
echo   on the first run (it downloads Java, Maven, and libraries).
echo   You can leave it running.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1" %*
set "RC=%ERRORLEVEL%"

rem If the user double-clicked this from Explorer, the window would
rem vanish before they could read the result. %cmdcmdline% contains
rem this script's name only when launched that way (not from an open
rem terminal), so pause exactly in that case.
echo %cmdcmdline% | find /i "%~nx0" >nul 2>&1
if not errorlevel 1 (
    echo.
    echo   ----------------------------------------------------------
    echo   Press any key to close this window . . .
    pause >nul
)

endlocal & exit /b %RC%
