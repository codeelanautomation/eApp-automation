package com.hexure.firelight.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.AfterClass;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "html:target/cucumberClientModuleTagState.html",
                "json:target/cucumber-reports/cucumberClientModuleTagState.json",
                "html:target/cucumber-reports/cucumberClientModuleTagState",
                "rerun:target/failedrun.txt",
                "json:target/cucumber-reports/cucumberClientModuleTagState.json",
                "pretty",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        features = {"classpath:"},
        tags = "",
        glue = {"com.hexure.firelight.stepdefinitions"},
        monochrome = true,
        publish = true
)
public class RunFireLightTest {

    public static void main(String[] args) {
        // Add the UniqueTestCounter listener to your test run
        JUnitCore core = new JUnitCore();
        core.addListener(new UniqueTestCounter());

        // Run your tests
        core.run(RunFireLightTest.class);
    }

    @AfterClass
    public static void cssStyleUpdate() {
    }
}