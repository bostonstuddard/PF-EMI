@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title PF-EMI 1.21.11 Repair + Build

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "RELEASE=1"

if not exist "%JAVA_HOME%\bin\java.exe" (
	echo [PF-EMI] ERROR: Expected Java 21 was not found at:
	echo %JAVA_HOME%
	echo.
	pause
	exit /b 1
)

echo [PF-EMI] Repairing build configuration...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0FORCE_JAVA21_REPAIR.ps1"

if errorlevel 1 (
	echo.
	echo [PF-EMI] Repair failed. See the PowerShell error above.
	pause
	exit /b 1
)

echo.
echo [PF-EMI] Java runtime:
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo [PF-EMI] Wrapper/runtime verification:
call gradlew.bat --version --no-daemon

if errorlevel 1 (
	echo.
	echo [PF-EMI] Gradle wrapper verification failed.
	pause
	exit /b 1
)

echo.
echo [PF-EMI] Building Fabric 1.21.11...
call gradlew.bat :fabric:clean :fabric:build --no-daemon

if errorlevel 1 (
	echo.
	echo [PF-EMI] Build failed. See the Gradle output above.
	pause
	exit /b 1
)

if not exist "dist" mkdir "dist"
if exist "dist\PF-EMI-1.21.11.jar" del /q "dist\PF-EMI-1.21.11.jar"

set "FOUND_JAR="
for %%F in ("fabric\build\libs\*.jar") do (
	echo %%~nxF | findstr /i "sources api dev-shadow" >nul
	if errorlevel 1 (
		copy /y "%%~fF" "dist\PF-EMI-1.21.11.jar" >nul
		set "FOUND_JAR=1"
	)
)

if not defined FOUND_JAR (
	echo.
	echo [PF-EMI] ERROR: Gradle succeeded but no main Fabric jar was found.
	echo Check fabric\build\libs\
	pause
	exit /b 1
)

echo.
echo [PF-EMI] Build complete.
echo [PF-EMI] Output: dist\PF-EMI-1.21.11.jar
start "" explorer.exe "%CD%\dist"
exit /b 0
