package com.hexure.firelight.runner;

import com.hexure.firelight.runner.UniqueTestCounter;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

import java.io.*;

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
        tags = {"@E2EWizardTestFlow"},
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

    @AfterClass
    public static void cssStyleUpdate() {
        {
            {
                String cssFilePath = System.getProperty("user.dir") + "\\target\\cucumber-reports\\cucumber.html\\style.css"; // Specify the path to your cucumber.html file
                String newCSSRule1 = "details { background: #C5D88A; }\n"; // New CSS rule 1
                String newCSSRule2 = ".cucumber-report .step .embedded-text { background: #C5D88A; }\n"; // New CSS rule 2

                try {
                    // Read the existing CSS file content
                    BufferedReader reader = new BufferedReader(new FileReader(cssFilePath));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();

                    // Append the new CSS rules to the content
                    content.append(newCSSRule1);
                    content.append(newCSSRule2);

                    // Write the updated content back to the CSS file
                    BufferedWriter writer = new BufferedWriter(new FileWriter(cssFilePath));
                    writer.write(content.toString());
                    writer.close();

                    System.out.println("CSS file updated successfully 1.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

