@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem ============================================================
rem PF-EMI clean release builder
rem
rem Output names are based on minecraft_version in gradle.properties:
rem   PF-EMI-<mc version>.jar
rem   PF-EMI-<mc version>-NEOFORGE.jar
rem
rem Example for Minecraft 1.21.3:
rem   PF-EMI-1.21.3.jar
rem   PF-EMI-1.21.3-NEOFORGE.jar
rem ============================================================

set "OUT_DIR=%~dp0release"
set "MC_VERSION="

if not exist "gradle.properties" (
	echo [PF-EMI] ERROR: gradle.properties was not found in the project root.
	pause
	exit /b 1
)

rem Read minecraft_version directly from gradle.properties so the filename
rem automatically follows whatever Minecraft version this port targets.
for /f "usebackq tokens=1,* delims==" %%A in ("gradle.properties") do (
	if /I "%%A"=="minecraft_version" set "MC_VERSION=%%B"
)

rem Trim leading spaces if present.
for /f "tokens=* delims= " %%A in ("!MC_VERSION!") do set "MC_VERSION=%%A"

if not defined MC_VERSION (
	echo [PF-EMI] ERROR: Could not find minecraft_version in gradle.properties.
	pause
	exit /b 1
)

title PF-EMI !MC_VERSION! Build

echo [PF-EMI] Target Minecraft version: !MC_VERSION!

where java >nul 2>&1
if errorlevel 1 (
	echo [PF-EMI] ERROR: Java was not found in PATH.
	echo Install Java 21 and make sure java.exe is available in PATH.
	pause
	exit /b 1
)

echo.
echo [PF-EMI] Java runtime found in PATH:
java -version
if errorlevel 1 (
	pause
	exit /b 1
)

rem Resolve java.home from the exact java.exe that succeeded above.
set "PFEMI_JAVA_HOME="
for /f "tokens=1,* delims==" %%A in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /C:"java.home ="') do (
	set "PFEMI_JAVA_HOME=%%B"
)
for /f "tokens=* delims= " %%A in ("!PFEMI_JAVA_HOME!") do set "PFEMI_JAVA_HOME=%%A"

if not defined PFEMI_JAVA_HOME (
	echo.
	echo [PF-EMI] ERROR: Could not determine java.home from Java in PATH.
	pause
	exit /b 1
)

if not exist "!PFEMI_JAVA_HOME!\bin\java.exe" (
	echo.
	echo [PF-EMI] ERROR: Resolved Java home is invalid:
	echo [PF-EMI]   !PFEMI_JAVA_HOME!
	pause
	exit /b 1
)

rem Force Gradle onto the same Java 21 runtime.
set "JAVA_HOME=!PFEMI_JAVA_HOME!"
set "PATH=!JAVA_HOME!\bin;%PATH%"

echo.
echo [PF-EMI] Gradle runtime verification:
call gradlew.bat --version --no-daemon "-Dorg.gradle.java.home=!JAVA_HOME!"
if errorlevel 1 (
	echo.
	echo [PF-EMI] ERROR: Gradle could not start with Java 21.
	pause
	exit /b 1
)

rem Start with one completely clean release folder.
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo.
echo [PF-EMI] Building final Fabric and NeoForge jars for Minecraft !MC_VERSION!...

rem remapJar creates the distributable mod jars without intentionally building
rem sources/API artifacts.
call gradlew.bat :fabric:clean :neoforge:clean :fabric:remapJar :neoforge:remapJar --no-daemon "-Dorg.gradle.java.home=!JAVA_HOME!"
if errorlevel 1 (
	echo.
	echo [PF-EMI] Build failed. See the Gradle output above.
	pause
	exit /b 1
)

rem Locate only the final distributable jars.
set "FABRIC_JAR="
for %%F in ("fabric\build\libs\*.jar") do (
	set "JAR_NAME=%%~nxF"
	echo(!JAR_NAME! | findstr /I /C:"dev-shadow" /C:"sources" /C:"api" >nul
	if errorlevel 1 set "FABRIC_JAR=%%~fF"
)

set "NEOFORGE_JAR="
for %%F in ("neoforge\build\libs\*.jar") do (
	set "JAR_NAME=%%~nxF"
	echo(!JAR_NAME! | findstr /I /C:"dev-shadow" /C:"sources" /C:"api" >nul
	if errorlevel 1 set "NEOFORGE_JAR=%%~fF"
)

if not defined FABRIC_JAR (
	echo.
	echo [PF-EMI] ERROR: Could not find the final Fabric jar.
	echo [PF-EMI] Check fabric\build\libs\
	pause
	exit /b 1
)

if not defined NEOFORGE_JAR (
	echo.
	echo [PF-EMI] ERROR: Could not find the final NeoForge jar.
	echo [PF-EMI] Check neoforge\build\libs\
	pause
	exit /b 1
)

set "FABRIC_OUT=%OUT_DIR%\PF-EMI-!MC_VERSION!.jar"
set "NEOFORGE_OUT=%OUT_DIR%\PF-EMI-!MC_VERSION!-NEOFORGE.jar"

copy /Y "!FABRIC_JAR!" "!FABRIC_OUT!" >nul
if errorlevel 1 (
	echo [PF-EMI] ERROR: Failed to copy the Fabric jar.
	pause
	exit /b 1
)

copy /Y "!NEOFORGE_JAR!" "!NEOFORGE_OUT!" >nul
if errorlevel 1 (
	echo [PF-EMI] ERROR: Failed to copy the NeoForge jar.
	pause
	exit /b 1
)

rem Remove noisy loader build/libs folders after the clean jars have been copied.
if exist "fabric\build\libs" rmdir /s /q "fabric\build\libs"
if exist "neoforge\build\libs" rmdir /s /q "neoforge\build\libs"

echo.
echo ============================================================
echo [PF-EMI] BUILD COMPLETE
echo ============================================================
echo [PF-EMI] Fabric:   release\PF-EMI-!MC_VERSION!.jar
echo [PF-EMI] NeoForge: release\PF-EMI-!MC_VERSION!-NEOFORGE.jar
echo ============================================================

echo [PF-EMI] Opening release folder...
start "" explorer.exe "%OUT_DIR%"

exit /b 0
