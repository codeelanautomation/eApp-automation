package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.Enums.EnumsJSONProp;
import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class XMLData_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final CreateApplicationPage onCreateApplicationPage;
    private final XmlDataPage onXMLDataPage;
    private final ReviewPage onReviewPage;
    private final LoginPage onLoginPage;


    public XMLData_StepDefinitions(TestContext context) {
        onCommonMethodsPage = context.getPageObjectManager().getCommonMethodPage();
        onCreateApplicationPage = context.getPageObjectManager().getCreateApplicationPage();
        onReviewPage = context.getPageObjectManager().getReviewPage();
        onXMLDataPage = context.getPageObjectManager().getXmlDataPage();
        onLoginPage = context.getPageObjectManager().getLoginPage();
        testContext = context;
        driver = context.getDriver();

    }

    @Then("User completes the application")
    public void completeApp() throws IOException {
        waitForPageToLoad(driver);
        clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
        onXMLDataPage.setPageObjects(testContext, driver);
        onXMLDataPage.completeDataEntry();

        if (!onCommonMethodsPage.getLstBtnClose().isEmpty())
            clickElement(driver, onCommonMethodsPage.getBtnClose());

        onXMLDataPage.completeReviewSignature();
        clickElement(driver, onCreateApplicationPage.getBtnCloseApp());
    }

    @Then("User Extracts the URL Link for {string} and Stores it")
    public void user_Extracts_the_URL_Link_and_Stores_it(String clientName) {
        waitForPageToLoad(driver);
        captureScreenshot(driver, testContext, false);
        String appURL = driver.getCurrentUrl();
        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, EnumsJSONProp.APPURL.getText(), appURL);
        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, EnumsJSONProp.APPGUID.getText(), appURL.substring(appURL.indexOf("AppGuid=") + 8, appURL.indexOf("&")));
        System.out.println("Url Generated is :" + appURL);
    }

}