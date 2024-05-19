package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import cucumber.api.java.en.Then;
import org.openqa.selenium.WebDriver;

public class Home_StepDefinitions extends FLUtilities {
    private final WebDriver driver;
    private final TestContext testContext;
    private WebDriver driverEdge = null;

    public Home_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
    }

    @Then("User clicks {string} Tab")
    public void userClicksTab(String activityType) {
        captureScreenshot(driver, testContext, false);
        clickElement(driver, "//td[@class='top']//span[text()='" + activityType + "']");
    }
}
