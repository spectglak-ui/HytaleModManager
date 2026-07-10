@echo off
setlocal EnableDelayedExpansion
title HMM Build

rem ============================================================
rem  Hytale Mod Manager - Windows Build Script
rem  JDK 25  |  Maven 3.9+  |  WiX Toolset 7
rem
rem  Usage:
rem    build.bat             Auto (MSI if WiX found, else app-image)
rem    build.bat msi         Force MSI  (requires WiX 7 + EULA accepted)
rem    build.bat exe         Force EXE  (requires WiX 7 + EULA accepted)
rem    build.bat app-image   Folder only, no WiX needed
rem ============================================================

rem --- CONFIGURATION - edit before each release ---------------
set "APP_NAME=Hytale Mod Manager"
set "APP_VERSION=1.2.0"
set "APP_VENDOR=Spectglack"
set "APP_DESC=Gestionnaire et mise a jour des mods Hytale"
set "APP_UUID=f3a1b2c3-d4e5-6789-abcd-ef0123456789"
set "MAIN_CLASS=com.hytale.modmanager.Main"
set "JAR_NAME=hytale-mod-manager.jar"
set "ICON_REL=src\main\resources\com\hytale\modmanager\icon\icon.ico"
set "OUTPUT_DIR=target\installer"
set "LOG_FILE=target\jpackage.log"
rem ------------------------------------------------------------

rem Absolute project directory
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "ICON=%ROOT%\%ICON_REL%"

echo.
echo ===========================================================
echo   %APP_NAME% v%APP_VERSION% - Windows Build
echo ===========================================================
echo.

rem ============================================================
rem  1/8  Java
rem ============================================================
echo [1/8]  Checking Java...

where java >nul 2>&1
if errorlevel 1 goto :ERR_NO_JAVA

for /f "tokens=3" %%L in ('java -version 2^>^&1 ^| findstr /i "version"') do set "_JV=%%~L"
set _JV=%_JV:"=%
for /f "delims=.-" %%M in ("%_JV%") do set "JMAJ=%%M"
echo   Java %_JV%  (major: %JMAJ%)
if %JMAJ% LSS 17 goto :ERR_OLD_JAVA
echo   [OK] Java %JMAJ%
goto :STEP2

:ERR_NO_JAVA
echo.
echo   ERROR: java not found in PATH.
echo   Download JDK 17+: https://adoptium.net/
goto :FAILED

:ERR_OLD_JAVA
echo   ERROR: Java %JMAJ% is too old. Java 17 minimum required.
goto :FAILED

rem ============================================================
rem  2/8  jpackage
rem ============================================================
:STEP2
echo.
echo [2/8]  Checking jpackage...

where jpackage >nul 2>&1
if errorlevel 1 goto :ERR_NO_JPACKAGE

jpackage --version > "%TEMP%\hmm_jpv.tmp" 2>&1
set /p _JPV=<"%TEMP%\hmm_jpv.tmp"
del /f /q "%TEMP%\hmm_jpv.tmp" 2>nul
echo   [OK] jpackage %_JPV%
goto :STEP3

:ERR_NO_JPACKAGE
echo.
echo   ERROR: jpackage not found.
echo   Included in JDK 14+. Make sure JAVA_HOME points to a full JDK.
goto :FAILED

rem ============================================================
rem  3/8  Maven
rem ============================================================
:STEP3
echo.
echo [3/8]  Checking Maven...

set "MVN="
where mvn.cmd >nul 2>&1
if not errorlevel 1 set "MVN=mvn.cmd"
if not defined MVN where mvn >nul 2>&1
if not errorlevel 1 if not defined MVN set "MVN=mvn"
if not defined MVN goto :ERR_NO_MAVEN

for /f "tokens=3" %%V in ('call %MVN% --version 2^>^&1 ^| findstr /i "apache maven"') do (
    echo   [OK] Maven %%V
    goto :STEP4
)
:STEP4

rem ============================================================
rem  4/8  WiX Toolset 7
rem
rem  Detection uses only single-line if statements with goto.
rem  No nested if-blocks, which avoids the CMD parenthesis
rem  counting bug triggered by paths like (x86) or .dotnet.
rem ============================================================
echo.
echo [4/8]  Checking WiX Toolset 7...

set "WIX="
set "ITYPE=app-image"

rem -- Check 1: wix in PATH
where wix >nul 2>&1
if not errorlevel 1 goto :WIX_FOUND_PATH

rem -- Check 2: dotnet global tools default location
rem    Path built into variable first to avoid parser issues.
set "_W1=%USERPROFILE%\.dotnet\tools\wix.exe"
if exist "%_W1%" goto :WIX_FOUND_VAR

