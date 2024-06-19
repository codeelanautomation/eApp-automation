package com.hexure.firelight.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.AfterClass;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"classpath:"},
        tags = "@Test",
        glue = {"com.hexure.firelight.stepdefinitions"},
        monochrome = true,
        publish = true
)
public class RunIntegrationTest {

    public static void main(String[] args) {
        // Add the UniqueTestCounter listener to your test run
        JUnitCore core = new JUnitCore();
        core.addListener(new UniqueTestCounter());

        // Run your tests
        core.run(RunIntegrationTest.class);
    }

    @AfterClass
    public static void tempDelete() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            deleteDirectoryContents(tempDir);
            System.out.println("All files in the temp directory have been deleted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectoryContents(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                attemptToDeleteFile(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                attemptToDeleteFile(dir); // Delete directory after its contents are deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void attemptToDeleteFile(Path filePath) throws IOException {
        int maxRetries = 1;
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                Files.deleteIfExists(filePath);
                return;
            } catch (IOException e) {
            }
        }
    }
}