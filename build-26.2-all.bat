@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title PF-EMI Minecraft 26.2 - Three Loader Build

echo [PF-EMI] Minecraft 26.2 / Fabric + NeoForge + Forge
for /f "tokens=2 delims==" %%V in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /C:"java.specification.version ="') do set "VER=%%V"
for /f "tokens=*" %%V in ("%VER%") do set "VER=%%V"
java -version
if not "%VER%"=="25" (
 echo [PF-EMI] Java 25 must be installed and selected in PATH.
 pause
 exit /b 2
)

call build-26x.bat 26.2
if errorlevel 1 (
 echo [PF-EMI] Fabric/NeoForge 26.2 build FAILED. Forge was not started.
 pause
 exit /b 1
)

call build-forge-26x.bat 26.2
if errorlevel 1 (
 echo [PF-EMI] Forge 26.2 build FAILED. Existing Fabric/NeoForge output is preserved.
 pause
 exit /b 1
)

echo [PF-EMI] Three-loader 26.2 build completed. Runtime testing remains necessary.
if exist "release" start "" explorer.exe "%CD%\release"
pause
exit /b 0
