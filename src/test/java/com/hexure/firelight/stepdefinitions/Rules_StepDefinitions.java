package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.SoftAssertionHandlerPage;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import cucumber.api.java.en.Given;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.hexure.firelight.libraies.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import javax.swing.text.MaskFormatter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rules_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    int countValidation = 1;
    DateTimeFormatter format = DateTimeFormatter.ofPattern(("MMddyyyy"));
    DateTimeFormatter formatWithSlash = DateTimeFormatter.ofPattern(("MM/dd/yyyy"));
    LocalDate todaysDate = LocalDate.now();
    String prefilledValue = "";

    public Rules_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCommonMethodsPage = testContext.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
    }

    private static int findColumnIndex(Row headerRow, String columnName) {
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (columnName.equalsIgnoreCase(getCellValue(cell))) {
                return cell.getColumnIndex();
            }
        }
        return -1; // Column not found
    }

    private static String getCellValue(Cell cell) {
        return cell == null ? "" : cell.toString().trim();
    }

    public static String getExcelColumnValue(String excelFilePath, String sheetName, int rowIndex, int columnIndex) {
        try {
            Workbook workbook = WorkbookFactory.create(new File(excelFilePath));
            Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in the workbook.");
            }

            Row row = sheet.getRow(rowIndex);
            Cell cell = row.getCell(columnIndex);

            String excelValue;
            if (cell != null && cell.getCellType() == CellType.STRING) {
                excelValue = cell.getStringCellValue();
            } else if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                excelValue = String.valueOf(((XSSFCell) cell).getRawValue());
            } else {
                excelValue = "";
            }
            workbook.close();
            return excelValue.trim();
        } catch (FileNotFoundException e) {
            // Handle the FileNotFoundException
            System.err.println("Either File not found or File is Open " + excelFilePath);
        } catch (Exception ignored) {
            // Handle other exceptions
        }
        return null;
    }

    @Given("User validate wizard fields for workbook {string}")
    public void verify_form_data_with_inbound_XML_from_Excel_and_Xml(String excelFileName) {
        String excelFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFileName;
        String sheetName = "E-App Wizard Spec";
        String field;
        String section;
        String condition = "";
        String expectedOperator = "";
        String expectedResult;
        String conditionAnother;
        String expectedResultAnother = "";
        String conditionElse = "";
        String expectedResultElse;
        String conditionElseAnother;
        String expectedResultElseAnother;
        String requiredFirstAttribute;
        String requiredSecondAttribute;
        String requiredFirstAttributeElse;
        String requiredSecondAttributeElse;
        String requiredAttributeValue;
        String dependentPrefilledCondition;
        boolean expectedFlag;
        List<String> actualOptions;
        List<String> expectedOptions;
        List<String> listConditions;
        List<String> expectedResults;
        List<String> listFieldValueConditions;
        String dataType = "";
        String wizardControlType;
        String order;
        String testData;

        try {
            FileInputStream fileInputStream = new FileInputStream(excelFilePath);
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheet(sheetName);
            Iterator<Row> iterator = sheet.iterator();
            // Assuming the fifth row contains headers
            Row headerRow = iterator.next();

            int fieldColumnIndex = findColumnIndex(headerRow, EnumsCommon.FIELD.getText());
            int sectionColumnIndex = findColumnIndex(headerRow, EnumsCommon.SECTION.getText());
            List<String> rulesList = Arrays.asList("ListOptions", "ValidationRules", "RulesWizard", "Length", "Format");
            for (int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++) {
                field = getExcelColumnValue(excelFilePath, sheetName, rowIndex + 1, fieldColumnIndex);
                section = getExcelColumnValue(excelFilePath, sheetName, rowIndex + 1, sectionColumnIndex);
                String valueJson = testContext.getMapTestData().get(field).trim();
                wizardControlType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
                order = JsonPath.read(valueJson, "$.Order").toString().trim();
                String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();

                expectedResult = "";
                if (!(field.toLowerCase().contains("lookup") | valueJson.toLowerCase().contains("hide for day") | commonTag.equalsIgnoreCase("No Tag"))) {
                    moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                    for (String rule : rulesList) {
                        try {
                            switch (rule) {
                                case "ListOptions":
                                    conditionAnother = "";
                                    expectedResult = "";
                                    String options = JsonPath.read(valueJson, "$.ListOptions").toString().trim();
                                    if (options.contains(";"))
                                        expectedOptions = Arrays.asList(options.split(";"));
                                    else
                                        expectedOptions = Arrays.asList(testContext.getMapTestData().get(options.replaceAll(" ", "")).split(", "));

                                    if (valueJson.contains("DisplayRules")) {
                                        if (Pattern.compile("(.*?) = (.*?) (AND|and) (.*?) = (.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim()).find()) {
                                            listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*?) (AND|and) (.*?) = (.*)", "DisplayRules", "");
                                            condition = listFieldValueConditions.get(0);
                                            expectedResult = listFieldValueConditions.get(1);
                                            conditionAnother = listFieldValueConditions.get(3);
                                            expectedResultAnother = listFieldValueConditions.get(4);
                                        } else if (Pattern.compile("(.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim()).find()) {
                                            listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "DisplayRules", "");
                                            condition = listFieldValueConditions.get(0);
                                            expectedResult = listFieldValueConditions.get(1);
                                        }
                                    }

                                    if (expectedResult.isEmpty()) {
                                        verifyOptions(valueJson, field, expectedOptions, "", "", "", "", "List Options");
                                    } else {
                                        for (String result : expectedResult.split(", ")) {
                                            setDependentCondition(condition, "=", valueJson, result);
                                            if (!conditionAnother.isEmpty()) {
                                                setDependentCondition(conditionAnother, "=", valueJson, expectedResultAnother);
                                                verifyOptions(valueJson, field, expectedOptions, condition, result, conditionAnother, expectedResultAnother, "List Options");
                                            } else
                                                verifyOptions(valueJson, field, expectedOptions, condition, result, "", "", "List Options");
                                        }
                                    }
                                    break;
                                case "RulesWizard":
                                    conditionAnother = "";
                                    for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split(";")) {
                                        distinctRule = distinctRule.replaceAll("(\\d+\\.\\s*)?","").trim().replaceFirst("\\.$", "").trim();
                                        System.out.println(distinctRule);
                                        if (!(distinctRule.toLowerCase().contains("lookup") | distinctRule.toLowerCase().contains("not required to use") | distinctRule.toLowerCase().contains("implemented then specify") | distinctRule.toLowerCase().contains("skip for automation"))) {
                                            if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) = (.*?),? then SHOW Options (.*)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) = (.*?),? then SHOW Options (.*)\\.?", "", distinctRule);
                                                condition = listConditions.get(1);
                                                expectedResult = listConditions.get(2);
                                                requiredSecondAttribute = listConditions.get(3);

                                                for (String result : expectedResult.split(", ")) {
                                                    setDependentCondition(condition, "=", valueJson, result);
                                                    switch (wizardControlType) {
                                                        case "Dropdown":
                                                        case "State Dropdown":
                                                            expectedOptions = Arrays.asList(requiredSecondAttribute.split(", "));
                                                            actualOptions = getOptions(valueJson, wizardControlType);
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, dataType + " Options when " + condition + " is " + result, actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                                            break;
                                                    }
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?").matcher(distinctRule).find()) {
                                                List<String> listExpectedConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?", "", distinctRule);
                                                condition = listExpectedConditions.get(1);
                                                expectedResults = Arrays.asList(listExpectedConditions.get(2).split(" AND|and "));
                                                conditionElse = expectedResult = conditionAnother = expectedResultAnother = "";

                                                if (Pattern.compile("(.*?) = (.*?) (AND|and) (.*?) = (.*)").matcher(condition).find()) {
                                                    listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*?) (AND|and) (.*?) = (.*)", "", condition);
                                                    conditionElse = listFieldValueConditions.get(0);
                                                    expectedResult = listFieldValueConditions.get(1);
                                                    conditionAnother = listFieldValueConditions.get(3);
                                                    expectedResultAnother = listFieldValueConditions.get(4);
                                                } else if (Pattern.compile("(.*?) = (.*)").matcher(condition).find()) {
                                                    listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "", condition);
                                                    conditionElse = listFieldValueConditions.get(0);
                                                    expectedResult = listFieldValueConditions.get(1);
                                                }

                                                for (String result : expectedResult.split(", ")) {
                                                    setDependentCondition(conditionElse, "=", valueJson, result);
                                                    if (!conditionAnother.isEmpty())
                                                        setDependentCondition(conditionAnother, "=", valueJson, expectedResultAnother);
                                                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                                                        setVisibilityRules(expectedResults.get(0).trim(), valueJson, wizardControlType, order, field, conditionElse, expectedOperator, result, expectedResults.get(1), condition, distinctRule);
                                                        setVisibilityRules(expectedResults.get(1).trim(), valueJson, wizardControlType, order, field, conditionElse, expectedOperator, result, "", condition, distinctRule);
                                                    } else
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + conditionElse + " is " + result, true, false, false, testContext);
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) = (.*?) and (.*?) = (.*?),? then (.*?) and (.*?), else if (.*?) = (.*?) or (.*?) = (.*?),? then (.*?) and (.*)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) = (.*?) and (.*?) = (.*?),? then (.*?) and (.*?), else if (.*?) = (.*?) or (.*?) = (.*?),? then (.*?) and (.*)\\.", "", distinctRule);
                                                condition = listConditions.get(1);
                                                expectedResult = listConditions.get(2);
                                                conditionAnother = listConditions.get(3);
                                                expectedResultAnother = listConditions.get(4);
                                                requiredFirstAttribute = listConditions.get(5);
                                                requiredSecondAttribute = listConditions.get(6);
                                                conditionElse = listConditions.get(7);
                                                expectedResultElse = listConditions.get(8);
                                                conditionElseAnother = listConditions.get(9);
                                                expectedResultElseAnother = listConditions.get(10);
                                                requiredFirstAttributeElse = listConditions.get(11);
                                                requiredSecondAttributeElse = listConditions.get(12);
                                                dependentPrefilledCondition = "";

                                                for (String result : expectedResult.split(", ")) {
                                                    for (String resultAnother : expectedResultAnother.split(", ")) {
                                                        setDependentCondition(condition, "=", valueJson, result);
                                                        setDependentCondition(conditionAnother, "=", valueJson, resultAnother);
                                                        setVisibilityRules(requiredFirstAttribute, valueJson, wizardControlType, order, field, condition, expectedOperator, result, "", conditionAnother, distinctRule);
                                                        setVisibilityRules(requiredSecondAttribute, valueJson, wizardControlType, order, field, condition, expectedOperator, result, "", conditionAnother, distinctRule);
                                                    }
                                                }
                                                if (requiredFirstAttributeElse.contains("prefilled with")) {
                                                    listConditions = getDisplayRuleConditions(valueJson, "prefilled with (.*)", "", requiredFirstAttributeElse);
                                                    requiredFirstAttributeElse = "prefilled with";
                                                    dependentPrefilledCondition = listConditions.get(0);
                                                    testData = setTestData(testContext.getMapTestData().get(dependentPrefilledCondition).trim());
                                                    setDependentCondition(dependentPrefilledCondition, "=", valueJson, testData);
                                                }
                                                for (String result : expectedResultElse.split(", ")) {
                                                    setDependentCondition(conditionElse, "=", valueJson, result);
                                                    setVisibilityRules(requiredFirstAttributeElse, valueJson, wizardControlType, order, field, conditionElse, expectedOperator, result, requiredSecondAttributeElse, dependentPrefilledCondition, distinctRule);
                                                    setVisibilityRules(requiredSecondAttributeElse, valueJson, wizardControlType, order, field, conditionElse, expectedOperator, result, requiredSecondAttributeElse, dependentPrefilledCondition, distinctRule);
                                                }
                                                for (String result : expectedResultElseAnother.split(", ")) {
                                                    setDependentCondition(conditionElseAnother, "=", valueJson, result);
                                                    setVisibilityRules(requiredFirstAttributeElse, valueJson, wizardControlType, order, field, conditionElseAnother, expectedOperator, result, requiredSecondAttributeElse, dependentPrefilledCondition, distinctRule);
                                                    setVisibilityRules(requiredSecondAttributeElse, valueJson, wizardControlType, order, field, conditionElseAnother, expectedOperator, result, requiredSecondAttributeElse, dependentPrefilledCondition, distinctRule);
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) (.*?) (.*?),? then (.*?) and (.*?), else if (.*?) = (.*?),? then (.*?) and (.*)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) (.*?) (.*?),? then (.*?) and (.*?), else if (.*?) = (.*?),? then (.*?) and (.*)\\.?", "", distinctRule);
                                                condition = listConditions.get(1);
                                                expectedOperator = listConditions.get(2);
                                                expectedResult = listConditions.get(3);
                                                requiredFirstAttribute = listConditions.get(4);
                                                requiredSecondAttribute = listConditions.get(5);
                                                conditionElse = listConditions.get(6);
                                                expectedResultElse = listConditions.get(7);
                                                requiredFirstAttributeElse = listConditions.get(8);
                                                requiredSecondAttributeElse = listConditions.get(9);
                                                dependentPrefilledCondition = "";

                                                for (String result : expectedResult.split(", ")) {
                                                    setDependentCondition(condition, expectedOperator, valueJson, result);
                                                    setVisibilityRules(requiredFirstAttribute, valueJson, wizardControlType, order, field, condition, expectedOperator, result, "", condition, distinctRule);
                                                    setVisibilityRules(requiredSecondAttribute, valueJson, wizardControlType, order, field, condition, expectedOperator, result, "", condition, distinctRule);
                                                }
                                                for (String result : expectedResultElse.split(", ")) {
                                                    if (result.equalsIgnoreCase("blank")) {
                                                        result = "";
                                                    }
                                                    setDependentCondition(conditionElse, "=", valueJson, result);
                                                    if (requiredFirstAttributeElse.contains("prefilled with")) {
                                                        listConditions = getDisplayRuleConditions(valueJson, "prefilled with (.*)", "", requiredFirstAttributeElse);
                                                        requiredFirstAttributeElse = "prefilled with";
                                                        dependentPrefilledCondition = listConditions.get(0);
                                                        testData = setTestData(testContext.getMapTestData().get(dependentPrefilledCondition).trim());
                                                        setDependentCondition(dependentPrefilledCondition, "=", valueJson, testData);
                                                    }
                                                    setVisibilityRules(requiredFirstAttributeElse, valueJson, wizardControlType, order, field, conditionElse, expectedOperator, result, requiredSecondAttributeElse, dependentPrefilledCondition, distinctRule);
                                                    setVisibilityRules(requiredSecondAttributeElse, valueJson, wizardControlType, order, field, conditionElse, expectedOperator, result, requiredSecondAttributeElse, dependentPrefilledCondition, distinctRule);
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) = (.*?) (AND|and) (.*?) = (.*?),? then (.*)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) = (.*?) (AND|and) (.*?) = (.*),? then (.*?)\\.?", "", distinctRule);
                                                condition = listConditions.get(1);
                                                expectedResult = listConditions.get(2);
                                                conditionAnother = listConditions.get(4);
                                                expectedResultAnother = listConditions.get(5);
                                                String[] requiredPrefilledAttribute = listConditions.get(6).split(", ");

                                                for (String result : expectedResult.split(", ")) {
                                                    setDependentCondition(condition, "=", valueJson, result);
                                                    if (!conditionAnother.isEmpty())
                                                        setDependentCondition(conditionAnother, "=", valueJson, expectedResultAnother);

                                                    for (String prefilledAttribute : requiredPrefilledAttribute) {
                                                        testData = setTestData(testContext.getMapTestData().get(prefilledAttribute.split(" = ")[1]).trim());
                                                        setDependentCondition(prefilledAttribute.split(" = ")[1], "=", valueJson, testData);
                                                        verifyData(testContext.getMapTestData().get(prefilledAttribute.split(" = ")[0]).trim(), prefilledAttribute.split(" = ")[0], condition, result, testData, "", distinctRule);
                                                    }
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) = (.*?),? then (.*?) = (.*?)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) = (.*?),? then (.*?) = (.*?)\\.?", "", distinctRule);
                                                condition = listConditions.get(1);
                                                expectedResult = listConditions.get(2);
                                                requiredFirstAttribute = listConditions.get(3);
                                                requiredSecondAttribute = listConditions.get(4);

                                                for (String result : expectedResult.split(", ")) {
                                                    setDependentCondition(condition, "=", valueJson, result);
                                                    if (requiredFirstAttribute.equalsIgnoreCase("SHOW options")) {
                                                        switch (wizardControlType) {
                                                            case "Dropdown":
                                                            case "State Dropdown":
                                                                expectedOptions = Arrays.asList(requiredSecondAttribute.split(", "));
                                                                actualOptions = getOptions(valueJson, wizardControlType);
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, dataType + " Options when " + condition + " is " + result, actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                                                break;
                                                        }
                                                    }
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?(Default|Placeholder) = (.*)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(.*?) = (.*)\\.?", "", distinctRule);
                                                requiredFirstAttribute = listConditions.get(1);
                                                requiredAttributeValue = listConditions.get(2);

                                                if (valueJson.contains("DisplayRules")) {
                                                    if (Pattern.compile("(.*?) = (.*?) (AND|and) (.*?) = (.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim()).find()) {
                                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*?) (AND|and) (.*?) = (.*)", "DisplayRules", "");
                                                        condition = listFieldValueConditions.get(0);
                                                        expectedResult = listFieldValueConditions.get(1);
                                                        conditionAnother = listFieldValueConditions.get(3);
                                                        expectedResultAnother = listFieldValueConditions.get(4);
                                                    } else if (Pattern.compile("(.*?) = (.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim()).find()) {
                                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "DisplayRules", "");
                                                        condition = listFieldValueConditions.get(0);
                                                        expectedResult = listFieldValueConditions.get(1);
                                                    }
                                                }

                                                if (expectedResult.isEmpty()) {
                                                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                                                        if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                            verifyData(valueJson, field, "", "", requiredAttributeValue, "", distinctRule);
                                                            if (valueJson.contains("ValidationRules"))
                                                                handleValidationRules(valueJson, "", "", field);
                                                            if (requiredFirstAttribute.equalsIgnoreCase("Placeholder"))
                                                                handlePlaceholderRules(valueJson, "", "", field, requiredAttributeValue, distinctRule);
                                                            handleSectionRules(valueJson, wizardControlType, section, order, field, distinctRule);
                                                        } else
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Field does not exists", true, "true", true, testContext);
                                                    } else
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, "true", true, testContext);
                                                } else {
                                                    for (String result : expectedResult.split(", ")) {
                                                        setDependentCondition(condition, "=", valueJson, result);
                                                        if (!conditionAnother.isEmpty())
                                                            setDependentCondition(conditionAnother, "=", valueJson, expectedResultAnother);

                                                        if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                                                            if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                                switch (requiredAttributeValue.toLowerCase().trim()) {
                                                                    case "blank":
                                                                        verifyData(valueJson, field, condition, result, requiredAttributeValue, "", distinctRule);
                                                                        break;
                                                                    case "unselected":
                                                                    case "unchecked":
                                                                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                                                                            expectedFlag = element.getAttribute("aria-checked").equalsIgnoreCase("false");
                                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Radio button \"" + element.getAttribute("title") + "\" " + requiredAttributeValue.toLowerCase().trim() + " by default when " + condition + " is " + result, expectedFlag, "true", expectedFlag, testContext);
                                                                        }
                                                                        break;
                                                                }
                                                                if (valueJson.contains("ValidationRules"))
                                                                    handleValidationRules(valueJson, condition, result, field);
                                                                handleSectionRules(valueJson, wizardControlType, section, order, field, distinctRule);
                                                            } else
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Field does not exists when " + condition + " is " + result, true, "true", true, testContext);
                                                        } else
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + condition + " is " + result, true, "true", true, testContext);
                                                    }
                                                }
                                            } else if (Pattern.compile("(\\d+\\.\\s*)?(Age is calculated on age last birth date|Always enabled|Optional)\\.?").matcher(distinctRule).find()) {
                                                listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(Age is calculated on age last birth date|Always enabled|Optional)\\.?", "", distinctRule);
                                                requiredAttributeValue = listConditions.get(1);
                                                if (valueJson.contains("DisplayRules")) {
                                                    listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "DisplayRules", "");
                                                    condition = listFieldValueConditions.get(0);
                                                    expectedResult = listFieldValueConditions.get(1);
                                                }

                                                switch (requiredAttributeValue) {
                                                    case "Age is calculated on age last birth date":
                                                        for (String result : expectedResult.split(", ")) {
                                                            setDependentCondition(condition, "=", valueJson, result);
                                                            LocalDate birthDatePastMonth = todaysDate.minusYears(25).plusMonths(-1);
                                                            LocalDate birthDateFutureMonth = todaysDate.minusYears(25).plusMonths(1);
                                                            sendKeys(driver, getElement(valueJson, wizardControlType, null), birthDatePastMonth.format(format));
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Age when " + condition + " is " + result + " and birth date is " + birthDatePastMonth.format(formatWithSlash), "25", String.valueOf(calculateAge(birthDatePastMonth, todaysDate)), String.valueOf(calculateAge(birthDatePastMonth, todaysDate)).equalsIgnoreCase("25"), testContext);
                                                            sendKeys(driver, getElement(valueJson, wizardControlType, null), birthDateFutureMonth.format(format));
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Age when " + condition + " is " + result + " and birth date is " + birthDateFutureMonth.format(formatWithSlash), "24", String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)), String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)).equalsIgnoreCase("24"), testContext);
                                                        }
                                                        break;
                                                    case "Always enabled":
                                                        expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Field is always enabled", expectedFlag, "true", expectedFlag, testContext);
                                                        break;
                                                    case "Optional":
                                                        String error = clickRedBubble(valueJson);
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Optional Field Validation", error, "", error.equalsIgnoreCase(""), testContext);
                                                        break;
                                                }
                                            } else {
                                                System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                                                onSoftAssertionHandlerPage.assertSkippedRules(driver, field, distinctRule, testContext);
                                            }
                                        } else
                                            onSoftAssertionHandlerPage.assertSkippedRules(driver, field, distinctRule, testContext);
                                    }
                                    break;
                                case "Length":
                                    if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                        getAttributeValue(field, valueJson, order, wizardControlType, rule, "maxLength", "Length");
                                    break;
                                case "Format":
                                    if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                        getAttributeValue(field, valueJson, order, wizardControlType, rule, "mask", "Format");
                                    break;
                            }
                        } catch (PathNotFoundException e) {
                            System.out.println("Field " + field + " does not have rule \"" + rule + "\"");
                        }
                    }
                } else
                    onSoftAssertionHandlerPage.assertSkippedElement(driver, field, testContext);
            }
            onSoftAssertionHandlerPage.afterScenario(testContext);
            workbook.close();
            fileInputStream.close();
        } catch (IOException e) {
            System.out.println(excelFileName + " could not be opened due to some reason");
        }
    }

    public void setVisibilityRules(String requiredAttribute, String valueJson, String wizardControlType, String order, String field, String condition, String expectedOperator, String result, String secondAttribute, String dependentPrefilledCondition, String distinctRule) {
        boolean expectedFlag;
        WebElement elem;

        if (expectedOperator.equalsIgnoreCase(""))
            expectedOperator = "is";

        if (Pattern.compile("SET (.*?) = (.*)").matcher(requiredAttribute).find()) {
            List<String> listConditions = getDisplayRuleConditions(valueJson, "SET (.*?) = (.*)", "", requiredAttribute);
            String conditionFirst = listConditions.get(0);
            String expectedResultFirst = listConditions.get(1);
            String testData = setTestData(testContext.getMapTestData().get(expectedResultFirst).trim());
            if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                if (!getElements(valueJson, wizardControlType).isEmpty()) {
                    setDependentCondition(expectedResultFirst, "=", valueJson, testData);
                    verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, condition, result, prefilledValue, testData, distinctRule);
                    setDependentCondition(expectedResultFirst, "=", valueJson, "");
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Field does not exists", true, "true", true, testContext);
            } else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, "true", true, testContext);
        }
        else {
            switch (requiredAttribute.toLowerCase()) {
                case "show":
                    expectedFlag = !getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Field is shown when " + condition + " " + expectedOperator + " " + result, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "enable":
                    if (!(wizardControlType.equals("Radio Button"))) {
                        expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Field is enabled when " + condition + " is " + result, expectedFlag, "true", expectedFlag, testContext);
                    } else {
                        for (WebElement element : getElements(valueJson, wizardControlType))
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Radio button " + element.getAttribute("title") + " is enabled when " + condition + " is " + result, element.isEnabled(), "true", element.isEnabled(), testContext);
                    }
                    break;
                case "disable":
                    if (!secondAttribute.equalsIgnoreCase("hide")) {
                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                            expectedFlag = element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput");
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" disabled when " + condition + " is " + result, expectedFlag, "true", expectedFlag, testContext);
                        }
                    }
                    break;
                case "set to no":
                    elem = getElement(valueJson, "radioField", "No");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Radio button No is selected when " + condition + " is " + result, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to yes":
                    elem = getElement(valueJson, "radioField", "Yes");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Radio button Yes is selected when " + condition + " is " + result, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to self":
                    verifyData(valueJson, field, condition, result, getDisplayRuleConditions(valueJson, "set to (.*)", "", requiredAttribute.toLowerCase()).get(0), "", distinctRule);
                    break;
                case "prefilled with":
                    if (valueJson.contains("\"Format\"")) {
                        String format = JsonPath.read(valueJson, "$.Format").toString().trim().replaceAll("[a-zA-Z]", "#");
                        MaskFormatter formatter;
                        try {
                            formatter = new MaskFormatter(format);
                            formatter.setValueContainsLiteralCharacters(false);
                            prefilledValue = formatter.valueToString(prefilledValue);
                        } catch (ParseException ignored) {
                        }
                    }
                    verifyData(valueJson, field, condition, result, prefilledValue, requiredAttribute, distinctRule);
                    setDependentCondition(dependentPrefilledCondition, "=", valueJson, "");
                    break;
                case "hide":
                    expectedFlag = getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Hidden Rule when " + condition + " is " + result, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "read only":
                    for (WebElement element : getElements(valueJson, wizardControlType)) {
                        expectedFlag = (element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput"));
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" read only when " + condition + " is " + result, expectedFlag, "true", expectedFlag, testContext);
                    }
                    break;
                default:
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, field, distinctRule, testContext);
            }
        }
    }

    public void handleSectionRules(String valueJson, String wizardControlType, String section, String order, String field, String distinctRule) {
        boolean expectedFlag = getElementSection(valueJson, wizardControlType).getText().equalsIgnoreCase(section);
        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule,"Field is displayed under section " + section, expectedFlag, "true", expectedFlag, testContext);
    }

    public int calculateAge(LocalDate dob, LocalDate currentDate) {
        return Period.between(dob, currentDate).getYears();
    }

    public void verifyOptions(String valueJson, String field, List<String> expectedOptions, String condition, String result, String conditionAnother, String expectedResultAnother, String distinctRule) {
        String dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
        List<String> actualOptions = getOptions(valueJson, dataType);
        if (condition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, dataType + " Options", actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
        else if (conditionAnother.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, dataType + " Options when " + condition + " is " + result, actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, dataType + " Options when " + condition + " is " + result + " and " + conditionAnother + " is " + expectedResultAnother, actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
    }

    public void setDependentCondition(String condition, String expectedOperator, String valueJson, String result) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        moveToPage(JsonPath.read(valueDependentJson, "$.Page").toString().trim(), JsonPath.read(valueDependentJson, "$.ModuleSectionName").toString().trim());
        if (expectedOperator.equalsIgnoreCase("=")) {
            setValue(valueDependentJson, result);
        } else if (expectedOperator.equalsIgnoreCase("<>")) {
            new Select(getElement(valueDependentJson, "dropdown", null)).selectByIndex(1);
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(2000);
        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
    }

    public void setValue(String valueDependentJson, String result) {
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                if (result.equalsIgnoreCase("1")) {
                    Select elem = new Select(getElement(valueDependentJson, "dropdown", null));
                    elem.selectByIndex(Integer.parseInt(result));
                    prefilledValue = elem.getFirstSelectedOption().getText().trim();
                } else
                    new Select(getElement(valueDependentJson, "dropdown", null)).selectByVisibleText(result);
                syncElement(driver, getElement(valueDependentJson, "dropdown", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "checkbox":
                checkBoxSelectYesNO(result, getElement(valueDependentJson, "checkbox", null));
                syncElement(driver, getElement(valueDependentJson, "checkbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "radio button":
                checkBoxSelectYesNO("check", getElement(valueDependentJson, "radioField", result));
                syncElement(driver, getElement(valueDependentJson, "radioField", result), EnumsCommon.TOCLICKABLE.getText());
                break;
            default:
                sendKeys(driver, getElement(valueDependentJson, "single line textbox", null), result);
                syncElement(driver, getElement(valueDependentJson, "single line textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(1000);
    }

    public String setTestData(String valueDependentJson) {
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                prefilledValue = "1";
                break;
            case "zip":
                prefilledValue = "12345";
                break;
            case "tin":
            case "ssn":
                prefilledValue = "1234567890";
                break;
            case "dob":
            case "mm/dd/yyyy":
                prefilledValue = todaysDate.format(format);
                break;
            default:
                prefilledValue = "TestValue";
        }
        return prefilledValue;
    }

    public void moveToPage(String pageHeader, String formHeader) {
        boolean flag = false;
        if (!(onCommonMethodsPage.getPageHeader().getText().equalsIgnoreCase(pageHeader) & onCommonMethodsPage.getFormHeader().getText().equalsIgnoreCase(formHeader))) {
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
            List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
            for (WebElement element : mandetoryFormList) {
                String form = element.getAttribute("innerText");
                if (form.equals(pageHeader)) {
                    clickElement(driver, element);
                    flag = true;
                    break;
                }
            }
            if (!flag)
                clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameCollapse());
        }
    }

    public boolean verifyPage(String pageHeader, String formHeader) {
        boolean flag = false;
        if (!onCommonMethodsPage.getList_WizardPageNameExpand().isEmpty())
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
        List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
        for (WebElement element : mandetoryFormList) {
            String form = element.getAttribute("innerText");
            if (form.equals(pageHeader)) {
                clickElement(driver, element);
                flag = true;
                break;
            }
        }
        if (!flag)
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameCollapse());
        return flag;
    }

    public List<WebElement> getElements(String valueJson, String datatype) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "radio button":
                return findElements(driver, String.format(onCommonMethodsPage.getRadioFieldCheckbox(), commonTag));
            case "dropdown":
            case "state dropdown":
                return findElements(driver, String.format(onCommonMethodsPage.getSelectField(), commonTag));
            default:
                return findElements(driver, String.format(onCommonMethodsPage.getInputField(), commonTag));
        }
    }

    public WebElement getElement(String valueJson, String datatype, String optionalValue) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return findElement(driver, String.format(onCommonMethodsPage.getSelectField(), commonTag));
            case "checkbox":
                return findElement(driver, String.format(onCommonMethodsPage.getRadioFieldCheckbox(), commonTag));
            case "radiofield":
                return findElement(driver, String.format(onCommonMethodsPage.getRadioFieldWithOption(), commonTag, optionalValue));
            case "errortype":
                switch (optionalValue.toLowerCase()) {
                    case "dropdown":
                    case "state dropdown":
                        return findElement(driver, String.format(onCommonMethodsPage.getSelectErrorField(), commonTag));
                    case "radio button":
                        return findElement(driver, String.format(onCommonMethodsPage.getRadioErrorField(), commonTag));
                    default:
                        return findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), commonTag));
                }
            default:
                return findElement(driver, String.format(onCommonMethodsPage.getInputField(), commonTag));
        }
    }

    public WebElement getElementSection(String valueJson, String datatype) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return findElement(driver, String.format(onCommonMethodsPage.getSectionSelect(), commonTag));
            case "checkbox":
            case "radio button":
                return findElement(driver, String.format(onCommonMethodsPage.getSectionRadio(), commonTag));
            default:
                return findElement(driver, String.format(onCommonMethodsPage.getSectionInput(), commonTag));
        }
    }

    public List<String> getOptions(String valueJson, String dataType) {
        List<String> actualOptions = new ArrayList<>();
        switch (dataType) {
            case "Dropdown":
            case "State Dropdown":
                List<WebElement> dropdownOptions = new Select(getElement(valueJson, "dropdown", null)).getOptions();
                for (WebElement element : dropdownOptions) {
                    if (element.getText().equalsIgnoreCase(""))
                        actualOptions.add("Blank");
                    else
                        actualOptions.add(element.getText());
                }
                break;
            case "Radio Button":
                List<WebElement> radioOptions = getElements(valueJson, "Radio Button");
                for (WebElement element : radioOptions) {
                    actualOptions.add(element.getAttribute("title"));
                }
                break;
        }
        return actualOptions;
    }

    public List<String> getDisplayRuleConditions(String valueJson, String requiredPattern, String parameter, String distinctRule) {
        Pattern pattern = Pattern.compile(requiredPattern);
        Matcher matcher;
        if (parameter.equalsIgnoreCase(""))
            matcher = pattern.matcher(distinctRule);
        else
            matcher = pattern.matcher(JsonPath.read(valueJson, "$." + parameter).toString().trim());

        int count = 1;
        List<String> options = new ArrayList<>();
        while (matcher.find()) {
            while (count <= matcher.groupCount()) {
                options.add(matcher.group(count));
                count++;
            }
        }
        return options;
    }

    public void verifyData(String valueJson, String field, String condition, String result, String requiredAttributeValue, String attribute, String distinctRule) {
        String expectedText;
        try {
            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
                case "state dropdown":
                case "dropdown":
                    expectedText = new Select(getElement(valueJson, "dropdown", null)).getFirstSelectedOption().getText().trim();
                    printResults(condition, valueJson, field, requiredAttributeValue, expectedText, result, attribute, distinctRule);
                    break;
                case "radio button":
                    List<WebElement> radioOptions = getElements(valueJson, "Radio Button");
                    for (WebElement element : radioOptions) {
                        expectedText = element.getAttribute("aria-checked");
                        if (expectedText.equals("false"))
                            expectedText = "unchecked";
                        else
                            expectedText = "checked";
                        printResults(field, valueJson, field, requiredAttributeValue, expectedText, element.getAttribute("title"), attribute, distinctRule);
                    }
                    break;
                case "checkbox":
                    expectedText = getElement(valueJson, "checkbox", null).getAttribute("aria-checked");
                    if (expectedText.equals("false"))
                        expectedText = "unchecked";
                    else
                        expectedText = "checked";
                    printResults(condition, valueJson, field, requiredAttributeValue, expectedText, result, attribute, distinctRule);
                    break;
                default:
                    expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                    printResults(condition, valueJson, field, requiredAttributeValue, expectedText, result, attribute, distinctRule);
                    break;
            }
        } catch (NullPointerException e) {
            onSoftAssertionHandlerPage.assertNoElement(driver, field, condition + " = " + result, testContext);
        }
    }

    public void printResults(String condition, String valueJson, String field, String requiredAttributeValue, String expectedText, String result, String attribute, String distinctRule) {
        if (expectedText.equalsIgnoreCase(""))
            expectedText = "blank";
        if (condition.isEmpty()) {
            if (requiredAttributeValue.equalsIgnoreCase(""))
                requiredAttributeValue = "blank";
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Default Value ", requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim()), testContext);
        } else if (attribute.equalsIgnoreCase("prefilled with"))
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Prefilled Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim()), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Default Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim()), testContext);
    }

    public void getAttributeValue(String field, String valueJson, String order, String wizardControlType, String rule, String attribute, String distinctRule) {
        List<String> listConditions;
        String dependentCondition = "";
        String dependentResult = "";
        String conditionAnother = "";
        String expectedResultAnother = "";
        if (!valueJson.contains("DisplayRules")) {
            if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                if (!getElements(valueJson, wizardControlType).isEmpty())
                    getLength(valueJson, attribute, rule, field, "", "", distinctRule);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, rule + " Validations -> Field does not exists", true, "true", true, testContext);
            } else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, false, false, testContext);
        } else {
            if (Pattern.compile("(.*?) = (.*?) (AND|and) (.*?) = (.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim()).find()) {
                listConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*?) (AND|and) (.*?) = (.*)", "DisplayRules", "");
                dependentCondition = listConditions.get(0);
                dependentResult = listConditions.get(1);
                conditionAnother = listConditions.get(3);
                expectedResultAnother = listConditions.get(4);
            } else if (Pattern.compile("(.*?) = (.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim()).find()) {
                listConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "DisplayRules", "");
                dependentCondition = listConditions.get(0);
                dependentResult = listConditions.get(1);
            }

            for (String result : dependentResult.split(", ")) {
                setDependentCondition(dependentCondition, "=", valueJson, result);
                if (!conditionAnother.isEmpty())
                    setDependentCondition(conditionAnother, "=", valueJson, expectedResultAnother);

                if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                    if (!getElements(valueJson, wizardControlType).isEmpty())
                        getLength(valueJson, attribute, rule, field, dependentCondition, result, distinctRule);
                    else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, rule + " Validations -> Field does not exists", true, "true", true, testContext);
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, field, distinctRule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, false, false, testContext);
            }
        }
    }

    public void getLength(String valueJson, String attribute, String rule, String field, String dependentCondition, String dependentResult, String distinctRule) {
        boolean expectedFlag;
        if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().equalsIgnoreCase("email")) {
            sendKeys(driver, getElement(valueJson, "input", null), JsonPath.read(valueJson, "$.Format").toString());
            expectedFlag = findElements(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Format of " + field + " is " + JsonPath.read(valueJson, "$.Format").toString(), expectedFlag, "true", expectedFlag, testContext);
        } else {
            try {
                String expectedText = getElement(valueJson, "input", null).getAttribute(attribute);

                if (expectedText.equals("99/99/9999"))
                    expectedText = "MM/dd/yyyy";
                if (rule.equalsIgnoreCase("format"))
                    expectedText = expectedText.replaceAll("9", "#");

                if (dependentCondition.equalsIgnoreCase(""))
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " when " + dependentCondition + " is " + dependentResult, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);

                handleTextLengthFields(valueJson, rule, field, dependentCondition, dependentResult, distinctRule);
            } catch (NullPointerException e) {
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Field does not have attribute " + attribute , false, true, false, testContext);
            }
        }
    }

    public void handleTextLengthFields(String valueJson, String rule, String field, String dependentCondition, String dependentResult, String distinctRule) {
        int attributeValue = Integer.parseInt(JsonPath.read(valueJson, "$.Length").toString().trim());
        String allowedChars = "9";
        String expectedText;
        String expectedFormat;
        String temp;
        MaskFormatter formatter;
        String error;
        String format;
        try {
            if (rule.equalsIgnoreCase("length")) {
                for (int length = attributeValue - 1; length <= attributeValue + 1; length++) {
                    temp = RandomStringUtils.random(length, allowedChars);
                    setValue(valueJson, temp);
                    expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                    error = clickRedBubble(valueJson);
                    if (length == attributeValue + 1)
                        temp = temp.substring(0, temp.length() - 1);

                    if (valueJson.contains("\"Format\"")) {
                        format = JsonPath.read(valueJson, "$.Format").toString().trim().replaceAll("[a-zA-Z]", "#");
                        formatter = new MaskFormatter(format);
                        formatter.setValueContainsLiteralCharacters(false);
                        temp = formatter.valueToString(temp);
                    }

                    if (length == attributeValue - 1) {
                        if (error.isEmpty()) {
                            if (dependentCondition.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when " + dependentCondition + " is " + dependentResult + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        } else {
                            if (dependentCondition.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when length is " + length, error, error, true, testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when " + dependentCondition + " is " + dependentResult + " and length is " + length, error, error, true, testContext);
                        }
                    } else {
                        if (dependentCondition.isEmpty())
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when " + dependentCondition + " is " + dependentResult + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                    }
                }
                setValue(valueJson, "");
            } else {
                temp = RandomStringUtils.random(attributeValue, allowedChars);
                setValue(valueJson, temp);
                expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                expectedFormat = getElement(valueJson, "single line textbox", null).getAttribute("mask");
                if (dependentCondition.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + "validations when length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, rule + " validations when " + dependentCondition + " is " + dependentResult + " and length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePlaceholderRules(String valueJson, String dependentCondition, String dependentResult, String field, String requiredAttributeValue, String distinctRule) {
        String expectedAttribute = getElement(valueJson, JsonPath.read(valueJson, "$.WizardControlType"), "").getAttribute("placeholder");
        if (dependentCondition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Placeholder value", expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Placeholder value when " + dependentCondition + " is " + dependentResult, expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);

    }

    public void handleValidationRules(String valueJson, String dependentCondition, String dependentResult, String field) {
        for (String distinctRule : JsonPath.read(valueJson, "$.ValidationRules").toString().trim().split((";"))) {
            distinctRule = distinctRule.replaceAll("(\\d+\\.\\s*)?","").trim();
            System.out.println(distinctRule);
            if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) results in an age that is less than (.*?) or greater than (.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) results in an age that is less than (.*?) or greater than (.*?),? then (.*?): (.*)", "", distinctRule);
                String minValue = listConditions.get(2);
                String maxValue = listConditions.get(3);
                String requiredErrorMessage = listConditions.get(5);
                String error;
                LocalDate dob = todaysDate.minusYears(Long.parseLong(minValue)).minusMonths(8);

                sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                error = clickRedBubble(valueJson);
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Validation Rule -> Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) > Integer.parseInt(minValue), testContext);

                dob = todaysDate.minusYears(Long.parseLong(maxValue) + 1).minusMonths(1);
                sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                error = clickRedBubble(valueJson);
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Validation Rule -> Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) < Integer.parseInt(maxValue), testContext);
            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) (.*?) (.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) (.*?) (.*?),? then (.*?): (.*)", "", distinctRule);
                String expectedResult = listConditions.get(3);
                String expectedOperator = listConditions.get(2);
                String requiredErrorMessage = listConditions.get(5);
                handleErrorMessage(expectedOperator, expectedResult, valueJson, requiredErrorMessage, field, dependentCondition, dependentResult, distinctRule);
            } else {
                System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                onSoftAssertionHandlerPage.assertSkippedRules(driver, field, distinctRule, testContext);
            }
        }
    }

    public void handleErrorMessage(String expectedOperator, String expectedResult, String valueJson, String requiredErrorMessage, String field, String dependentCondition, String dependentResult, String distinctRule) {
        String inputValue = "";
        String error;
        if (expectedResult.equalsIgnoreCase("Invalid")) {
            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString()) {
                case "TIN":
                    inputValue = testContext.getMapTestData().get("InvalidTin").trim();
                    break;
                case "SSN":
                    inputValue = testContext.getMapTestData().get("InvalidSSN").trim();
                    break;
                case "Email":
                    inputValue = validateInvalidEmail(valueJson, field, requiredErrorMessage, distinctRule);
                    break;
            }
            sendKeys(driver, getElement(valueJson, "single line textbox", null), inputValue);
        } else if (expectedResult.equalsIgnoreCase("current date")) {
            switch (expectedOperator) {
                case "=":
                    inputValue = todaysDate.format(format);
                    break;
                case ">":
                    inputValue = todaysDate.plusDays(1).format(format);
                    break;
            }
            sendKeys(driver, getElement(valueJson, "single line textbox", null), inputValue);
        }

        error = clickRedBubble(valueJson);
        switch (expectedResult.toLowerCase()) {
            case "blank":
                if (dependentCondition.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Validation Rule -> Mandatory Field Validation" + field + " is " + expectedResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Validation Rule -> Mandatory Field Validation when " + dependentCondition + " is " + dependentResult + " and " + field + " is " + expectedResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                break;
            case "invalid":
            case "current date":
                if (dependentCondition.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Validation Rule -> Invalid Value Validation when " + field + " is " + inputValue, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, "Validation Rule -> Invalid Value Validation when " + field + " is " + inputValue + " and " + dependentCondition + " is " + dependentResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                sendKeys(driver, getElement(valueJson, "single line textbox", null), "");
                break;
            default:
                onSoftAssertionHandlerPage.assertSkippedRules(driver, field, distinctRule, testContext);
        }
    }

    public String clickRedBubble(String valueJson) {
        String error;
        try {
            if (onCommonMethodsPage.getListErrors().isEmpty())
                clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            if (!(JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Checkbox") | JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Radio Button")))
                clickElement(driver, getElement(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes").toString(), JsonPath.read(valueJson, "$.WizardControlTypes").toString()));
            WebElement errorElement = getElement(valueJson, "errortype", JsonPath.read(valueJson, "$.WizardControlTypes").toString());
            error = errorElement.getText();
        } catch (NullPointerException e) {
            // Handle null pointer exception
            error = "";
        }
        return error;
    }

    public String validateInvalidEmail(String valueJson, String field, String requiredErrorMessage, String distinctRule) {
        List<String> invalidEmails = Arrays.asList(testContext.getMapTestData().get("InvalidEmail").trim().split(","));
        String lastInvalidEmail = invalidEmails.get(invalidEmails.size() - 1);
        for (String invalidEmail : invalidEmails) {
            sendKeys(driver, getElement(valueJson, "input", null), invalidEmail);
            String error = clickRedBubble(valueJson);
            String validationMessage = "Invalid Value Validation when " + field + " is " + invalidEmail;
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, distinctRule, validationMessage, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
        }
        return lastInvalidEmail;
    }
}






