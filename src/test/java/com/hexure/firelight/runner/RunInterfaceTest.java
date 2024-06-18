package com.hexure.firelight.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"classpath:"},
        tags = "@Test",
        glue = {"com.hexure.firelight.stepdefinitions"},
        monochrome = true,
        publish = true
)
public class RunInterfaceTest {

    public static void main(String[] args) {
        // Add the UniqueTestCounter listener to your test run
        JUnitCore core = new JUnitCore();
        core.addListener(new UniqueTestCounter());

        // Run your tests
        core.run(RunInterfaceTest.class);
    }
}