rem -- Check 3: Program Files 64-bit
set "_W1=%ProgramFiles%\WiX Toolset\bin\wix.exe"
if exist "%_W1%" goto :WIX_FOUND_VAR

rem -- Not found
echo   WiX 7 not found.
echo   Install: dotnet tool install --global wix
echo   Or: https://github.com/wixtoolset/wix/releases
echo.
echo   Fallback: app-image format (folder, no installer).
echo   Re-run  build.bat msi  after installing WiX.
goto :WIX_DONE

:WIX_FOUND_PATH
set "WIX=wix"
goto :WIX_SHOW_VER

:WIX_FOUND_VAR
set "WIX=%_W1%"

:WIX_SHOW_VER
"%WIX%" --version > "%TEMP%\hmm_wixv.tmp" 2>&1
set /p _WV=<"%TEMP%\hmm_wixv.tmp"
del /f /q "%TEMP%\hmm_wixv.tmp" 2>nul
echo   [OK] WiX %_WV%
echo   Path: %WIX%
rem
rem  WiX 7 OSMF EULA: must be accepted once before building.
rem  Official command (WiX 7.0.0):  wix eula accept wix7
rem  We do NOT try to auto-accept here. If the EULA has not
rem  been accepted, jpackage will fail with error WIX7015 and
rem  the diagnostic at step 8 will print the exact fix.
rem
set "ITYPE=msi"

:WIX_DONE
rem Command-line argument overrides auto-detected type
if /I "%~1"=="msi"       set "ITYPE=msi"
if /I "%~1"=="exe"       set "ITYPE=exe"
if /I "%~1"=="app-image" set "ITYPE=app-image"
echo   Installer type: %ITYPE%

rem ============================================================
rem  5/8  Icon
rem ============================================================
echo.
echo [5/8]  Checking icon...

if not exist "%ICON%" goto :ERR_NO_ICON
for %%A in ("%ICON%") do set "ISIZ=%%~zA"
if %ISIZ% LSS 200 goto :ERR_BAD_ICON
echo   [OK] %ICON%  (%ISIZ% bytes)
goto :STEP6

:ERR_NO_ICON
echo.
echo   ERROR: Icon not found: %ICON%
goto :FAILED

:ERR_BAD_ICON
echo.
echo   ERROR: Icon file invalid (%ISIZ% bytes).
goto :FAILED

rem ============================================================
rem  6/8  Maven build (full output, no -q)
rem ============================================================
:STEP6
echo.
echo [6/8]  Maven build  (mvn clean package)...
echo -----------------------------------------------------------
echo.

cd /d "%ROOT%"
if exist "target\%JAR_NAME%" del /f /q "target\%JAR_NAME%"

call %MVN% clean package
set "MVN_EC=%ERRORLEVEL%"

echo.
echo -----------------------------------------------------------

if %MVN_EC% NEQ 0 goto :ERR_MAVEN

rem ============================================================
rem  7/8  Validate JAR
rem ============================================================
echo.
echo [7/8]  Validating JAR...

if not exist "target\%JAR_NAME%" goto :ERR_NO_JAR
for %%A in ("target\%JAR_NAME%") do set "JSIZ=%%~zA"
echo   JAR: target\%JAR_NAME%  (%JSIZ% bytes)
if %JSIZ% LSS 500000 echo   WARNING: JAR is small. Check maven-shade-plugin.
goto :STEP8

:ERR_MAVEN
echo   ERROR: Maven build failed. See output above.
goto :FAILED

:ERR_NO_JAR
echo   ERROR: JAR not produced: target\%JAR_NAME%
echo   Check maven-shade-plugin in pom.xml.
goto :FAILED

rem ============================================================
rem  8/8  jpackage
rem        Syntax compatible with JDK 22-25 and WiX 7.
rem        No --win-per-user-install (absent = system-wide).
rem ============================================================
:STEP8
echo.
echo [8/8]  Generating installer  (%ITYPE%)...
echo -----------------------------------------------------------
echo.

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if not exist "target"       mkdir "target"
del /f /q "%OUTPUT_DIR%\*.msi" 2>nul
del /f /q "%OUTPUT_DIR%\*.exe" 2>nul

echo   Running jpackage... (1-3 minutes)
echo   Full output below and saved to: %LOG_FILE%
echo.

jpackage ^
    --verbose ^
    --type %ITYPE% ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --vendor "%APP_VENDOR%" ^
    --description "%APP_DESC%" ^
    --input "target" ^
    --main-jar "%JAR_NAME%" ^
    --main-class "%MAIN_CLASS%" ^
    --dest "%OUTPUT_DIR%" ^
    --icon "%ICON%" ^
    --win-menu ^
    --win-menu-group "%APP_NAME%" ^
    --win-shortcut ^
    --win-upgrade-uuid %APP_UUID% ^
    --java-options "-Dfile.encoding=UTF-8" ^
    > "%LOG_FILE%" 2>&1

