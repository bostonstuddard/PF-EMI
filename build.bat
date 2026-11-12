@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem ============================================================
rem PF-EMI clean release builder
rem Minecraft 1.21.11 loader targets:
rem 	Fabric
rem 	Forge
rem 	NeoForge
rem ============================================================

set "OUT_DIR=%~dp0release"
set "MC_VERSION="

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

if /I not "!MC_VERSION!"=="1.21.11" (
	echo [PF-EMI] ERROR: This release builder expects Minecraft 1.21.11.
	echo [PF-EMI] gradle.properties currently targets !MC_VERSION!.
	pause
	exit /b 1
)

title PF-EMI !MC_VERSION! Build

echo [PF-EMI] Target Minecraft version: !MC_VERSION!
echo [PF-EMI] Loader targets: Fabric, Forge, NeoForge

echo.
echo [PF-EMI] Looking for a Java 21 JDK...
set "PFEMI_JAVA_HOME="

for %%J in (
	"C:\Program Files\Java\jdk-21.0.12.1"
	"C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
	"C:\Program Files\Eclipse Adoptium\jdk-21*"
	"C:\Program Files\Microsoft\jdk-21*"
	"C:\Program Files\Zulu\zulu-21*"
) do (
	if not defined PFEMI_JAVA_HOME (
		for /d %%D in (%%J) do (
			if exist "%%~fD\bin\java.exe" set "PFEMI_JAVA_HOME=%%~fD"
		)
	)
)

if not defined PFEMI_JAVA_HOME (
	where java >nul 2>&1
	if not errorlevel 1 (
		for /f "tokens=1,* delims==" %%A in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /C:"java.home ="') do set "PFEMI_JAVA_HOME=%%B"
		for /f "tokens=* delims= " %%A in ("!PFEMI_JAVA_HOME!") do set "PFEMI_JAVA_HOME=%%A"
	)
)

if not defined PFEMI_JAVA_HOME (
	echo [PF-EMI] ERROR: Could not find Java 21.
	pause
	exit /b 1
)

set "JAVA_HOME=!PFEMI_JAVA_HOME!"
set "PATH=!JAVA_HOME!\bin;%PATH%"
set "JAVA_VERSION="
set "JAVA_MAJOR="
set "JAVA_INFO_FILE=%TEMP%\pfemi-java-info-!RANDOM!-!RANDOM!.txt"

rem Do not run the quoted Java path inside FOR /F command substitution.
rem cmd.exe can split C:\Program Files\... at the space in that context.
"!JAVA_HOME!\bin\java.exe" -XshowSettings:properties -version >"!JAVA_INFO_FILE!" 2>&1
if errorlevel 1 (
	echo.
	echo [PF-EMI] ERROR: Java exists at !JAVA_HOME! but could not be executed.
	if exist "!JAVA_INFO_FILE!" type "!JAVA_INFO_FILE!"
	if exist "!JAVA_INFO_FILE!" del /q "!JAVA_INFO_FILE!" >nul 2>&1
	pause
	exit /b 1
)

for /f "tokens=2 delims==" %%V in ('findstr /C:"java.specification.version =" "!JAVA_INFO_FILE!"') do set "JAVA_VERSION=%%V"
for /f "tokens=* delims= " %%V in ("!JAVA_VERSION!") do set "JAVA_VERSION=%%V"
if defined JAVA_VERSION for /f "tokens=1 delims=." %%V in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%V"

echo [PF-EMI] Using Java:
echo [PF-EMI] 	!JAVA_HOME!
"!JAVA_HOME!\bin\java.exe" -version
if exist "!JAVA_INFO_FILE!" del /q "!JAVA_INFO_FILE!" >nul 2>&1

if not "!JAVA_MAJOR!"=="21" (
	echo.
	if defined JAVA_VERSION (
		echo [PF-EMI] ERROR: Java 21 is required. Detected Java !JAVA_VERSION!.
	) else (
		echo [PF-EMI] ERROR: Java 21 is required, but the Java version could not be detected.
	)
	pause
	exit /b 1
)

echo.
echo [PF-EMI] Gradle runtime verification:
call gradlew.bat --version --no-daemon "-Dorg.gradle.java.home=!JAVA_HOME!"
if errorlevel 1 (
	echo.
	echo [PF-EMI] ERROR: Gradle could not start with Java 21.
	pause
	exit /b 1
)

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

rem Remove stale loader build products so an old jar can never be mistaken
rem for the current build.
for %%L in (fabric forge neoforge) do (
	if exist "%%L\build\libs" rmdir /s /q "%%L\build\libs"
)

echo.
echo [PF-EMI] Building Fabric, Forge, and NeoForge for Minecraft !MC_VERSION!...
call gradlew.bat :fabric:clean :forge:clean :neoforge:clean :fabric:remapJar :forge:remapJar :neoforge:remapJar --no-daemon "-Dorg.gradle.java.home=!JAVA_HOME!"
if errorlevel 1 (
	echo.
	echo [PF-EMI] BUILD FAILED. The release folder has not been populated.
	pause
	exit /b 1
)

call :FindFinalJar fabric FABRIC_JAR
if errorlevel 1 goto :missing_jar
call :FindFinalJar forge FORGE_JAR
if errorlevel 1 goto :missing_jar
call :FindFinalJar neoforge NEOFORGE_JAR
if errorlevel 1 goto :missing_jar

set "FABRIC_OUT=%OUT_DIR%\PF-EMI-!MC_VERSION!.jar"
set "FORGE_OUT=%OUT_DIR%\PF-EMI-!MC_VERSION!-FORGE.jar"
set "NEOFORGE_OUT=%OUT_DIR%\PF-EMI-!MC_VERSION!-NEOFORGE.jar"

copy /Y "!FABRIC_JAR!" "!FABRIC_OUT!" >nul || goto :copy_failed
copy /Y "!FORGE_JAR!" "!FORGE_OUT!" >nul || goto :copy_failed
copy /Y "!NEOFORGE_JAR!" "!NEOFORGE_OUT!" >nul || goto :copy_failed

for %%L in (fabric forge neoforge) do (
	if exist "%%L\build\libs" rmdir /s /q "%%L\build\libs"
)

echo.
echo ============================================================
echo [PF-EMI] BUILD COMPLETE
 echo ============================================================
echo [PF-EMI] Fabric:   release\PF-EMI-!MC_VERSION!.jar
echo [PF-EMI] Forge:    release\PF-EMI-!MC_VERSION!-FORGE.jar
echo [PF-EMI] NeoForge: release\PF-EMI-!MC_VERSION!-NEOFORGE.jar
echo ============================================================
echo.
echo [PF-EMI] Opening release folder...
start "" explorer.exe "%OUT_DIR%"
exit /b 0

:FindFinalJar
set "%~2="
for %%F in ("%~1\build\libs\*.jar") do (
	set "JAR_NAME=%%~nxF"
	echo(!JAR_NAME! | findstr /I /C:"dev-shadow" /C:"sources" /C:"api" /C:"filtered" >nul
	if errorlevel 1 set "%~2=%%~fF"
)
if not defined %~2 exit /b 1
exit /b 0

:missing_jar
echo.
echo [PF-EMI] ERROR: A final distributable jar could not be found.
echo [PF-EMI] Check fabric\build\libs, forge\build\libs, and neoforge\build\libs.
pause
exit /b 1

:copy_failed
echo.
echo [PF-EMI] ERROR: Failed to copy a final jar into release\.
pause
exit /b 1
