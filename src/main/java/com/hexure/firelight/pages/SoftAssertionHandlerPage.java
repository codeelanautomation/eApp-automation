package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.*;
import cucumber.api.Scenario;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SoftAssertionHandlerPage extends FLUtilities {
    private final List<List<String>> assertions = new ArrayList<>();

    public SoftAssertionHandlerPage(WebDriver driver) {
        initElements(driver);
    }
    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void assertTrue(String countValidation, String order, String field, String rule, Object actualValue, Object expectedValue, boolean condition, TestContext testContext) {
        String result = "Passed";
        if(!condition)
            result = "Failed";
        assertions.add(Arrays.asList(countValidation, order, field, rule, actualValue.toString(), expectedValue.toString(), result));
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
    }

    private void printResults(List<List<String>> assertions, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        String resultSet = "";
        if (!assertions.isEmpty()) {
            resultSet += "<table border=\"1\" width=\"400\"> <tr> <th>S.No</th>  <th>Order</th>  <th>Field</th> <th>Rule</th> <th>Actual Value (UI)</th> <th>Expected Value (Excel Template)</th> <th>Result</th> </tr>";

            for (List<String> assertion : assertions) {
                if(assertion.get(6).contains("Passed"))
                    resultSet += "<tr style='color: green; font-weight: bold; background-color: #C5D88A;'> <td>" + assertion.get(0) + "</td> <td>"+ assertion.get(1) + "</td> <td>" + assertion.get(2) + "</td> <td>" + assertion.get(3) + "</td> <td>" + assertion.get(4) + "</td> <td>" + assertion.get(5) + "</td> <td>" + assertion.get(6) + "</td> </tr>";
                else
                    resultSet += "<tr style='color: red; font-weight: bold; background-color: #C5D88A;'> <td>" + assertion.get(0) + "</td> <td>"+ assertion.get(1) + "</td> <td>" + assertion.get(2) + "</td> <td>" + assertion.get(3) + "</td> <td>" + assertion.get(4) + "</td> <td>" + assertion.get(5) + "</td> <td>" + assertion.get(6) +  "</td> </tr>";
            }
            resultSet += "</table>";
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

