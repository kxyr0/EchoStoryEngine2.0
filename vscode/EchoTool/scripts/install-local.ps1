param(
    [string]$VsixPath = "",
    [string]$ManualExtensionsRoot = "$env:USERPROFILE\.vscode\extensions"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Find-CodeCommand {
    $command = Get-Command code -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        "$env:LOCALAPPDATA\Programs\Microsoft VS Code\bin\code.cmd",
        "$env:LOCALAPPDATA\Programs\Microsoft VS Code Insiders\bin\code-insiders.cmd",
        "$env:ProgramFiles\Microsoft VS Code\bin\code.cmd",
        "${env:ProgramFiles(x86)}\Microsoft VS Code\bin\code.cmd",
        "$env:LOCALAPPDATA\Programs\Microsoft VS Code\Code.exe",
        "$env:ProgramFiles\Microsoft VS Code\Code.exe"
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    return $null
}

function Get-ExtensionIdentity {
    $manifestPath = Join-Path $root "package.json"
    if (-not (Test-Path -LiteralPath $manifestPath)) {
        throw "package.json not found at $manifestPath"
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if (-not $manifest.publisher -or -not $manifest.name -or -not $manifest.version) {
        throw "package.json must define publisher, name, and version for manual install."
    }

    return @{
        Publisher = [string]$manifest.publisher
        Name = [string]$manifest.name
        Version = [string]$manifest.version
        FolderName = "$($manifest.publisher).$($manifest.name)-$($manifest.version)"
    }
}

function Assert-PathInside {
    param(
        [string]$Parent,
        [string]$Child
    )

    $parentFull = [System.IO.Path]::GetFullPath($Parent).TrimEnd('\') + '\'
    $childFull = [System.IO.Path]::GetFullPath($Child)
    if (-not $childFull.StartsWith($parentFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside expected directory. Parent=$parentFull Child=$childFull"
    }
}

function Install-VsixManually {
    param(
        [string]$ResolvedVsix
    )

    $identity = Get-ExtensionIdentity
    $extensionsRoot = [System.IO.Path]::GetFullPath($ManualExtensionsRoot)
    $destination = Join-Path $extensionsRoot $identity.FolderName
    Assert-PathInside -Parent $extensionsRoot -Child $destination

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("echostory-vsix-" + [System.Guid]::NewGuid().ToString("N"))
    $zipPath = Join-Path $tempRoot "extension.zip"
    $extractPath = Join-Path $tempRoot "extract"

    try {
        New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
        Copy-Item -LiteralPath $ResolvedVsix -Destination $zipPath -Force
        Expand-Archive -LiteralPath $zipPath -DestinationPath $extractPath -Force

        $sourceExtension = Join-Path $extractPath "extension"
        if (-not (Test-Path -LiteralPath (Join-Path $sourceExtension "package.json"))) {
            throw "VSIX archive does not contain extension/package.json."
        }

        New-Item -ItemType Directory -Path $extensionsRoot -Force | Out-Null
        if (Test-Path -LiteralPath $destination) {
            Assert-PathInside -Parent $extensionsRoot -Child $destination
            Remove-Item -LiteralPath $destination -Recurse -Force
        }

        Copy-Item -LiteralPath $sourceExtension -Destination $destination -Recurse
        Write-Host "VS Code CLI was not found."
        Write-Host "Manually installed $($identity.Publisher).$($identity.Name) $($identity.Version) to:"
        Write-Host $destination
        Write-Host "Open or restart VS Code after installing it; VS Code reads user extensions from this directory."
    }
    finally {
        if (Test-Path -LiteralPath $tempRoot) {
            Assert-PathInside -Parent ([System.IO.Path]::GetTempPath()) -Child $tempRoot
            Remove-Item -LiteralPath $tempRoot -Recurse -Force
        }
    }
}

if (-not $VsixPath) {
    $latest = Get-ChildItem -LiteralPath $root -Filter "*.vsix" -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $latest) {
        throw "No .vsix package found in $root. Run npm run package first."
    }
    $VsixPath = $latest.FullName
}

$resolvedVsix = Resolve-Path -LiteralPath $VsixPath
$code = Find-CodeCommand
if (-not $code) {
    Install-VsixManually -ResolvedVsix $resolvedVsix.Path
    exit 0
}

Write-Host "Installing $resolvedVsix"
& $code --install-extension "$resolvedVsix" --force
Write-Host "Installed EchoStory Engine Authoring Tools."
