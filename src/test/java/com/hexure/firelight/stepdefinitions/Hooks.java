package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.PageObjectManager;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.After;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class Hooks extends FLUtilities {
    private TestContext testContext;

    public Hooks(TestContext context) {
        testContext = context;
    }

    private long startTime;
    private long endTime;

    @Before
    public void setUp(Scenario scenario) {
        loadConfigData(testContext);

        if (testContext.getDriver() == null) {
            testContext.setDriver(getWebDriver(testContext));
        }
        testContext.setPageObjectManager(new PageObjectManager(testContext.getDriver()));
        testContext.setScenario(scenario);
        startTime = System.currentTimeMillis();
    }

    @After
    public void cleanUp() {
        closeBrowser(testContext);
        endTime = System.currentTimeMillis();
        String difference = String.format("%dh %dm %ds", TimeUnit.MILLISECONDS.toHours(endTime - startTime), TimeUnit.MILLISECONDS.toMinutes(endTime - startTime), TimeUnit.MILLISECONDS.toSeconds(endTime - startTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime - startTime)));
        testContext.getScenario().write("<div width='100%' style='font-size:2vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'> Duration : " + difference + "</div>");
    }

}
