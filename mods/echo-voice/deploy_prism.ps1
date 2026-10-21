[CmdletBinding()]
param(
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repository = Split-Path -Parent $MyInvocation.MyCommand.Path
$modsRoot = Split-Path -Parent $repository
$dialogueRoot = Join-Path $modsRoot 'echo-dialogue'
$adapterRoot = Join-Path $modsRoot 'echo-dialogue-voice-adapter'
$instanceName = [string]::Concat([char[]](1060, 1086, 1088, 1084, 1072, 32, 1052, 1099, 1089, 1083, 1080))
$instancesRoot = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances'
$destination = Join-Path (Join-Path $instancesRoot $instanceName) 'minecraft\mods'
$unknownDialogueId = 'echo_dialogue'
$targetIds = @($unknownDialogueId, 'echo_voice', 'echo_dialogue_voice_adapter')

function Invoke-CheckedGradle {
    param([string]$Root, [string[]]$Tasks)

    Push-Location $Root
    try {
        & (Join-Path $Root 'gradlew.bat') @Tasks --offline --no-daemon --console=plain --max-workers=1
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle failed in $Root with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Get-JarModIds {
    param([string]$Path)

    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $archive.GetEntry('META-INF/mods.toml')
        if ($null -eq $entry) {
            return @()
        }
        $reader = [System.IO.StreamReader]::new($entry.Open(), [System.Text.Encoding]::UTF8, $true)
        try {
            $text = $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
        $ids = @()
        $modBlocks = [regex]::Matches($text, '(?ms)^\s*\[\[mods\]\]\s*(.*?)(?=^\s*\[\[|\z)')
        foreach ($block in $modBlocks) {
            $match = [regex]::Match($block.Groups[1].Value, '(?m)^\s*modId\s*=\s*"([^"]+)"')
            if ($match.Success) { $ids += $match.Groups[1].Value }
        }
        return @($ids)
    } finally {
        $archive.Dispose()
    }
}

function Assert-ExclusiveAccess {
    param([string]$Path)

    try {
        $stream = [System.IO.File]::Open($Path, 'Open', 'ReadWrite', 'None')
        $stream.Dispose()
    } catch {
        throw "FAILED_LOCKED: $Path ($($_.Exception.Message))"
    }
}

function Get-ArtifactInfo {
    param([string]$ModId, [string]$Source, [string]$FileName)

    if (-not (Test-Path -LiteralPath $Source -PathType Leaf)) {
        throw "Production JAR is missing: $Source"
    }
    $ids = @(Get-JarModIds -Path $Source)
    if ($ids -notcontains $ModId) {
        throw "JAR $Source does not declare required modId $ModId."
    }
    $file = Get-Item -LiteralPath $Source
    return [pscustomobject]@{
        ModId = $ModId
        Source = $file.FullName
        FileName = $FileName
        Length = $file.Length
        Hash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
        Stage = Join-Path $destination ($FileName + '.deploying')
        Final = Join-Path $destination $FileName
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$oldGradleOptions = $env:GRADLE_OPTS
$env:GRADLE_OPTS = (($oldGradleOptions, '-Dnet.minecraftforge.gradle.check.certs=false') |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join ' '

try {
    if (-not $SkipBuild) {
        Invoke-CheckedGradle -Root $dialogueRoot -Tasks @('build', 'publishDialogueIntegrationArtifacts')
        Invoke-CheckedGradle -Root $repository -Tasks @('check', 'build')
        Invoke-CheckedGradle -Root $adapterRoot -Tasks @('check', 'build')
    }

    if (-not (Test-Path -LiteralPath $destination -PathType Container)) {
        throw "Prism mods directory does not exist: $destination"
    }

    $artifacts = @(
        Get-ArtifactInfo -ModId $unknownDialogueId `
            -Source (Join-Path $dialogueRoot 'dialogue-forge-1.20.1\build\libs\echo-dialogue-0.14.0-all.jar') `
            -FileName 'echo-dialogue-0.14.0-all.jar'
        Get-ArtifactInfo -ModId 'echo_voice' `
            -Source (Join-Path $repository 'voice-forge-1.20.1\build\libs\echo-voice-0.2.1-all.jar') `
            -FileName 'echo-voice-0.2.1-all.jar'
        Get-ArtifactInfo -ModId 'echo_dialogue_voice_adapter' `
            -Source (Join-Path $adapterRoot 'build\libs\echo-dialogue-voice-adapter-0.1.0.jar') `
            -FileName 'echo-dialogue-voice-adapter-0.1.0.jar'
    )

    $existing = @()
    foreach ($jar in Get-ChildItem -LiteralPath $destination -Filter '*.jar' -File) {
        try {
            $ids = @(Get-JarModIds -Path $jar.FullName)
            if (@($ids | Where-Object { $targetIds -contains $_ }).Count -gt 0) {
                $existing += $jar
            }
        } catch {
            Write-Warning "Skipped unreadable unrelated JAR: $($jar.FullName)"
        }
    }

    foreach ($path in @($existing.FullName) + @($artifacts.Stage)) {
        if ($path -and (Test-Path -LiteralPath $path -PathType Leaf)) {
            Assert-ExclusiveAccess -Path $path
        }
    }

    foreach ($artifact in $artifacts) {
        if (Test-Path -LiteralPath $artifact.Stage) {
            Remove-Item -LiteralPath $artifact.Stage -Force
        }
        Copy-Item -LiteralPath $artifact.Source -Destination $artifact.Stage
        $staged = Get-Item -LiteralPath $artifact.Stage
        $stagedHash = (Get-FileHash -LiteralPath $artifact.Stage -Algorithm SHA256).Hash
        if ($staged.Length -ne $artifact.Length -or $stagedHash -ne $artifact.Hash) {
            throw "Staging verification failed for $($artifact.ModId)."
        }
    }

    $rollbackRoot = Join-Path (Split-Path -Parent $destination) ('.echo-voice-rollback-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $rollbackRoot | Out-Null
    $movedOld = @()
    $installed = @()
    try {
        foreach ($old in $existing) {
            $backup = Join-Path $rollbackRoot $old.Name
            Move-Item -LiteralPath $old.FullName -Destination $backup
            $movedOld += [pscustomobject]@{ Original = $old.FullName; Backup = $backup }
        }
        foreach ($artifact in $artifacts) {
            Move-Item -LiteralPath $artifact.Stage -Destination $artifact.Final
            $installed += $artifact.Final
        }

        foreach ($artifact in $artifacts) {
            $deployedHash = (Get-FileHash -LiteralPath $artifact.Final -Algorithm SHA256).Hash
            if ($deployedHash -ne $artifact.Hash) {
                throw "Post-deployment hash mismatch for $($artifact.ModId)."
            }
        }

        $counts = @{}
        foreach ($id in $targetIds) { $counts[$id] = 0 }
        foreach ($jar in Get-ChildItem -LiteralPath $destination -Filter '*.jar' -File) {
            foreach ($id in @(Get-JarModIds -Path $jar.FullName)) {
                if ($counts.ContainsKey($id)) { $counts[$id]++ }
            }
        }
        foreach ($id in $targetIds) {
            if ($counts[$id] -ne 1) { throw "Duplicate verification failed for ${id}: $($counts[$id]) JARs." }
        }

        Remove-Item -LiteralPath $rollbackRoot -Recurse -Force
        Write-Output "DEPLOYMENT SUCCESS: $destination"
        foreach ($artifact in $artifacts) {
            Write-Output "$($artifact.ModId) | $($artifact.FileName) | $($artifact.Length) bytes | SHA-256 $($artifact.Hash)"
        }
        Write-Output 'old duplicate JARs = 0'
    } catch {
        foreach ($path in $installed) {
            if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
        }
        foreach ($item in $movedOld) {
            if (Test-Path -LiteralPath $item.Backup) {
                Move-Item -LiteralPath $item.Backup -Destination $item.Original
            }
        }
        foreach ($artifact in $artifacts) {
            if (Test-Path -LiteralPath $artifact.Stage) { Remove-Item -LiteralPath $artifact.Stage -Force }
        }
        if ((Test-Path -LiteralPath $rollbackRoot) -and
                @(Get-ChildItem -LiteralPath $rollbackRoot -Force).Count -eq 0) {
            Remove-Item -LiteralPath $rollbackRoot -Force
        }
        throw "Deployment rolled back: $($_.Exception.Message)"
    }
} finally {
    $env:GRADLE_OPTS = $oldGradleOptions
}
