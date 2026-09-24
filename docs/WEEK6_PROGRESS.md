# Week 6 Progress and Backlog

This record follows the project plan established during the earlier planning
weeks. It distinguishes the working MVP from future DevOps milestones.

## Completed

- Sponsorship CRUD is available through the Thymeleaf UI.
- Sponsorship search supports child ID, child name, sponsor name and exact status.
- Status values and status updates are available for sponsorship records.
- Dashboard counts are available at `/sponsorships/dashboard`.
- The landing page links to sponsorship management.
- Java 17 compatibility and `server.port=8001` were verified with Maven.
- Integration tests cover landing navigation, search, dashboard rendering and a
  status transition.
- A real Git merge conflict was created and resolved during Week 6 collaboration.
- Release tag `v1.0.0` was created after the verified MVP commit was merged.

## Pending backlog

- Add authentication, user accounts and role-based authorization.
- Add automated browser tests with Selenium.
- Add Jenkins pipeline configuration.
- Add Docker image and container deployment configuration.
- Add Ansible provisioning and deployment automation.
- Add MySQL deployment configuration and health-check endpoint.

## Week 6 verification

```bash
mvn clean test
```

Expected result: Maven `BUILD SUCCESS` with all project tests passing.
