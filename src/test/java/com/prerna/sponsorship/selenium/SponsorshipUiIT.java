package com.prerna.sponsorship.selenium;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SponsorshipUiIT {

    private static final String BASE_URL = System.getProperty("ui.base-url", "http://localhost:8001");
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

    private WebDriver driver;
    private WebDriverWait wait;
    private String testChildId;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1440,1000");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
    }

    @AfterEach
    void cleanUpTestRecordAndCloseBrowser() {
        try {
            if (driver != null && testChildId != null) {
                String searchUrl = BASE_URL + "/sponsorships?search="
                        + URLEncoder.encode(testChildId, StandardCharsets.UTF_8);
                driver.get(searchUrl);
                List<WebElement> rows = driver.findElements(testRecordRow());
                if (!rows.isEmpty()) {
                    rows.get(0).findElement(By.cssSelector("a.delete")).click();
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(testRecordRow()));
                }
            }
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @Test
    void landingPageLoadsWithExpectedTitleAndMainContent() {
        driver.get(BASE_URL + "/");

        assertEquals("Child Education Sponsorship System", driver.getTitle());
        assertEquals("Child Education Sponsorship System", headingText());
        assertTrue(driver.findElement(By.cssSelector("a[href='/sponsorships']")).isDisplayed());
    }

    @Test
    void sponsorshipListAndDashboardNavigationWorks() {
        driver.get(BASE_URL + "/");
        driver.findElement(By.cssSelector("a[href='/sponsorships']")).click();
        assertEquals("Sponsorship Records", headingText());

        driver.findElement(By.cssSelector("a[href='/sponsorships/dashboard']")).click();
        assertEquals("Sponsorship Dashboard", headingText());
        assertTrue(driver.getCurrentUrl().endsWith("/sponsorships/dashboard"));

        driver.findElement(By.cssSelector("a[href='/sponsorships']")).click();
        assertEquals("Sponsorship Records", headingText());
    }

    @Test
    void createSponsorshipThroughForm() {
        createTestRecord();

        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(testRecordRow()));
        assertTrue(row.getText().contains(testChildId));
        assertTrue(row.getText().contains("Automated Child"));
    }

    @Test
    void searchFindsAutomationRecord() {
        createTestRecord();

        WebElement search = driver.findElement(By.cssSelector("form[action='/sponsorships'] input[name='search']"));
        search.clear();
        search.sendKeys(testChildId);
        driver.findElement(By.cssSelector("form[action='/sponsorships'] button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("search="));
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(testRecordRow()));
        assertTrue(row.getText().contains("Automated Child"));
    }

    @Test
    void statusWorkflowUpdatesAutomationRecord() {
        createTestRecord();

        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(testRecordRow()));
        new Select(row.findElement(By.cssSelector("select[name='status']"))).selectByValue("ACTIVE");
        row.findElement(By.cssSelector("button[type='submit']")).click();

        By updatedStatus = testRecordStatusCell();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(updatedStatus, "ACTIVE"));
        assertEquals("ACTIVE", wait.until(ExpectedConditions.visibilityOfElementLocated(updatedStatus)).getText());
    }

    private void createTestRecord() {
        testChildId = "AUTO-SEL-" + UUID.randomUUID();
        driver.get(BASE_URL + "/");
        driver.findElement(By.cssSelector("a[href='/sponsorships']")).click();
        driver.findElement(By.cssSelector("a.button[href='/sponsorships/new']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("childId")));

        driver.findElement(By.id("childId")).sendKeys(testChildId);
        driver.findElement(By.id("childName")).sendKeys("Automated Child");
        WebElement age = driver.findElement(By.id("age"));
        age.clear();
        age.sendKeys("12");
        new Select(driver.findElement(By.id("gender"))).selectByValue("Other");
        driver.findElement(By.id("educationLevel")).sendKeys("Grade 7");
        driver.findElement(By.id("school")).sendKeys("Automated UI School");
        driver.findElement(By.id("sponsorName")).sendKeys("Automated UI Sponsor");
        WebElement amount = driver.findElement(By.id("sponsorshipAmount"));
        amount.clear();
        amount.sendKeys("125.50");
        WebElement startDate = driver.findElement(By.id("startDate"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                startDate, LocalDate.now().toString());
        new Select(driver.findElement(By.id("status"))).selectByValue("PENDING");
        String submittedFormValues = driver.findElements(By.cssSelector(
                "form[action='/sponsorships/save'] input, form[action='/sponsorships/save'] select"))
                .stream()
                .map(element -> element.getAttribute("name") + "="
                        + (element.getTagName().equals("select")
                                ? new Select(element).getFirstSelectedOption().getAttribute("value")
                                : element.getAttribute("value")))
                .reduce((first, next) -> first + ", " + next)
                .orElse("<no controls>");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        boolean completed = false;
        try {
            completed = wait.until(webDriver -> webDriver.getCurrentUrl().equals(BASE_URL + "/sponsorships")
                    || hasVisibleValidationError(webDriver));
        } catch (TimeoutException ignored) {
            // Include the current browser response below when submission does not complete.
        }

        if (!completed || !driver.getCurrentUrl().equals(BASE_URL + "/sponsorships")) {
            throw new AssertionError("Sponsorship submission did not redirect. Current URL: "
                    + driver.getCurrentUrl() + "; title: " + driver.getTitle()
                    + "; visible body text: " + driver.findElement(By.tagName("body")).getText()
                    + "; submitted controls: " + submittedFormValues
                    + "; page source: " + driver.getPageSource());
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(testRecordRow()));
    }

    private boolean hasVisibleValidationError(WebDriver webDriver) {
        List<WebElement> errors = webDriver.findElements(By.cssSelector(
                "[role='alert'], .error, .alert, .invalid-feedback, form :invalid"));
        return errors.stream().anyMatch(WebElement::isDisplayed);
    }

    private String headingText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1"))).getText();
    }

    private By testRecordRow() {
        return By.xpath("//tbody/tr[td[1][normalize-space()='" + testChildId + "']]");
    }

    private By testRecordStatusCell() {
        return By.xpath("//tbody/tr[td[1][normalize-space()='" + testChildId + "']]/td[8]");
    }
}
