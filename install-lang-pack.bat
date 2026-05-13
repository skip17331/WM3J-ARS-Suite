@echo off
REM Install one of the bundled language packs (de / fr / it / pt) into
REM %USERPROFILE%\.j-hub\lang\<module>\ so every module picks it up on
REM next launch (or live, for modules that listen for STATION_CONFIG).
REM
REM Usage:
REM   install-lang-pack.bat <lang>          - install for every module
REM   install-lang-pack.bat <lang> <module> - install for one module only
REM
REM   lang   : de | fr | it | pt
REM   module : j-digi | j-bridge | j-map | j-sat | morse-trainer
REM            (en + es are embedded in every jar - no pack needed)

setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "SRC_ROOT=%SCRIPT_DIR%i18n-packs"
set "DEST_ROOT=%USERPROFILE%\.j-hub\lang"

set "LANG=%~1"
set "MODULE=%~2"

if "%LANG%"=="" (
    echo Usage: %~nx0 ^<lang^> [module]
    echo   lang   : de ^| fr ^| it ^| pt
    echo   module : j-digi ^| j-bridge ^| j-map ^| j-sat ^| morse-trainer
    echo            ^(omit to install pack for every module^)
    exit /b 2
)

if /I "%LANG%"=="en" (
    echo en is embedded in every module - no pack to install.
    exit /b 0
)
if /I "%LANG%"=="es" (
    echo es is embedded in every module - no pack to install.
    exit /b 0
)
if /I not "%LANG%"=="de" if /I not "%LANG%"=="fr" if /I not "%LANG%"=="it" if /I not "%LANG%"=="pt" (
    echo error: unknown language '%LANG%'. Supported: de, fr, it, pt.
    exit /b 1
)

if not exist "%SRC_ROOT%" (
    echo error: %SRC_ROOT% not found. Run from your ARS Suite checkout.
    exit /b 1
)

echo Installing %LANG% language pack...

if not "%MODULE%"=="" (
    call :install_one "%MODULE%"
) else (
    for %%M in (j-digi j-bridge j-map j-sat morse-trainer) do call :install_one "%%M"
)

echo.
echo Done. Set Language = %LANG% in J-Hub - Station - Regional Settings.
goto :eof

:install_one
set "MOD=%~1"
set "SRC=%SRC_ROOT%\%MOD%\messages_%LANG%.properties"
if not exist "%SRC%" (
    echo   skip %MOD% - no %LANG% pack shipped for this module
    goto :eof
)
set "DST_DIR=%DEST_ROOT%\%MOD%"
if not exist "%DST_DIR%" mkdir "%DST_DIR%"
copy /Y "%SRC%" "%DST_DIR%\messages_%LANG%.properties" >nul
echo   installed %MOD% -^> %DST_DIR%\messages_%LANG%.properties
goto :eof
