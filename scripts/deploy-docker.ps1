$ErrorActionPreference = 'Stop'

$containerName = 'child-education-sponsorship'
$projectLabelValue = 'child-education-sponsorship'
$port = 8001
$buildNumber = $env:BUILD_NUMBER

if ([string]::IsNullOrWhiteSpace($buildNumber) -or $buildNumber -notmatch '^\d+$') {
    throw 'BUILD_NUMBER must be set to a numeric Jenkins build number before Docker deployment.'
}

$image = "child-education-sponsorship:$buildNumber"
$serverVersion = & docker info --format '{{.ServerVersion}}' 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker Engine is not available to this Jenkins agent: $serverVersion"
}
$imageId = & docker image inspect --format '{{.Id}}' $image 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "The build image '$image' does not exist; Docker Deployment cannot continue."
}

$existingImage = & docker container inspect --format '{{.Config.Image}}' $containerName 2>$null
if ($LASTEXITCODE -eq 0) {
    $existingImage = ($existingImage | Out-String).Trim()
    $labelsJson = & docker container inspect --format '{{json .Config.Labels}}' $containerName 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect existing container '$containerName'; it was not changed."
    }
    $labels = ($labelsJson | Out-String).Trim() | ConvertFrom-Json
    $existingLabel = if ($labels) { $labels.PSObject.Properties['com.prerna.project'].Value } else { $null }
    $isProjectContainer = $existingLabel -eq $projectLabelValue -or
        $existingImage -match '(^|/)child-education-sponsorship:[^/]+$'
    if (-not $isProjectContainer) {
        throw "Container '$containerName' is not identified as this project (image '$existingImage', label '$existingLabel'); it was not changed."
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
    Write-Output "Removed only the identified prior project container '$containerName'; the sponsorship-data volume was preserved."
} elseif ($LASTEXITCODE -ne 1) {
    throw "Docker could not reliably inspect the existing container '$containerName'; deployment was stopped."
}

$runOutput = & docker run -d `
    --name $containerName `
    --label "$projectLabel=$projectLabelValue" `
    --publish "${port}:${port}" `
    --volume 'sponsorship-data:/app/data' `
    $image 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker failed to start '$image': $runOutput"
}
Write-Output "Started container '$containerName' from '$image'. Container ID: $runOutput"

$state = & docker container inspect --format '{{.State.Status}}' $containerName 2>$null
if ($LASTEXITCODE -ne 0 -or ($state | Out-String).Trim() -ne 'running') {
    $logs = & docker logs --tail 60 $containerName 2>&1
    Write-Output $logs
    throw "Container '$containerName' did not remain running after launch."
}
$publishedPorts = & docker port $containerName "${port}/tcp" 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Could not verify the published container port: $publishedPorts"
}
Write-Output "Container status: running. Port mapping: $publishedPorts"
Write-Output 'Persistent database volume: sponsorship-data:/app/data'
