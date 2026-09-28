# Ansible-Provisioned Child Education Sponsorship System

**Name:** Prerna Navnit Kale  
**Roll Number:** 24102B2001  
**Class:** BE CMPN B

## Project

A small web-based Child Education Sponsorship System designed to centralize sponsorship records and demonstrate a complete DevOps lifecycle.

## Implemented MVP

- Landing page with navigation to sponsorship management
- Sponsorship record creation, listing, editing and deletion
- Search by child ID, child name, sponsor name and exact status
- Pending, Active, Completed and Cancelled status values
- Status updates from the records page
- Summary dashboard with total and per-status counts

Authentication, user accounts and role-based authorization are planned but are not
implemented in the current MVP.

## Technology Stack

- Java 17
- Spring Boot 3.5.5
- Maven
- Thymeleaf
- Spring Data JPA
- H2 for local development

The Jenkins Pipeline-as-Code definition is in `Jenkinsfile`. Week 8 adds local
deployment of the packaged JAR on a Windows Jenkins agent; Jenkins itself must
be installed and configured separately.

## Run locally

```bash
mvn clean test
mvn spring-boot:run
```

Open http://localhost:8001

H2 console: http://localhost:8001/h2-console

The required Java version is 17. The test suite uses Spring Boot integration
tests with an in-memory H2 database.

## Verification commands

```bash
mvn clean test
```

## Jenkins CI/CD

Configure Jenkins with JDK 17 and Maven 3 tools named `JDK17` and `Maven3`, then
create a Pipeline job from this Git repository using the `Jenkinsfile`. The
agent running the job must be Windows for the Week 8 Deploy stage. Jenkins may
run on Java 21 and port 8000; the deployed Spring Boot application uses Java 17
and port 8001.

The pipeline checks out the source, builds, tests, packages and archives the
Spring Boot JAR, then runs `scripts/deploy.ps1` to start that JAR on port 8001.
The deploy script replaces a previous instance of this application when it
owns port 8001, writes per-build stdout/stderr logs under `deployment-logs/`,
and checks `http://localhost:8001/`. It fails without stopping an unrecognized
process that occupies the application port. Concurrent Jenkins builds are
disabled because they share this local port. See [Week 7 progress](docs/WEEK7_PROGRESS.md)
for initial Jenkins setup and [Week 8 progress](docs/WEEK8_PROGRESS.md) for
deployment and verification details.

## Planned DevOps flow

GitHub → Jenkins → Maven Build → Selenium → Docker → Deployment → Ansible → Health Check
