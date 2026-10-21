[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string] $PrismMinecraftDir = "",
    [string] $SourceRoot = "",
    [string] $PackName = "jdh",
    [switch] $IncludeStoryConfigs,
    [switch] $AllowDirtyStoryGit
)

Set-StrictMode -Version 3.0
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = Join-Path $PSScriptRoot "..\src\main\resources"
}

if ([string]::IsNullOrWhiteSpace($PrismMinecraftDir)) {
    $instancesRoot = Join-Path $env:APPDATA "PrismLauncher\instances"
    if (-not (Test-Path -LiteralPath $instancesRoot)) {
        throw "Prism instances root does not exist. Pass -PrismMinecraftDir explicitly."
    }
    $candidates = @(Get-ChildItem -LiteralPath $instancesRoot -Directory | ForEach-Object {
            $minecraft = Join-Path $_.FullName "minecraft"
            $packRoot = Join-Path $minecraft (Join-Path "story\content-pack" $PackName)
            if (Test-Path -LiteralPath $packRoot) {
                $minecraft
            }
        })
    if ($candidates.Count -eq 1) {
        $PrismMinecraftDir = $candidates[0]
    }
    elseif ($candidates.Count -eq 0) {
        throw "No Prism minecraft directory with story/content-pack/$PackName was found. Pass -PrismMinecraftDir explicitly."
    }
    else {
        $joined = ($candidates -join [Environment]::NewLine)
        throw "More than one Prism minecraft directory with story/content-pack/$PackName was found. Pass -PrismMinecraftDir explicitly.`n$joined"
    }
}

