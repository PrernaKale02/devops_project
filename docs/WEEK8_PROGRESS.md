# Week 8 Progress: Jenkins Pipeline-as-Code Deployment

## Objective

Extend the Week 7 Jenkins CI pipeline with a local deployment stage that starts
the packaged Spring Boot JAR, without changing the application's Java 17
version or existing application behavior.

## Pipeline stages

The declarative pipeline keeps the existing stages and runs them in order:

1. **Checkout**: checks out the configured source branch.
2. **Build**: cleans and compiles the application.
3. **Test**: runs the Maven test suite.
4. **Package**: creates the executable JAR and archives `target/*.jar`.
5. **Deploy**: on a Windows agent, runs `scripts/deploy.ps1` against the
   packaged `target/child-education-sponsorship-0.1.0.jar`.

Concurrent Jenkins builds are disabled so two builds cannot compete for the
same local application port.

## Deployment approach

The PowerShell deployment script runs from the checked-out workspace and uses
workspace-relative paths. It checks the listener on port 8001 before deployment.
If the listener is a Java process whose command line identifies this
application, the script stops it before starting the new JAR. It fails without
stopping the process if the port belongs to an unrecognized service. The new
JAR is started as a separate background process; Jenkins waits only for the
script and health check, not for the long-running Spring Boot process.

Standard output, standard error, and the process ID are stored in a per-build
folder under `deployment-logs/`. On health-check failure, the new process is
stopped and the last 40 lines of each available log are printed in Jenkins.

## Port configuration

- Jenkins: port 8000, running on Java 21 as configured outside this project.
- Spring Boot application: port 8001, using the project's existing Java 17
  configuration.
- Application health URL: `http://localhost:8001/`.

The application port remains configured in
`src/main/resources/application.properties`; the deployment script also passes
`--server.port=8001` to make the intended deployment port explicit.

## Health-check approach

After starting the JAR, the script makes up to 30 HTTP requests to the root URL,
with a two-second interval and a five-second request timeout. An HTTP status in
the 2xx range is success. If the application exits or no successful response
arrives before the retry window ends, the script stops the new process and
exits with an error, causing the Jenkins Deploy stage and pipeline to fail.

## Jenkins configuration

Use the existing Pipeline job configured from SCM with `Jenkinsfile`. Configure
Jenkins tools named `JDK17` and `Maven3` as in Week 7. The agent executing this
pipeline must be Windows and have access to PowerShell, `Get-NetTCPConnection`,
CIM process queries, Java 17 through the configured JDK tool, and the workspace.
Ensure any existing application listener on port 8001 runs under the Jenkins
agent account or that account has permission to stop it. No Docker installation
or new Jenkins plugin is required for deployment.

The Jenkins controller/runtime can remain on Java 21 and port 8000. The
application runs on the Windows agent at port 8001. If Jenkins runs the job on
a remote agent, the app and `localhost:8001` health check refer to that agent,
not the controller machine.

## Verification steps

1. Run `mvn clean test` and confirm the build and tests pass.
2. Run `mvn -B package -DskipTests` to produce the executable JAR.
3. On Windows, run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\deploy.ps1`.
4. Confirm the script reports a successful health check for
   `http://localhost:8001/` and that the URL responds with a 2xx HTTP status.
5. Inspect the generated stdout/stderr logs under `deployment-logs/`.
6. Run the Jenkins Pipeline job on its Windows agent and confirm all five
   stages pass and the Package artifact is archived.

Local verification completed: `mvn clean test` passed with 3 tests, 0 failures,
0 errors, and 0 skipped. `mvn -B package -DskipTests` produced the executable
JAR. Running `scripts/deploy.ps1` with `JAVA_HOME` set to Java 17.0.6 started the
application, and both the script health check and a separate GET to
`http://localhost:8001/` returned HTTP 200. The active listener was confirmed
to use the Java 17 executable. The run created stdout, stderr, and PID files
under `deployment-logs/`; stderr was empty and stdout contained Spring Boot
startup logs. The configured Jenkins job itself has not yet been run for Week 8.

## Known limitations

- This is a single-machine academic deployment, not production process
  supervision or zero-downtime deployment.
- The application uses the existing in-memory H2 database, so data is not
  preserved across application restarts.
- The deployment script currently requires a Windows Jenkins agent and standard
  Windows PowerShell networking/process cmdlets.
- Replacing an existing listener requires the Jenkins agent account to have
  permission to stop that process; unrelated or unrecognized processes are not
  stopped automatically.
- Jenkins job execution has not been included in repository-local verification;
  it must be confirmed in the configured Jenkins instance.
- The application has no dedicated readiness endpoint; the root page is used
  for the basic HTTP health check.
