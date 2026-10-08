# Week 12 Progress: Jenkins-Docker Continuous Deployment

## Objective

Use Docker as the Week 12 deployment mechanism, optionally publish an image when Jenkins registry credentials are configured, health-check the deployed application, and run the unchanged Week 10 Selenium suite against that container. The Week 8 direct-JAR deployment is not part of the Week 12 path.

## Pipeline stages

The active `Jenkinsfile` stages are ordered as follows:

1. **Checkout**: checks out the configured SCM revision.
2. **Build**: runs the existing Maven clean compile.
3. **Test**: runs the existing Surefire tests.
4. **Package**: creates the JAR and archives `target/*.jar`.
5. **Docker Build**: builds from the unchanged repository Dockerfile and tags `child-education-sponsorship:%BUILD_NUMBER%` plus `child-education-sponsorship:latest`.
6. **Docker Registry Push**: conditionally publishes both tags when the `DOCKER_REGISTRY_CREDENTIALS` Jenkins environment setting supplies a credential ID; otherwise it skips.
7. **Docker Deployment**: replaces only the identified project container, uses host/container port `8001:8001`, and mounts `sponsorship-data:/app/data`.
8. **Health Check**: retries `http://localhost:8001/` and fails unless it returns HTTP 200.
9. **Selenium Tests**: runs the unchanged `mvn failsafe:integration-test failsafe:verify` after deployment and health check, then publishes existing JUnit/Failsafe reports.

The pipeline uses Windows PowerShell and Docker Desktop on a Windows Jenkins agent. Maven, Docker, deployment, health-check, and Selenium failures fail their stages.

## Docker build and deployment implementation

The existing Java 17 multi-stage `Dockerfile` and `.dockerignore` were left unchanged. The Docker Build stage tags the Jenkins build number and `latest`.

`scripts/deploy-docker.ps1` deploys the build-number image. It verifies the specifically named existing container's project label or image identity, then stops/removes only that container. It does not inspect, stop, or kill Windows processes. It launches the replacement with label `com.prerna.project=child-education-sponsorship`, mapping `8001:8001`, and named volume `sponsorship-data:/app/data`; it never removes the volume. If Docker cannot bind the required port, deployment fails.

`scripts/health-check-docker.ps1` retries the root URL up to 30 times and prints recent container logs on failure. `scripts/push-docker-image.ps1` obtains credentials only through Jenkins credential binding and sends the password to `docker login` over standard input; it contains no credentials.

## Registry discovery

The interactive Windows user's Docker configuration uses the Docker Desktop credential helper. Jenkins runs as `LocalSystem`, so that interactive login is not assumed available to Jenkins. Unauthenticated Jenkins API, crumb, and pipeline-linter requests returned HTTP 403. Jenkins credential-store contents could not be inspected, so no registry credential ID or push result is known. No registry login or push has been attempted. A Jenkins username/password credential and `DOCKER_REGISTRY_CREDENTIALS` configuration are required for a registry push; no token is stored in this repository.

## Failure motivating the correction

Jenkins build **#11** failed in the Week 8 direct-JAR Deploy stage before reaching the Week 12 Docker stages. The reported failure was host port 8001 occupied by PID 8196, `wslrelay.exe`; the script did not stop that unrecognized process. The root cause was invoking direct-JAR deployment before Docker. The active Week 12 path now omits that stage and does not inspect or stop Windows processes.

## Verification completed on 2026-10-08

Prior Week 11 evidence recorded Docker CLI/server version 29.8.2 (build `7fc2dff`) running on Docker Desktop with WSL2. The existing Week 11 image was `child-education-sponsorship:1.0.0`; its container had been verified on `8001:8001` with `sponsorship-data:/app/data`, and `http://localhost:8001/` returned HTTP 200. Those checks are not Week 12 Jenkins deployment results.

Prior local Maven verification recorded `mvn clean test` as BUILD SUCCESS (4 tests) and `mvn failsafe:integration-test failsafe:verify` as BUILD SUCCESS (5 Selenium tests) against the existing Dockerized app. The Selenium tests were not modified. These are prior local results, not results from the corrected Jenkins pipeline.

For this correction, `scripts/deploy-docker.ps1` parsed with zero PowerShell syntax errors. Jenkins `/login` returned HTTP 200, while unauthenticated `/api/json` returned HTTP 403. Docker CLI access from this shell failed with permission denied on the Docker Engine named pipe.

## Jenkins verification and limitations

The corrected Jenkinsfile was not submitted to Jenkins' Declarative Pipeline linter, and the corrected pipeline was not run because this session has no Jenkins authentication. No corrected build number or stage result exists. Docker Engine access was denied from this shell, so no new image was built, no container was replaced, and no post-deployment health check or Selenium run occurred. Build #11 is the only known Week 12 Jenkins build and failed before Docker Build. Registry push status remains unknown because Jenkins credentials could not be inspected.

Week 12 must remain incomplete until an authenticated Jenkins build reaches Docker Build, Docker Deployment, Health Check, and Selenium Tests successfully.

## Screenshots to capture

1. Jenkins stage view with actual build number and completed stages, including registry push or skip result.
2. Docker Build console output showing both actual image tags.
3. Registry push console output and repository tags if push is configured; keep credentials hidden.
4. Docker Deployment output and `docker ps`/`docker inspect` evidence showing image, `8001:8001`, and `sponsorship-data:/app/data`.
5. Health Check console output with HTTP 200 from `http://localhost:8001/`.
6. Jenkins Failsafe/JUnit report for the Selenium run.
7. Dashboard and sponsorship list screenshots showing seeded records.

## Conclusion

The active pipeline no longer runs the Week 8 direct-JAR deployment and now orders Docker Build, conditional registry push, Docker Deployment, Health Check, then unchanged Selenium tests. This fixes the reported pipeline ordering cause without touching host processes or the data volume. The corrected Jenkins build has not been run, so Week 12 is not yet verified complete.