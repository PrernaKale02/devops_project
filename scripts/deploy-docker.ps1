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
$imageId = & docker image inspect --format '{{.Id}}' $image 2>&1
$imageInspectExitCode = $LASTEXITCODE
if ($imageInspectExitCode -ne 0) {
    throw "The build image '$image' does not exist or could not be inspected (exit $imageInspectExitCode): $imageId"
}
$existingNameOutput = & docker container ls -a --filter "name=^/$containerName$" --format '{{.Names}}' 2>&1
$listExitCode = $LASTEXITCODE
if ($listExitCode -ne 0) {
    throw "Could not list containers before deployment (exit $listExitCode): $existingNameOutput"
}
$existingName = ($existingNameOutput | Select-Object -First 1 | Out-String).Trim()
if ($existingName -eq $containerName) {
    Write-Output "Existing project container found: $containerName"

    $runningOutput = & docker container inspect --format '{{.State.Running}}' $containerName 2>&1
    $runningExitCode = $LASTEXITCODE
    if ($runningExitCode -ne 0) {
        throw "Could not inspect existing project container '$containerName' (exit $runningExitCode): $runningOutput"
    }
    $running = ($runningOutput | Out-String).Trim()

    if ($running -eq 'true') {
        Write-Output 'Stopping existing project container...'
        $stopOutput = & docker stop $containerName 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to stop existing project container '$containerName': $stopOutput"
        }
    }

    Write-Output 'Removing existing project container...'
    $removeOutput = & docker rm $containerName 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to remove existing project container '$containerName': $removeOutput"
    }
    Write-Output 'Removed the project container; sponsorship-data volume was preserved.'
} else {
    Write-Output 'No existing project container found. Continuing with first deployment.'
}
$dockerRunArguments = @(
    'run'
    '-d'
    '--name'
    $containerName
    '--label'
    "app=$projectLabelValue"
    '--publish'
    "${port}:${port}"
    '--volume'
    'sponsorship-data:/app/data'
    $image
)
$runOutput = & docker @dockerRunArguments 2>&1
$runExitCode = $LASTEXITCODE
if ($runExitCode -ne 0) {
    Write-Output $runOutput
    exit 1
}
Write-Output "Started container '$containerName' from '$image'. Container ID: $runOutput"

$state = & docker container inspect --format '{{.State.Status}}' $containerName 2>&1
$stateExitCode = $LASTEXITCODE
if ($stateExitCode -ne 0 -or ($state | Out-String).Trim() -ne 'running') {
    $logs = & docker logs --tail 60 $containerName 2>&1
    $logsExitCode = $LASTEXITCODE
    if ($logsExitCode -eq 0) { Write-Output $logs }
    throw "Container '$containerName' did not remain running (inspect exit $stateExitCode): $state"
}
$psOutput = & docker ps --filter "name=^/$containerName$" --format '{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}' 2>&1
$psExitCode = $LASTEXITCODE
if ($psExitCode -ne 0 -or -not ($psOutput | Out-String).Contains($containerName)) {
    throw "Container '$containerName' is not present as a running container in docker ps (exit $psExitCode): $psOutput"
}
Write-Output 'docker ps verification:'
Write-Output $psOutput

$inspectJson = & docker container inspect $containerName 2>&1
$newInspectExitCode = $LASTEXITCODE
if ($newInspectExitCode -ne 0) {
    throw "Could not inspect deployed container '$containerName' (exit $newInspectExitCode): $inspectJson"
}
$inspect = ($inspectJson | Out-String) | ConvertFrom-Json
$container = $inspect[0]
if ($container.Config.Image -ne $image) {
    throw "Container image mismatch: expected '$image', found '$($container.Config.Image)'."
}
if (-not $container.State.Running) {
    throw "Container '$containerName' is not running according to docker inspect."
}
$portBinding = $container.NetworkSettings.Ports."${port}/tcp"
if (-not ($portBinding | Where-Object { $_.HostPort -eq [string]$port })) {
    throw "Container '$containerName' is not published on host port $port."
}
if (-not ($container.Mounts | Where-Object { $_.Name -eq 'sponsorship-data' -and $_.Destination -eq '/app/data' })) {
    throw "Container '$containerName' is missing the sponsorship-data:/app/data volume mount."
}
Write-Output "docker inspect verification: image=$($container.Config.Image); running=$($container.State.Running); port=$port`:$port; volume=sponsorship-data:/app/data"
