package com.hexure.firelight.runner;

import com.hexure.firelight.runner.UniqueTestCounter;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "json:target/cucumber-html-report",
                "json:target/cucumber-reports/cucumber.xml",
                "html:target/cucumber-reports/cucumber.html",
                "rerun:target/failedrun.txt",
                "json:target/cucumber-reports/cucumber.json",
                "pretty",
                "io.qameta.allure.cucumber4jvm.AllureCucumber4Jvm"
        },
        features = {"classpath:"},
        tags = {"@End2EndFlow"},
        glue = {"com.hexure.firelight.stepdefinitions"},
        dryRun = false
)
public class RunFireLightTest {

    public static void main(String[] args) {
        // Add the UniqueTestCounter listener to your test run
        org.junit.runner.JUnitCore core = new org.junit.runner.JUnitCore();
        core.addListener(new UniqueTestCounter());

        // Run your tests
        core.run(RunFireLightTest.class);
    }


}

