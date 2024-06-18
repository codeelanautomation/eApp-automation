package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.CommonMethodsPage;
import io.cucumber.java.en.Then;
import org.openqa.selenium.WebDriver;

public class Home_StepDefinitions extends FLUtilities {
    private final WebDriver driver;
    private final TestContext testContext;
    private final CommonMethodsPage onCommonMethodsPage;

    public Home_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCommonMethodsPage = context.getPageObjectManager().getCommonMethodPage();
    }

    @Then("User clicks {string} Tab")
    public void userClicksTab(String activityType) {
        captureScreenshot(driver, testContext, false);
        clickElement(driver, "//td[@class='top']//span[text()='" + activityType + "']");
    }

    @Then("User open application from recent activity")
    public void userOpenApplication() {
        captureScreenshot(driver, testContext, false);
        clickElement(driver, onCommonMethodsPage.getListRecentApplication());
        String url = driver.getCurrentUrl().substring(0,driver.getCurrentUrl().lastIndexOf("AppGuid=") + 8);
        driver.get(url + testContext.getMapTestData().get("appGuid"));
    }
}
