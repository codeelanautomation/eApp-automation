package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.E2EFlowDataPage;
import com.hexure.firelight.pages.ExcelHandlerPage;
import cucumber.api.java.en.Given;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForeSightE2EToJSON_StepDefinitions {

    private final ExcelHandlerPage onExcelHandlerPage;
    private final E2EFlowDataPage onE2EFlowDataPage;
    private final TestContext testContext;
    private static final Logger Log = LogManager.getLogger(ForeSightE2EToJSON_StepDefinitions.class);

    public ForeSightE2EToJSON_StepDefinitions(TestContext context) {
        testContext = context;
        onExcelHandlerPage = testContext.getPageObjectManager().getExcelHandlerPage();
        onE2EFlowDataPage = testContext.getPageObjectManager().getE2EFlowDataPage();
    }

    @Given("Create {string} file for client {string} for eApp E2E flow")
    public void createForesightTestData(String jsonFile, String clientName) {
        onE2EFlowDataPage.createForesightE2ETestData(jsonFile, clientName);
    }
}