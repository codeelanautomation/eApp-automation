package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.*;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoftAssertionHandlerPage extends FLUtilities {
    private final List<List<String>> assertions = new ArrayList<>();
    private final List<List<String>> assertionsNoElement = new ArrayList<>();
    private final List<List<String>> skippedRules = new ArrayList<>();
    private final List<List<String>> skippedElements = new ArrayList<>();
    private int fieldCount = 0;
    private StringBuilder resultSetSkippedRules = new StringBuilder();

    public SoftAssertionHandlerPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void assertTrue(WebDriver driver, String countValidation, String order, String jurisdictionApplication, String moduleName, String field, String distinctRule, String rule, Object actualValue, Object expectedValue, boolean condition, TestContext testContext) {
        String result = "Passed";
        if (!condition) {
            captureScreenshot(driver, testContext, condition);
            result = "Failed";
        }
        assertions.add(Arrays.asList(countValidation, order, jurisdictionApplication, moduleName, field, distinctRule, rule, actualValue.toString(), expectedValue.toString(), result));
        fieldCount = Integer.parseInt(order);
    }

    public void assertNoElement(WebDriver driver, String field, String message, TestContext testContext) {
        assertionsNoElement.add(Arrays.asList(field, message));
    }

    public void assertSkippedElement(WebDriver driver, String order, String jurisdictionApplication, String moduleName, String field, String reason, TestContext testContext) {
        skippedElements.add(Arrays.asList(order, jurisdictionApplication, moduleName, field, reason));
    }

    public void assertSkippedRules(WebDriver driver, String order, String jurisdictionApplication, String moduleName, String field, String rule, String reason, TestContext testContext) {
        skippedRules.add(Arrays.asList(order, jurisdictionApplication, moduleName, field, rule, reason));
    }

    public void afterScenario(TestContext testContext, int fieldsEvaluated) {
        // Print all assertions in the report at the end of the scenario
        assertAll(testContext, fieldsEvaluated);
    }

    public void assertAll(TestContext testContext, int fieldsEvaluated) {
        printResults(assertions, testContext, fieldsEvaluated);
        // printNoElementResults(assertionsNoElement, testContext);
        printSkippedElements(skippedElements, testContext);
        printSkippedRules(testContext);
    }

    private void printResults(List<List<String>> assertions, TestContext testContext, int fieldsEvaluated) {
        Scenario scenario = testContext.getScenario();
        StringBuilder resultSet = new StringBuilder();
        int countPassed = 0;
        int countFailed = 0;
        int skippedRulesCount = countSkippedRules(skippedRules, testContext);

        if (!assertions.isEmpty()) {
            resultSet.append("<table border=\"1\" width=\"100%\"> <tr> <th style=\"white-space: pre-wrap; width: 2%; vertical-align:top; padding-top: 5px; padding-bottom: 5px; padding-right:10px;\">S.No.</th>  <th style=\"white-space: pre-wrap; width: 2%; vertical-align:top; padding-top: 5px; padding-bottom: 5px; padding-right:10px;\">Order</th> <th style=\"white-space: pre-wrap; width: 2%; vertical-align:top; padding-top: 5px; padding-bottom: 5px; padding-right:10px;\">Jurisdiction</th> <th style=\"white-space: pre-wrap; width: 2%; vertical-align:top; padding-top: 5px; padding-bottom: 5px; padding-right:10px;\">Module Name</th> <th>Field</th> <th>Rule</th> <th>Validations</th> <th>Actual Value (UI)</th> <th>Expected Value (Excel Template)</th> <th>Result</th> </tr>");

            for (List<String> assertion : assertions) {
                if (assertion.get(9).contains("Passed")) {
                    countPassed++;
                    resultSet.append("<tr style='color: green; font-weight: bold; background-color: #C5D88A;'> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(0)).append("</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(1)).append("</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(2)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(3)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(4)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(5).trim()).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(6).trim()).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(7)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(8)).append("</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(9)).append("</td> </tr>");
                } else {
                    countFailed++;
                    resultSet.append("<tr style='color: red; font-weight: bold; background-color: #C5D88A;'> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(0)).append("</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(1)).append("</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(2)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(3)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(4)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(5).trim()).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(6).trim()).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(7)).append("</td> <td style='white-space: pre-wrap; min-width: 250px; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(8)).append("</td> <td style='vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>").append(assertion.get(9)).append("</td> </tr>");
                }
            }
            testContext.getScenario().log("<div width='100%' style='font-size:1.6vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>Execution Summary : </div>");
            scenario.log("<table border=\"1\" width=\"400\"> <tr style='font-weight: bold; background-color: #C5D88A;'> </tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Fields Evaluated</td> <td>" + fieldsEvaluated + "</td></tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Total Rules Evaluated </td> <td>" + (countFailed + countPassed + skippedRulesCount) + "</td> </tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Rules Validation Passed</td> <td><font color=\"green\">" + countPassed + "</font></td> </tr> <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Rules Validation Failed</td> <td><font color=\"red\">" + countFailed + "</font></td></tr>   <tr style='font-weight: bold; background-color: #C5D88A;'> <td>Rules Validation Skipped</td> <td><font color=\"red\">" + skippedRulesCount + "</font></td></tr><table>");
            resultSet.append("</table>");
            scenario.log(resultSet.toString());
        }
    }

    private void printNoElementResults(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        StringBuilder resultSet = new StringBuilder();
        if (!assertions.isEmpty()) {
            resultSet.append("<table border=\"1\" width=\"100%\"> <tr> <th>Field</th>  <th>Message</th></tr>");

            for (List<String> assertion : assertions) {
                resultSet.append("<tr style='color: red; font-weight: bold; background-color: #C5D88A;'> <td>").append(assertion.get(0)).append("</td> <td>").append(assertion.get(1)).append("</td> </tr>");
            }
            resultSet.append("</table>");
            scenario.log(resultSet.toString());
        }
    }

    private void printSkippedElements(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        StringBuilder resultSet = new StringBuilder();
        int count = 1;
        if (!assertions.isEmpty()) {
            testContext.getScenario().log("<div width='50%' style='font-size:1.3vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>List Of Fields Not Validated : </div>");
            resultSet.append("<div style='background-color: #C5D88A;'></div> <table border=\"1\" width=\"100%\"> <tr style='font-weight: bold; background-color: #C5D88A;'> <th style='white-space: pre-wrap; width: 2%; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>S.No.</th> <th style='white-space: pre-wrap; width: 5%; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>Order</th> <th style='white-space: pre-wrap; width: 5%; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>Jurisdiction</th> <th style='white-space: pre-wrap; width: 5%; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>Module Name</th> <th style='white-space: pre-wrap; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>Fields Not Validated</th> <th style='white-space: pre-wrap; vertical-align:top; padding-top: 5px; padding-bottom: 5px;'>Reason</th> </tr>");

            for (List<String> assertion : assertions) {
                resultSet.append("<tr style='background-color: #C5D88A;'> <td style='white-space: pre-wrap;'>").append(count++).append("</td> <td>").append(assertion.get(0)).append("</td> <td>").append(assertion.get(1)).append("</td> <td>").append(assertion.get(2)).append("</td> <td>").append(assertion.get(3)).append("</td> <td>").append(assertion.get(4)).append("</td> </tr>");
            }
            resultSet.append("</table>");
            scenario.log(resultSet.toString());
        }
    }

    private int countSkippedRules(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        int serialNumber = 1; // Start with 1
        if (!assertions.isEmpty()) {
            resultSetSkippedRules.append("<table border=\"1\" width=\"100%\"> <tr> <th>S.No.</th> <th>Order</th> <th>Jurisdiction</th> <th>Module Name</th> <th>Field</th>  <th>Skipped Rule</th>  <th>Reason for skip</th></tr>");

            for (List<String> assertion : assertions) {
                resultSetSkippedRules.append("<tr style='color: black; background-color: #C5D88A;'> <td>").append(serialNumber++).append("</td> <td>").append(assertion.get(0)).append("</td> <td>").append(assertion.get(1)).append("</td> <td>").append(assertion.get(2)).append("</td> <td>").append(assertion.get(3)).append("</td> <td>").append(assertion.get(4)).append("</td> <td>").append(assertion.get(5)).append("</td> </tr>");
            }
            resultSetSkippedRules.append("</table> <div> </div><div style=\"text-align: center; font-weight: bold; font-size: 24px;\">End of the Cucumber Report</div>\n");
        }
        return serialNumber - 1;
    }

    private void printSkippedRules(TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        testContext.getScenario().log("<div width='100%' style='font-size:1.3vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>List Of Rules Skipped : </div>");
        scenario.log(resultSetSkippedRules.toString());
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