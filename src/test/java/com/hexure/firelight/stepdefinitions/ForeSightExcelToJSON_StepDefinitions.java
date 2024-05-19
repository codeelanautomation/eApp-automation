package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.E2EFlowDataPage;
import com.hexure.firelight.pages.ExcelHandlerPage;
import cucumber.api.java.en.Given;


public class ForeSightExcelToJSON_StepDefinitions {

    private final ExcelHandlerPage onExcelHandlerPage;
    private final E2EFlowDataPage onE2EFlowDataPage;
    private final TestContext testContext;

    public ForeSightExcelToJSON_StepDefinitions(TestContext context) {
        testContext = context;
        onExcelHandlerPage = testContext.getPageObjectManager().getExcelHandlerPage();
        onE2EFlowDataPage = testContext.getPageObjectManager().getE2EFlowDataPage();
    }

    /**
     * Create input JSON, runner and feature files from given spec and created flow interface
     *
     * @param jsonFile
     * @param excelFile
     */
    @Given("Create {string} file for eApp flow with interface file {string}")
    public void createForesightTestDataInterface(String jsonFile, String excelFile) {
        onE2EFlowDataPage.createForesightTestDataInterface(jsonFile, excelFile);
    }
}