function Resolve-RequiredPath {
    param(
        [string] $Path,
        [string] $Label
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Label does not exist: $Path"
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Get-FullPath {
    param([string] $Path)
    return [System.IO.Path]::GetFullPath($Path)
}

function Assert-PathUnder {
    param(
        [string] $Child,
        [string] $Parent,
        [string] $Label
    )

    $childFull = (Get-FullPath $Child).TrimEnd('\', '/')
    $parentFull = (Get-FullPath $Parent).TrimEnd('\', '/')
    $prefix = $parentFull + [System.IO.Path]::DirectorySeparatorChar
    if (-not ($childFull.Equals($parentFull, [System.StringComparison]::OrdinalIgnoreCase) -or
            $childFull.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase))) {
        throw "$Label is outside protected root. child=$childFull parent=$parentFull"
    }
}

function Get-RelativeChildPath {
    param(
        [string] $Base,
        [string] $Child
    )

    $baseFull = (Get-FullPath $Base).TrimEnd('\', '/')
    $childFull = Get-FullPath $Child
    $prefix = $baseFull + [System.IO.Path]::DirectorySeparatorChar
    if (-not $childFull.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is not under base. child=$childFull base=$baseFull"
    }
    return $childFull.Substring($prefix.Length)
}

function Test-SameFileContent {
    param(
        [string] $Left,
        [string] $Right
    )

    if (-not (Test-Path -LiteralPath $Left) -or -not (Test-Path -LiteralPath $Right)) {
        return $false
    }
    $leftItem = Get-Item -LiteralPath $Left
    $rightItem = Get-Item -LiteralPath $Right
    if ($leftItem.Length -ne $rightItem.Length) {
        return $false
    }
    $leftHash = Get-Sha256Hex -Path $Left
    $rightHash = Get-Sha256Hex -Path $Right
    return $leftHash -eq $rightHash
}

function Get-Sha256Hex {
    param([string] $Path)

    $sha = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::OpenRead((Get-FullPath $Path))
    try {
        return [System.BitConverter]::ToString($sha.ComputeHash($stream)).Replace("-", "")
    }
    finally {
        $stream.Dispose()
        $sha.Dispose()
    }
}

function Add-CopyPlanFromDirectory {
    param(
        [System.Collections.Generic.List[object]] $Plan,
        [string] $SourceDirectory,
        [string] $TargetDirectory,
        [string] $TargetLabel
    )

    if (-not (Test-Path -LiteralPath $SourceDirectory)) {
        return
    }

    Get-ChildItem -LiteralPath $SourceDirectory -Recurse -File | ForEach-Object {
        $relative = Get-RelativeChildPath -Base $SourceDirectory -Child $_.FullName
        $target = Join-Path $TargetDirectory $relative
        $Plan.Add([pscustomobject]@{
                Source = $_.FullName
                Target = $target
                Label  = Join-Path $TargetLabel $relative
            })
    }
}

$projectRoot = Resolve-RequiredPath -Path (Join-Path $PSScriptRoot "..") -Label "Project root"
$sourceRootResolved = Resolve-RequiredPath -Path $SourceRoot -Label "Source root"
$minecraftRoot = Resolve-RequiredPath -Path $PrismMinecraftDir -Label "Prism minecraft directory"
$storyRoot = Join-Path $minecraftRoot "story"
$contentPackRoot = Join-Path $storyRoot (Join-Path "content-pack" $PackName)

Assert-PathUnder -Child $storyRoot -Parent $minecraftRoot -Label "Story root"
Assert-PathUnder -Child $contentPackRoot -Parent $storyRoot -Label "Content pack root"

if (-not (Test-Path -LiteralPath $storyRoot)) {
    throw "Prism story root does not exist: $storyRoot"
}

$storyGit = Join-Path $storyRoot ".git"
if (Test-Path -LiteralPath $storyGit) {
    $status = & git -C $storyRoot status --short
    if ($LASTEXITCODE -ne 0) {
        throw "git status failed for Prism story root: $storyRoot"
    }
    if ($status -and -not $AllowDirtyStoryGit) {
        $joinedStatus = ($status -join [Environment]::NewLine)
        throw "Prism story git has uncommitted changes. Review them first or pass -AllowDirtyStoryGit intentionally.`n$joinedStatus"
    }
}
elseif (-not $AllowDirtyStoryGit) {
    throw "Prism story root is not a git repository. Refusing live deploy without -AllowDirtyStoryGit: $storyRoot"
}

$plan = [System.Collections.Generic.List[object]]::new()
Add-CopyPlanFromDirectory -Plan $plan `
    -SourceDirectory (Join-Path $sourceRootResolved "data") `
    -TargetDirectory (Join-Path $contentPackRoot "data") `
    -TargetLabel (Join-Path (Join-Path "content-pack" $PackName) "data")
Add-CopyPlanFromDirectory -Plan $plan `
    -SourceDirectory (Join-Path $sourceRootResolved "assets") `
    -TargetDirectory (Join-Path $contentPackRoot "assets") `
    -TargetLabel (Join-Path (Join-Path "content-pack" $PackName) "assets")

$packMeta = Join-Path $sourceRootResolved "pack.mcmeta"
if (Test-Path -LiteralPath $packMeta) {
    $plan.Add([pscustomobject]@{
            Source = $packMeta
            Target = Join-Path $contentPackRoot "pack.mcmeta"
            Label  = Join-Path (Join-Path "content-pack" $PackName) "pack.mcmeta"
        })
}

if ($IncludeStoryConfigs) {
    Add-CopyPlanFromDirectory -Plan $plan `
        -SourceDirectory (Join-Path $sourceRootResolved "story\configs") `
        -TargetDirectory (Join-Path $storyRoot "configs") `
        -TargetLabel "configs"
}

$changes = [System.Collections.Generic.List[object]]::new()
foreach ($item in $plan) {
    Assert-PathUnder -Child $item.Target -Parent $storyRoot -Label "Deploy target"
    if (Test-SameFileContent -Left $item.Source -Right $item.Target) {
        continue
    }
    $exists = Test-Path -LiteralPath $item.Target
    $changes.Add([pscustomobject]@{
            Source    = $item.Source
            Target    = $item.Target
            Label     = $item.Label
            Overwrite = $exists
        })
}

if ($changes.Count -eq 0) {
    Write-Host "Prism story deploy: nothing to copy."
    return
}

Write-Host "Prism story deploy plan:"
foreach ($item in $changes) {
    $mode = if ($item.Overwrite) { "overwrite" } else { "new" }
    Write-Host " - [$mode] $($item.Label)"
}

if ($WhatIfPreference) {
    Write-Host "WhatIf: no files copied, no backup written."
    return
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $projectRoot (Join-Path "build\prism-deploy-backups" $timestamp)
$overwrites = @($changes | Where-Object { $_.Overwrite })
if ($overwrites.Count -gt 0) {
    New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
    foreach ($item in $overwrites) {
        $relativeTarget = Get-RelativeChildPath -Base $storyRoot -Child $item.Target
        $backupTarget = Join-Path $backupRoot $relativeTarget
        $backupParent = Split-Path -Parent $backupTarget
        New-Item -ItemType Directory -Path $backupParent -Force | Out-Null
        if ($PSCmdlet.ShouldProcess($backupTarget, "backup $($item.Label)")) {
            Copy-Item -LiteralPath $item.Target -Destination $backupTarget -Force
        }
    }
    Write-Host "Backed up overwritten Prism files to: $backupRoot"
}

foreach ($item in $changes) {
    $targetParent = Split-Path -Parent $item.Target
    New-Item -ItemType Directory -Path $targetParent -Force | Out-Null
    if ($PSCmdlet.ShouldProcess($item.Target, "copy $($item.Label)")) {
        Copy-Item -LiteralPath $item.Source -Destination $item.Target -Force
    }
}

Write-Host "Prism story deploy: copied $($changes.Count) file(s), delete operations: 0."
