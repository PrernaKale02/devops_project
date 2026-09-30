# Week 10 Progress: Continuous Testing in Jenkins

## Objective

Run the existing Week 9 Selenium/Failsafe UI suite as a Jenkins pipeline stage
against the application deployed locally on the same Windows agent. Publish
JUnit-compatible Surefire and Failsafe results and fail the build when the
Selenium command fails.

## Jenkins Continuous-Testing Setup

The existing pipeline has one top-level `agent any`, then runs Checkout, Build,
Test, Package, and Deploy. The new **Selenium Tests** stage runs immediately
after Deploy on that same allocated agent, so `localhost:8001` refers to the
machine hosting the deployed application. No new Jenkins controller or agent
port is configured; Jenkins remains on port 8000 and Spring Boot remains on
port 8001.

The Jenkins agent must be Windows, have Chrome installed, and use the
configured `JDK17` and `Maven3` tools. The existing Selenium tests use Chrome in
headless mode and Selenium Manager to resolve the matching ChromeDriver. The
agent needs network access on the first run if Selenium Manager must download a
driver. No Docker or Ansible is introduced.

The existing Deploy stage starts the packaged application before Selenium
runs. Its process cookie allows the app to remain available after the deploy
script exits. The Selenium stage uses the same workspace and agent and does not
start or stop a second application instance.

## Selenium/Failsafe Command

The Jenkins stage runs exactly:

```text
mvn failsafe:integration-test failsafe:verify
```

Maven Failsafe discovers the existing `SponsorshipUiIT` suite through the
`*IT` naming convention. Selenium tests remain separate from the existing
Surefire tests and do not require changes to those tests.

## Test Report Publication

The Selenium stage has an `always` post action that publishes
`target/*-reports/TEST-*.xml` through Jenkins' `junit` publisher. The pattern
includes both Maven Surefire and Failsafe XML files. Reports are collected even
when the Maven step fails, provided the reports were generated. The Jenkins
JUnit plugin must be available to the Pipeline job.

## Failure Behavior

A non-zero exit from the Failsafe command fails the `Selenium Tests` stage and
pipeline. The `junit` publisher records individual test failures in Jenkins;
it does not replace the Maven exit status. A missing XML report also fails
report publication rather than silently making the test stage look successful.

## Defect/Failure Observed

During Week 9 Selenium execution, the create flow posted to
`/sponsorships/save` and received HTTP 400 instead of the controller's expected
redirect. Captured browser control values showed `age=120` and
`sponsorshipAmount=125500.0` because Selenium appended digits to the primitive
number inputs' initial `0` and `0.0` values. Chrome also submitted
`startDate=60929-02-20` after localized keyboard entry, which could not bind to
`LocalDate`.

## Fix Applied

Only the Selenium helper in `SponsorshipUiIT.java` was changed: it clears the
numeric inputs before typing and sets the native date input to an ISO date via
JavaScript, dispatching input/change events. No application, controller,
template, or existing integration-test behavior was changed.

A later local full-suite run exposed an intermittent
`StaleElementReferenceException` in the status-update flow: the test retained a
row `WebElement` while the status POST navigated and replaced the records page.
The Selenium test now waits for and reads the status cell through a fresh
record-specific locator, resolving the current DOM after navigation. No
application code was changed.

## Successful Rerun

The focused `createSponsorshipThroughForm` test passed after the form helper
fix. After the status locator fix, focused
`statusWorkflowUpdatesAutomationRecord` passed and the complete Selenium suite
passed: 5 tests, 0 failures, 0 errors, and 0 skipped. The normal Surefire suite
also passed locally: 3 tests, 0 failures, 0 errors, and 0 skipped. Chrome and
Selenium Manager worked in the local Windows environment. These are local test
results; the Jenkins pipeline stage has not yet been run, so no Jenkins success
or published Jenkins report is claimed here.

## Evidence to Capture

When the pipeline is run in Jenkins, capture:

- The Blue Ocean or classic stage view showing Selenium Tests after Deploy.
- The Selenium stage console output showing the exact Failsafe command and
  successful Maven result.
- The Jenkins test report showing all five Selenium tests and their outcomes.
- The build's archived artifacts/report view containing Surefire and Failsafe
  XML reports, if the Jenkins configuration archives them separately.
- For failure-path evidence, a controlled failing Selenium run showing the
  stage failing while its generated XML is still published.

## Verification Status

Jenkins execution is pending. Confirm the Windows agent can run headless Chrome,
Selenium Manager can resolve its driver, and the deployed application is
reachable at `http://localhost:8001/` from that same agent during the Selenium
stage. Week 10 does not claim an actual Jenkins run.
