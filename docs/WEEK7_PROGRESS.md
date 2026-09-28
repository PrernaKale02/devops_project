# Week 7 Progress: Jenkins Continuous Integration

## Objective

Add a Jenkins CI pipeline for the Child Education Sponsorship System using Java
17 and Maven, while preserving the existing Week 6 application functionality.

## Jenkins setup

Jenkins is not installed or started as part of this repository change. To run
the pipeline manually:

1. Install and start Jenkins on a machine with Git, Java 17, and Maven 3
   available to the Jenkins agent. The pipeline supports both Windows and Unix
   agents. Ensure the Pipeline and Git plugins are installed.
2. In **Manage Jenkins > Tools**, add a JDK installation named `JDK17` and a
   Maven installation named `Maven3`. Configure the JDK path or automatic
   installer and the Maven 3 installation for the Jenkins agent.
3. Ensure the Jenkins agent can access
   `https://github.com/PrernaKale02/devops_project`. Configure repository
   credentials in Jenkins only if the repository is made private.

## Jenkins job configuration

1. Select **New Item**, enter a job name, choose **Pipeline**, and select **OK**.
2. Under **Pipeline**, set **Definition** to **Pipeline script from SCM**.
3. Set **SCM** to **Git** and set the repository URL to
   `https://github.com/PrernaKale02/devops_project`.
4. Set **Branch Specifier** to `*/main`.
5. Set **Script Path** to `Jenkinsfile`, save, and select **Build Now**.

No webhook or automatic trigger is required for the initial academic CI job.

## Build and test commands

- Build: `mvn -B clean compile -DskipTests`
- Test: `mvn -B test`
- Package: `mvn -B package -DskipTests`

Maven exits unsuccessfully when a test fails, which fails the Jenkins Test stage
and prevents the later Package stage from running.

## Artifact produced

The Spring Boot executable JAR is
`target/child-education-sponsorship-0.1.0.jar`. The Package stage archives
`target/*.jar` in Jenkins and fingerprints the artifact.

## Expected pipeline stages

1. **Checkout**: checks out the configured Git branch.
2. **Build**: cleans the Maven output and compiles the Java 17 application.
3. **Test**: runs the Maven test suite; test failures fail the pipeline.
4. **Package**: creates the executable Spring Boot JAR and archives it.

## Verification steps

1. In the repository, run `mvn clean test` and confirm **BUILD SUCCESS**.
2. In Jenkins, run the Pipeline job and confirm all four stages complete.
3. Open the completed build's **Artifacts** and confirm the JAR is available.
4. To verify test-failure handling, introduce a temporary failing test in a
   disposable local change, run the job, confirm the Test stage fails, and then
   discard that temporary change without committing it.

Local Maven verification passed with 3 tests, 0 failures, 0 errors, and 0
skipped. Jenkins was not run or verified as installed/running.
