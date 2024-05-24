package com.hexure.firelight.runner;

import io.qameta.allure.Allure;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import java.util.HashSet;
import java.util.Set;

public class UniqueTestCounter extends RunListener {

    private final Set<String> executedTests = new HashSet<>();
    private int actualTestCount = 0;

    @Override
    public void testStarted(Description description) {
        String testIdentifier = description.getClassName() + "#" + description.getMethodName();
        if (!executedTests.contains(testIdentifier)) {
            actualTestCount++;
            executedTests.add(testIdentifier);
        }
    }

    @Override
    public void testFinished(Description description) {
        Allure.addAttachment("Actual Test Count", String.valueOf(actualTestCount));
    }
}