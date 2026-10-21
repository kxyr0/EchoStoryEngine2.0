param(
    [Parameter(Mandatory = $true)]
    [int] $MinecraftProcessId
)

$ErrorActionPreference = 'Stop'
$modsDirectory = 'C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\mods'

$replacements = @(
    @{
        Source = 'D:\JDHSeason\mods\echo-magic\magic-forge-1.20.1\build\libs\echo-magic-0.8.11-all.jar'
        Previous = 'echo-magic-0.8.10-all.jar'
        Current = 'echo-magic-0.8.11-all.jar'
    },
    @{
        Source = 'D:\JDHSeason\mods\echo-fire-magic\fire-magic-forge-1.20.1\build\libs\echo-fire-magic-0.3.2.jar'
        Previous = 'echo-fire-magic-0.3.1.jar'
        Current = 'echo-fire-magic-0.3.2.jar'
    },
    @{
        Source = 'D:\JDHSeason\mods\echo-magic-weapons\build\libs\echo-magic-weapons-0.1.1.jar'
        Previous = 'echo-magic-weapons-0.1.0.jar'
        Current = 'echo-magic-weapons-0.1.1.jar'
    },
    @{
        Source = 'D:\JDHSeason\mods\echo-magic-epicfight-adapter\build\libs\echo-magic-epicfight-adapter-0.3.1.jar'
        Previous = 'echo-magic-epicfight-adapter-0.3.0.jar'
        Current = 'echo-magic-epicfight-adapter-0.3.1.jar'
    },
    @{
        Source = 'D:\JDHSeason\mods\echo-story-content-pack\build\libs\echo-story-content-pack-0.4.1-SNAPSHOT.jar'
        Previous = 'echo-story-content-pack-0.4.0-SNAPSHOT.jar'
        Current = 'echo-story-content-pack-0.4.1-SNAPSHOT.jar'
    }
)

Wait-Process -Id $MinecraftProcessId -ErrorAction SilentlyContinue

foreach ($replacement in $replacements) {
    $previous = Join-Path $modsDirectory $replacement.Previous
    $disabled = "$previous.disabled"
    $current = Join-Path $modsDirectory $replacement.Current

    if ((Test-Path -LiteralPath $previous) -and !(Test-Path -LiteralPath $disabled)) {
        Move-Item -LiteralPath $previous -Destination $disabled
    }
    Copy-Item -LiteralPath $replacement.Source -Destination $current -Force

    $sourceHash = (Get-FileHash -LiteralPath $replacement.Source -Algorithm SHA256).Hash
    $currentHash = (Get-FileHash -LiteralPath $current -Algorithm SHA256).Hash
    if ($sourceHash -ne $currentHash) {
        throw "Checksum mismatch after installing $($replacement.Current)"
    }
}
