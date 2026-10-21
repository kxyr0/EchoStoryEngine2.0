param([Parameter(Mandatory=$true)][string]$SkinFolder, [string[]]$Languages = @('ru', 'en'))
$ErrorActionPreference = 'Stop'
$projectFolder = Split-Path -Parent $PSScriptRoot
$skinDirectory = (Resolve-Path -LiteralPath $SkinFolder).Path
$appFile = Join-Path $projectFolder 'build\Release\EchoNpcStudio.exe'
$testsFile = Join-Path $projectFolder 'build\Release\EchoCoreTests.exe'
if (-not (Test-Path -LiteralPath $appFile)) { throw 'Build Release first using the commands in README.md.' }
& $testsFile
if ($LASTEXITCODE -ne 0) { throw 'Core tests failed.' }
$originalHashes = @{}
Get-ChildItem -LiteralPath $skinDirectory -Filter '*.png' -Recurse -File | ForEach-Object { $originalHashes[$_.FullName] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
$contentPackDirectory = $skinDirectory
while ($contentPackDirectory -and -not ((Test-Path -LiteralPath (Join-Path $contentPackDirectory 'assets')) -and (Test-Path -LiteralPath (Join-Path $contentPackDirectory 'data')))) { $contentPackDirectory = Split-Path -Parent $contentPackDirectory }
if ($contentPackDirectory) {
    Get-ChildItem -LiteralPath (Join-Path $contentPackDirectory 'data') -Filter '*.face-rig.es' -Recurse -File | ForEach-Object { $originalHashes[$_.FullName] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
    Get-ChildItem -LiteralPath (Join-Path $contentPackDirectory 'data') -Filter '*.npc.es' -Recurse -File | ForEach-Object { $originalHashes[$_.FullName] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
    $faceTextures = Join-Path $contentPackDirectory 'assets\jdh\textures\echo_animation\faces'
    if (Test-Path -LiteralPath $faceTextures) { Get-ChildItem -LiteralPath $faceTextures -Filter '*.png' -Recurse -File | ForEach-Object { $originalHashes[$_.FullName] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash } }
    $storyConfigs = Join-Path (Split-Path -Parent (Split-Path -Parent $contentPackDirectory)) 'configs'
    if (Test-Path -LiteralPath $storyConfigs) { Get-ChildItem -LiteralPath $storyConfigs -Filter '*.portrait-overlay.es' -Recurse -File | ForEach-Object { $originalHashes[$_.FullName] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash } }
    $benSkin = Join-Path $skinDirectory 'ben.png'
    if (Test-Path -LiteralPath $benSkin) { & $testsFile $benSkin; if ($LASTEXITCODE -ne 0) { throw 'Real face catalog tests failed.' } }
}
$runFolder = Join-Path $projectFolder ('build\verification-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
foreach ($renderer in @('hardware', 'warp')) {
  foreach ($language in $Languages) {
    if ($language -notmatch '^[a-zA-Z0-9_-]{1,32}$') { throw 'Invalid language code.' }
    $outputFolder = Join-Path $runFolder ($renderer + '-' + $language)
    $arguments = @('--lang', $language, '--smoke-test', ('"' + $outputFolder + '"'), ('"' + $skinDirectory + '"'))
    if ($renderer -eq 'warp') { $arguments = @('--warp') + $arguments }
    $process = Start-Process -FilePath $appFile -ArgumentList $arguments -WindowStyle Hidden -PassThru
    if (-not $process.WaitForExit(40000)) { Stop-Process -Id $process.Id; throw "$renderer test timed out." }
    if ($process.ExitCode -ne 0) {
        $failureFile = Join-Path $outputFolder 'failure.txt'
        if (Test-Path -LiteralPath $failureFile) { Get-Content -LiteralPath $failureFile }
        throw "$renderer test failed with exit code $($process.ExitCode)."
    }
    Get-Content -LiteralPath (Join-Path $outputFolder 'passed.txt')
    Write-Output "Language: $language"
  }
}
foreach ($originalFile in $originalHashes.Keys) {
    if ((Get-FileHash -LiteralPath $originalFile -Algorithm SHA256).Hash -ne $originalHashes[$originalFile]) { throw "Source asset changed: $originalFile" }
}
Write-Output "Source PNG, face rig and portrait profile hashes unchanged. Verification artifacts: $runFolder"
