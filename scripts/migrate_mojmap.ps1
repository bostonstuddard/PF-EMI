$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$staged = Join-Path $root 'ports\26x\src\main\java'
if (-not (Test-Path $staged)) { throw 'Run python ports/26x/scripts/prepare_port.py first.' }
Push-Location $root
try {
    & .\gradlew.bat ':xplat:migrateMappings' '--input' $staged '--output' $staged '--mappings' 'net.minecraft:mappings:1.21.11' '--overrideInputsIHaveABackup'
    if ($LASTEXITCODE -ne 0) { throw 'Loom mapping migration failed.' }
    $neoStaged = Join-Path $root 'ports\26x\neoforge\src\main\java'
    if (Test-Path $neoStaged) {
        & .\gradlew.bat ':xplat:migrateMappings' '--input' $neoStaged '--output' $neoStaged '--mappings' 'net.minecraft:mappings:1.21.11' '--overrideInputsIHaveABackup'
        if ($LASTEXITCODE -ne 0) { throw 'NeoForge adapter mapping migration failed.' }
    }
    $forgeStaged = Join-Path $root 'ports\26x\forge\src\main\java'
    if (Test-Path $forgeStaged) {
        & .\gradlew.bat ':xplat:migrateMappings' '--input' $forgeStaged '--output' $forgeStaged '--mappings' 'net.minecraft:mappings:1.21.11' '--overrideInputsIHaveABackup'
        if ($LASTEXITCODE -ne 0) { throw 'Forge adapter mapping migration failed.' }
    }
} finally { Pop-Location }
Write-Host 'Mojang-name conversion attempted; review Mixins, access widener and Fabric API changes.'
