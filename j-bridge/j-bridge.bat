@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
rem Resolve the jar by glob so a version bump never breaks this launcher.
rem Prefer the runnable fat/uber jar (shade adds a -shaded/-fat classifier);
rem fall back to the in-place shaded main jar; skip thin original-/sources/javadoc.
set "JAR="
for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\j-bridge-*-shaded.jar" "%SCRIPT_DIR%target\j-bridge-*-fat.jar" "%SCRIPT_DIR%target\j-bridge-*-jar-with-dependencies.jar" 2^>nul') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR for /f "delims=" %%F in ('dir /b /a-d /o-d "%SCRIPT_DIR%target\j-bridge-*.jar" 2^>nul ^| findstr /v /i "original- sources javadoc"') do if not defined JAR set "JAR=%SCRIPT_DIR%target\%%F"
if not defined JAR (
  echo Error: j-bridge jar not found in "%SCRIPT_DIR%target" - build it:  mvn -DskipTests -f "%SCRIPT_DIR%pom.xml" package
  exit /b 1
)
java -Dfile.encoding=UTF-8 -jar "%JAR%" %*
endlocal
