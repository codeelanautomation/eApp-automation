package com.hexure.firelight.runner;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;


@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"classpath:"},
        tags = {"@Test"},
        glue = {"com.hexure.firelight.stepdefinitions"},
        monochrome = true
)
public class RunIntegrationTest {

        public static void main(String[] args) {
                // Add the UniqueTestCounter listener to your test run
                org.junit.runner.JUnitCore core = new org.junit.runner.JUnitCore();
                core.addListener(new UniqueTestCounter());

                // Run your tests
                core.run(RunIntegrationTest.class);
        }


}

