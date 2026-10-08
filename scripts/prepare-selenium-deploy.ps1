$ErrorActionPreference = 'Stop'

$containerName = 'child-education-sponsorship'
$projectLabelValue = 'child-education-sponsorship'

$serverVersion = & docker info --format '{{.ServerVersion}}' 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker Engine is not available to this Jenkins agent: $serverVersion"
}

$containerImage = & docker container inspect --format '{{.Config.Image}}' $containerName 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Output "No existing $containerName container needs to be stopped before the Selenium deployment."
    exit 0
}

$containerImage = ($containerImage | Out-String).Trim()
$labelsJson = & docker container inspect --format '{{json .Config.Labels}}' $containerName 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "Could not inspect existing container '$containerName'; it was not changed."
}
$labels = ($labelsJson | Out-String).Trim() | ConvertFrom-Json
$containerLabel = if ($labels) { $labels.PSObject.Properties['com.prerna.project'].Value } else { $null }
$isProjectContainer = $containerLabel -eq $projectLabelValue -or
    $containerImage -match '(^|/)child-education-sponsorship:[^/]+$'
if (-not $isProjectContainer) {
    throw "Container '$containerName' is not identified as this project (image '$containerImage', label '$containerLabel'); it was not changed."
}

$running = & docker container inspect --format '{{.State.Running}}' $containerName 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "Could not determine whether '$containerName' is running; it was not changed."
}
if (($running | Out-String).Trim() -eq 'true') {
    $stopOutput = & docker stop $containerName 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Could not stop the existing project container '$containerName': $stopOutput"
    }
}

$removeOutput = & docker rm $containerName 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Could not remove the existing project container '$containerName': $removeOutput"
}
Write-Output "Removed only the identified project container '$containerName' before the Week 8 local deployment. The sponsorship-data volume was preserved."
