package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.E2EFlowDataPage;
import io.cucumber.java.en.Given;

public class ForeSightE2EToJSON_StepDefinitions {

    private final E2EFlowDataPage onE2EFlowDataPage;

    public ForeSightE2EToJSON_StepDefinitions(TestContext context) {
        onE2EFlowDataPage = context.getPageObjectManager().getE2EFlowDataPage();
    }

    @Given("Create {string} file for client {string} for eApp E2E flow")
    public void createForesightTestData(String jsonFile, String clientName) {
        onE2EFlowDataPage.createForesightE2ETestData(jsonFile, clientName);
    }
}