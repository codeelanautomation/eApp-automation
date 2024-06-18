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
    private final ImportXMLDataPage onImportXMLDataPage;
    private final ReviewPage onReviewPage;
    private final LoginPage onLoginPage;


    public XMLData_StepDefinitions(TestContext context) {
        onCommonMethodsPage = context.getPageObjectManager().getCommonMethodPage();
        onCreateApplicationPage = context.getPageObjectManager().getCreateApplicationPage();
        onReviewPage = context.getPageObjectManager().getReviewPage();
        onImportXMLDataPage = context.getPageObjectManager().getImportXMLDataPage();
        onLoginPage = context.getPageObjectManager().getLoginPage();
        testContext = context;
        driver = context.getDriver();
    }

    @Then("User completes the application")
    public void completeApp() throws IOException {
        waitForPageToLoad(driver);
        clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
        completeDataEntry();

        if (!onCommonMethodsPage.getLstBtnClose().isEmpty())
            clickElement(driver, onCommonMethodsPage.getBtnClose());

        completeReviewSignature();
        clickElement(driver, onCreateApplicationPage.getBtnCloseApp());
    }

    public void completeDataEntry() {
        while (!(onCommonMethodsPage.getLblPercent().getText().contains("100%") || getElements(driver, onCommonMethodsPage.getFormPages()).isEmpty())) {
            clickElement(driver, getElements(driver, onCommonMethodsPage.getFormPages()).get(0));
            if (!onCommonMethodsPage.getLstRedColorErrorValidationBubble().isEmpty())
                clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            while (!getElements(driver, onCommonMethodsPage.getErrorMessageElement()).isEmpty()) {
                WebElement elem = getRequiredElement();
                interactWithElement(elem);
            }
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
        }
    }

    public void completeReviewSignature() {
        while (onCreateApplicationPage.getLstConfirmationDialog().isEmpty()) {
            clickElementByJSE(driver, onCommonMethodsPage.getBtnContinue());
            waitForPageToLoad(driver);
            if(!findElementsByID(driver, onCreateApplicationPage.getBtnRequestReview()).isEmpty())
                clickElement(driver, findElementByID(driver, onCreateApplicationPage.getBtnRequestReview()));
            WebElement canvasElement = findElement(driver, onCreateApplicationPage.getCanvasCurrent());
            List<WebElement> lstCanvasElement = findElements(driver, onCreateApplicationPage.getCanvasCurrent());

            if (!lstCanvasElement.isEmpty()) {
                switch (canvasElement.getAttribute("id").toLowerCase()) {
                    case "signatures":
                        acceptESignature();
                        break;
                    case "review":
                        acceptReview();
                        break;
                }
            }
        }
    }

    public WebElement getRequiredElement() {
        if (!driver.findElements(onCommonMethodsPage.getInputElement()).isEmpty())
            return driver.findElement(onCommonMethodsPage.getInputElement());
        else if (!driver.findElements(onCommonMethodsPage.getSelectElement()).isEmpty())
            return driver.findElement(onCommonMethodsPage.getSelectElement());
        if (!driver.findElements(onCommonMethodsPage.getCheckboxElement()).isEmpty())
            return driver.findElement(onCommonMethodsPage.getCheckboxElement());
        return null;
    }

    public void acceptESignature() {
        clickElement(driver, onCreateApplicationPage.getLnkUseSignature());
        waitForPageToLoad(driver);
        int countUsers = onCreateApplicationPage.getLnkUserSignatures().size();
        int countUser = 0;
        while (countUser != countUsers) {
            WebElement elem = onCreateApplicationPage.getLnkUserSignatures().get(0);
            String user = elem.getAttribute("id");
            clickElement(driver, elem);
            if (!user.equalsIgnoreCase("agent")) {
                clickElement(driver, findElementByID(driver, onCreateApplicationPage.getSignNow()));
                sendKeys(driver, findElementByID(driver, onCreateApplicationPage.getIdentificationNumber()), "12345");
                clickElement(driver, findElementByID(driver, onCreateApplicationPage.getBtnSubmit()));
            }
            if (!findElementsByID(driver, onCreateApplicationPage.getChkAgreeAll()).isEmpty()) {
                clickElement(driver, findElementByID(driver, onCreateApplicationPage.getChkAgreeAll()));
                clickElement(driver, findElementByID(driver, onCreateApplicationPage.getPopOK()));
            } else {
                int countDocument = 0;
                int countDocuments = findElements(driver, onCreateApplicationPage.getTblDocuments()).size();
                while (countDocument != countDocuments) {
                    clickElement(driver, findElementByID(driver, onCreateApplicationPage.getChkAgree()));
                    sleepInMilliSeconds(3000);
                    countDocument++;
                }
            }
            userEnterDetailsCaptureSignature(user);
            userPerformSignature();
            clickElement(driver, onCreateApplicationPage.getBtnConsent());
            waitForPageToLoad(driver);
            countUser++;
        }
        sleepInMilliSeconds(1000);
    }

    public void acceptReview() {
        clickElement(driver, onReviewPage.getBtn_SendRequestToReviewer());
        clickElement(driver,onReviewPage.getLink_AddReviewer());
        clickElement(driver,onReviewPage.getCheckBox_Reviewer());
        sendKeys(driver, onReviewPage.getTxtBox_ReviewerName(), testContext.getMapTestData().get("producerName"));
        sendKeys(driver, onReviewPage.getTxtBox_ReviewerEmail(), "testHexure@gmail.com");
        clickElement(driver, onReviewPage.getBtn_sendEmailRequest());
        clickElement(driver, onReviewPage.getBtn_backToapp());
        syncElement(driver, onReviewPage.getPasscode_recipient(), EnumsCommon.TOVISIBLE.getText());
        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, EnumsJSONProp.REVIEWERPASSCODE.getText(), onReviewPage.getPasscode_recipient().getText());
        clickElement(driver,findElementByID(driver, onReviewPage.getLink_requestReviewReact()));
        String s = onReviewPage.getEmailMsg().getText().trim();
        addPropertyValueInJSON(testContext.getTestCaseID(),testContext, EnumsJSONProp.REVIEWERURL.getText(),s.substring(s.indexOf(", click on ") + 11, s.indexOf(", and")));
        clickElement(driver,findElementByID(driver, onReviewPage.getToolBar_Home()));
        clickElement(driver, findElementByID(driver, onCommonMethodsPage.getBtnLogOff()));
        driver.manage().deleteAllCookies();
        driver.get(testContext.getMapTestData().get("reviewerUrl"));
        sendKeys(driver, onReviewPage.getTxtBox_Passcode(), testContext.getMapTestData().get("reviewerPasscode"));
        clickElement(driver, onReviewPage.getBtn_enterPasscode());
        clickElement(driver, onReviewPage.getBtn_Review());
        clickElement(driver, onCommonMethodsPage.getPopup_Yes());
        driver.navigate().refresh();
        clickElement(driver, onReviewPage.getBtn_Approve());
        clickElement(driver, onCommonMethodsPage.getPopup_Yes());
        waitForPageToLoad(driver);
        sleepInMilliSeconds(3000);
        driver.manage().deleteAllCookies();
        openLoginPage(driver, testContext, "firelight");
        onLoginPage.getTxtboxUserName().sendKeys(testContext.getMapTestData().get("username"));
        onLoginPage.getTxtboxPassword().sendKeys(testContext.getMapTestData().get("password"));
        clickElement(driver, onLoginPage.getBtnSignIn());
        clickElement(driver, onCommonMethodsPage.getListRecentApplication());

