package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.PageObjectManager;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.After;
import lombok.Data;

@Data
public class Hooks extends FLUtilities
{
    private TestContext testContext;

    public Hooks(TestContext context)
    {
        testContext = context;
    }

    @Before
    public void setUp(Scenario scenario)
    {
        loadConfigData(testContext);

        if (testContext.getDriver() == null)
        {
            testContext.setDriver(getWebDriver(testContext));
        }
        testContext.setPageObjectManager(new PageObjectManager(testContext.getDriver()));
        testContext.setScenario(scenario);
    }
    @After
    public void cleanUp()
    {
        closeBrowser(testContext);
    }

}
