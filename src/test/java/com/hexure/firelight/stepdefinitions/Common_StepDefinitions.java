package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.*;
import com.hexure.firelight.libraies.Enums.EnumsJSONProp;
import com.hexure.firelight.pages.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

public class Common_StepDefinitions extends FLUtilities {
    private final WebDriver driver;
    private final TestContext testContext;
    private final CreateApplicationPage onCreateApplicationPage;
    private final DataEntryPage onDataEntryPage;
    private final CommonMethodsPage onCommonMethodsPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    private static final Logger Log = LogManager.getLogger(Common_StepDefinitions.class);

    public Common_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCreateApplicationPage = context.getPageObjectManager().getCreateApplicationPage();
        onDataEntryPage = context.getPageObjectManager().getDataEntryPage();
        onCommonMethodsPage = context.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
    }

    @Given("User is on FireLight login page for TestCase {string}")
    public void userIsOnFLLoginPage(String testCaseID) throws AWTException {
        commonSetup(testCaseID);
    }

    private void commonSetup(String testCaseID) throws AWTException {
        testContext.setTestCaseID(testCaseID.split("_")[1]);
        testContext.setModuleName(testCaseID.split("_")[0]);
        System.out.println("Environment = " + testContext.getEnvironment());
        System.out.println("ApplicationType = " + testContext.getAppType());
        System.out.println("TestCaseID = " + testContext.getTestCaseID());
        System.out.println("ModuleName = " + testContext.getModuleName());
        System.out.println("CaptureScreenshot = " + testContext.getCaptureScreenshot());
        openLoginPage(driver, testContext);
        testContext.setUiType(testContext.getMapTestData().get("uiType"));
        System.out.println("UI TYPE is = " + testContext.getMapTestData().get("uiType"));
//        Robot robot = new Robot();
//        System.out.println("About to zoom in");
//        for (int i = 0; i < 2; i++) {
//            robot.keyPress(KeyEvent.VK_CONTROL);
//            robot.keyPress(KeyEvent.VK_SUBTRACT);
//            robot.keyRelease(KeyEvent.VK_SUBTRACT);
//            robot.keyRelease(KeyEvent.VK_CONTROL);
//        }
        testContext.getScenario().write("<div style='width: 5%; position: absolute; top: 5px; font-size: 2vw; border: none; color: green; text-align: center; font-weight: bold; background-color: #C5D88A; left: 50%; transform: translateX(-50%);'>" + testContext.getTestCaseID() + "</div>");
        Log.info("TEST CASE " + testCaseID + " STARTED");
    }

    @Then("User clicks {string} button")
    public void userClicksButton(String whichButton) {
        waitForPageToLoad(driver);
        captureScreenshot(driver, testContext, false);
        switch (whichButton) {
            case "Create":
                clickElement(driver, onCreateApplicationPage.getBtn_Create());
                break;
            case "Close":
                if(!onCreateApplicationPage.getLstBtnClose().isEmpty())
                    clickElement(driver, onCreateApplicationPage.getBtnClose());
                break;
            default:
                clickElement(driver, onCommonMethodsPage.findElement(driver, String.format(onCommonMethodsPage.fieldWithTitleAttribute, whichButton, whichButton)));
                break;
        }
    }

    @Then("User enters new Application name")
    public void userEntersNewApplicationName() {
        String newAppName = "AT " + testContext.getMapTestData().get("product") + " " + getDate("newAppName");
        Allure.addAttachment("Application name is ", newAppName);
        Log.info("Application name is " + newAppName + "for test case " + testContext.getTestCaseID());
        onCreateApplicationPage.getTxtBox_newAppName().clear();
        onCreateApplicationPage.getTxtBox_newAppName().sendKeys(newAppName);
        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, EnumsJSONProp.NEWPRODUCTNAME.getText(), newAppName);
        captureScreenshot(driver, testContext, false);
    }

    @Then("User verifies Page heading {string} with form name {string} for data entry flow")
    public void verifyPageHeadingWithFormNameForDataEntryFlow(String pageName, String formName) {
        waitForPageToLoad(driver);
        captureScreenshot(driver, testContext, false);
        Assert.assertEquals("Data entry page header name mismatched", pageName, onDataEntryPage.getDataEntryPageHeader().getText());
        Assert.assertEquals("Data entry page header name mismatched", formName, onDataEntryPage.getFormName().getAttribute("innerText"));
        captureScreenshot(driver, testContext, false);
    }

    @Then("User opens {string} Required for Form {string}")
    public void openRequiredForForm(String formName, String formMenu) throws InterruptedException {
        waitForPageToLoad(driver);
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        String script = "return getComputedStyle(arguments[0]).getPropertyValue('color');";
        captureScreenshot(driver, testContext, false);
        onCreateApplicationPage.getWizardPageNameExpand().click();
        List<WebElement> mandetoryFormList = findElements(driver, String.format(onCreateApplicationPage.mandetoryFormList, formMenu));
        for (WebElement element : mandetoryFormList) {
            System.out.println("**********" + element.getAttribute("innerText"));
            String form = element.getAttribute("innerText");
            if (form.equals(formName)) {
                Assert.assertTrue("Page name is not in red color", executor.executeScript(script, element.findElement(By.xpath("//span"))).toString().contains("rgb(255, 0, 0)") || executor.executeScript(script, element.findElement(By.xpath("//span"))).toString().contains("rgb(241, 62, 29)"));
                element.click();
                break;
            }
        }
        waitForPageToLoad(driver);
//        Assert.assertEquals("Data entry page header name mismatched", formMenu, getElement(driver, onCommonMethods_reactPage.getDataEntryPageHeaderReact()).getText());
        Assert.assertEquals("Data entry page header name mismatched", formName, onDataEntryPage.getFormName().getAttribute("innerText"));
    }

    @Then("User opens {string} Optional for Form {string}")
    public void openOptionalForForm(String formName, String formMenu) {
        waitForPageToLoad(driver);
        clickElement(driver, findElement(driver, onCreateApplicationPage.getWizardPageNameExpandmvc()));
        sleepInMilliSeconds(3000);
        List<WebElement> mandetoryFormList = findElements(driver, String.format(onCreateApplicationPage.optionalFormList, formMenu));
        for (WebElement element : mandetoryFormList) {
            String form = element.getAttribute("innerText");
            if (form.equals(formName)) {
                element.click();
                break;
//                }
            }
        }
    }

    @Then("User clicks red bubble icon")
    public void userClicksRedBubbleIcon() {
        waitForPageToLoad(driver);
        captureScreenshot(driver, testContext, false);
        onCommonMethodsPage.getRedColorErrorValidationBubble().click();
    }

    @Then("User clicks on Create button on Rename window")
    public void userClicksOnCreateButtonOnRenameWindow() {
        captureScreenshot(driver, testContext, false);
        onCreateApplicationPage.getBtn_CreateActivity().click();
    }

}

