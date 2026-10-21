[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter(Mandatory)][ValidateSet('Archive', 'Restore', 'Status')][string] $Action,
    [string] $MinecraftDir = (Join-Path $env:APPDATA 'PrismLauncher\instances\Форма Мысли\minecraft')
)

Set-StrictMode -Version 3.0
$ErrorActionPreference = 'Stop'
$gameRoot = (Resolve-Path -LiteralPath $MinecraftDir).Path
$packRoot = Join-Path $gameRoot 'story\content-pack\jdh'
$marker = Join-Path $packRoot '.archived'
if (-not (Test-Path -LiteralPath (Join-Path $packRoot 'pack.mcmeta'))) {
    throw "Не найден JDH content-pack: $packRoot"
}
if ($Action -eq 'Status') {
    if (Test-Path -LiteralPath $marker) { Get-Content -LiteralPath $marker } else { 'Сюжетный content-pack активен.' }
    return
}
if (-not $PSCmdlet.ShouldProcess($packRoot, $Action)) { return }
if ($Action -eq 'Restore') {
    if (-not (Test-Path -LiteralPath $marker)) { 'Content-pack уже активен.'; return }
    Remove-Item -LiteralPath $marker
    'Content-pack включён. Перезапустите Minecraft. Рабочие файлы сохранены без перезаписи.'
    return
}
if (Test-Path -LiteralPath $marker) { 'Content-pack уже архивный.'; return }
$archiveRoot = Join-Path $gameRoot 'story-archives'
[void](New-Item -ItemType Directory -Path $archiveRoot -Force)
$archiveName = 'jdh-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$zipPath = Join-Path $archiveRoot ($archiveName + '.zip')
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($packRoot, $zipPath)
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    if ($zip.Entries.Count -eq 0) { throw 'Архив пуст: content-pack не отключён.' }
} finally { $zip.Dispose() }
$manifest = [ordered]@{
    version = 1
    archivedAt = [DateTimeOffset]::Now.ToString('o')
    packRoot = $packRoot
    snapshot = $zipPath
    sha256 = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath $marker -Encoding utf8
"Снимок: $zipPath"
'Content-pack отключён маркером .archived. Перезапустите Minecraft. Restore включает сохранённые рабочие файлы.'