set "JP_EC=%ERRORLEVEL%"

echo === jpackage output =======================================
type "%LOG_FILE%"
echo === end ===================================================
echo.

rem Check that an output file was produced
set "OUT="
for %%F in ("%OUTPUT_DIR%\*.msi") do set "OUT=%%F"
for %%F in ("%OUTPUT_DIR%\*.exe") do set "OUT=%%F"
if "%ITYPE%"=="app-image" if exist "%OUTPUT_DIR%\%APP_NAME%" set "OUT=%OUTPUT_DIR%\%APP_NAME%"

if %JP_EC% NEQ 0 goto :JP_DIAG
if not defined OUT goto :JP_DIAG
goto :SUCCESS

rem ============================================================
rem  jpackage diagnostics
rem ============================================================
:JP_DIAG
echo -----------------------------------------------------------
if %JP_EC% NEQ 0 (
    echo   ERROR: jpackage exited with code %JP_EC%.
) else (
    echo   ERROR: jpackage finished but no output file was produced.
)
echo.

rem -- WiX 7 OSMF EULA (WIX7015) --------------------------------
rem    Official fix for WiX 7.0.0: wix eula accept wix7
findstr /i "WIX7015" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 (
    echo   ==================================================
    echo   Root cause: WiX OSMF EULA not accepted (WIX7015)
    echo   ==================================================
    echo.
    echo   This has nothing to do with Java or Maven.
    echo   WiX 7 requires a one-time EULA acceptance per user.
    echo.
    echo   Run this command ONCE in any terminal, then retry:
    echo.
    echo     wix eula accept wix7
    echo.
    echo   To list available EULAs on your version of WiX:
    echo.
    echo     wix eula -h
    echo.
    echo   Then re-run:  build.bat %~1
    goto :FAILED
)

rem -- Other WiX issues ---------------------------------------
findstr /i "wix" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 (
    echo   WiX issue in log:
    echo     Verify: where wix
    echo     Reinstall: dotnet tool update --global wix
    echo     Workaround: build.bat app-image
    echo.
)

rem -- Icon issues --------------------------------------------
findstr /i "icon" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 (
    echo   Icon issue in log:
    echo     Path used: %ICON%
    echo     Must be a valid Windows ICO file.
    echo.
)

rem -- Version format issues ----------------------------------
findstr /i "version.*invalid\|invalid.*version" "%LOG_FILE%" >nul 2>&1
if not errorlevel 1 (
    echo   Version format issue:
    echo     Value: %APP_VERSION%  - must be X.Y.Z (e.g. 1.2.0)
    echo.
)

echo   Full log: %LOG_FILE%
goto :FAILED

rem ============================================================
rem  SUCCESS
rem ============================================================
:SUCCESS
echo.
echo ===========================================================
echo   BUILD SUCCESSFUL!
echo ===========================================================
echo.
echo   JAR:  target\%JAR_NAME%
echo   Run:  java -jar target\%JAR_NAME%
echo.
if "%ITYPE%"=="app-image" (
    echo   App folder: %OUTPUT_DIR%\%APP_NAME%\
    echo   Launch:     %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
    echo.
    echo   Note: app-image has no Start Menu entry or uninstaller.
    echo   To build a real installer, accept the WiX EULA first:
    echo     wix eula accept wix7
    echo   Then run:  build.bat msi
) else (
    echo   Installer (%ITYPE%):
    for %%F in ("%OUTPUT_DIR%\*.msi" "%OUTPUT_DIR%\*.exe") do (
        if exist "%%F" for %%A in ("%%F") do echo     %%F  (%%~zA bytes)
    )
    echo.
    echo   Creates:
    echo     + Desktop shortcut
    echo     + Start Menu entry  under  %APP_NAME%
    echo     + Entry in Installed Applications (clean uninstall)
    echo     + Upgrade UUID: %APP_UUID%
    echo       Future versions recognized as upgrades.
)
echo.
echo ===========================================================
echo.
pause
exit /b 0

rem ============================================================
rem  FAILURE
rem ============================================================
:FAILED
echo.
echo ===========================================================
echo   BUILD FAILED - See messages above.
echo ===========================================================
if exist "%LOG_FILE%" echo   Full jpackage log: %LOG_FILE%
echo.
pause
exit /b 1

:ERR_NO_MAVEN
echo.
echo   ERROR: mvn not found in PATH.
echo   Install Maven 3.8+: https://maven.apache.org/
goto :FAILED
