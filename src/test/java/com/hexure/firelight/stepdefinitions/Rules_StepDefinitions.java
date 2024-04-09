package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.LoginPage;
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
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Rules_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    private final LoginPage onLoginPage;
    int countValidation = 1;
    DateTimeFormatter format = DateTimeFormatter.ofPattern(("MMddyyyy"));
    DateTimeFormatter formatWithSlash = DateTimeFormatter.ofPattern(("MM/dd/yyyy"));
    LocalDate todaysDate = LocalDate.now();
    String prefilledValue = "";
    String difference;
    String moduleName;
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    List<List<String>> combinationConditions = new ArrayList<>();
    Map<String, String> howManyOperator = new HashMap<>();
    private long endTime;
    private LocalTime endLocalTime;

    public Rules_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCommonMethodsPage = testContext.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
        onLoginPage = testContext.getPageObjectManager().getLoginPage();
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
        String conditionElse;
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
        String reason;
        String order;
        String testData;
        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys = new ArrayList<>();
        List<String> skippedInvalidElements = new ArrayList<>();
        List<String> filteredElements = null;

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
                moduleName = JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim();
                String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
                reason = "";
                if(valueJson.contains("Reasonforskip"))
                    reason = JsonPath.read(valueJson, "$.Reasonforskip").toString().trim();
                combinationConditions = new ArrayList<>();
                howManyOperator = new HashMap<>();

                if (rowIndex > 192)
                    break;

                expectedResult = "";
                if (!(field.toLowerCase().contains("lookup") | valueJson.toLowerCase().contains("hide for day") | commonTag.equalsIgnoreCase("No Tag"))) {
                    moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                    if (verifyElementExists(valueJson, skippedInvalidElements, order, field)) {
                        combinationConditions.clear();
                        howManyOperator.clear();
                        for (String rule : rulesList) {
                            try {
                                switch (rule) {
                                    case "ListOptions":
                                        conditionAnother = "";
                                        condition = "";
                                        expectedResultAnother = "";
                                        expectedResult = "";
                                        String key = "";
                                        String values = "";
                                        String[] conditionValues;
                                        mapConditions.clear();
                                        String listConditionkeys = "";
                                        List<String> invalidTag = new ArrayList<>();

                                        String options = JsonPath.read(valueJson, "$.ListOptions").toString().trim();
                                        if (options.contains(";"))
                                            expectedOptions = Arrays.asList(options.split(";"));
                                        else
                                            expectedOptions = Arrays.asList(testContext.getMapTestData().get(options.replaceAll(" ", "")).split(", "));

                                        if (valueJson.contains("DisplayRules")) {
                                            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim());
                                            setCombinationConditions(valueJson, "([^\\s]+)\\s* (=|<>) (.*)");
                                        }
                                        if (invalidTag.isEmpty()) {
                                            if (combinationConditions.isEmpty()) {
                                                verifyOptions(valueJson, field, expectedOptions, "", "", "", "", "List Options");
                                            } else {
                                                for (List<String> result : combinationConditions) {
                                                    for (String condition1 : result) {
                                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                                                        key = listFieldValueConditions.get(0).trim();
                                                        values = listFieldValueConditions.get(1).trim();
                                                        listConditionkeys = findKeyExistsJSON(key);
                                                        if (!listConditionkeys.equalsIgnoreCase(""))
                                                            break;
                                                        setConditions1(key, valueJson, values, "=");
                                                    }
                                                    if (listConditionkeys.equalsIgnoreCase("")) {
                                                        if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim()))
                                                            moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                                                        verifyOptions(valueJson, field, expectedOptions, key, values, conditionAnother, expectedResultAnother, "List Options");
                                                    } else
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, "ListOptions", "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                                                }
                                                combinationConditions.clear();
                                                howManyOperator.clear();
                                            }
                                        } else
                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, "ListOptions", "Key " + invalidTag + " not a valid tag", false, "true", false, testContext);
                                        break;
                                    case "RulesWizard":
                                        conditionAnother = "";
                                        for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split(";")) {
                                            distinctRule = distinctRule.replaceFirst("(\\d+\\.\\s*)?", "").trim().replaceFirst("\\.$", "").trim();
                                            System.out.println(order + ". " + field + " -> " + distinctRule);
                                            if (!(distinctRule.toLowerCase().contains("lookup") | distinctRule.toLowerCase().contains("not required to use") | distinctRule.toLowerCase().contains("implemented then specify") | distinctRule.toLowerCase().contains("skip for automation"))) {
                                                invalidTag = getInvalidTags(skippedInvalidElements, distinctRule);
                                                if (invalidTag.isEmpty()) {
                                                    if (Pattern.compile("(\\d+\\.\\s*)?If (.*?),? then (?i)SHOW Options (.*)\\.?").matcher(distinctRule).find()) {
                                                        listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?),? then (?i)SHOW Options (.*)\\.?", "", distinctRule);
                                                        condition = listConditions.get(1);
                                                        requiredSecondAttribute = listConditions.get(2);
                                                        key = values = "";
                                                        mapConditions.clear();
                                                        listConditionkeys = "";

                                                        for (String eachCondition : condition.trim().split(("AND"))) {
                                                            listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s* = (.*)", "", eachCondition.trim());
                                                            key = listFieldValueConditions.get(0).trim();
                                                            conditionValues = listFieldValueConditions.get(1).trim().trim().split(", ");
                                                            // Add the key-value pairs to the map
                                                            for (String value : conditionValues) {
                                                                List<String> valuesList = mapConditions.getOrDefault(key, new ArrayList<>());
                                                                valuesList.add(value.trim());
                                                                mapConditions.put(key, valuesList);
                                                            }
                                                        }
                                                        allKeys = new ArrayList<>(mapConditions.keySet());
                                                        generateCombinations(allKeys, new ArrayList<>(), mapConditions);

                                                        for (List<String> result : combinationConditions) {
                                                            for (String condition1 : result) {
                                                                listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                                                                key = listFieldValueConditions.get(0).trim();
                                                                values = listFieldValueConditions.get(1).trim();
                                                                listConditionkeys = findKeyExistsJSON(key);
                                                                if (!listConditionkeys.equalsIgnoreCase(""))
                                                                    break;
                                                                setConditions1(key, valueJson, values, "=");
                                                            }
                                                            if (listConditionkeys.equalsIgnoreCase("")) {
                                                                if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim()))
                                                                    moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                                                                switch (wizardControlType) {
                                                                    case "Dropdown":
                                                                    case "State Dropdown":
                                                                        expectedOptions = Arrays.asList(requiredSecondAttribute.split(", "));
                                                                        actualOptions = getOptions(valueJson, wizardControlType);
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, dataType + " Options when " + key + " is " + values, actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                                                        break;
                                                                }
                                                            } else
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                                                        }
                                                        combinationConditions.clear();
                                                        howManyOperator.clear();
                                                    } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?").matcher(distinctRule).find()) {
                                                        List<String> listExpectedConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?", "", distinctRule);
                                                        condition = listExpectedConditions.get(1);
                                                        expectedResults = Arrays.asList(listExpectedConditions.get(2).split(" (?i)AND "));
                                                        conditionElse = "";
                                                        key = values = "";
                                                        mapConditions.clear();
                                                        String displayedText = "";
                                                        listConditionkeys = "";
                                                        boolean hidden = false;
                                                        if (expectedResults.get(1).equalsIgnoreCase("hide"))
                                                            hidden = true;

                                                        for (String eachCondition : condition.trim().split(("AND"))) {
                                                            listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s* (=|<>) (.*)", "", eachCondition.trim());
                                                            key = listFieldValueConditions.get(0).trim();
                                                            expectedOperator = listFieldValueConditions.get(1).trim();
                                                            conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
                                                            howManyOperator.put(key, expectedOperator);
                                                            // Add the key-value pairs to the map
                                                            for (String value : conditionValues) {
                                                                List<String> valuesList = mapConditions.getOrDefault(key, new ArrayList<>());
                                                                valuesList.add(value.trim());
                                                                mapConditions.put(key, valuesList);
                                                            }
                                                        }
                                                        allKeys = new ArrayList<>(mapConditions.keySet());
                                                        generateCombinations(allKeys, new ArrayList<>(), mapConditions);

                                                        for (List<String> result : combinationConditions) {
                                                            displayedText = " when ";
                                                            for (String condition1 : result) {
                                                                listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                                                                key = listFieldValueConditions.get(0).trim();
                                                                values = listFieldValueConditions.get(1).trim();
                                                                displayedText += key + " is " + values + " and ";
                                                                listConditionkeys = findKeyExistsJSON(key);
                                                                if (!listConditionkeys.equalsIgnoreCase(""))
                                                                    break;
                                                                setConditions1(key, valueJson, values, howManyOperator.get(key));
                                                            }
                                                            if (listConditionkeys.equalsIgnoreCase("")) {
                                                                if (displayedText.trim().endsWith("and"))
                                                                    displayedText = displayedText.substring(0, displayedText.length() - 4);
                                                                if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                                                                    moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                                                                    setVisibilityRules(expectedResults.get(0).trim(), valueJson, wizardControlType, order, field, expectedResults.get(1), distinctRule, result);
                                                                    setVisibilityRules(expectedResults.get(1).trim(), valueJson, wizardControlType, order, field, "", distinctRule, result);
                                                                } else
                                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists " + displayedText, true, hidden, hidden, testContext);
                                                            } else
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                                                        }
                                                        combinationConditions.clear();
                                                        howManyOperator.clear();
                                                    } else if (Pattern.compile("(\\d+\\.\\s*)?If ([^\\s]+)\\s* = (.*?) (?i)AND ([^\\s]+)\\s* = (.*?),? then (.*)\\.?$").matcher(distinctRule).find()) {
                                                        listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If ([^\\s]+)\\s* = (.*?) (?i)AND ([^\\s]+)\\s* = (.*),? then (.*?)\\.?$", "", distinctRule);
                                                        condition = listConditions.get(1);
                                                        expectedResult = listConditions.get(2);
                                                        conditionAnother = listConditions.get(3);
                                                        expectedResultAnother = listConditions.get(4);
                                                        String[] requiredPrefilledAttribute = listConditions.get(6).split(", ");

                                                        for (String result : expectedResult.split(", ")) {
                                                            setConditions(condition, valueJson, result, conditionAnother, expectedResultAnother, "=", "=");
                                                            for (String prefilledAttribute : requiredPrefilledAttribute) {
                                                                testData = setTestData(testContext.getMapTestData().get(prefilledAttribute.split(" = ")[1]).trim());
                                                                setDependentCondition(prefilledAttribute.split(" = ")[1], "=", valueJson, testData);
                                                                verifyData(testContext.getMapTestData().get(prefilledAttribute.split(" = ")[0]).trim(), prefilledAttribute.split(" = ")[0], condition, result, testData, "", distinctRule);
                                                            }
                                                        }
                                                    } else if (Pattern.compile("(\\d+\\.\\s*)?(Default|Placeholder) = (.*)\\.?").matcher(distinctRule).find()) {
                                                        listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(.*?) = (.*)\\.?", "", distinctRule);
                                                        requiredFirstAttribute = listConditions.get(1);
                                                        requiredAttributeValue = listConditions.get(2);
                                                        key = values = "";
                                                        mapConditions.clear();
                                                        listConditionkeys = "";

                                                        if (valueJson.contains("DisplayRules"))
                                                            setCombinationConditions(valueJson, "(.*?) (=|<>) (.*)");

                                                        if (combinationConditions.isEmpty()) {   //Display rules is not available for this field
                                                            if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                                                                moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                                                                if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                                    if (requiredFirstAttribute.equalsIgnoreCase("Placeholder"))
                                                                        handlePlaceholderRules(valueJson, "", "", field, requiredAttributeValue, distinctRule);
                                                                    else {
                                                                        verifyData(valueJson, field, "", "", requiredAttributeValue, "", distinctRule);
                                                                        if (valueJson.contains("ValidationRules"))
                                                                            handleValidationRules(valueJson, "", "", field, order);
                                                                        handleSectionRules(valueJson, wizardControlType, section, order, field, distinctRule);
                                                                    }
                                                                } else
                                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field does not exists", true, "true", true, testContext);
                                                            } else
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, "true", true, testContext);
                                                        } else {
                                                            for (List<String> result : combinationConditions) {
                                                                for (String condition1 : result) {
                                                                    listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*:\\s*(.*?)(?:;|$)", "", condition1.trim());
                                                                    key = listFieldValueConditions.get(0).trim();
                                                                    values = listFieldValueConditions.get(1).trim();
                                                                    listConditionkeys = findKeyExistsJSON(key);
                                                                    if (!listConditionkeys.equalsIgnoreCase(""))
                                                                        break;
                                                                    setConditions1(key, valueJson, values, howManyOperator.get(key));
                                                                }
                                                                if (listConditionkeys.equalsIgnoreCase("")) {
                                                                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                                                                        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                                                                        if (requiredFirstAttribute.equalsIgnoreCase("Placeholder"))
                                                                            handlePlaceholderRules(valueJson, "", "", field, requiredAttributeValue, distinctRule);
                                                                        else {
                                                                            if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                                                switch (requiredAttributeValue.toLowerCase().trim()) {
                                                                                    case "blank":
                                                                                    case "united states":
                                                                                        verifyData(valueJson, field, key, values, requiredAttributeValue, "", distinctRule);
                                                                                        break;
                                                                                    case "unselected":
                                                                                    case "unchecked":
                                                                                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                                                                                            expectedFlag = element.getAttribute("aria-checked").equalsIgnoreCase("false");
                                                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" " + requiredAttributeValue.toLowerCase().trim() + " by default when " + key + " is " + values, expectedFlag, "true", expectedFlag, testContext);
                                                                                        }
                                                                                        break;
                                                                                    default:
                                                                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order,moduleName, field, distinctRule, "No matching case for Default values", testContext);
                                                                                }
                                                                                if (valueJson.contains("ValidationRules"))
                                                                                    handleValidationRules(valueJson, key, values, field, order);
                                                                                handleSectionRules(valueJson, wizardControlType, section, order, field, distinctRule);
                                                                            } else
                                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field does not exists when " + condition + " is " + result, true, "true", true, testContext);
                                                                        }
                                                                    } else
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + condition + " is " + result, true, "true", true, testContext);
                                                                } else
                                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                                                            }
                                                            combinationConditions.clear();
                                                            howManyOperator.clear();
                                                        }
                                                    } else if (Pattern.compile("(\\d+\\.\\s*)?(Age is calculated on age last birth date|Always enabled|Optional)\\.?").matcher(distinctRule).find()) {
                                                        listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(Age is calculated on age last birth date|Always enabled|Optional)\\.?", "", distinctRule);
                                                        requiredAttributeValue = listConditions.get(1);
                                                        mapConditions.clear();
                                                        key = values = "";
                                                        listConditionkeys = "";

                                                        if (valueJson.contains("DisplayRules"))
                                                            setCombinationConditions(valueJson, "([^\\s]+)\\s* (=|<>) (.*)");

                                                        switch (requiredAttributeValue) {
                                                            case "Age is calculated on age last birth date":
                                                                for (List<String> result : combinationConditions) {
                                                                    for (String condition1 : result) {
                                                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                                                                        key = listFieldValueConditions.get(0).trim();
                                                                        values = listFieldValueConditions.get(1).trim();
                                                                        listConditionkeys = findKeyExistsJSON(key);
                                                                        if (!listConditionkeys.equalsIgnoreCase(""))
                                                                            break;
                                                                        setConditions1(key, valueJson, values, "=");
                                                                    }
                                                                    if (listConditionkeys.equalsIgnoreCase("")) {
                                                                        if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim()))
                                                                            moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                                                                        LocalDate birthDatePastMonth = todaysDate.minusYears(25).plusMonths(-1);
                                                                        LocalDate birthDateFutureMonth = todaysDate.minusYears(25).plusMonths(1);
                                                                        sendKeys(driver, getElement(valueJson, wizardControlType, null), birthDatePastMonth.format(format));
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Age when " + key + " is " + values + " and birth date is " + birthDatePastMonth.format(formatWithSlash), "25", String.valueOf(calculateAge(birthDatePastMonth, todaysDate)), String.valueOf(calculateAge(birthDatePastMonth, todaysDate)).equalsIgnoreCase("25"), testContext);
                                                                        sendKeys(driver, getElement(valueJson, wizardControlType, null), birthDateFutureMonth.format(format));
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Age when " + key + " is " + values + " and birth date is " + birthDateFutureMonth.format(formatWithSlash), "24", String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)), String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)).equalsIgnoreCase("24"), testContext);
                                                                    } else
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                                                                }
                                                                combinationConditions.clear();
                                                                howManyOperator.clear();
                                                                break;
                                                            case "Always enabled":
                                                                expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field is always enabled", expectedFlag, "true", expectedFlag, testContext);
                                                                break;
                                                            case "Optional":
                                                                String error = clickRedBubble(valueJson);
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Optional Field Validation", error, "", error.equalsIgnoreCase(""), testContext);
                                                                break;
                                                        }
                                                    } else {
                                                        System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order,moduleName, field, distinctRule, "Rule does not match any criteria for field", testContext);
                                                    }
                                                } else
                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Key " + invalidTag + " not a valid tag", false, "true", false, testContext);
                                            } else
                                                onSoftAssertionHandlerPage.assertSkippedRules(driver, order,moduleName, field, distinctRule, reason, testContext);
                                        }
                                        break;
                                    case "Length":
                                        invalidTag = new ArrayList<>();
                                        if (valueJson.contains("DisplayRules"))
                                            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim());
                                        if (invalidTag.isEmpty()) {
                                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                                getAttributeValue(field, valueJson, order, wizardControlType, rule, "maxLength", "Length");
                                        } else
                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, "Length", "Key " + invalidTag + " not a valid tag", false, "true", false, testContext);
                                        break;
                                    case "Format":
                                        invalidTag = new ArrayList<>();
                                        if (valueJson.contains("DisplayRules"))
                                            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim());
                                        if (invalidTag.isEmpty()) {
                                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                                getAttributeValue(field, valueJson, order, wizardControlType, rule, "mask", "Format");
                                        } else
                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, "Length", "Key " + invalidTag + " not a valid tag", false, "true", false, testContext);
                                        break;
                                }
                            } catch (PathNotFoundException e) {
                                System.out.println("Field " + field + " does not have rule \"" + rule + "\"");
                            }
                        }
                    } else {
                        skippedInvalidElements.add(field);
                        System.out.println("The skipped invalid elements" + skippedInvalidElements);
                    }
                } else {
                    skippedInvalidElements.add(field);
                    onSoftAssertionHandlerPage.assertSkippedElement(driver, order,moduleName, field, "Either field is Lookup, hide for day 1 or has No Tag", testContext);
                }
            }
            printFinalResults();
            workbook.close();
            fileInputStream.close();
        } catch (IOException e) {
            System.out.println(excelFileName + " could not be opened due to some reason");
        }
    }

    public String findKeyExistsJSON(String condition) {
        if (!testContext.getMapTestData().containsKey(condition))
            return condition;
        return "";
    }

    public void setConditions(String condition, String valueJson, String result, String conditionAnother, String expectedResultAnother, String expectedOperator, String expectedOperatorAnother) {
        setDependentCondition(condition, expectedOperator, valueJson, result);
        if (!conditionAnother.isEmpty())
            setDependentCondition(conditionAnother, expectedOperatorAnother, valueJson, expectedResultAnother);
    }

    public void setConditions1(String condition, String valueJson, String result, String expectedOperator) {
        setDependentCondition(condition, expectedOperator, valueJson, result);
    }

    public void setVisibilityRules(String requiredAttribute, String valueJson, String wizardControlType, String order, String field, String secondAttribute, String distinctRule, List<String> result) {
        boolean expectedFlag;
        WebElement elem;
        String displayedText = " when ";

        for (String key : result)
            displayedText += key.split(":")[0].trim() + " is " + key.split(":")[1].trim() + " and ";
        if (displayedText.trim().endsWith("and"))
            displayedText = displayedText.substring(0, displayedText.length() - 4);

        if (Pattern.compile("SET ([^\\s]+)\\s* = (.*)").matcher(requiredAttribute).find()) {
            List<String> listConditions = getDisplayRuleConditions(valueJson, "SET ([^\\s]+)\\s* = (.*)", "", requiredAttribute);
            String conditionFirst = listConditions.get(0);
            String expectedResultFirst = listConditions.get(1);
            if (testContext.getMapTestData().containsKey(expectedResultFirst)) {
                String testData = setTestData(testContext.getMapTestData().get(expectedResultFirst).trim());
                if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                    moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                    if (!getElements(valueJson, wizardControlType).isEmpty()) {
                        setDependentCondition(expectedResultFirst, "=", valueJson, testData);
                        verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, result.get(0).split(":")[0].trim(), result.get(0).split(":")[1].trim(), prefilledValue, testData, distinctRule);
                        setDependentCondition(expectedResultFirst, "=", valueJson, "");
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field does not exists", true, "true", true, testContext);
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, "true", true, testContext);
            } else {
                if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                    if (!getElements(valueJson, wizardControlType).isEmpty()) {
                        verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, result.get(0).split(":")[0].trim(), result.get(0).split(":")[1].trim(), expectedResultFirst, "", distinctRule);
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field does not exists", true, "true", true, testContext);
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, "true", true, testContext);
            }
        } else {
            switch (requiredAttribute.toLowerCase()) {
                case "show":
                    expectedFlag = !getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field is shown" + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "enable":
                    if (!(wizardControlType.equals("Radio Button"))) {
                        expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Field is enabled " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    } else {
                        for (WebElement element : getElements(valueJson, wizardControlType))
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Radio button " + element.getAttribute("title") + " is enabled " + displayedText, element.isEnabled(), "true", element.isEnabled(), testContext);
                    }
                    break;
                case "disable":
                    if (!secondAttribute.equalsIgnoreCase("hide")) {
                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                            expectedFlag = element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput");
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" disabled " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                        }
                    }
                    break;
                case "set to no":
                    elem = getElement(valueJson, "radioField", "No");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Radio button No is selected" + displayedText, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to yes":
                    elem = getElement(valueJson, "radioField", "Yes");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Radio button Yes is selected " + displayedText, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to self":
                case "set to united states":
                    verifyData(valueJson, field, "", "", getDisplayRuleConditions(valueJson, "set to (.*)", "", requiredAttribute.toLowerCase()).get(0), "", distinctRule);
                    break;
//                case "prefilled with":
//                    if (valueJson.contains("\"Format\"")) {
//                        String format = JsonPath.read(valueJson, "$.Format").toString().trim().replaceAll("[a-zA-Z]", "#");
//                        MaskFormatter formatter;
//                        try {
//                            formatter = new MaskFormatter(format);
//                            formatter.setValueContainsLiteralCharacters(false);
//                            prefilledValue = formatter.valueToString(prefilledValue);
//                        } catch (ParseException ignored) {
//                        }
//                    }
//                    verifyData(valueJson, field, result.get(0).split(":")[0].trim(), result.get(0).split(":")[1].trim(), prefilledValue, requiredAttribute, distinctRule);
//                    setDependentCondition(dependentPrefilledCondition, "=", valueJson, "");
//                    break;
                case "hide":
                    expectedFlag = getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Hidden Rule when " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "read only":
                    for (WebElement element : getElements(valueJson, wizardControlType)) {
                        expectedFlag = (element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput"));
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" read only" + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    }
                    break;
                default:
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order,moduleName, field, distinctRule, "Rule does not match any visibility condition", testContext);
            }
        }
    }

    public void setCombinationConditions(String valueJson, String pattern) {
        List<String> listFieldValueConditions;
        String key = "";
        String[] conditionValues;
        String expectedOperator = "";

        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys = new ArrayList<>();
        for (String eachCondition : JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(("AND"))) {
            listFieldValueConditions = getDisplayRuleConditions(valueJson, pattern, "", eachCondition.trim());
            key = listFieldValueConditions.get(0).trim();
            expectedOperator = listFieldValueConditions.get(1).trim();
            conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
            howManyOperator.put(key, expectedOperator);
            // Add the key-value pairs to the map
            for (String value : conditionValues) {
                List<String> valuesList = mapConditions.getOrDefault(key, new ArrayList<>());
                valuesList.add(value.trim());
                mapConditions.put(key, valuesList);
            }
        }
        allKeys = new ArrayList<>(mapConditions.keySet());
        generateCombinations(allKeys, new ArrayList<>(), mapConditions);
    }

    public void handleSectionRules(String valueJson, String wizardControlType, String section, String order, String field, String distinctRule) {
        boolean expectedFlag = getElementSection(valueJson, wizardControlType, section);
        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, "Section Information", "Field is displayed under section " + section, expectedFlag, "true", expectedFlag, testContext);
    }

    public int calculateAge(LocalDate dob, LocalDate currentDate) {
        return Period.between(dob, currentDate).getYears();
    }

    public void verifyOptions(String valueJson, String field, List<String> expectedOptions, String condition, String result, String conditionAnother, String expectedResultAnother, String distinctRule) {
        String dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
        List<String> actualOptions = getOptions(valueJson, dataType);
        if (condition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, dataType + " Options", actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
        else if (conditionAnother.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, dataType + " Options when " + condition + " is " + result, actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, dataType + " Options when " + condition + " is " + result + " and " + conditionAnother + " is " + expectedResultAnother, actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
    }

    public void setDependentCondition(String condition, String expectedOperator, String valueJson, String result) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        if (verifyPage(JsonPath.read(valueDependentJson, "$.Page").toString().trim(), JsonPath.read(valueDependentJson, "$.ModuleSectionName").toString().trim())) {
            moveToPage(JsonPath.read(valueDependentJson, "$.Page").toString().trim(), JsonPath.read(valueDependentJson, "$.ModuleSectionName").toString().trim());
            if (expectedOperator.equalsIgnoreCase("=")) {
                setValue(valueDependentJson, result);
            } else if (expectedOperator.equalsIgnoreCase("<>")) {
                new Select(getElement(valueDependentJson, "dropdown", null)).selectByIndex(1);
            }
            waitForPageToLoad(driver);
            sleepInMilliSeconds(2000);
        } else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueDependentJson, "$.Order").toString().trim(),moduleName, JsonPath.read(valueDependentJson, "$.CommonTag").toString().trim(), "", "Page " + JsonPath.read(valueDependentJson, "$.Page").toString().trim() + " does not exists when " + condition + " is " + result, true, false, false, testContext);
    }

    public void setValue(String valueDependentJson, String result) {
        waitForPageToLoad(driver);
        if (result.equalsIgnoreCase("Blank")) {
            result = "";
        }
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

    public void resetValue(String valueDependentJson, String result) {
        waitForPageToLoad(driver);
        if(result.equalsIgnoreCase("Blank"))
        {
            result = "";
        }
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                new Select(getElement(valueDependentJson, "dropdown", null)).selectByVisibleText(result);
                syncElement(driver, getElement(valueDependentJson, "dropdown", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "checkbox":
                checkBoxSelectYesNO("uncheck", getElement(valueDependentJson, "checkbox", null));
                syncElement(driver, getElement(valueDependentJson, "checkbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "radio button":
                checkBoxSelectYesNO("unselected", getElement(valueDependentJson, "radioField", result));
                syncElement(driver, getElement(valueDependentJson, "radioField", result), EnumsCommon.TOCLICKABLE.getText());
                break;
            default:
                sendKeys(driver, getElement(valueDependentJson, "single line textbox", null), "");
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
        waitForPageToLoad(driver);
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
        waitForPageToLoad(driver);
    }

    public boolean verifyPage(String pageHeader, String formHeader) {
        boolean flag = false;
        waitForPageToLoad(driver);
        if (!onCommonMethodsPage.getList_WizardPageNameExpand().isEmpty())
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
        List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
        for (WebElement element : mandetoryFormList) {
            String form = element.getAttribute("innerText");
            if (form.equals(pageHeader)) {
                flag = true;
                break;
            }
        }
        if (!onCommonMethodsPage.getList_WizardPageNameCollapse().isEmpty())
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameCollapse());
        return flag;
    }

    public List<WebElement> getElements(String valueJson, String datatype) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "radio button":
            case "checkbox":
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

    public boolean getElementSection(String valueJson, String datatype, String section) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return findElements(driver, String.format(onCommonMethodsPage.getSectionSelect(), section, commonTag)).size() > 0;
            case "checkbox":
            case "radio button":
                return findElements(driver, String.format(onCommonMethodsPage.getSectionRadio(), section, commonTag)).size() > 0;
            default:
                return findElements(driver, String.format(onCommonMethodsPage.getSectionInput(), section, commonTag)).size() > 0;
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
                    if (expectedText.equalsIgnoreCase(""))
                        expectedText = "blank";
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
                    if (expectedText.equalsIgnoreCase(""))
                        expectedText = "blank";
                    printResults(condition, valueJson, field, requiredAttributeValue, expectedText, result, attribute, distinctRule);
                    break;
            }
        } catch (NullPointerException e) {
            onSoftAssertionHandlerPage.assertNoElement(driver, field, condition + " = " + result, testContext);
        }
    }

    public List<String> getInvalidTags(List<String> skippedInvalidElements, String valueJson) {
        return skippedInvalidElements.stream().filter(valueJson::contains).collect(Collectors.toList());
    }

    public void printResults(String condition, String valueJson, String field, String requiredAttributeValue, String expectedText, String result, String attribute, String distinctRule) {
        if (condition.isEmpty()) {
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Default Value ", requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim()), testContext);
        } else if (attribute.equalsIgnoreCase("prefilled with"))
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Prefilled Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim()), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Default Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim()), testContext);
    }

    public void getAttributeValue(String field, String valueJson, String order, String wizardControlType, String rule, String attribute, String distinctRule) {
        List<String> listConditions;
        String dependentCondition = "";
        String dependentResult = "";
        String conditionAnother = "";
        String expectedResultAnother = "";
        List<String> listFieldValueConditions = new ArrayList<>();
        String key = "";
        String[] conditionValues;
        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys = new ArrayList<>();
        String values = "";
        String listConditionkeys = "";
        String displayedText = "";


        if (!valueJson.contains("DisplayRules")) {
            if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                if (!getElements(valueJson, wizardControlType).isEmpty())
                    getLength(valueJson, attribute, rule, field, distinctRule, null);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, rule + " Validations -> Field does not exists", true, "true", true, testContext);
            } else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, false, false, testContext);
        } else {
            setCombinationConditions(valueJson, "([^\\s]+)\\s* (=|<>) (.*)");

            for (List<String> result : combinationConditions) {
                displayedText = " when ";
                for (String condition1 : result) {
                    listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: \\s*(.*?)(?:;|$)", "", condition1.trim());
                    key = listFieldValueConditions.get(0).trim();
                    values = listFieldValueConditions.get(1).trim();
                    listConditionkeys = findKeyExistsJSON(key);
                    displayedText += key + " is " + values + " and ";
                    if (!listConditionkeys.equalsIgnoreCase(""))
                        break;
                    setConditions1(key, valueJson, values, "=");
                }
                if (listConditionkeys.equalsIgnoreCase("")) {
                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                        if (!getElements(valueJson, wizardControlType).isEmpty())
                            getLength(valueJson, attribute, rule, field, distinctRule, displayedText);
                        else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, rule + " Validations -> Field does not exists", true, "true", true, testContext);
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, false, false, testContext);
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
            }
            combinationConditions.clear();
            howManyOperator.clear();
        }
    }

    public void getLength(String valueJson, String attribute, String rule, String field, String distinctRule, String displayedText) {
        boolean expectedFlag;

        if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().equalsIgnoreCase("email")) {
            sendKeys(driver, getElement(valueJson, "input", null), JsonPath.read(valueJson, "$.Format").toString());
            expectedFlag = findElements(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Format of " + field + " is " + JsonPath.read(valueJson, "$.Format").toString(), expectedFlag, "true", expectedFlag, testContext);
        } else {
            try {
                String expectedText = getElement(valueJson, "input", null).getAttribute(attribute);

                if (expectedText.equals("99/99/9999"))
                    expectedText = "MM/dd/yyyy";
                if (rule.equalsIgnoreCase("format"))
                    expectedText = expectedText.replaceAll("9", "#");

                if (combinationConditions.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + displayedText, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);

                handleTextLengthFields(valueJson, rule, field, distinctRule, displayedText, combinationConditions);
            } catch (NullPointerException e) {
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Field does not have attribute " + attribute, false, true, false, testContext);
            }
        }
    }

    public void handleTextLengthFields(String valueJson, String rule, String field, String distinctRule, String displayedText, List<List<String>> combinationConditions) {
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
                        if (error.isEmpty() & expectedText.isEmpty()) {
                            if (combinationConditions.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations when length is " + length, "Not a mandatory field or value less than given length", temp, true, testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, "Not a mandatory field or value less than given length", temp, true, testContext);
                        } else if (error.isEmpty()) {
                            if (combinationConditions.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations when length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        } else {
                            if (combinationConditions.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations when length is " + length, error, error, true, testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, error, error, true, testContext);
                        }
                    } else {
                        if (combinationConditions.isEmpty())
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations when length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                    }
                }
                setValue(valueJson, "");
            } else {
                temp = RandomStringUtils.random(attributeValue, allowedChars);
                setValue(valueJson, temp);
                expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                expectedFormat = getElement(valueJson, "single line textbox", null).getAttribute("mask");
                if (combinationConditions.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + "validations when length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePlaceholderRules(String valueJson, String dependentCondition, String dependentResult, String field, String requiredAttributeValue, String distinctRule) {
        String expectedAttribute = getElement(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes"), "").getAttribute("placeholder");
        if (dependentCondition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Placeholder value", expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Placeholder value when " + dependentCondition + " is " + dependentResult, expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);

    }

    public void handleValidationRules(String valueJson, String dependentCondition, String dependentResult, String field, String order) {
        for (String distinctRule : JsonPath.read(valueJson, "$.ValidationRules").toString().trim().split((";"))) {
            distinctRule = distinctRule.replaceFirst("(\\d+\\.\\s*)?", "").trim();
            System.out.println(field + " -> " + distinctRule);
            String expectedResult;
            String condition;
            String expectedOperator = "";

            if(!dependentCondition.isEmpty())
                setConditions1(dependentCondition, valueJson, dependentResult, howManyOperator.get(dependentCondition));
            if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim()))
                moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());

            if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) results in an age that is less than (.*?) or greater than (.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) results in an age that is less than (.*?) or greater than (.*?),? then (.*?): (.*)", "", distinctRule);
                String minValue = listConditions.get(2);
                String maxValue = listConditions.get(3);
                String requiredErrorMessage = listConditions.get(5);
                String error;
                LocalDate dob = todaysDate.minusYears(Long.parseLong(minValue)).minusMonths(8);

                sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                error = clickRedBubble(valueJson);
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) > Integer.parseInt(minValue), testContext);

                dob = todaysDate.minusYears(Long.parseLong(maxValue) + 1).minusMonths(1);
                sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                error = clickRedBubble(valueJson);
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) < Integer.parseInt(maxValue), testContext);
            } else if (Pattern.compile("(\\d+\\.\\s*)?If ([^\\s]+)\\s* (.*?) (.*?) (?i)AND ([^\\s]+)\\s* (.*?) (.*?) (?i)AND ([^\\s]+)\\s* (.*?) (.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If ([^\\s]+)\\s* (.*?) (.*?) (?i)AND ([^\\s]+)\\s* (.*?) (.*?) (?i)AND ([^\\s]+)\\s* (.*?) (.*?),? then (.*?): (.*)", "", distinctRule);
                String requiredErrorMessage = listConditions.get(11);
                condition = listConditions.get(1);
                expectedOperator = listConditions.get(2);
                expectedResult = listConditions.get(3);
                String conditionAnother = listConditions.get(4);
                String expectedOperatorAnother = listConditions.get(5);
                String expectedResultAnother = listConditions.get(6);
                String conditionThird = listConditions.get(7);
                String expectedOperatorThird = listConditions.get(8);
                String expectedResultThird = listConditions.get(9);
                List<String> expectedRangeValues = new ArrayList<>();

                if (conditionAnother.equalsIgnoreCase(conditionThird)) {
                    expectedRangeValues.add(String.valueOf(Integer.parseInt(expectedResultThird) - Integer.parseInt(expectedResultAnother) - 1));
                    expectedRangeValues.add(String.valueOf(Integer.parseInt(expectedResultThird)));
                    expectedRangeValues.add(String.valueOf(Integer.parseInt(expectedResultAnother) + 1));
                    expectedOperatorAnother = "<";
                }
                for (String result : expectedResult.split(", ")) {
                    setConditions(condition, valueJson, result, conditionAnother, expectedResultAnother, expectedOperator, expectedOperatorAnother);
                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                        if (expectedRangeValues.isEmpty())
                            handleErrorMessage(expectedOperatorAnother, expectedResultAnother, valueJson, requiredErrorMessage, field, condition, result, distinctRule, order);
                        else {
                            for (String expectedValue : expectedRangeValues)
                                handleErrorMessage("=", expectedValue, valueJson, requiredErrorMessage, field, condition, result, distinctRule, order);
                        }
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order").toString().trim(),moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + condition + " is " + result, true, false, false, testContext);
                }
            } else if (Pattern.compile("(\\d+\\.\\s*)?If ([^\\s]+)\\s* (=|>|<) (.*?) (?i)AND ([^\\s]+)\\s* (.*?) (.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If ([^\\s]+)\\s* (=|>|<) (.*?) (?i)AND (.*?) (.*?) (.*?),? then (.*?): (.*)", "", distinctRule);
                String requiredErrorMessage = listConditions.get(8);
                condition = listConditions.get(1);
                expectedOperator = listConditions.get(2);
                expectedResult = listConditions.get(3);
                String conditionAnother = listConditions.get(4);
                String expectedOperatorAnother = listConditions.get(5);
                String expectedResultAnother = listConditions.get(6);

                for (String result : expectedResult.split(", ")) {
                    setConditions(condition, valueJson, result, "", "", "=", "=");
                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                        handleErrorMessage(expectedOperatorAnother, expectedResultAnother, valueJson, requiredErrorMessage, field, condition, result, distinctRule, order);
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order").toString().trim(),moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + condition + " is " + result, true, false, false, testContext);
                }
            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) (<|=|>)(.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) (<|=|>)(.*?),? then (.*?): (.*)", "", distinctRule);
                condition = listConditions.get(1).trim();
                expectedResult = listConditions.get(3).trim();
                expectedOperator = listConditions.get(2).trim();
                String requiredErrorMessage = listConditions.get(5).trim();
                for (String result : expectedResult.split(", ")) {
                    handleErrorMessage(expectedOperator, result, valueJson, requiredErrorMessage, field, dependentCondition, dependentResult, distinctRule, order);
                }
            } else {
                System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order,moduleName, field, distinctRule, "Validation rules does not match any criteria", testContext);
            }
        }
    }

    public void handleErrorMessage(String expectedOperator, String expectedResult, String valueJson, String requiredErrorMessage, String field, String dependentCondition, String dependentResult, String distinctRule, String order) {
        String inputValue = "";
        List<String> dateFields = Arrays.asList("dob", "date", "mm/dd/yyyy");
        String error;
        List<String> dateCondition = new ArrayList<>();

        List<String> expectedValues = Arrays.asList("Invalid", "Yes","Husband", "Wife", "Spouse");
        if (expectedValues.stream().anyMatch(expectedResult::contains)) {
            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().toLowerCase()) {
                case "tin":
                    inputValue = testContext.getMapTestData().get("InvalidTin").trim();
                    break;
                case "ssn":
                    inputValue = testContext.getMapTestData().get("InvalidSSN").trim();
                    break;
                case "date":
                case "dob":
                case "mm/dd/yyyy":
                    inputValue = testContext.getMapTestData().get("InvalidDate").trim();
                    break;
                case "email":
                case "single line textbox":
                    inputValue = validateInvalidEmail(valueJson, field, requiredErrorMessage, distinctRule);
                    break;
                case "radio button":
                    inputValue = expectedResult;
                    break;
            }
            setValue(valueJson,inputValue);
            //sendKeys(driver, getElement(valueJson, "single line textbox", null), inputValue);
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
        } else if (dateFields.contains(JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase())) {
            if (expectedResult.toLowerCase().contains(" "))
                dateCondition = Arrays.asList(expectedResult.split(" "));

            switch (expectedOperator) {
                case "<":
                    inputValue = todaysDate.minusYears(Long.parseLong(expectedResult)).plusDays(1).format(format);
                    break;
                case "=":
                    if(!expectedResult.equalsIgnoreCase("blank"))
                        inputValue = todaysDate.minusYears(Long.parseLong(expectedResult)).format(format);
                    break;
                case ">":
                    if (!dateCondition.isEmpty() && dateCondition.get(1).equalsIgnoreCase("months")) {
                        inputValue = todaysDate.minusMonths(Long.parseLong(dateCondition.get(0))).minusDays(1).format(format);
                        expectedResult = "current date";
                    } else
                        inputValue = todaysDate.minusYears(Long.parseLong(expectedResult) + 1).minusDays(1).format(format);
                    break;
            }
            sendKeys(driver, getElement(valueJson, "single line textbox", null), inputValue);
        }


        error = clickRedBubble(valueJson);
        if (dateFields.stream().anyMatch(JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()::contains)) {
            if (dependentCondition.isEmpty())
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> " + " Value Validation when " + field + " is " + inputValue + " and age is " + expectedResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> " + " Value Validation when " + field + " is " + inputValue + " and " + dependentCondition + " is " + dependentResult + " and age " + expectedOperator + " " + expectedResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            sendKeys(driver, getElement(valueJson, "single line textbox", null), "");
        } else {
            switch (expectedResult.toLowerCase()) {
                case "blank":
                    if (dependentCondition.isEmpty())
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> Mandatory Field Validation" + field + " is " + expectedResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                    else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> Mandatory Field Validation when " + dependentCondition + " is " + dependentResult + " and " + field + " is " + expectedResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                    break;
                case "invalid":
                case "current date":
                case "yes":
                case "husband":
                case "wife":
                case "spouse":
                    if (dependentCondition.isEmpty())
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> Invalid Value Validation when " + field + " is " + inputValue, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                    else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, "Validation Rule -> Invalid Value Validation when " + field + " is " + inputValue + " and " + dependentCondition + " is " + dependentResult, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
                    if(JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Radio Button"))
                    {
                        resetValue(valueJson,inputValue);
                    }
                    else
                        resetValue(valueJson,"");
                    break;
                default:
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order,moduleName, field, distinctRule, "Validation rule does not have required condition", testContext);
            }
        }
    }

    public String clickRedBubble(String valueJson) {
        waitForPageToLoad(driver);
        String error;
        waitForPageToLoad(driver);
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
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, field, distinctRule, validationMessage, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
        }
        return lastInvalidEmail;
    }

    public void printFinalResults() {
        endTime = System.currentTimeMillis();
        endLocalTime = LocalTime.now();
        long durationMillis = endTime - onLoginPage.getStartTime();
        long hours = durationMillis / (1000 * 60 * 60);
        long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = ((durationMillis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        difference = String.format("%dh %dm %ds", hours, minutes, seconds);
        testContext.getScenario().write("<div width='100%' style='font-size:1.6vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>Cucumber Report : " + LocalDate.now() + "</div>");
        testContext.getScenario().write("<div width='100%' style='font-size:1.2vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>" + timeFormat.format(onLoginPage.getStartLocalTime()) + " - " + timeFormat.format(endLocalTime) + "(" + difference + ")</div>");
        onSoftAssertionHandlerPage.afterScenario(testContext);
    }

    private void generateCombinations(List<String> keys, List<String> combination, Map<String, List<String>> keyValuesMap) {
        if (keys.isEmpty()) {
            // Print the combination
            combinationConditions.add(combination);
            return;
        }
        String currentKey = keys.get(0);
        List<String> currentValues = keyValuesMap.get(currentKey);
        for (String value : currentValues) {
            // Add the current key-value pair to the combination
            List<String> newCombination = new ArrayList<>(combination);
            newCombination.add(currentKey + ": " + value);
            // Recur with the remaining keys
            generateCombinations(keys.subList(1, keys.size()), newCombination, keyValuesMap);
        }
    }

    public boolean verifyElementExists(String valueJson, List<String> skippedInvalidElements, String order, String field) {
        List<String> listFieldValueConditions;
        String key = "";
        String[] conditionValues;
        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys = new ArrayList<>();
        String listConditionkeys = "";
        String values = "";
        List<String> invalidTag = new ArrayList<>();

        if (valueJson.contains("DisplayRules")) {
            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim());
            setCombinationConditions(valueJson, "([^\\s]+)\\s* (=|<>) (.*)");
        }

        if (invalidTag.isEmpty()) {
            if (combinationConditions.isEmpty()) {
                if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                    moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                    if (getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).size() > 0)
                        return getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).size() > 0;
                    else
                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order,moduleName, field, "Either Wizard Control type or Common tag is incorrect", testContext);
                } else {
                    onSoftAssertionHandlerPage.assertSkippedElement(driver, order,moduleName, field, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + key + " is " + values, testContext);
                }
            } else {
                List<String> result = combinationConditions.get(0);
                for (String condition1 : result) {
                    listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                    key = listFieldValueConditions.get(0).trim();
                    values = listFieldValueConditions.get(1).trim();
                    listConditionkeys = findKeyExistsJSON(key);
                    if (!listConditionkeys.equalsIgnoreCase(""))
                        break;
                    setConditions1(key, valueJson, values, howManyOperator.get(key));
                }
                if (listConditionkeys.equalsIgnoreCase("")) {
                    if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
                        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                        if (getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).size() > 0)
                            return getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).size() > 0;
                        else
                            onSoftAssertionHandlerPage.assertSkippedElement(driver, order,moduleName, field, "Either Wizard Control type or Common tag is incorrect", testContext);
                    } else {
                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order,moduleName, field, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists when " + key + " is " + values, testContext);
                    }
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"),moduleName, JsonPath.read(valueJson, "$.CommonTag"), "", "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                combinationConditions.clear();
                howManyOperator.clear();
            }
        } else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order,moduleName, field, "ListOptions", "Key " + invalidTag + " not a valid tag", false, "true", false, testContext);
        return false;
    }
}






