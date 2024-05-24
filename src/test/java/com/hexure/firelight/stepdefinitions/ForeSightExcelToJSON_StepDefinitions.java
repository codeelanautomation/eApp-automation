package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.E2EFlowDataPage;
import io.cucumber.java.en.Given;


public class ForeSightExcelToJSON_StepDefinitions {

    private final E2EFlowDataPage onE2EFlowDataPage;

    public ForeSightExcelToJSON_StepDefinitions(TestContext context) {
        onE2EFlowDataPage = context.getPageObjectManager().getE2EFlowDataPage();
    }

    /**
     * Create input JSON, runner and feature files from given spec and created flow interface
     *
     * @param jsonFile  - JSON File to be created
     * @param excelFile - Spec file provided
     */
    @Given("Create {string} file for eApp flow with interface file {string}")
    public void createForesightTestDataInterface(String jsonFile, String excelFile) {
        onE2EFlowDataPage.createForesightTestDataInterface(jsonFile, excelFile);
    }
}