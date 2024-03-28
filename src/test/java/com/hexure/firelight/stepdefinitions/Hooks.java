package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.PageObjectManager;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.After;
import gherkin.lexer.Th;
import lombok.Data;
import org.junit.jupiter.api.AfterAll;

import java.io.*;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Data
public class Hooks extends FLUtilities {
    private TestContext testContext;

    public Hooks(TestContext context) {
        testContext = context;
    }

    private long startTime;

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
        cucumberReport();
    }
    public static void cucumberReport()
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