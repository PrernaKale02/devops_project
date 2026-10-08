# Week 11 Progress: Docker Image and Container Lifecycle

## Objective

Build and tag the Child Education Sponsorship System image, run the application
in Docker on port 8001, verify the UI and existing seeded sponsorship data, and
prove that the container can stop and start while retaining its H2 database.

## Docker architecture

The application is built in a Maven/Java 17 stage. The executable Spring Boot
JAR is copied into a Java 17 JRE runtime stage. The runtime runs as the
unprivileged `spring` user, listens on container port 8001, and binds to
`0.0.0.0`. The existing H2 file database resolves to `/app/data/sponsorshipdb`
inside the image. A named Docker volume, `sponsorship-data`, is mounted at
`/app/data` so records survive container stop/start.

## Dockerfile and `.dockerignore`

The existing `Dockerfile` correctly builds
`target/child-education-sponsorship-0.1.0.jar` using
`maven:3.9-eclipse-temurin-17`, then copies that JAR into
`eclipse-temurin:17-jre`. It declares port 8001 and `/app/data` and starts Java
with `--server.address=0.0.0.0`. No Dockerfile change was needed.

`.dockerignore` excludes Git metadata, IDE configuration, Maven output, local
database files, deployment logs, environment files, docs, tests, and
development/deployment files from the build context. The application source,
Maven descriptor, and runtime resources remain included.

## Docker availability

Commands run on 2026-10-08:

```powershell
docker --version
docker info
docker system df
```

Docker CLI version: `29.8.2`, build `7fc2dff`. The successful `docker info`
reported context `desktop-linux`, Docker Desktop server version `29.8.2`,
Linux kernel `5.15.167.4-microsoft-standard-WSL2`, and 12 CPUs. Before the
build, the server reported 0 containers, 0 images, and `docker system df`
reported 0 bytes used by images, containers, local volumes, and build cache.

The sandboxed shell initially could not read the Docker client config or access
the Docker API named pipe. Retrying the Docker commands with elevated access
succeeded. No Docker installation or configuration changes were made.

## Image build and details

Build command:

```powershell
docker build -t child-education-sponsorship:1.0.0 .
```

Result: `BUILD SUCCESS`; Maven packaged the application and Docker exported the
image under the requested tag.

`docker images` showed:

```text
IMAGE                               ID             DISK USAGE   CONTENT SIZE
child-education-sponsorship:1.0.0   83512f1aac02   543MB        162MB
```

`docker image inspect child-education-sponsorship:1.0.0` reported:

- Repository/tag: `child-education-sponsorship:1.0.0`
- Image ID: `sha256:83512f1aac026fea64b1e11c7d472924822332fefe6a539e9bd8b0cdff46176d`
- Created: `2026-10-08T04:20:13.609899496Z`
- Image size: `542643641` bytes (Docker CLI disk usage rounded to 543 MB)
- Runtime metadata: Java 17.0.20.1, exposed `8001/tcp`, user `spring:spring`,
  entrypoint binds Spring to `0.0.0.0`, volume `/app/data`

## Port check and container run

Immediately before launch, `Get-NetTCPConnection -LocalPort 8001 -State Listen`
returned no listener (`PORT_8001_FREE`). Port 8001 was used without stopping
any process.

Command:

```powershell
docker run -d --name child-education-sponsorship -p 8001:8001 -v sponsorship-data:/app/data child-education-sponsorship:1.0.0
```

Docker returned container ID
`6752d9e201ab56da7a5dc680c38f01166f2b8c528eb256c79877c02bfc534656`.
`docker inspect` confirmed the container was running, mapped host `0.0.0.0:8001`
and `[::]:8001` to container `8001/tcp`, and mounted volume `sponsorship-data`
at `/app/data`.

`docker ps` evidence while running:

```text
CONTAINER ID   IMAGE                               STATUS       PORTS                                         NAMES
6752d9e201ab   child-education-sponsorship:1.0.0   Up           0.0.0.0:8001->8001/tcp, [::]:8001->8001/tcp   child-education-sponsorship
```

