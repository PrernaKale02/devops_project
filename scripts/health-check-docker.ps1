$ErrorActionPreference = 'Stop'

$url = 'http://localhost:8001/'
$containerName = 'child-education-sponsorship'
$healthy = $false
$lastError = 'No successful HTTP response was received.'

for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            $healthy = $true
            break
        }
        $lastError = "Health endpoint returned HTTP $($response.StatusCode), expected HTTP 200."
    } catch {
        $lastError = $_.Exception.Message
    }
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    Write-Output "Docker container logs (last 60 lines):"
    & docker logs --tail 60 $containerName 2>&1
    throw "Docker health check failed for $url. $lastError"
}

Write-Output "Health check passed: $url returned HTTP $($response.StatusCode)."
