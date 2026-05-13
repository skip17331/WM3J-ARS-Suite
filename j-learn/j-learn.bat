@echo off
REM J-Learn launcher — starts the standalone reference-library web app on
REM port 8082. The app keeps running in this console; close it to stop.
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%target\j-learn-1.0.45.jar"

if not exist "%JAR%" (
  echo Error: j-learn jar not found at %JAR%
  echo Build it first:  mvn -DskipTests -f "%SCRIPT_DIR%pom.xml" install
  endlocal
  exit /b 1
)

java -jar "%JAR%" %*
endlocal
