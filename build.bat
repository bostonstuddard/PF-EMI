@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title PF-EMI 26.3 - Fabric / NeoForge / Forge

rem One Windows entry point: build.bat [--forge-only]
rem JDK 25; pauses once on both success and failure. Never opens Explorer.
set "FORGE_ONLY=0"
if /I "%~1"=="--forge-only" set "FORGE_ONLY=1"
if not "%~1"=="" if not "%FORGE_ONLY%"=="1" (
    echo [PF-EMI] Usage: build.bat [--forge-only]
    set "RESULT=2"
    goto done
)

call :CheckPrerequisites
if errorlevel 1 (
    set "RESULT=!ERRORLEVEL!"
    goto done
)
if "%FORGE_ONLY%"=="1" (
    call :BuildForge
) else (
    call :BuildAll
)
set "RESULT=!ERRORLEVEL!"

:done
echo.
if not "!RESULT!"=="0" (
    echo ================================================================
    echo [PF-EMI] BUILD FAILED ^(exit !RESULT!^). See the errors above.
    echo ================================================================
) else (
    echo ================================================================
    echo [PF-EMI] BUILD COMPLETE - release folder:
    if "!FORGE_ONLY!"=="0" (
        echo   1.0+26.3+fabric.jar
        echo   1.0+26.3+neoforge.jar
    )
    echo   1.0+26.3+forge.jar
    echo ================================================================
)
echo [PF-EMI] Press any key to close this window.
pause >nul
endlocal & exit /b %RESULT%

:CheckPrerequisites
if not exist "gradlew.bat" (
    echo [PF-EMI] ERROR: Missing root Gradle wrapper.
    exit /b 2
)
if not exist "forge\gradlew.bat" (
    echo [PF-EMI] ERROR: Missing Forge Gradle wrapper.
    exit /b 2
)
if not exist "scripts\verify-forge-jar.ps1" (
    echo [PF-EMI] ERROR: Missing Forge artifact validation helper.
    exit /b 2
)
where powershell.exe >nul 2>nul
if errorlevel 1 (
    echo [PF-EMI] ERROR: Windows PowerShell is needed for Forge JAR verification.
    exit /b 2
)
where java >nul 2>nul
if errorlevel 1 (
    echo [PF-EMI] ERROR: Java 25 not found on PATH.
    exit /b 2
)
set "JAVA_VERSION="
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /I /C:"version"') do set "JAVA_VERSION=%%~V"
for /f "tokens=1 delims=." %%V in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%V"
if not "!JAVA_MAJOR!"=="25" (
    echo [PF-EMI] ERROR: Minecraft 26.3 needs Java 25. Found !JAVA_VERSION!.
    exit /b 2
)
java -version
rem Native access is required for the Gradle toolchain, not for Minecraft itself.
set "JAVA_OPTS=--enable-native-access=ALL-UNNAMED %JAVA_OPTS%"
if not exist "release" mkdir "release"
exit /b 0

:BuildAll
rem Root project contains the Fabric, NeoForge and shared xplat modules.
for %%L in (fabric neoforge) do (
    if exist "release\1.0+26.3+%%L.jar" del /q "release\1.0+26.3+%%L.jar"
)
echo [PF-EMI] Building Fabric and NeoForge for Minecraft 26.3...
call gradlew.bat --console=plain -PmcTarget=26.3 clean printPortTarget :fabric:build :neoforge:build :fabric:shadowJar :neoforge:shadowJar
if errorlevel 1 exit /b !ERRORLEVEL!

set "FABRIC_JAR="
set "NEO_JAR="
set /a FABRIC_COUNT=0
set /a NEO_COUNT=0
for %%F in ("fabric\build\libs\*+fabric.jar") do if exist "%%~fF" (
    set /a FABRIC_COUNT+=1 >nul
    set "FABRIC_JAR=%%~fF"
)
for %%F in ("neoforge\build\libs\*+neoforge.jar") do if exist "%%~fF" (
    set /a NEO_COUNT+=1 >nul
    set "NEO_JAR=%%~fF"
)
if not "!FABRIC_COUNT!"=="1" (
    echo [PF-EMI] ERROR: Expected exactly one Fabric release JAR; found !FABRIC_COUNT!.
    exit /b 1
)
if not "!NEO_COUNT!"=="1" (
    echo [PF-EMI] ERROR: Expected exactly one NeoForge release JAR; found !NEO_COUNT!.
    exit /b 1
)
for %%F in ("!FABRIC_JAR!") do if %%~zF LSS 100000 (
    echo [PF-EMI] ERROR: Fabric JAR is too small.
    exit /b 1
)
for %%F in ("!NEO_JAR!") do if %%~zF LSS 100000 (
    echo [PF-EMI] ERROR: NeoForge JAR is too small.
    exit /b 1
)
copy /Y "!FABRIC_JAR!" "release\1.0+26.3+fabric.jar" >nul
if errorlevel 1 exit /b 1
copy /Y "!NEO_JAR!" "release\1.0+26.3+neoforge.jar" >nul
if errorlevel 1 exit /b 1

echo [PF-EMI] Fabric and NeoForge builds complete; starting Forge...
call :BuildForge
exit /b !ERRORLEVEL!

:BuildForge
rem Forge is an independent ForgeGradle project, NOT part of root settings.gradle.
set "FORGE_CONFIG=%CD%\forge\src\main\resources\emi-forge26.mixins.json"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CD%\scripts\verify-forge-jar.ps1" -SourceConfig "!FORGE_CONFIG!"
if errorlevel 1 exit /b 1
if exist "release\1.0+26.3+forge.jar" del /q "release\1.0+26.3+forge.jar"
echo [PF-EMI] Building Forge 26.3...
pushd "forge"
call gradlew.bat --no-configuration-cache --console=plain clean build
set "FORGE_BUILD_RESULT=!ERRORLEVEL!"
popd
if not "!FORGE_BUILD_RESULT!"=="0" exit /b !FORGE_BUILD_RESULT!
set "FORGE_JAR="
set /a FORGE_COUNT=0
for %%F in ("forge\build\libs\*+forge.jar") do if exist "%%~fF" (
    set /a FORGE_COUNT+=1 >nul
    set "FORGE_JAR=%%~fF"
)
if not "!FORGE_COUNT!"=="1" (
    echo [PF-EMI] ERROR: Expected exactly one Forge release JAR; found !FORGE_COUNT!.
    exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CD%\scripts\verify-forge-jar.ps1" -JarPath "!FORGE_JAR!"
if errorlevel 1 exit /b 1
copy /Y "!FORGE_JAR!" "release\1.0+26.3+forge.jar" >nul
if errorlevel 1 exit /b 1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CD%\scripts\verify-forge-jar.ps1" -JarPath "%CD%\release\1.0+26.3+forge.jar"
if errorlevel 1 exit /b 1
exit /b 0
