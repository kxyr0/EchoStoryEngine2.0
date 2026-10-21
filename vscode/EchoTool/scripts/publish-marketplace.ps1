param(
    [string]$Pat = $env:VSCE_PAT
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if (-not $Pat) {
    throw "VSCE_PAT is not set. Create a Visual Studio Marketplace publisher token and set VSCE_PAT before publishing."
}

Push-Location $root
try {
    npm run package
    npx vsce publish --pat $Pat
}
finally {
    Pop-Location
}
