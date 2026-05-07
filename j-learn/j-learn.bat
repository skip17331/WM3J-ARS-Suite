@echo off
REM J-Learn launcher - opens the J-Learn tab in J-Hub's web UI.
REM If J-Hub isn't running, starts it in the background first.
setlocal
set "SCRIPT_DIR=%~dp0"
set "HUB_DIR=%SCRIPT_DIR%..\j-hub"
set "HUB_PORT=8081"
set "LEARN_URL=http://localhost:%HUB_PORT%/#learn"

REM Probe to see if J-Hub is already up.
powershell -NoProfile -Command "try { (Invoke-WebRequest -Uri 'http://localhost:%HUB_PORT%/' -UseBasicParsing -TimeoutSec 1) | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 (
  echo J-Hub already running.
  start "" "%LEARN_URL%"
  endlocal
  exit /b 0
)

if not exist "%HUB_DIR%\target\j-hub-1.0.0.jar" (
  echo Error: j-hub jar not found at %HUB_DIR%\target\j-hub-1.0.0.jar
  echo Build it first: cd %HUB_DIR% ^&^& mvn package -DskipTests
  endlocal
  exit /b 1
)

echo Starting J-Hub...
if not exist "%HUB_DIR%\logs" mkdir "%HUB_DIR%\logs"
pushd "%HUB_DIR%"
start "J-Hub" /MIN cmd /c "start.bat > logs\j-learn-launcher.log 2>&1"
popd

for /l %%i in (1,1,30) do (
  powershell -NoProfile -Command "try { (Invoke-WebRequest -Uri 'http://localhost:%HUB_PORT%/' -UseBasicParsing -TimeoutSec 1) | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
  if not errorlevel 1 (
    start "" "%LEARN_URL%"
    echo J-Hub up. J-Learn tab opened in browser.
    endlocal
    exit /b 0
  )
  timeout /t 1 /nobreak >nul
)

echo Error: J-Hub did not come up within 30 seconds.
echo Check %HUB_DIR%\logs\j-learn-launcher.log
endlocal
exit /b 1
