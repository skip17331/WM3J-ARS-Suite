@echo off
rem ============================================================
rem  ARS Suite - Windows installer bootstrap.
rem
rem  Prerequisites:
rem    - Java 21 on PATH    (java --version)
rem    - Maven on PATH      (mvn --version) - only if the installer
rem                           jar isn't built yet
rem    - j-log-engine has been installed first  (mvn install)
rem    - All nine user-facing modules built:  mvn package each
rem        j-hub
rem        j-log, j-map, j-digi, j-bridge, j-sat
rem        j-vault         (shack inventory + estate handoff PDF)
rem        j-learn         (reference library web app, port 8082)
rem        morse-trainer   (Morse code learning + sending practice)
rem
rem  What this script does:
rem    1. Builds the installer jar if not present.
rem    2. Runs the installer, which:
rem         - generates per-module .bat launchers if missing,
rem         - writes Start-Menu shortcuts under
rem           %APPDATA%\Microsoft\Windows\Start Menu\Programs\ARS Suite\
rem    3. Never touches j-hub.json, databases, or logs - safe to
rem       re-run as an upgrade.
rem ============================================================

setlocal
set "SCRIPT_DIR=%~dp0"
set "INSTALLER_JAR=%SCRIPT_DIR%installer\target\j-installer-1.0.6.jar"

where java >nul 2>&1
if errorlevel 1 (
    echo error: Java 21 is required but 'java' was not found on PATH.
    echo        Install Temurin 21 from https://adoptium.net/temurin/releases/
    exit /b 1
)

if not exist "%INSTALLER_JAR%" (
    echo [bootstrap] Installer jar not found - building...
    where mvn >nul 2>&1
    if errorlevel 1 (
        echo error: Maven is required to build the installer but 'mvn' was not found.
        exit /b 1
    )
    call mvn -q -DskipTests -f "%SCRIPT_DIR%installer\pom.xml" package
    if errorlevel 1 (
        echo error: Installer build failed.
        exit /b 1
    )
)

java -jar "%INSTALLER_JAR%" --root "%SCRIPT_DIR%" %*
endlocal
