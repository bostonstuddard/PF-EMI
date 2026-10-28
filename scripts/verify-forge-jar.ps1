param(
    [string]$SourceConfig,
    [string]$JarPath
)

$ErrorActionPreference = 'Stop'

function Validate-Config([string]$jsonText, [string]$description) {
    $config = $jsonText | ConvertFrom-Json
    if ($config.compatibilityLevel -cne 'JAVA_21') {
        throw "$description specifies '$($config.compatibilityLevel)' instead of JAVA_21. Forge's bundled Mixin 0.8.7 cannot parse JAVA_25."
    }
    if ($config.package -cne 'dev.emi.emi.mixin') {
        throw "$description has an unexpected mixin package: $($config.package)"
    }
    # The input mixins have Forge-specific event replacements since v10.
    # Reject obsolete Forge input mixins; 26.3 uses native input handling.
    foreach ($legacy in @('MouseMixin', 'KeyboardMixin')) {
        if (@($config.client) -ccontains $legacy) {
            throw "$description still registers $legacy; apply v10 and rebuild before launching."
        }
    }
    Write-Host "[PF-EMI] OK: $description uses JAVA_21 and the v10 native Forge input hooks."
}

try {
    if (-not $SourceConfig -and -not $JarPath) {
        throw 'Specify -SourceConfig and/or -JarPath.'
    }
    if ($SourceConfig) {
        if (-not (Test-Path -LiteralPath $SourceConfig -PathType Leaf)) {
            throw "Mixin source configuration not found: $SourceConfig"
        }
        Validate-Config ([IO.File]::ReadAllText($SourceConfig)) "Source '$SourceConfig'"
    }
    if ($JarPath) {
        if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
            throw "Forge mod JAR not found: $JarPath"
        }
        if ((Get-Item -LiteralPath $JarPath).Length -lt 100000) {
            throw "Forge JAR is unexpectedly small: $JarPath"
        }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $jar = [IO.Compression.ZipFile]::OpenRead($JarPath)
        try {
            $configEntries = @($jar.Entries | Where-Object { $_.FullName -ceq 'emi-forge26.mixins.json' })
            if ($configEntries.Count -ne 1) {
                throw "Expected precisely one 'emi-forge26.mixins.json' in the JAR; found $($configEntries.Count)."
            }
            $reader = [IO.StreamReader]::new($configEntries[0].Open())
            try { $contents = $reader.ReadToEnd() }
            finally { $reader.Dispose() }
            Validate-Config $contents "Packaged JAR '$JarPath'"

            if (-not $jar.GetEntry('META-INF/mods.toml')) {
                throw 'Forge JAR is missing META-INF/mods.toml.'
            }
            # Validate every required Forge class, not only the @Mod entrypoint.
            # A ForgeGradle source-set regression can otherwise produce a
            # superficially valid JAR containing only the shared xplat classes.
            $requiredForgeClasses = @(
                'dev/emi/emi/platform/forge/EmiForge.class',
                'dev/emi/emi/platform/forge/EmiClientForge.class',
                'dev/emi/emi/platform/forge/EmiAgnosForge.class',
                'dev/emi/emi/platform/forge/EmiPacketHandler.class',
                'dev/emi/emi/platform/forge/EmiRecipeSync.class',
                'dev/emi/emi/api/forge/ForgeEmiStack.class'
            )
            $missingForgeClasses = @($requiredForgeClasses | Where-Object { -not $jar.GetEntry($_) })
            if ($missingForgeClasses.Count -gt 0) {
                $platformClasses = @($jar.Entries |
                    Where-Object { $_.FullName -like 'dev/emi/emi/platform/forge/*.class' } |
                    Select-Object -ExpandProperty FullName -First 12)
                $detail = if ($platformClasses.Count -eq 0) { '(none)' } else { $platformClasses -join ', ' }
                throw "Forge JAR is missing required classes: $($missingForgeClasses -join ', '). Packaged Forge platform classes: $detail"
            }
            $manifest = $jar.GetEntry('META-INF/MANIFEST.MF')
            if (-not $manifest) { throw 'Forge JAR is missing the manifest.' }
            $reader2 = [IO.StreamReader]::new($manifest.Open())
            try { $manifestContents = $reader2.ReadToEnd() }
            finally { $reader2.Dispose() }
            if ($manifestContents -notmatch '(?m)^MixinConfigs:\s*emi-forge26\.mixins\.json\s*$') {
                throw 'JAR manifest does not register the expected Forge Mixin configuration.'
            }
        } finally {
            $jar.Dispose()
        }
        $hash = (Get-FileHash -LiteralPath $JarPath -Algorithm SHA256).Hash
        Write-Host "[PF-EMI] OK: Forge mod classes, manifest, metadata, and Mixin JSON found."
        Write-Host "[PF-EMI] JAR SHA-256: $hash"
    }
} catch {
    [Console]::Error.WriteLine("[PF-EMI] ERROR: $($_.Exception.Message)")
    exit 1
}
exit 0
