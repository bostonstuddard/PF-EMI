@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem PF-EMI 26.x Fabric + NeoForge + experimental Forge builds.
rem This source checkout is compatible with 26.2 only.
set "MC_TARGET=%~1"
if "%MC_TARGET%"=="" set "MC_TARGET=26.2"
if not "%MC_TARGET%"=="26.2" (
	echo [PF-EMI] ERROR: This source checkout is for Minecraft 26.2, not %MC_TARGET%.
	echo [PF-EMI] Other version profiles alone do not port the Java source.
	exit /b 2
)
set "PROFILE=versions\%MC_TARGET%.properties"
if not exist "%PROFILE%" (
	echo [PF-EMI] ERROR: Missing %PROFILE%.
	exit /b 2
)
if not exist "gradlew.bat" (
	echo [PF-EMI] ERROR: Run this script from the source project's root folder.
	exit /b 2
)

where java >nul 2>nul
if errorlevel 1 (
	echo [PF-EMI] ERROR: Java was not found. Minecraft 26.2 requires Java 25.
	exit /b 2
)
java -version

rem Keep original gradle.properties unchanged; apply the 26.2 profile to Gradle.
set "PROFILE_ARGS="
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%PROFILE%") do (
	if not "%%A"=="" (
		set "PROFILE_ARGS=!PROFILE_ARGS! -P%%A=%%B"
	)
)

rem No previous releases should be mistaken for a newly successful build.
if not exist "release" mkdir "release"
if exist "release\PF-EMI-%MC_TARGET%.jar" del /q "release\PF-EMI-%MC_TARGET%.jar"
if exist "release\PF-EMI-%MC_TARGET%-NEOFORGE.jar" del /q "release\PF-EMI-%MC_TARGET%-NEOFORGE.jar"
if exist "release\PF-EMI-%MC_TARGET%-FORGE.jar" del /q "release\PF-EMI-%MC_TARGET%-FORGE.jar"

echo.
echo [PF-EMI] Building Fabric and NeoForge for Minecraft %MC_TARGET%...
echo [PF-EMI] Forge is built separately through ForgeGradle 7 after Fabric and NeoForge.
call gradlew.bat %PROFILE_ARGS% "-PmcTarget=%MC_TARGET%" clean printPortTarget :fabric:build :neoforge:build :fabric:shadowJar :neoforge:shadowJar
set "BUILD_RESULT=!ERRORLEVEL!"
if not "!BUILD_RESULT!"=="0" (
	echo.
	echo [PF-EMI] ERROR: Gradle build failed with code !BUILD_RESULT!.
	echo [PF-EMI] The release folder has not been populated.
	exit /b !BUILD_RESULT!
)

rem In this project 'shadowJar' (NOT the root :jar) is the playable release artifact.
rem The version automatically ends in +fabric.jar or +neoforge.jar.
set "FABRIC_JAR="
set "NEOFORGE_JAR="
set /a FABRIC_COUNT=0
set /a NEOFORGE_COUNT=0
for %%F in ("fabric\build\libs\*+fabric.jar") do (
	if exist "%%~fF" (
		set /a FABRIC_COUNT+=1 >nul
		set "FABRIC_JAR=%%~fF"
	)
)
for %%F in ("neoforge\build\libs\*+neoforge.jar") do (
	if exist "%%~fF" (
		set /a NEOFORGE_COUNT+=1 >nul
		set "NEOFORGE_JAR=%%~fF"
	)
)
if not "!FABRIC_COUNT!"=="1" (
	echo [PF-EMI] ERROR: Expected exactly one Fabric release JAR; found !FABRIC_COUNT!.
	echo [PF-EMI] Check fabric\build\libs\ and ensure :fabric:shadowJar succeeded.
	exit /b 1
)
if not "!NEOFORGE_COUNT!"=="1" (
	echo [PF-EMI] ERROR: Expected exactly one NeoForge release JAR; found !NEOFORGE_COUNT!.
	echo [PF-EMI] Check neoforge\build\libs\ and ensure :neoforge:shadowJar succeeded.
	exit /b 1
)

rem Catch accidental tiny root/API/dev outputs masquerading as release JARs.
for %%F in ("!FABRIC_JAR!") do (
	if %%~zF LSS 100000 (
		echo [PF-EMI] ERROR: Fabric release JAR is unexpectedly small [%%~zF bytes].
		exit /b 1
	)
)
for %%F in ("!NEOFORGE_JAR!") do (
	if %%~zF LSS 100000 (
		echo [PF-EMI] ERROR: NeoForge release JAR is unexpectedly small [%%~zF bytes].
		exit /b 1
	)
)

if not exist "release" mkdir "release"
copy /Y "!FABRIC_JAR!" "release\PF-EMI-%MC_TARGET%.jar" >nul
if errorlevel 1 exit /b 1
copy /Y "!NEOFORGE_JAR!" "release\PF-EMI-%MC_TARGET%-NEOFORGE.jar" >nul
if errorlevel 1 exit /b 1

echo.
echo ============================================================
echo [PF-EMI] FABRIC AND NEOFORGE BUILD COMPLETE
 echo ============================================================
echo   release\PF-EMI-%MC_TARGET%.jar
echo   release\PF-EMI-%MC_TARGET%-NEOFORGE.jar
echo.
echo [PF-EMI] Building experimental Forge 26.2 using ForgeGradle...
if not exist "forge\gradlew.bat" (
    echo [PF-EMI] ERROR: Forge wrapper not found.
    exit /b 1
)
pushd "forge"
call gradlew.bat clean build
set "FORGE_BUILD_RESULT=!ERRORLEVEL!"
popd
if not "!FORGE_BUILD_RESULT!"=="0" (
    echo [PF-EMI] Forge FAILED with exit !FORGE_BUILD_RESULT!. Fabric and NeoForge JARs remain available.
    exit /b !FORGE_BUILD_RESULT!
)
set "FORGE_JAR="
set /a FORGE_COUNT=0
for %%F in ("forge\build\libs\PF-EMI-*.jar") do (
    if exist "%%~fF" (
        if /I not "%%~nxF"=="PF-EMI-sources.jar" (
            set /a FORGE_COUNT+=1 >nul
            set "FORGE_JAR=%%~fF"
        )
    )
)
if not "!FORGE_COUNT!"=="1" (
    echo [PF-EMI] ERROR: Expected one Forge mod JAR, found !FORGE_COUNT!.
    exit /b 1
)
for %%F in ("!FORGE_JAR!") do (
    if %%~zF LSS 100000 (
        echo [PF-EMI] ERROR: Forge JAR looks too small.
        exit /b 1
    )
)
copy /Y "!FORGE_JAR!" "release\PF-EMI-%MC_TARGET%-FORGE.jar" >nul
if errorlevel 1 exit /b 1

echo [PF-EMI] Forge release: release\PF-EMI-%MC_TARGET%-FORGE.jar
echo [PF-EMI] Minecraft runtime compatibility still needs in-game testing.
echo ============================================================
start "" explorer.exe "%CD%\release"
exit /b 0
