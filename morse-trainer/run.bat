@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
rem Resolve the jar by glob so a version bump never breaks this launcher.
rem Newest match wins; skip sources/javadoc side-artifacts.
set "JAR="
for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\morse-trainer-*.jar" 2^>nul ^| findstr /v /i "sources javadoc shaded fat"') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR (
  echo Error: morse-trainer jar not found in "%SCRIPT_DIR%target" - build it:  mvn -DskipTests -f "%SCRIPT_DIR%pom.xml" package
  exit /b 1
)
java -Dfile.encoding=UTF-8 -jar "%JAR%" %*
endlocal
