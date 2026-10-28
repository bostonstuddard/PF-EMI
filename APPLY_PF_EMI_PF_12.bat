@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo [PF-EMI pf.12] Checking source root...
if not exist "xplat\src\main\java\dev\emi\emi\ProjectFlintPlugin.java" (
  echo ERROR: Extract patch entries directly over your PF-EMI source directory.
  exit /b 1
)
findstr /c:"mod_version=1.1.23-pf.12" gradle.properties >nul 2>&1
if errorlevel 1 (
  echo ERROR: This source root does not contain the pf.12 gradle.properties update.
  exit /b 1
)
if exist "APPLY_PF_EMI_PF_11.bat" del /q "APPLY_PF_EMI_PF_11.bat"
echo [PF-EMI pf.12] Patch applied. Changelog and checklist are in CHANGELOG.md and docs.
echo [PF-EMI pf.12] Running Fabric build...
call build.bat
exit /b %errorlevel%
