$ErrorActionPreference = 'Stop'

$registryHost = $env:DOCKER_REGISTRY_HOST
$namespace = $env:DOCKER_REGISTRY_NAMESPACE
$repository = $env:DOCKER_REGISTRY_REPOSITORY
$username = $env:DOCKER_REGISTRY_USERNAME
$password = $env:DOCKER_REGISTRY_PASSWORD
$buildNumber = $env:BUILD_NUMBER

if ([string]::IsNullOrWhiteSpace($registryHost) -or $registryHost -notmatch '^[A-Za-z0-9.-]+(?::\d+)?$') {
    if ([string]::IsNullOrWhiteSpace($registryHost)) {
        $registryHost = 'docker.io'
    } else {
        throw 'DOCKER_REGISTRY_HOST must be a registry host name, optionally with a port.'
    }
}
if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($password)) {
    throw 'Registry credentials were not provided through Jenkins credentials binding.'
}
if ([string]::IsNullOrWhiteSpace($buildNumber) -or $buildNumber -notmatch '^\d+$') {
    throw 'BUILD_NUMBER must be set to a numeric Jenkins build number before registry publishing.'
}
if ([string]::IsNullOrWhiteSpace($repository)) {
    if ([string]::IsNullOrWhiteSpace($namespace)) {
        $namespace = $username
    }
    if ($namespace -notmatch '^[A-Za-z0-9._-]+$') {
        throw 'DOCKER_REGISTRY_NAMESPACE must be a valid registry namespace.'
    }
    if ($registryHost -eq 'docker.io') {
        $repository = "docker.io/$namespace/child-education-sponsorship"
    } else {
        $repository = "$registryHost/$namespace/child-education-sponsorship"
    }
} elseif ($repository -notmatch '^(?:[A-Za-z0-9.-]+(?::\d+)?/)?[A-Za-z0-9._-]+/child-education-sponsorship$') {
    throw 'DOCKER_REGISTRY_REPOSITORY must identify a namespace and the child-education-sponsorship repository.'
}

$dockerCommand = Get-Command docker.exe -ErrorAction Stop
$loginInfo = New-Object System.Diagnostics.ProcessStartInfo
$loginInfo.FileName = $dockerCommand.Source
$loginInfo.Arguments = "login `"$registryHost`" --username `"$username`" --password-stdin"
$loginInfo.UseShellExecute = $false
$loginInfo.RedirectStandardInput = $true
$loginInfo.RedirectStandardOutput = $true
$loginInfo.RedirectStandardError = $true
$loginProcess = New-Object System.Diagnostics.Process
$loginProcess.StartInfo = $loginInfo
[void]$loginProcess.Start()
$loginProcess.StandardInput.WriteLine($password)
$loginProcess.StandardInput.Close()
$loginStdout = $loginProcess.StandardOutput.ReadToEnd()
$loginStderr = $loginProcess.StandardError.ReadToEnd()
$loginProcess.WaitForExit()
if ($loginProcess.ExitCode -ne 0) {
    throw "Docker registry login failed: $loginStdout $loginStderr"
}
Write-Output "Authenticated to registry '$registryHost' using the Jenkins credential binding."

try {
    $versionedLocal = "child-education-sponsorship:$buildNumber"
    $versionedRemote = "${repository}:$buildNumber"
    $latestRemote = "${repository}:latest"

    & docker image inspect $versionedLocal *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Local build image '$versionedLocal' was not found; no registry image was pushed."
    }

    & docker tag $versionedLocal $versionedRemote
    if ($LASTEXITCODE -ne 0) { throw "Could not tag '$versionedRemote'." }
    & docker tag $versionedLocal $latestRemote
    if ($LASTEXITCODE -ne 0) { throw "Could not tag '$latestRemote'." }

    & docker push $versionedRemote
    if ($LASTEXITCODE -ne 0) { throw "Push failed for '$versionedRemote'." }
    & docker push $latestRemote
    if ($LASTEXITCODE -ne 0) { throw "Push failed for '$latestRemote'." }
    Write-Output "Pushed registry tags '$versionedRemote' and '$latestRemote'."
} finally {
    & docker logout $registryHost *> $null
}
