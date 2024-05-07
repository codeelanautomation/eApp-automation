package com.hexure.firelight.runner;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "json:target/cucumberClientModuleTagState-html-report",
                "json:target/cucumber-reports/cucumberClientModuleTagState.xml",
                "html:target/cucumber-reports/cucumberClientModuleTagState",
                "rerun:target/failedrun.txt",
                "json:target/cucumber-reports/cucumberClientModuleTagState.json",
                "pretty",
                "io.qameta.allure.cucumber4jvm.AllureCucumber4Jvm"
        },
        features = {"classpath:"},
        tags = {""},
        glue = {"com.hexure.firelight.stepdefinitions"},
        monochrome = true
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
        String cssFilePath = System.getProperty("user.dir") + "\\target\\cucumber-reports\\cucumberClientModuleTagState\\style.css"; // Specify the path to your cucumber.html file
        String newCSSRule1 = "details { background: #C5D88A; }\n"; // New CSS rule 1
        String newCSSRule2 = ".cucumber-report .step .embedded-text { background: #C5D88A; }\n"; // New CSS rule 2
        String newCSSRule3 = "body { background: #C5D88A; }\n";

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
            content.append(newCSSRule3);

            // Write the updated content back to the CSS file
            BufferedWriter writer = new BufferedWriter(new FileWriter(cssFilePath));
            writer.write(content.toString());
            writer.close();

            System.out.println("CSS file updated successfully 1.");
            renameReportFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void renameReportFile() {
        // Specify the desired new name for the report
        String newReportName = "cucumberClientModuleTagState.html";
        // Construct the paths for the original and new report files
        Path originalReportPath = new File("target/cucumber-reports/cucumberClientModuleTagState/index.html").toPath();
        Path newReportPath = new File("target/cucumber-reports/cucumberClientModuleTagState/" + newReportName).toPath();

        Path sourceDir = Paths.get("target/cucumber-reports/cucumberClientModuleTagState");
        Path targetDir = Paths.get("target/cucumber-report/cucumberClientModuleTagState");

        try {
            // Generate the new report file by moving the original one
            Files.move(originalReportPath, newReportPath, StandardCopyOption.REPLACE_EXISTING);
            moveDirectory(sourceDir, targetDir);
        } catch (Exception e) {
            System.err.println("Failed to rename report: " + e.getMessage());
        }
    }

    public static void moveDirectory(Path sourceDir, Path targetDir) throws IOException {
        // Delete the target directory if it already exists
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }

        // Create the target directory
        Files.createDirectories(targetDir);

        // Move the source directory to the target location
        Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}