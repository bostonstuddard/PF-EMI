@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem ============================================================
rem PF-EMI clean release builder
rem
rem This project MUST run Gradle with Java 21.
rem The script intentionally ignores Java 25 in PATH and searches
rem for an installed Java 21 JDK before Gradle is started.
rem
rem Optional override:
rem   set PFEMI_JAVA21_HOME=C:\Path\To\jdk-21
rem
rem Output:
rem   PF-EMI-<mc version>.jar
rem   PF-EMI-<mc version>-NEOFORGE.jar
rem ============================================================

set "OUT_DIR=%~dp0release"
set "MC_VERSION="
set "PFEMI_JAVA_HOME="
set "JAVA_CHECK=%TEMP%\pfemi-java21-check-%RANDOM%-%RANDOM%.txt"

if not exist "gradle.properties" (
	echo [PF-EMI] ERROR: gradle.properties was not found in the project root.
	pause
	exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("gradle.properties") do (
	if /I "%%A"=="minecraft_version" set "MC_VERSION=%%B"
)
for /f "tokens=* delims= " %%A in ("!MC_VERSION!") do set "MC_VERSION=%%A"

if not defined MC_VERSION (
	echo [PF-EMI] ERROR: Could not find minecraft_version in gradle.properties.
	pause
	exit /b 1
)

title PF-EMI !MC_VERSION! Build

echo [PF-EMI] Target Minecraft version: !MC_VERSION!
echo.
echo [PF-EMI] Looking for a Java 21 JDK...

rem 1. Explicit PF-EMI override, if the user has set one.
if defined PFEMI_JAVA21_HOME call :try_java_home "%PFEMI_JAVA21_HOME%"

rem 2. Existing JAVA_HOME, but only if it is actually Java 21.
if not defined PFEMI_JAVA_HOME if defined JAVA_HOME call :try_java_home "%JAVA_HOME%"

rem 3. Every java.exe currently visible in PATH.
if not defined PFEMI_JAVA_HOME (
	for /f "delims=" %%J in ('where java.exe 2^>nul') do (
		if not defined PFEMI_JAVA_HOME (
			for %%P in ("%%~dpJ..") do call :try_java_home "%%~fP"
		)
	)
)

rem 4. Common Windows JDK install locations.
if not defined PFEMI_JAVA_HOME (
	for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do if not defined PFEMI_JAVA_HOME call :try_java_home "%%~fD"
	for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do if not defined PFEMI_JAVA_HOME call :try_java_home "%%~fD"
	for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do if not defined PFEMI_JAVA_HOME call :try_java_home "%%~fD"
	for /d %%D in ("%ProgramFiles%\Zulu\zulu-21*") do if not defined PFEMI_JAVA_HOME call :try_java_home "%%~fD"
	for /d %%D in ("%ProgramFiles%\BellSoft\LibericaJDK-21*") do if not defined PFEMI_JAVA_HOME call :try_java_home "%%~fD"
	for /d %%D in ("%USERPROFILE%\.jdks\*21*") do if not defined PFEMI_JAVA_HOME call :try_java_home "%%~fD"
)

if exist "%JAVA_CHECK%" del /q "%JAVA_CHECK%" >nul 2>&1

if not defined PFEMI_JAVA_HOME (
	echo.
	echo [PF-EMI] ERROR: No Java 21 JDK was found.
	echo [PF-EMI] Java 25 cannot run this Gradle 8.14 / Loom build.
	echo.
	echo Install a Java 21 JDK ^(Temurin 21 recommended^) and run this file again.
	echo Or set an explicit path before building:
	echo.
	echo     set PFEMI_JAVA21_HOME=C:\Path\To\jdk-21
	echo     build.bat
	echo.
	pause
	exit /b 1
)

rem Force Gradle and all child processes onto the verified Java 21 JDK.
set "JAVA_HOME=!PFEMI_JAVA_HOME!"
set "PATH=!JAVA_HOME!\bin;%PATH%"

rem Also override common Gradle/Java launcher variables that can silently
rem point at a different JDK on developer machines.
set "JDK_HOME=!JAVA_HOME!"
set "GRADLE_JAVA_HOME=!JAVA_HOME!"

echo.
echo [PF-EMI] Using Java 21 JDK:
echo [PF-EMI]   !JAVA_HOME!
"!JAVA_HOME!\bin\java.exe" -version
if errorlevel 1 (
	echo.
	echo [PF-EMI] ERROR: The selected Java 21 runtime could not start.
	pause
	exit /b 1
)

echo.
echo [PF-EMI] Gradle runtime verification:
call gradlew.bat --version --no-daemon "-Dorg.gradle.java.home=!JAVA_HOME!"
if errorlevel 1 (
	echo.
	echo [PF-EMI] ERROR: Gradle could not start with the selected Java 21 JDK.
	pause
	exit /b 1
)

rem Start with one completely clean release folder.
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo.
echo [PF-EMI] Building final Fabric and NeoForge jars for Minecraft !MC_VERSION!...
call gradlew.bat :fabric:clean :neoforge:clean :fabric:remapJar :neoforge:remapJar --no-daemon "-Dorg.gradle.java.home=!JAVA_HOME!"
if errorlevel 1 (
	echo.
	echo [PF-EMI] Build failed. See the Gradle output above.
	pause
	exit /b 1
)

rem Locate only final distributable jars.
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

rem Remove noisy loader build/libs folders after copying the release jars.
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

:try_java_home
if defined PFEMI_JAVA_HOME exit /b 0
set "CANDIDATE=%~1"
if not defined CANDIDATE exit /b 0
if not exist "%CANDIDATE%\bin\java.exe" exit /b 0

"%CANDIDATE%\bin\java.exe" -XshowSettings:properties -version > "%JAVA_CHECK%" 2>&1
findstr /C:"java.specification.version = 21" "%JAVA_CHECK%" >nul 2>&1
if not errorlevel 1 set "PFEMI_JAVA_HOME=%CANDIDATE%"
exit /b 0
