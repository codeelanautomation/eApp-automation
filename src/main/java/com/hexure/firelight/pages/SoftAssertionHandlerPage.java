package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.*;
import cucumber.api.Scenario;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SoftAssertionHandlerPage extends FLUtilities {
    private final List<List<String>> assertions = new ArrayList<>();
    private final List<List<String>> assertionsNoElement = new ArrayList<>();
    private final List<List<String>> skippedRules = new ArrayList<>();
    private final List<String> skippedElements = new ArrayList<>();
    int fieldCount = 0;

    public SoftAssertionHandlerPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void assertTrue(WebDriver driver, String countValidation, String order, String field, String distinctRule, String rule, Object actualValue, Object expectedValue, boolean condition, TestContext testContext) {
        String result = "Passed";
        if (!condition) {
            captureScreenshot(driver, testContext, condition);
            result = "Failed";
        }
        assertions.add(Arrays.asList(countValidation, order, field, distinctRule, rule, actualValue.toString(), expectedValue.toString(), result));
        fieldCount = Integer.parseInt(order);
    }

    public void assertNoElement(WebDriver driver, String field, String message, TestContext testContext) {
        assertionsNoElement.add(Arrays.asList(field, message));
    }

    public void assertSkippedElement(WebDriver driver, String field, TestContext testContext) {
        skippedElements.add(field);
    }

    public void assertSkippedRules(WebDriver driver, String field, String rule, TestContext testContext) {
        skippedRules.add(Arrays.asList(field, rule));
    }

//    public void assertEquals(String message, Object actual, Object expected, TestContext testContext) {
//        assertTrue(message, actual.equals(expected), testContext);
//    }

    public void afterScenario(TestContext testContext) {
        // Print all assertions in the report at the end of the scenario
        assertAll(testContext);
    }

    public void assertAll(TestContext testContext) {
        printResults(assertions, testContext);
        printNoElementResults(assertionsNoElement, testContext);
        printSkippedElements(skippedElements, testContext);
        printSkippedRules(skippedRules, testContext);
    }

    private void printResults(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        String resultSet = "";
        int countPassed = 0;
        int countFailed = 0;

        if (!assertions.isEmpty()) {
            resultSet += "<table border=\"1\" width=\"100%\"> <tr> <th>S.No</th>  <th>Order</th>  <th>Field</th> <th>Rule</th> <th>Validations</th> <th>Actual Value (UI)</th> <th>Expected Value (Excel Template)</th> <th>Result</th> </tr>";

            for (List<String> assertion : assertions) {
                if (assertion.get(7).contains("Passed")) {
                    countPassed++;
                    resultSet += "<tr style='color: green; font-weight: bold; background-color: #C5D88A;'> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(0) + "</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(1) + "</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(2) + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(3) +"</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(4) + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(5).trim() + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(6).trim() + "</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(7) + "</td> </tr>";
                }
                else {
                    countFailed++;
                    resultSet += "<tr style='color: red; font-weight: bold; background-color: #C5D88A;'> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(0) + "</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(1) + "</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(2) + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(3) + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(4) + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(5).trim() + "</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(6).trim() + "</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>" + assertion.get(7) + "</td> </tr>";
                }
            }
            testContext.getScenario().write("<div width='100%' style='font-size:1.6vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>Execution Summary </div>");
            scenario.write("<table border=\"1\" width=\"400\"> <tr style='font-weight: bold; background-color: #C5D88A;'> </tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Fields Evaluated</td> <td>" + fieldCount + "</td></tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Total Rules Evaluated </td> <td>" + (countFailed + countPassed) + "</td> </tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Rules Validation Passed</td> <td>" + countPassed + "</td> </tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Rules Validation Failed</td> <td>" + countFailed + "</td></tr><table>");
            resultSet += "</table>";
            scenario.write(resultSet);
        }
    }

    private void printNoElementResults(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        String resultSet = "";
        if (!assertions.isEmpty()) {
            resultSet += "<table border=\"1\" width=\"100%\"> <tr> <th>Field</th>  <th>Message</th></tr>";

            for (List<String> assertion : assertions) {
                resultSet += "<tr style='color: red; font-weight: bold; background-color: #C5D88A;'> <td>" + assertion.get(0) + "</td> <td>" + assertion.get(1) + "</td> </tr>";
            }
            resultSet += "</table>";
            scenario.write(resultSet);
        }
    }

    private void printSkippedElements(List<String> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        String resultSet = "";
        int count = 1;
        if (!assertions.isEmpty()) {
            resultSet += "<div style='background-color: #C5D88A;'> </div> <table border=\"1\" width=\"100%\"> <tr style='font-weight: bold; background-color: #C5D88A;'> <th>S.No</th> <th>Fields Not Validated</th> </tr>";

            for (String assertion : assertions) {
                resultSet += "<tr style='background-color: #C5D88A;'> <td>" + (count++) + "</td> <td>" + assertion+ "</td> </tr>";
            }
            resultSet += "</table>";
            scenario.write(resultSet);
        }
    }

    private void printSkippedRules(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        String resultSet = "";
        if (!assertions.isEmpty()) {
            resultSet += "<table border=\"1\" width=\"100%\"> <tr> <th>S.No.</th> <th>Field</th>  <th>Skipped Rule</th></tr>";

            int serialNumber = 1; // Start with 1
            for (List<String> assertion : assertions) {
                resultSet += "<tr style='color: red; font-weight: bold; background-color: #C5D88A;'> <td>" + serialNumber++ + "</td> <td>" + assertion.get(0) + "</td> <td>" + assertion.get(1) + "</td> </tr>";
            }
            resultSet += "</table> <div> </div><div style=\"text-align: center; font-weight: bold; font-size: 24px;\">End of the cucumber report</div>\n";
            scenario.write(resultSet);
        }
    }

    private String formatAssertion(boolean condition, String message) {
        String result = condition
                ? "<div class='assertion' style='color: green; font-weight: bold; background-color: #C5D88A;'>Assertion Passed: </div>"
                : "<div class='assertion' style='color: red; font-weight: bold; background-color: #C5D88A;'>Assertion Failed: </div>";

        // Append the assertion message
        result += message;
        return result;
    }

}