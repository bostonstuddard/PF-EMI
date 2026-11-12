@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem Forge-only build; Fabric and NeoForge projects are never built or modified here.
rem v9 checks source AND resulting Forge JAR, preventing stale JAVA_25 runtime crashes.
set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=26.2"
if /I not "%TARGET%"=="26.2" (
    echo [PF-EMI] ERROR: This Forge source targets Minecraft 26.2 only.
    pause
    exit /b 2
)
if not exist "forge\gradlew.bat" (
    echo [PF-EMI] ERROR: Missing forge\gradlew.bat. Run from the project root.
    pause
    exit /b 2
)
if not exist "verify-forge26-jar.ps1" (
    echo [PF-EMI] ERROR: Missing verify-forge26-jar.ps1 from the v9 patch.
    pause
    exit /b 2
)
where powershell.exe >nul 2>nul
if errorlevel 1 (
    echo [PF-EMI] ERROR: Windows PowerShell is required to check the JAR.
    pause
    exit /b 2
)

set "SOURCE_CONFIG=%CD%\forge\src\main\resources\emi-forge26.mixins.json"
echo.
echo [PF-EMI] Checking source Mixin config before building...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CD%\verify-forge26-jar.ps1" -SourceConfig "%SOURCE_CONFIG%"
if errorlevel 1 (
    echo [PF-EMI] STOPPED: Source config still has JAVA_25 or cannot be read.
    echo [PF-EMI] Extract the v9 ZIP into the correct PF-EMI ^(26.2^) root folder.
    pause
    exit /b 2
)

if not exist "release" mkdir "release"
if exist "release\PF-EMI-%TARGET%-FORGE.jar" del /q "release\PF-EMI-%TARGET%-FORGE.jar"
set "LOG_FILE=%CD%\FORGE_BUILD_LOG_%TARGET%.txt"
echo [PF-EMI] Building Forge for Minecraft %TARGET%...
echo [PF-EMI] Full log: %LOG_FILE%
pushd "forge"
call gradlew.bat --no-configuration-cache --console=plain clean build --stacktrace > "%LOG_FILE%" 2>&1
set "BUILD_RESULT=!ERRORLEVEL!"
popd
if not "!BUILD_RESULT!"=="0" goto build_failed

set "FOUND=0"
set "JAR="
for %%F in ("forge\build\libs\PF-EMI-*+forge.jar") do (
    if exist "%%~fF" (
        set /a FOUND+=1 >nul
        set "JAR=%%~fF"
    )
)
if not "!FOUND!"=="1" (
    echo [PF-EMI] ERROR: Expected exactly one Forge release JAR; found !FOUND!.
    start "" notepad.exe "%LOG_FILE%"
    pause
    exit /b 1
)

echo [PF-EMI] Inspecting compiled mod JAR before copying...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CD%\verify-forge26-jar.ps1" -JarPath "!JAR!"
if errorlevel 1 (
    echo [PF-EMI] ERROR: Build produced a JAR with stale or incomplete Forge resources.
    pause
    exit /b 1
)
copy /Y "!JAR!" "release\PF-EMI-%TARGET%-FORGE.jar" >nul
if errorlevel 1 (
    echo [PF-EMI] ERROR: Failed to copy Forge JAR into release folder.
    pause
    exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CD%\verify-forge26-jar.ps1" -JarPath "%CD%\release\PF-EMI-%TARGET%-FORGE.jar"
if errorlevel 1 (
    echo [PF-EMI] ERROR: Release copy did not pass validation.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo [PF-EMI] VERIFIED FORGE BUILD: release\PF-EMI-%TARGET%-FORGE.jar
echo ============================================================
echo.
rem The provided launch log identified this Modrinth profile, despite its old name.
set "MODS_DIR=%APPDATA%\ModrinthApp\profiles\Forge 1.21.7\mods"
if exist "!MODS_DIR!\" (
    echo [PF-EMI] Your previous crash came from this Modrinth profile:
    echo     !MODS_DIR!
    echo.
    choice /C YN /N /M "Replace that profile's Forge JAR with this VERIFIED new build? [Y/N] "
    if errorlevel 2 goto completed
    if exist "!MODS_DIR!\PF-EMI-%TARGET%-FORGE.jar" (
        copy /Y "!MODS_DIR!\PF-EMI-%TARGET%-FORGE.jar" "!MODS_DIR!\PF-EMI-%TARGET%-FORGE.jar.bak" >nul
    )
    copy /Y "release\PF-EMI-%TARGET%-FORGE.jar" "!MODS_DIR!\PF-EMI-%TARGET%-FORGE.jar" >nul
    if errorlevel 1 (
        echo [PF-EMI] ERROR: Unable to copy to the Modrinth mods folder.
        goto completed
    )
    echo [PF-EMI] Installed into the selected Modrinth profile.
    echo [PF-EMI] Previous JAR backed up as a .jar.bak file if present.
) else (
    echo [PF-EMI] Copy the VERIFIED release JAR into the mods folder for your Forge profile.
)
:completed
echo [PF-EMI] Start Minecraft again and capture a NEW log if it still crashes.
start "" explorer.exe "%CD%\release"
pause
exit /b 0

:build_failed
echo.
echo ============================================================
echo [PF-EMI] FORGE BUILD FAILED [exit !BUILD_RESULT!]
echo [PF-EMI] Upload FORGE_BUILD_LOG_%TARGET%.txt from your project root.
echo ============================================================
start "" notepad.exe "%LOG_FILE%"
pause
exit /b !BUILD_RESULT!
