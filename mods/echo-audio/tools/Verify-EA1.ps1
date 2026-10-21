param([string]$ModsRoot = 'D:\JDHSeason\mods')
$ErrorActionPreference = 'Stop'
$steps = @(
    @{ Repo = 'echo-voice'; Tasks = @('check', 'build') },
    @{ Repo = 'echo-audio'; Tasks = @('check', 'build', ':audio-forge-1.20.1:jarJar') },
    @{ Repo = 'echo-scene'; Tasks = @('check', 'build', ':scene-forge-1.20.1:jarJar') },
    @{ Repo = 'echo-observation'; Tasks = @('check', 'build', 'jarJar') },
    @{ Repo = 'echo-story-engine'; Tasks = @('check', ':story-forge-1.20.1:jarJar') },
    @{ Repo = 'echo-content-doctor'; Tasks = @('check', 'build') }
)
# These composite builds share compilation outputs. Run repositories sequentially.
foreach ($step in $steps) {
    $repoPath = Join-Path $ModsRoot $step.Repo
    $reportDirectory = Join-Path $repoPath 'build/reports/ea1'
    New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
    $logPath = Join-Path $reportDirectory 'verification.log'
    Push-Location -LiteralPath $repoPath
    try {
        $taskArguments = $step.Tasks
        & .\gradlew.bat @taskArguments --no-parallel --console=plain *> $logPath
        if ($LASTEXITCODE -ne 0) {
            Get-Content -LiteralPath $logPath -Tail 65
            throw "EA1 verification failed: $($step.Repo); log: $logPath"
        }
        Write-Output "EA1 PASS: $($step.Repo)"
    } finally { Pop-Location }
}
