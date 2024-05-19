package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.*;
import com.jayway.jsonpath.JsonPath;
import cucumber.api.java.en.Given;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import java.util.*;

public class Rules_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CreateApplicationPage onCreateApplicationPage;
    public WizardFlowDataPage onWizardFlowDataPage;
    String jurisdictionStatesCode = "";
    List<String> skippedInvalidElements = new ArrayList<>();
    int fieldsEvaluated = 0;
    String executedJurisdiction = "";

    public Rules_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCreateApplicationPage = testContext.getPageObjectManager().getCreateApplicationPage();
        onWizardFlowDataPage = testContext.getPageObjectManager().getWizardFlowDataPage();
    }

    /**
     * This method set parameters to create application based on available Jurisdictions per module
     *
     * @param product, productType, module, jurisdiction1
     */
    @Given("User clicks application for Product {string} and Product Type {string} and validate wizard fields for module {string} and jurisdiction {string}")
    public void createAppAndValidateWizard(String product, String productType, String module, String jurisdiction1) {
        captureScreenshot(driver, testContext, false);
        String[] States = testContext.getMapTestData().get("JurisdictionRules").split(",");

        String moduleJurisdictionMapping;
        if (module.equalsIgnoreCase("All") || jurisdiction1.equalsIgnoreCase("All")) {
            for (String jurisdiction : States) {
                skippedInvalidElements.clear();
                moduleJurisdictionMapping = testContext.getMapTestData().get(jurisdiction).trim();
                String[] moduleValues = JsonPath.read(moduleJurisdictionMapping, "$.Module").toString().trim().split(", ");
                jurisdictionStatesCode = JsonPath.read(moduleJurisdictionMapping, "$.State").toString().trim();
                for (String moduleValue : moduleValues) {
                    if (moduleValue.equalsIgnoreCase(module) | moduleValue.equalsIgnoreCase("All"))
                        createApplication(jurisdiction, product, productType, module);
                }
            }
        } else {
            moduleJurisdictionMapping = testContext.getMapTestData().get(jurisdiction1).trim();
            jurisdictionStatesCode = JsonPath.read(moduleJurisdictionMapping, "$.State").toString().trim();
            createApplication(jurisdiction1, product, productType, module);
        }
        onWizardFlowDataPage.printFinalResults(fieldsEvaluated);
    }

    /**
     * This method create Firelight application
     *
     * @param jurisdiction, product, productType, module
     */
    public void createApplication(String jurisdiction, String product, String productType, String module) {
        executedJurisdiction = jurisdiction;
        onCreateApplicationPage.createApplication(testContext, driver, product, productType, jurisdiction);
        validateWizard(module);
    }

    public void validateWizard(String module) {
        if (!onCreateApplicationPage.getLstBtnClose().isEmpty())
            clickElement(driver, onCreateApplicationPage.getBtnClose());
        verifyFormDataWithInboundXml(module);
        waitForPageToLoad(driver);
        clickElement(driver, onCreateApplicationPage.getBtnHome());
        clickElement(driver, onCreateApplicationPage.getBtnPopupOK());
        waitForPageToLoad(driver);
    }

    /**
     * This method will iterate over each field of a module and start validating it.
     *
     * @param module
     */
    public void verifyFormDataWithInboundXml(String module) {
        String moduleNameValue;
        Set<String> fieldList = new LinkedHashSet<>(Arrays.asList(testContext.getMapTestData().get("fieldList").split(", ")));
        onWizardFlowDataPage.setPageObjects(testContext, driver);
        for (String fieldName : fieldList) {
            moduleNameValue = JsonPath.read(testContext.getMapTestData().get(fieldName).trim(), "$.ModuleSectionName").toString().trim();
            if (module.equalsIgnoreCase(moduleNameValue) | module.equalsIgnoreCase("All")) {
                onWizardFlowDataPage.wizardTesting(fieldName);
                fieldsEvaluated++;
            }
        }
    }

}




