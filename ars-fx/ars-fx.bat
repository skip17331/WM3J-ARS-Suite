@echo off
rem Production launcher for the installed ARS Suite (runs the packaged jar).
rem   ars-fx.bat                -> docked app (J-Hub + all modules)
rem   ars-fx.bat --module log   -> one module un-docked (loose), attached to the hub
rem   ars-fx.bat --hub          -> background hub only (system tray)
rem The installer's Start-Menu shortcuts call this with different args.
setlocal
set "SCRIPT_DIR=%~dp0"

rem Resolve the runnable jar (prefer a windows/linux dist, else any ars-fx jar).
set "JAR="
for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\ars-fx-windows.jar" "%SCRIPT_DIR%target\ars-fx-linux.jar" 2^>nul') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\ars-fx-*.jar" 2^>nul ^| findstr /v /i "original- sources javadoc"') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR (
  echo Error: ars-fx jar not found in "%SCRIPT_DIR%target" - build it:  mvn -DskipTests -f "%SCRIPT_DIR%pom.xml" package
  exit /b 1
)

cd /d "%SCRIPT_DIR%"
if "%~1"=="--hub" (
  java -Dfile.encoding=UTF-8 -cp "%JAR%" com.ars.fx.HubServer
) else (
  java -Dfile.encoding=UTF-8 -jar "%JAR%" %*
)
endlocal
