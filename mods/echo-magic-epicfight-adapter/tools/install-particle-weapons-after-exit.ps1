param([int] $MinecraftProcessId)
$ErrorActionPreference = 'Stop'
$modsDirectory = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\mods'
$sourceJar = 'D:\JDHSeason\mods\echo-magic-epicfight-adapter\build\libs\echo-magic-epicfight-adapter-0.3.14.jar'
$statusLog = 'D:\JDHSeason\mods\echo-magic-epicfight-adapter\tools\install-particle-weapons-status.log'
function JarHash([string] $path) {
    $stream = [System.IO.File]::OpenRead($path)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return [System.BitConverter]::ToString($sha.ComputeHash($stream)) }
    finally { $sha.Dispose(); $stream.Dispose() }
}
try {
    $expectedHash = JarHash $sourceJar
    if (![System.IO.File]::Exists([System.IO.Path]::Combine($modsDirectory, 'echo-magic-0.8.17-all.jar'))) { throw 'Required Echo Magic 0.8.17 is missing.' }
    [System.IO.File]::WriteAllText($statusLog, 'Waiting for Minecraft to exit.')
    if ($MinecraftProcessId -gt 0) {
        try { $game = [System.Diagnostics.Process]::GetProcessById($MinecraftProcessId) } catch { $game = $null }
        if ($game) { $game.WaitForExit(); $game.Dispose() }
    }
    if ([System.Diagnostics.Process]::GetProcessesByName('javaw').Length -gt 0) { throw 'A Java client is still running; JARs were left untouched.' }
    if ((JarHash $sourceJar) -ne $expectedHash) { throw 'Source JAR changed while waiting.' }
    foreach ($oldJar in [System.IO.Directory]::GetFiles($modsDirectory, 'echo-magic-epicfight-adapter-*.jar')) {
        if ([System.IO.Path]::GetFileName($oldJar) -eq 'echo-magic-epicfight-adapter-0.3.14.jar') { continue }
        $backupPath = $oldJar + '.disabled'
        if ([System.IO.File]::Exists($backupPath)) { $backupPath += '.' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff') }
        [System.IO.File]::Move($oldJar, $backupPath)
    }
    $installedJar = [System.IO.Path]::Combine($modsDirectory, 'echo-magic-epicfight-adapter-0.3.14.jar')
    [System.IO.File]::Copy($sourceJar, $installedJar, $true)
    if ((JarHash $installedJar) -ne $expectedHash) { throw 'Installed JAR checksum mismatch.' }
    [System.IO.File]::WriteAllText($statusLog, 'Installed echo-magic-epicfight-adapter-0.3.14.jar; SHA256 verified.')
} catch {
    [System.IO.File]::WriteAllText($statusLog, $_.Exception.Message)
    throw
}
