package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.LoginPage;
import io.cucumber.java.en.Then;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;

public class Login_StepDefinitions extends FLUtilities {
    private final WebDriver driver;
    private final LoginPage onLoginPage;
    private final TestContext testContext;

    public Login_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onLoginPage = context.getPageObjectManager().getLoginPage();
    }

    @Then("User on Login Page enters valid username as {string} and password and clicks Login button")
    public void userOnLoginPageEntersValidUsernameAsAndPasswordAndClicksLoginButton(String userName) {
        testContext.setCurrentTestUserName(userName);
        System.out.println("Current Test UserName = " + testContext.getCurrentTestUserName());
        onLoginPage.getTxtboxUserName().sendKeys(configProperties.getProperty(testContext.getCurrentTestUserName()));
        onLoginPage.getTxtboxPassword().sendKeys(configProperties.getProperty(testContext.getCurrentTestUserName() + "-password"));
        captureScreenshot(driver, testContext, false);
        clickElement(driver, onLoginPage.getBtnSignIn());
    }

    @Then("User verifies the {string} page should be open")
    public void user_verifies_the_page_should_be_open(String pageName) {
        captureScreenshot(driver, testContext, false);
        Assert.assertEquals(pageName + " page was mismatched", pageName, driver.getTitle().trim());
    }
}
