package com.hexure.firelight.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.AfterClass;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

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

    @AfterClass
    public static void tempDelete() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            AtomicInteger totalDeletedCount = new AtomicInteger();

            Files.walk(tempDir)
                    .parallel() // Process files in parallel
                    .forEach(path -> {
                        try {
                            deleteFile(path);
                            totalDeletedCount.incrementAndGet();
                        } catch (IOException e) {
//                                System.err.println("Failed to delete " + path.toString() + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile(Path path) throws IOException {
        int maxRetries = 3;
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException e) {
                // Log the failure, or you can retry
                if (retry == maxRetries - 1) {
                    throw e; // Throw exception if max retries reached
                }
            }
        }
    }
}