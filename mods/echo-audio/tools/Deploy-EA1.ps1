param(
    [string]$Minecraft = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft',
    [string[]]$ModIds = @()
)
$ErrorActionPreference = 'Stop'
$modsRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$active = [IO.Path]::GetFullPath((Join-Path $Minecraft 'mods'))
$report = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot '../build/reports/ea1/release.json') | ConvertFrom-Json
$running = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -in @('java.exe', 'javaw.exe') -and $_.CommandLine -match 'cpw\.mods\.bootstraplauncher\.BootstrapLauncher|net\.minecraft\.client\.main\.Main'
}
if ($running) { throw 'Minecraft is running; close it before replacing production jars.' }
$backup = Join-Path $Minecraft ('mod-backups/ea1-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $backup | Out-Null
$staged = @()
$saved = @()
$installed = @()
try {
    foreach ($jar in ($report.jars | Where-Object { $ModIds.Count -eq 0 -or $_.mod_id -in $ModIds })) {
        if ((Get-FileHash -LiteralPath $jar.path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jar.sha256) {
            throw "Release changed after verification: $($jar.path)"
        }
        $target = Join-Path $active ([IO.Path]::GetFileName($jar.path))
        $temporary = $target + '.deploying'
        Copy-Item -LiteralPath $jar.path -Destination $temporary -Force
        if ((Get-FileHash -LiteralPath $temporary -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jar.sha256) {
            throw "Staged SHA-256 mismatch: $temporary"
        }
        $staged += @{ Temporary = $temporary; Target = $target; Pattern = ($jar.mod_id.Replace('_', '-') + '-[0-9]*.jar') }
    }
    foreach ($entry in $staged) {
        foreach ($old in (Get-ChildItem -LiteralPath $active -Filter '*.jar' -File |
                Where-Object { $_.Name -like $entry.Pattern })) {
            $resolved = [IO.Path]::GetFullPath($old.FullName)
            if (-not $resolved.StartsWith($active + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
                throw "Active jar escaped mods directory: $resolved"
            }
            $savedPath = Join-Path $backup $old.Name
            Move-Item -LiteralPath $resolved -Destination $savedPath
            $saved += @{ Original = $resolved; Backup = $savedPath }
        }
        # Same-volume rename makes each fully verified jar visible atomically.
        [IO.File]::Move($entry.Temporary, $entry.Target)
        $installed += $entry.Target
    }
    $story = Join-Path $Minecraft 'story'
    & git -C $story status --short *> (Join-Path $backup 'story-status-before.txt')
    $fixtures = Join-Path $modsRoot 'echo-audio/audio-forge-1.20.1/src/test/resources/audio'
    $sounds = Join-Path $story 'content-pack/jdh/sounds/ea1'
    New-Item -ItemType Directory -Force -Path $sounds | Out-Null
    foreach ($pair in @(@('test_tone.wav','test_tone.wav'), @('test_loop.ogg','test_loop_ogg.ogg'), @('test_loop.mp3','test_loop_mp3.mp3'))) {
        $source = Join-Path $fixtures $pair[0]
        $target = Join-Path $sounds $pair[1]
        if (Test-Path -LiteralPath $target) {
            if ((Get-FileHash -LiteralPath $target).Hash -ne (Get-FileHash -LiteralPath $source).Hash) {
                throw "User-owned fixture already exists; preserved: $target"
            }
            continue
        }
        $temporary = $target + '.deploying'
        Copy-Item -LiteralPath $source -Destination $temporary
        if ((Get-FileHash -LiteralPath $temporary).Hash -ne (Get-FileHash -LiteralPath $source).Hash) { throw "Fixture hash mismatch: $target" }
        [IO.File]::Move($temporary, $target)
    }
    Write-Output "EA1 installed; backup: $backup"
} catch {
    foreach ($path in $installed) { [IO.File]::Delete($path) }
    foreach ($entry in $saved) { Move-Item -LiteralPath $entry.Backup -Destination $entry.Original }
    throw
} finally {
    foreach ($entry in $staged) {
        if (Test-Path -LiteralPath $entry.Temporary) { Remove-Item -LiteralPath $entry.Temporary }
    }
}