//        onImportXMLDataPage.setCredentialsAndXmlData(driver);
//        clickElement(driver, onCreateApplicationPage.getBtnNext());
        driver.get(testContext.getMapTestData().get("appUrl"));
        waitForPageToLoad(driver);
    }

    public String getDataItemID(WebElement elem, String tagName) {
        if (tagName.equalsIgnoreCase("div"))
            return driver.findElement(onCommonMethodsPage.getComponentClass()).getAttribute("data-dataitemid");
        return elem.getAttribute("data-dataitemid");
    }

    public void interactWithElement(WebElement elem) {
        String tagName = elem.getTagName();
        String dataItemId = getDataItemID(elem, tagName);
        List<String> potentialValues = Arrays.asList("Test", "12242018", "1234567890", "12.11", "Test1234", "6.7", "100");

        if (tagName.equalsIgnoreCase("input")) {
            for (String value : potentialValues) {
                boolean temp = tryEnteringValue(elem, tagName, value);
                if (temp) {
                    temp = isErrorMessageDisplayed(elem, dataItemId);
                    if (!temp) {
                        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, dataItemId, value);
                        System.out.println("Successfully entered value: " + value);
                        return;
                    }
                }
            }
        } else {
            boolean temp = tryEnteringValue(elem, tagName, "");
            if (temp) {
                System.out.println("Successfully selected first option from dropdown or checked the checkbox");
                return;
            } else
                System.out.println("Could not selected first option from dropdown or checked the checkbox");
        }
    }

    private boolean tryEnteringValue(WebElement element, String tagName, String value) {
        if (!setValue(tagName, value, element)) {
            clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            if (!setValue(tagName, value, element))
                return false;
            clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
        }
        return true;
    }

    private boolean setValue(String tagName, String value, WebElement element) {
        try {
            switch (tagName.toLowerCase()) {
                case "input":
                    sendKeys(driver, element, value);
                    break;
                case "select":
                    Select dropdown = new Select(element);
                    dropdown.selectByIndex(1);
                    break;
                case "div":
                    // Assuming the div represents a custom checkbox, click to toggle selection
                    if (!element.isSelected())
                        clickElement(driver, element);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown element type: " + tagName);
            }
            sleepInMilliSeconds(2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void userEnterDetailsCaptureSignature(String user) {
        captureScreenshot(driver, testContext, false);
        sendKeys(driver, driver.findElement(By.id("txtCity")), "Alabama");
        if (!user.equalsIgnoreCase("agent"))
            sendKeys(driver, driver.findElement(By.id("txtFullName")), "test");
    }

    public void userPerformSignature() {
        captureScreenshot(driver, testContext, false);
        addDigitalSignature(driver, driver.findElement(By.id("sigPad")));
    }


    private boolean isErrorMessageDisplayed(WebElement element, String dataItemId) {
        try {
            WebElement element1 = getRequiredElement();

            String tagName = element1.getTagName();
            String dataItemIDNew = getDataItemID(element1, tagName);
            return dataItemIDNew.equalsIgnoreCase(dataItemId);
        } catch (Exception e) {
            return false;
        }
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