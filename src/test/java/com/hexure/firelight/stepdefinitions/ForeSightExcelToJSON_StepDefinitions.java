package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.E2EFlowDataPage;
import com.hexure.firelight.pages.XmlUtilityPage;
import io.cucumber.java.en.Given;


public class ForeSightExcelToJSON_StepDefinitions {

    private final E2EFlowDataPage onE2EFlowDataPage;
    private final XmlUtilityPage onXMLUtilityPage;
    private final TestContext testContext;

    public ForeSightExcelToJSON_StepDefinitions(TestContext context) {
        onE2EFlowDataPage = context.getPageObjectManager().getE2EFlowDataPage();
        onXMLUtilityPage = context.getPageObjectManager().getXmlUtilityPage();
        this.testContext = context;
    }

    /**
     * Create input JSON, runner and feature files from given spec and created flow interface
     *
     * @param jsonFile  - JSON File to be created
     * @param excelFile - Spec file provided
     */
    @Given("Create {string} file for eApp flow with interface file {string} and sheet {string}")
    public void createForesightTestDataInterface(String jsonFile, String excelFile, String sheetName) {
        onE2EFlowDataPage.createForesightTestDataInterface(jsonFile, excelFile, onXMLUtilityPage, sheetName);
    }


    /**
     * Create input JSON, runner and feature files from given spec and created flow interface
     *
     * @param excelFile - Spec file provided
     */
    @Given("Update json with updated values from {string}")
    public void updateJSON(String excelFile) {
        onE2EFlowDataPage.updateJSON(excelFile, testContext);
    }
}