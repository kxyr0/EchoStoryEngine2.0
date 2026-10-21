param([switch]$Console)

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

function Unblock-ProjectFiles {
    try {
        Get-ChildItem -LiteralPath $PSScriptRoot -File -ErrorAction SilentlyContinue |
            Unblock-File -ErrorAction SilentlyContinue
        $scripts = Join-Path $PSScriptRoot ".venv\Scripts"
        if (Test-Path -LiteralPath $scripts) {
            Get-ChildItem -LiteralPath $scripts -File -ErrorAction SilentlyContinue |
                Unblock-File -ErrorAction SilentlyContinue
        }
    } catch {
    }
}

function Find-Python {
    $py = Get-Command py -ErrorAction SilentlyContinue
    if ($py) { return @($py.Source, "-3") }
    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($python) { return @($python.Source) }
    return $null
}

Unblock-ProjectFiles

$venvPython = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"
if (-not (Test-Path -LiteralPath $venvPython)) {
    $hostPython = Find-Python
    if (-not $hostPython) { throw "Python 3 was not found." }
    $hostExe = $hostPython[0]
    $hostArgs = @()
    if ($hostPython.Count -gt 1) { $hostArgs = $hostPython[1..($hostPython.Count - 1)] }
    & $hostExe @hostArgs -m venv ".venv"
}

$depsFlag = Join-Path $PSScriptRoot ".venv\.deps-installed"
if (-not (Test-Path -LiteralPath $depsFlag)) {
    & $venvPython -m pip install --upgrade pip
    & $venvPython -m pip install -r (Join-Path $PSScriptRoot "requirements.txt")
    New-Item -ItemType File -Path $depsFlag -Force | Out-Null
}

$env:PYTHONPATH = Split-Path -Parent $PSScriptRoot
$appPath = Join-Path $PSScriptRoot "app.py"
if ($Console) {
    & $venvPython $appPath
    exit $LASTEXITCODE
}

$pythonw = Join-Path $PSScriptRoot ".venv\Scripts\pythonw.exe"
Start-Process -FilePath $pythonw -ArgumentList ('"' + $appPath + '"') -WorkingDirectory $PSScriptRoot
