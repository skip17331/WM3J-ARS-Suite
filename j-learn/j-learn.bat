@echo off
REM J-Learn launcher - starts the standalone reference-library web app on
REM port 8082. The app keeps running in this console; close it to stop.
setlocal
set "SCRIPT_DIR=%~dp0"
rem Resolve the jar by glob so a version bump never breaks this launcher.
rem Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
rem fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
set "JAR="
for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\j-learn-*-shaded.jar" "%SCRIPT_DIR%target\j-learn-*-fat.jar" "%SCRIPT_DIR%target\j-learn-*-jar-with-dependencies.jar" 2^>nul') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\j-learn-*.jar" 2^>nul ^| findstr /v /i "original- sources javadoc"') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR (
  echo Error: j-learn jar not found in "%SCRIPT_DIR%target"
  echo Build it first:  mvn -DskipTests -f "%SCRIPT_DIR%pom.xml" install
  exit /b 1
)
cd /d "%SCRIPT_DIR%"
java -jar "%JAR%" %*
endlocal
