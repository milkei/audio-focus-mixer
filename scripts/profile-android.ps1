# Capture comparable Android app memory / CPU diagnostics.
# Keep output private: dumpsys and ps may contain device-specific information.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("before", "after")]
    [string]$Label,
    [ValidateSet("idle", "scroll", "refresh")]
    [string]$Scenario = "idle"
)

$ErrorActionPreference = "Stop"
$packageName = "dev.milkei.audiofocusmixer"
$null = Get-Command adb -ErrorAction Stop
$appPidOutput = & adb shell pidof $packageName
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($appPidOutput)) {
    throw "Audio Focus Mixer is not running. Start the app, then retry."
}
$appPid = ($appPidOutput.Trim() -split "\s+")[0]
$directory = Join-Path (Split-Path $PSScriptRoot -Parent) ".perf-results"
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$prefix = "$Label-$Scenario"

& adb shell dumpsys meminfo $packageName |
    Out-File -FilePath (Join-Path $directory "$prefix-meminfo.txt") -Encoding utf8

& adb shell top -b -n 5 -d 1 -p $appPid |
    Out-File -FilePath (Join-Path $directory "$prefix-top.txt") -Encoding utf8

& adb shell ps -A |
    Select-String -Pattern "audiofocus" |
    Out-File -FilePath (Join-Path $directory "$prefix-processes.txt") -Encoding utf8

Write-Output "Captured PSS, CPU samples, and process list under $directory"
Write-Output "Compare identical scenarios and look for additional Shizuku user-service processes."