## Container logs and health

`docker logs child-education-sponsorship` showed Spring Boot `v3.5.5` starting
with Java `17.0.20.1`, H2 connecting at
`jdbc:h2:file:./data/sponsorshipdb`, and Tomcat starting on port 8001. The log
ended startup with `Started ChildSponsorshipApplication`; there were no startup
or database errors. Spring emitted its default `spring.jpa.open-in-view`
warning; it did not prevent startup or requests.

The root endpoint returned HTTP 200. Application URL:
`http://localhost:8001/`.

## Application and seeded data verification

PowerShell `Invoke-WebRequest` checks returned:

| Request | Result |
| --- | --- |
| `/` | HTTP 200; page title `Child Education Sponsorship System` |
| `/sponsorships/dashboard` | HTTP 200; dashboard heading present |
| `/sponsorships` | HTTP 200; all 8 seeded IDs present (`CH-1001` to `CH-1008`) |
| `/sponsorships?search=CH-1001` | HTTP 200; target present and `CH-1002` absent |
| `/sponsorships?status=ACTIVE` | HTTP 200; ACTIVE record present and PENDING `CH-1003` absent |
| `/sponsorships/children` | HTTP 200 |
| `/sponsorships/sponsors` | HTTP 200 |
| `/sponsorships/1` | HTTP 200 |
| `/css/portal.css` | HTTP 200; 8,666 bytes |

The dashboard HTML referenced `portal.css`; the CSS request succeeded. The
existing `SampleSponsorshipDataSeeder` inserts eight idempotent sample
sponsorships. Records were verified through the actual sponsorship-list HTML,
not a frontend-only data source.

## Stop/start lifecycle verification

Commands:

```powershell
docker stop child-education-sponsorship
docker ps -a --filter name=child-education-sponsorship
docker start child-education-sponsorship
docker ps
docker logs --tail 35 child-education-sponsorship
```

After stop, `docker ps -a` showed `Exited (143)`. After start, `docker ps`
showed the same container `Up` with the same host/container port mapping. The
post-start log showed Spring Boot and H2 starting successfully again. Root and
dashboard returned HTTP 200 after restart, and all 8 seeded sponsorship IDs
were still present, confirming persistence through the named volume. The
container was left running.

## Tests

- `mvn clean test`: **BUILD SUCCESS**, 4 tests, 0 failures, 0 errors, 0 skipped.
  The initial sandboxed attempt could not access Maven Central; retrying with
  elevated network access succeeded.
- `mvn failsafe:integration-test failsafe:verify`: **BUILD SUCCESS**, 5 Selenium
  UI tests, 0 failures, 0 errors, 0 skipped, run against the Docker app at
  `localhost:8001`. Selenium logged a CDP implementation warning for Chrome
  `154.0.8037.98`; the suite still completed successfully.

No UI, Jenkins, Selenium, or Ansible files were modified.

## Issues and fixes

The Docker CLI required elevated access in this execution environment to read
its config and access the Docker API. Docker checks and operations succeeded
when retried with that access. Host Maven also needed elevated network access
to resolve Maven Central dependencies. No application or Dockerfile fixes were
necessary. The browser emitted a CDP version warning during Selenium; all tests
passed.

## Evidence and conclusion

The evidence recorded above came from actual Docker build, image inspection,
container listing/logs/inspection, HTTP requests, and Maven/Selenium output on
2026-10-08. The final `docker system df` showed 1 image using 542.6 MB, 1
container using 90.11 kB, 1 volume using 32.77 kB, and 1.489 GB build cache
(946.4 MB reclaimable). No screenshots were captured. The image was built, the
container ran successfully on `8001:8001`, the dashboard and seeded sample
records were verified, search/filter and sponsorship pages responded as
expected, and the container successfully stopped and restarted with its data
intact. Week 11 Docker image/container lifecycle verification is complete.
