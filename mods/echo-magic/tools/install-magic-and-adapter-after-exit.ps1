param([int] $MinecraftProcessId)
$ErrorActionPreference = 'Stop'
& 'D:\JDHSeason\mods\echo-magic\tools\install-dense-trails-after-exit.ps1' -MinecraftProcessId $MinecraftProcessId
& 'D:\JDHSeason\mods\echo-magic-epicfight-adapter\tools\install-particle-weapons-after-exit.ps1' -MinecraftProcessId 0