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
        long durationMillis = endTime - startTime;
        long hours = durationMillis / (1000 * 60 * 60);
        long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = ((durationMillis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        String difference = String.format("%dh %dm %ds", hours, minutes, seconds);
        testContext.getScenario().write("<div width='100%' style='font-size:2vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'> Duration : " + difference + "</div>");
    }
}