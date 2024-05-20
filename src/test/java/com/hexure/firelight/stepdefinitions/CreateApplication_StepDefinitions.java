package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.Enums.EnumsJSONProp;
import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.CreateApplicationPage;
import io.cucumber.java.en.Then;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

public class CreateApplication_StepDefinitions extends FLUtilities {
    private final WebDriver driver;
    private final CreateApplicationPage onCreateApplicationPage;
    private final TestContext testContext;

    public CreateApplication_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCreateApplicationPage = context.getPageObjectManager().getCreateApplicationPage();
    }

    @Then("User selects Jurisdiction {string}")
    public void userSelectsJurisdiction(String jurisdiction) {
        captureScreenshot(driver, testContext, false);
        new Select(onCreateApplicationPage.getDd_Jurisdiction()).selectByVisibleText(jurisdiction);
        //TODO: remove below line if not required
        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, EnumsJSONProp.JURISDICTION.getText(), jurisdiction);
    }

    @Then("User selects {string} from JSON")
    public void userSelectsJurisdictionJSON(String option) {
        userSelectsJurisdiction(testContext.getMapTestData().get(option));
    }

    @Then("User selects Product Type Dropdown {string}")
    public void userSelectsProductTypeDropdown(String productType) {
        waitForPageToLoad(driver);
        sleepInMilliSeconds(2000);
        waitUntilDropDownListPopulated(driver, new Select(onCreateApplicationPage.getDd_ProductType()));
        captureScreenshot(driver, testContext, false);
        new Select(onCreateApplicationPage.getDd_ProductType()).selectByVisibleText(productType);
    }

    @Then("User selects {string} Dropdown from JSON")
    public void userSelectsProductTypeDropdownJSON(String productType) {
        userSelectsProductTypeDropdown(testContext.getMapTestData().get(productType));
    }

    @Then("User opens Given Product {string} for application")
    public void UserOpensGivenProductForApp(String product) {
        captureScreenshot(driver, testContext, false);
        sleepInMilliSeconds(2000);
        syncElement(driver, findElement(driver, String.format(onCreateApplicationPage.list_OfProducts, product)), EnumsCommon.TOCLICKABLE.getText());
        try {
            clickElement(driver, findElement(driver, String.format(onCreateApplicationPage.list_OfProducts, product)));
        } catch (StaleElementReferenceException e) {
            clickElement(driver, findElement(driver, String.format(onCreateApplicationPage.list_OfProducts, product)));
        }
        addPropertyValueInJSON(testContext.getTestCaseID(), testContext, EnumsJSONProp.PRODUCT.getText(), product);
    }


    @Then("User opens Given Product from JSON for application")
    public void UserOpensGivenProductForAppJSON() {
        UserOpensGivenProductForApp(testContext.getMapTestData().get("ProductType"));
    }

}
