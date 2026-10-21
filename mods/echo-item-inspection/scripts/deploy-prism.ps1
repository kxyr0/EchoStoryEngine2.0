[CmdletBinding()]
param(
    [string]$Source,
    [string]$ModsDirectory = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\mods',
    [string]$BackupDirectory = 'D:\JDHSeason\deployment-backups\echo-item-inspection'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repository = Split-Path -Parent $PSScriptRoot
$modId = 'echo_item_inspection'
if ([string]::IsNullOrWhiteSpace($Source)) {
    $properties = Get-Content -LiteralPath (Join-Path $repository 'gradle.properties')
    $versions = @($properties | Where-Object { $_ -match '^mod_version=' })
    if ($versions.Count -ne 1) { throw 'Expected exactly one mod_version in gradle.properties.' }
    $version = $versions[0] -replace '^mod_version=', ''
    if ($version -notmatch '^[0-9][A-Za-z0-9._+-]*$') { throw 'Invalid production version.' }
    $Source = Join-Path $repository "inspection-forge-1.20.1\build\libs\echo-item-inspection-$version-all.jar"
}

function Get-DeclaredModIds {
    param([string]$Path)
    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
    } catch [System.IO.IOException] {
        throw "FAILED_LOCKED: $Path ($($_.Exception.Message))"
    }
    try {
        $entry = $archive.GetEntry('META-INF/mods.toml')
        if ($null -eq $entry) { return @() }
        $reader = [System.IO.StreamReader]::new($entry.Open(), [System.Text.Encoding]::UTF8, $true)
        try { $descriptor = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $ids = @()
        $blocks = [regex]::Matches($descriptor, '(?ms)^\s*\[\[mods\]\]\s*(.*?)(?=^\s*\[\[|\z)')
        foreach ($block in $blocks) {
            $match = [regex]::Match($block.Groups[1].Value, '(?m)^\s*modId\s*=\s*"([^"\r\n]+)"')
            if ($match.Success) { $ids += $match.Groups[1].Value }
        }
        return @($ids)
    } finally { $archive.Dispose() }
}

function Assert-ExclusiveAccess {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return }
    try {
        $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open,
                [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        $stream.Dispose()
    } catch {
        throw "FAILED_LOCKED: $Path ($($_.Exception.Message))"
    }
}

function Get-Sha256 {
    param([string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Assert-ActivePath {
    param([string]$Path)
    $resolved = [System.IO.Path]::GetFullPath($Path)
    $parent = [System.IO.Path]::GetDirectoryName($resolved)
    if (-not $parent.Equals($script:modsRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing file operation outside active mods directory: $resolved"
    }
    return $resolved
}

function Find-ActiveInspectionJars {
    $matches = @()
    foreach ($jar in Get-ChildItem -LiteralPath $script:modsRoot -Filter '*.jar' -File) {
        if (@(Get-DeclaredModIds -Path $jar.FullName) -contains $script:modId) { $matches += $jar }
    }
    return @($matches)
}

$sourceFile = Get-Item -LiteralPath $Source
if ($sourceFile.PSIsContainer -or $sourceFile.Length -eq 0) { throw "Production JAR missing or empty: $Source" }
if (@(Get-DeclaredModIds -Path $sourceFile.FullName) -notcontains $modId) {
    throw "Source JAR does not declare $modId`: $($sourceFile.FullName)"
}
if (-not (Test-Path -LiteralPath $ModsDirectory -PathType Container)) {
    throw "Prism mods directory does not exist: $ModsDirectory"
}
$script:modsRoot = [System.IO.Path]::GetFullPath($ModsDirectory).TrimEnd('\', '/')
$backupRoot = [System.IO.Path]::GetFullPath($BackupDirectory)
if ($backupRoot.Equals($modsRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
        $backupRoot.StartsWith($modsRoot + [System.IO.Path]::DirectorySeparatorChar,
                [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Backup directory must be outside active mods.'
}
$final = Assert-ActivePath (Join-Path $modsRoot $sourceFile.Name)
$stage = Assert-ActivePath ($final + '.deploying')
if ($sourceFile.FullName.Equals($final, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Source JAR must be outside active mods.'
}
$sourceHash = Get-Sha256 $sourceFile.FullName
$oldJars = @(Find-ActiveInspectionJars)
$oldPaths = @($oldJars | ForEach-Object { $_.FullName })
foreach ($path in $oldPaths + @($final, $stage)) { Assert-ExclusiveAccess $path }
if ((Test-Path -LiteralPath $final) -and $oldPaths -notcontains $final) {
    throw "Destination file belongs to a different mod: $final"
}

$backupRun = Join-Path $backupRoot ((Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $backupRun -Force | Out-Null
$backups = @()
foreach ($old in $oldJars) {
    $backup = Join-Path $backupRun $old.Name
    $oldHash = Get-Sha256 $old.FullName
    Copy-Item -LiteralPath $old.FullName -Destination $backup
    if ((Get-Sha256 $backup) -ne $oldHash) { throw "Backup verification failed: $backup" }
    $backups += [pscustomobject]@{ Original = $old.FullName; Backup = $backup; Sha256 = $oldHash }
}

$activated = $false
try {
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Force }
    Copy-Item -LiteralPath $sourceFile.FullName -Destination $stage
    if ((Get-Item -LiteralPath $stage).Length -ne $sourceFile.Length -or (Get-Sha256 $stage) -ne $sourceHash) {
        throw "Staging verification failed: $stage"
    }
    try {
        if (Test-Path -LiteralPath $final) {
            [System.IO.File]::Replace($stage, $final, [System.Management.Automation.Language.NullString]::Value)
        }
        else { [System.IO.File]::Move($stage, $final) }
    } catch { throw "FAILED_LOCKED: $final ($($_.Exception.Message))" }
    $activated = $true
    foreach ($old in $oldJars) {
        if (-not $old.FullName.Equals($final, [System.StringComparison]::OrdinalIgnoreCase)) {
            $oldPath = Assert-ActivePath $old.FullName
            try { Remove-Item -LiteralPath $oldPath -Force }
            catch { throw "FAILED_LOCKED: $oldPath ($($_.Exception.Message))" }
        }
    }
    $active = @(Find-ActiveInspectionJars)
    $deployedHash = Get-Sha256 $final
    if ($active.Count -ne 1 -or $deployedHash -ne $sourceHash) {
        throw "Post-deployment verification failed: active=$($active.Count), destination=$final"
    }
    [pscustomobject]@{
        Status = 'DEPLOYED'
        ModId = $modId
        Source = $sourceFile.FullName
        Destination = $final
        Sha256 = $deployedHash
        ActiveJarCount = $active.Count
        ActiveDuplicates = [Math]::Max(0, $active.Count - 1)
        BackupDirectory = $backupRun
        PreservedBackups = @($backups | ForEach-Object { $_.Backup })
    } | ConvertTo-Json -Depth 4
} catch {
    $failure = $_.Exception.Message
    $rollbackErrors = @()
    if ($activated) {
        foreach ($backup in $backups) {
            $restore = Assert-ActivePath ($backup.Original + '.rollback')
            try {
                Copy-Item -LiteralPath $backup.Backup -Destination $restore -Force
                if ((Get-Sha256 $restore) -ne $backup.Sha256) { throw "Restore hash mismatch: $restore" }
                if (Test-Path -LiteralPath $backup.Original) {
                    [System.IO.File]::Replace($restore, $backup.Original,
                            [System.Management.Automation.Language.NullString]::Value)
                } else { [System.IO.File]::Move($restore, $backup.Original) }
            } catch { $rollbackErrors += $_.Exception.Message }
        }
        if ($oldPaths -notcontains $final -and (Test-Path -LiteralPath $final)) {
            try { Remove-Item -LiteralPath $final -Force } catch { $rollbackErrors += $_.Exception.Message }
        }
    }
    if (Test-Path -LiteralPath $stage) {
        try { Remove-Item -LiteralPath $stage -Force } catch { $rollbackErrors += $_.Exception.Message }
    }
    throw "Deployment failed; backups retained at $backupRun. $failure Rollback errors: $($rollbackErrors -join '; ')"
}
