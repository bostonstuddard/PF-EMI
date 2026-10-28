@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo [PF-EMI pf.11] Checking extracted source...
if not exist "xplat\src\main\java\dev\emi\emi\ProjectFlintPlugin.java" (
    echo ERROR: Extract patch files directly into your existing PF-EMI source root.
    exit /b 1
)
findstr /c:"mod_version=1.1.23-pf.11" gradle.properties >nul 2>&1
if errorlevel 1 (
    echo ERROR: pf.11 gradle.properties is not in this folder.
    exit /b 1
)
echo [PF-EMI pf.11] Removing obsolete standalone changelog fragments...
if exist "PF_EMI_PATCH_MANIFEST.md" del /q "PF_EMI_PATCH_MANIFEST.md"
if exist "PF_EMI_PATCH_NOTES-v2.md" del /q "PF_EMI_PATCH_NOTES-v2.md"
if exist "PF_EMI_PF10_AUDIT.md" del /q "PF_EMI_PF10_AUDIT.md"
if exist "PF_EMI_PF3_BUILD_FIX.md" del /q "PF_EMI_PF3_BUILD_FIX.md"
if exist "PF_EMI_PF5_HEAP_FIX.md" del /q "PF_EMI_PF5_HEAP_FIX.md"
if exist "PF_EMI_PF6_MIXIN_FIX.md" del /q "PF_EMI_PF6_MIXIN_FIX.md"
if exist "PF_EMI_PF7_3D_STRUCTURE_FIX.md" del /q "PF_EMI_PF7_3D_STRUCTURE_FIX.md"
if exist "PF_EMI_PF8_SOURCE_AUDIT.md" del /q "PF_EMI_PF8_SOURCE_AUDIT.md"
if exist "PF_EMI_PF9_STRUCTURE_UI.md" del /q "PF_EMI_PF9_STRUCTURE_UI.md"
echo [PF-EMI pf.11] Cleanup complete. The unified changelog is CHANGELOG.md.
echo [PF-EMI pf.11] Building Fabric...
call build.bat
exit /b %errorlevel%
