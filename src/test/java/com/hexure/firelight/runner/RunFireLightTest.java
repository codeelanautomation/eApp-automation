package com.hexure.firelight.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.AfterClass;
import org.junit.runner.JUnitCore;
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
                "html:target/cucumber-reports/cucumberClientModuleTagState/cucumberClientModuleTagState.html",
                "json:target/cucumber-reports/cucumberClientModuleTagState/cucumberClientModuleTagState.json",
                "rerun:target/failedrun.txt",
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
        Path sourceDir = Paths.get("target/cucumber-reports/cucumberClientModuleTagState");
        Path targetDir = Paths.get("target/cucumber-report/Client/cucumberModuleTagState");

        try {
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