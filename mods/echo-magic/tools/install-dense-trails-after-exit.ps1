param([int] $MinecraftProcessId)
$ErrorActionPreference = 'Stop'
$modsDirectory = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\mods'
$sourceJar = 'D:\JDHSeason\mods\echo-magic\magic-forge-1.20.1\build\libs\echo-magic-0.8.17-all.jar'
$statusLog = 'D:\JDHSeason\mods\echo-magic\tools\install-dense-trails-status.log'
$configPath = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\config\echo-magic-client.toml'
function JarHash([string] $path) {
    $stream = [IO.File]::OpenRead($path)
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($sha.ComputeHash($stream)) }
    finally { $sha.Dispose(); $stream.Dispose() }
}
try {
    $expectedHash = JarHash $sourceJar
    [IO.File]::WriteAllText($statusLog, 'Waiting for Minecraft to exit.')
    if ($MinecraftProcessId -gt 0) {
        try { $game = [Diagnostics.Process]::GetProcessById($MinecraftProcessId) } catch { $game = $null }
        if ($game) { $game.WaitForExit(); $game.Dispose() }
    }
    if ([Diagnostics.Process]::GetProcessesByName('javaw').Length -gt 0) { throw 'A Java client is still running; JARs were left untouched.' }
    if ((JarHash $sourceJar) -ne $expectedHash) { throw 'Source JAR changed while waiting.' }
    foreach ($oldJar in [IO.Directory]::GetFiles($modsDirectory, 'echo-magic-*-all.jar')) {
        if ([IO.Path]::GetFileName($oldJar) -eq 'echo-magic-0.8.17-all.jar') { continue }
        $backupPath = $oldJar + '.disabled'
        if ([IO.File]::Exists($backupPath)) { $backupPath += '.' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff') }
        [IO.File]::Move($oldJar, $backupPath)
    }
    $installedJar = [IO.Path]::Combine($modsDirectory, 'echo-magic-0.8.17-all.jar')
    [IO.File]::Copy($sourceJar, $installedJar, $true)
    if ((JarHash $installedJar) -ne $expectedHash) { throw 'Installed JAR checksum mismatch.' }
    if ([IO.File]::Exists($configPath)) {
        $configText = [IO.File]::ReadAllText($configPath)
        $updated = $configText.Replace('maximum_magic_particles_per_frame = 512', 'maximum_magic_particles_per_frame = 1024').Replace('maximum_magic_particles_active = 4096', 'maximum_magic_particles_active = 12288')
        if ($updated -ne $configText) {
            [IO.File]::Copy($configPath, $configPath + '.before-dense-trails.' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff'), $false)
            [IO.File]::WriteAllText($configPath, $updated, [Text.UTF8Encoding]::new($false))
        }
    }
    [IO.File]::WriteAllText($statusLog, 'Installed Echo Magic 0.8.17; SHA256 verified; particle budgets updated.')
} catch {
    [IO.File]::WriteAllText($statusLog, $_.Exception.Message)
    throw
}