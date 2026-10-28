@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title PF-EMI 1.1.23-pf.12 - Minecraft 1.21.11
if not exist "gradlew.bat" (
    echo [PF-EMI] ERROR: Missing gradlew.bat; extract the full PF-EMI source archive.
    exit /b 1
)
if not exist "settings.gradle" (
    echo [PF-EMI] ERROR: Extract this ZIP before building, then run build.bat in the source root.
    exit /b 1
)

rem Respect a user-configured JDK first; otherwise locate Java 21 from common paths.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto java_found
for %%P in (
    "C:\Program Files\Java\jdk-21.0.12.1"
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
    "C:\Program Files\Eclipse Adoptium\jdk-21"
) do if exist "%%~P\bin\java.exe" (
    set "JAVA_HOME=%%~P"
    goto java_found
)
for /f "delims=" %%I in ('where java 2^>nul') do (
    for %%D in ("%%~dpI..") do set "JAVA_HOME=%%~fD"
    goto java_found
)
echo [PF-EMI] ERROR: Java 21 was not found. Set JAVA_HOME to your Java 21 JDK.
exit /b 1
:java_found
set "PATH=%JAVA_HOME%\bin;%PATH%"
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /c:"21." >nul
if errorlevel 1 (
    echo [PF-EMI] ERROR: A Java 21 JDK is required. Found:
    "%JAVA_HOME%\bin\java.exe" -version
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [PF-EMI] ERROR: JAVA_HOME points to a JRE instead of a full JDK.
    exit /b 1
)
echo [PF-EMI] ProjectFlint integration pf.12 - Minecraft 1.21.11
call gradlew.bat :fabric:clean :fabric:build --no-daemon --max-workers=2
if errorlevel 1 (
    echo [PF-EMI] Build failed. Review the Gradle errors above.
    exit /b 1
)
if not exist dist mkdir dist
set "OUTPUT=dist\PF-EMI-1.21.11-pf.12.jar"
if exist "%OUTPUT%" del /q "%OUTPUT%"
for %%F in ("fabric\build\libs\*.jar") do (
    echo %%~nxF | findstr /i /c:"sources" /c:"dev-shadow" /c:"-api" >nul
    if errorlevel 1 if not exist "%OUTPUT%" copy /y "%%~fF" "%OUTPUT%" >nul
)
if not exist "%OUTPUT%" (
    echo [PF-EMI] ERROR: No deployable Fabric JAR found.
    exit /b 1
)
echo [PF-EMI] Build successful: %OUTPUT%
exit /b 0
