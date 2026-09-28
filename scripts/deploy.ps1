$ErrorActionPreference = 'Stop'

$port = 8001
$healthUrl = "http://localhost:$port/"
$workspace = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $workspace 'target/child-education-sponsorship-0.1.0.jar'
$logRoot = Join-Path $workspace 'deployment-logs'
$buildLabel = if ($env:BUILD_NUMBER) { "build-$env:BUILD_NUMBER" } else { Get-Date -Format 'yyyyMMdd-HHmmss' }
$logDirectory = Join-Path $logRoot $buildLabel
$stdoutLog = Join-Path $logDirectory 'application.stdout.log'
$stderrLog = Join-Path $logDirectory 'application.stderr.log'
$pidPath = Join-Path $logDirectory 'application.pid'

if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Packaged application JAR was not found: $jarPath"
}

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

$listenerIds = @(
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
)
foreach ($listenerId in $listenerIds) {
    $listener = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $listenerId" -ErrorAction SilentlyContinue
    if ($null -eq $listener) {
        continue
    }

    if ($listener.Name -notin @('java.exe', 'javaw.exe') -or $listener.CommandLine -notmatch '(?i)child-education-sponsorship') {
        throw "Port $port is occupied by an unrecognized process (PID $listenerId, $($listener.Name)); it was not stopped."
    }

    Write-Output "Stopping previous sponsorship application (PID $listenerId) on port $port."
    Stop-Process -Id $listenerId -Force
}

$stopDeadline = (Get-Date).AddSeconds(15)
while ((Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) -and (Get-Date) -lt $stopDeadline) {
    Start-Sleep -Seconds 1
}
if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
    throw "Port $port is still occupied after stopping the previous application."
}

$javaPath = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { $null }
if (-not $javaPath) {
    throw 'JAVA_HOME must point to the configured Java 17 installation.'
}
if (-not (Test-Path -LiteralPath $javaPath -PathType Leaf)) {
    throw "Java executable was not found: $javaPath"
}
$javaReleaseFile = Join-Path $env:JAVA_HOME 'release'
$javaRelease = if (Test-Path -LiteralPath $javaReleaseFile -PathType Leaf) {
    Get-Content -LiteralPath $javaReleaseFile | Where-Object { $_ -like 'JAVA_VERSION=*' }
} else {
    $null
}
if ($javaRelease -notmatch '^JAVA_VERSION="17(?:\.|"|-)') {
    throw "Java 17 is required to deploy this application. JAVA_HOME is $env:JAVA_HOME."
}
$javaPath = (Resolve-Path -LiteralPath $javaPath).Path
$arguments = "-jar `"$jarPath`" --server.port=$port"
$process = Start-Process -FilePath $javaPath `
    -ArgumentList $arguments `
    -WorkingDirectory $workspace `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -WindowStyle Hidden `
    -PassThru
$process.Id | Set-Content -LiteralPath $pidPath -Encoding Ascii
Write-Output "Started packaged application (PID $($process.Id)) on port $port."
Write-Output "Application logs: $logDirectory"

$healthy = $false
$lastHealthError = 'The application did not return a successful response before the timeout.'
for ($attempt = 1; $attempt -le 30; $attempt++) {
    $process.Refresh()
    if ($process.HasExited) {
        $lastHealthError = "Application process exited with code $($process.ExitCode)."
        break
    }

    try {
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
            $healthy = $true
            break
        }
        $lastHealthError = "Health endpoint returned HTTP $($response.StatusCode)."
    }
    catch {
        $lastHealthError = $_.Exception.Message
    }

    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    $process.Refresh()
    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
    Remove-Item -LiteralPath $pidPath -ErrorAction SilentlyContinue
    Write-Output "Application stdout (last 40 lines):"
    if (Test-Path -LiteralPath $stdoutLog) { Get-Content -LiteralPath $stdoutLog -Tail 40 }
    Write-Output "Application stderr (last 40 lines):"
    if (Test-Path -LiteralPath $stderrLog) { Get-Content -LiteralPath $stderrLog -Tail 40 }
    throw "Deployment health check failed for $healthUrl. $lastHealthError"
}

Write-Output "Health check passed: $healthUrl returned HTTP $($response.StatusCode)."
