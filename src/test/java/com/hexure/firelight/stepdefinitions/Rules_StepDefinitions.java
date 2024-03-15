package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.SoftAssertionHandlerPage;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import cucumber.api.java.en.Given;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.hexure.firelight.libraies.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Given("User validate wizard fields for workbook {string}")
    public void verify_form_data_with_inbound_XML_from_Excel_and_Xml(String excelFileName) throws IOException, InterruptedException {
        String excelFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFileName;
        String sheetName = "E-App Wizard Spec";
        String field = "";
        String section = "";
        FileInputStream fileInputStream = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet(sheetName);
        Iterator<Row> iterator = sheet.iterator();
        String condition = "";
        String expectedResult = "";
        String conditionElse = "";
        String expectedResultElse = "";
        String requiredFirstAttribute = "";
        String requiredSecondAttribute = "";
        String requiredFirstAttributeElse = "";
        String requiredSecondAttributeElse = "";
        String requiredAttributeValue = "";
        String dependentPrefilledCondition = "";
        boolean expectedFlag = false;
        List<String> actualOptions = new ArrayList<>();
        List<String> expectedOptions = new ArrayList<>();
        List<WebElement> radioOptions = new ArrayList<>();
        List<String> listConditions = new ArrayList<>();
        List<String> listFieldValueConditions = new ArrayList<>();
        String dataType = "";

        // Assuming the fifth row contains headers
        Row headerRow = iterator.next();

        int fieldColumnIndex = findColumnIndex(headerRow, EnumsCommon.FIELD.getText());
        int sectionColumnIndex = findColumnIndex(headerRow, EnumsCommon.SECTION.getText());
        List<String> rulesList = Arrays.asList("ListOptions", "ValidationRules", "RulesWizard", "Length", "Format");
        for (int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++) {
            field = getExcelColumnValue(excelFilePath, sheetName, rowIndex + 1, fieldColumnIndex);
            section = getExcelColumnValue(excelFilePath, sheetName, rowIndex + 1, sectionColumnIndex);
            String valueJson = testContext.getMapTestData().get(field).trim();
            expectedResult = "";

            moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());

            for (String rule : rulesList) {
                try {
                    switch (rule) {
                        case "ListOptions":
                            String options = JsonPath.read(valueJson, "$.ListOptions").toString().trim();
                            if (options.contains(";"))
                                expectedOptions = Arrays.asList(options.split(";"));
                            else
                                expectedOptions = Arrays.asList(testContext.getMapTestData().get(options.replaceAll(" ", "")).split(", "));
                            dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
                            actualOptions = getOptions(valueJson, dataType);
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, dataType + " Options", actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
                            break;
                        case "RulesWizard":
                            for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split((";"))) {
                                if (Pattern.compile("[(.*?).]* If (.*?) = (.*?), then (.*?) and (.*?), else if (.*?) = (.*?), then (.*?) and (.*)\\.").matcher(distinctRule).find()) {
                                    listConditions = getDisplayRuleConditions(valueJson, "[(.*?).]* If (.*?) = (.*?), then (.*?) and (.*?), else if (.*?) = (.*?), then (.*?) and (.*)\\.", "", distinctRule);
                                    condition = listConditions.get(0);
                                    expectedResult = listConditions.get(1);
                                    requiredFirstAttribute = listConditions.get(2);
                                    requiredSecondAttribute = listConditions.get(3);
                                    conditionElse = listConditions.get(4);
                                    expectedResultElse = listConditions.get(5);
                                    requiredFirstAttributeElse = listConditions.get(6);
                                    requiredSecondAttributeElse = listConditions.get(7);

                                    for (String result : expectedResult.split(", ")) {
                                        setDependentCondition(condition, valueJson, result);
                                        if (requiredFirstAttribute.equalsIgnoreCase("display")) {
                                            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim()) {
                                                case "Single Line Textbox":
                                                case "DOB":
                                                    expectedFlag = !getElements(valueJson, "input").isEmpty();
                                                    break;
                                                case "Radio Button":
                                                    expectedFlag = !getElements(valueJson, "radiobutton").isEmpty();
                                                    break;
                                            }
                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Field is displayed when " + condition + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                        }
                                        if (requiredSecondAttribute.equalsIgnoreCase("enable")) {
                                            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim()) {
                                                case "Single Line Textbox":
                                                case "DOB":
                                                    expectedFlag = getElement(valueJson, "input", null).isEnabled();
                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Field is enabled when " + condition + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                                    break;
                                                case "Radio Button":
                                                    radioOptions = getElements(valueJson, "radiobutton");
                                                    for (WebElement element : radioOptions)
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Radio button " + element.getAttribute("title") + " is enabled when " + condition + " is " + result, element.isEnabled(), "true", element.isEnabled(), testContext);
                                                    break;
                                            }
                                        }
                                    }
                                    for (String result : expectedResultElse.split(", ")) {
                                        setDependentCondition(conditionElse, valueJson, result);
                                        if(requiredFirstAttributeElse.contains("prefilled with")) {
                                            listConditions = getDisplayRuleConditions(valueJson, "prefilled with (.*)", "", requiredFirstAttributeElse);
                                            requiredFirstAttributeElse = "prefilled with";
                                            dependentPrefilledCondition = listConditions.get(0);
                                        }
                                        switch (requiredFirstAttributeElse.toLowerCase()) {
                                            case "disable":
                                                if (!requiredSecondAttributeElse.equalsIgnoreCase("hide")) {
                                                    radioOptions = getElements(valueJson, "radiobutton");
                                                    for (WebElement element : radioOptions)
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Radio button \"" + element.getAttribute("title") + "\" disabled when " + condition + " is " + result, element.getAttribute("aria-checked"), "false", element.getAttribute("class").contains("disabled"), testContext);
                                                }
                                                break;
                                            case "set to no":
                                                WebElement element = getElement(valueJson, "radiofield", "No");
                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Radio button No is selected when " + conditionElse + " is " + result, element.getAttribute("aria-checked"), "true", element.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                                                break;
                                            case "prefilled with":
                                                setDependentCondition(dependentPrefilledCondition, valueJson, "TestValue");
                                                verifyData(valueJson, field, conditionElse, result, "TestValue");
                                                break;
                                        }
                                        switch (requiredSecondAttributeElse.toLowerCase()) {
                                            case "hide":
                                                expectedFlag = getElements(valueJson, "input").isEmpty();
                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Hidden Rule when " + conditionElse + " is " + result, "true", expectedFlag, expectedFlag, testContext);
                                                break;
                                            case "read only":
                                                radioOptions = getElements(valueJson, "radiobutton");
                                                for (WebElement element : radioOptions) {
                                                    expectedFlag = (element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput"));
                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Radio button \"" + element.getAttribute("title") + "\" read only when " + condition + " is " + result, expectedFlag, "true", expectedFlag, testContext);
                                                }
                                                break;
                                        }
                                    }
                                } else if (Pattern.compile("[(.*?).]* If (.*?) = (.*?), then (.*?) = (.*?)\\.").matcher(distinctRule).find()) {
                                    listConditions = getDisplayRuleConditions(valueJson, "[(.*?).]* If (.*?) = (.*?), then (.*?) = (.*?)\\.", "", distinctRule);
                                    condition = listConditions.get(0);
                                    expectedResult = listConditions.get(1);
                                    requiredFirstAttribute = listConditions.get(2);
                                    requiredSecondAttribute = listConditions.get(3);

                                    for (String result : expectedResult.split(", ")) {
                                        setDependentCondition(condition, valueJson, result);
                                        if (requiredFirstAttribute.equalsIgnoreCase("SHOW options")) {
                                            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim()) {
                                                case "Dropdown":
                                                    expectedOptions = Arrays.asList(requiredSecondAttribute.split(", "));
                                                    dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
                                                    actualOptions = getOptions(valueJson, dataType);
                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, dataType + " Options when " + condition + " is " + result, actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                                    break;
                                            }
                                        }
                                    }
                                } else if (Pattern.compile("[(.*?).]* (.*?) = (.*?)\\.").matcher(distinctRule).find()) {
                                    listConditions = getDisplayRuleConditions(valueJson, "[(.*?).]* (.*?) = (.*?)\\.", "", distinctRule);
                                    requiredAttributeValue = listConditions.get(1);

                                    if(valueJson.contains("FieldValues")) {
                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "FieldValues", "");
                                        condition = listFieldValueConditions.get(0);
                                        expectedResult = listFieldValueConditions.get(1);
                                    }
                                    if (expectedResult.isEmpty())
                                        verifyData(valueJson, field, "", "", requiredAttributeValue);
                                    else {
                                        for (String result : expectedResult.split(", ")) {
                                            setDependentCondition(condition, valueJson, result);
                                            switch (requiredAttributeValue.toLowerCase().trim()) {
                                                case "blank":
                                                    verifyData(valueJson, field, condition, result, requiredAttributeValue);
                                                    break;
                                                case "unselected":
                                                    radioOptions = getElements(valueJson, "radiobutton");
                                                    for (WebElement element : radioOptions)
                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Radio button \"" + element.getAttribute("title") + "\" unselected by default when " + condition + " is " + result, element.getAttribute("aria-checked"), "false", element.getAttribute("aria-checked").equalsIgnoreCase("false"), testContext);
                                                    break;
                                            }
                                            if (valueJson.contains("ValidationRules"))
                                                handleValidationRules(valueJson, condition, result, field);
                                        }
                                    }
                                } else if (Pattern.compile("[(.*?).]* (.*?)\\.").matcher(distinctRule).find()) {
                                    listConditions = getDisplayRuleConditions(valueJson, "[(.*?).]* (.*?)\\.", "", distinctRule);
                                    requiredAttributeValue = listConditions.get(0);
                                    if(valueJson.contains("FieldValues")) {
                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "FieldValues", "");
                                        condition = listFieldValueConditions.get(0);
                                        expectedResult = listFieldValueConditions.get(1);
                                    }

                                    switch (requiredAttributeValue) {
                                        case "Age is calculated on age last birth date":
                                            for (String result : expectedResult.split(", ")) {
                                                setDependentCondition(condition, valueJson, result);
                                                LocalDate birthDatePastMonth = todaysDate.minusYears(25).plusMonths(-1);
                                                LocalDate birthDateFutureMonth = todaysDate.minusYears(25).plusMonths(1);
                                                sendKeys(driver, getElement(valueJson, "input", null), birthDatePastMonth.format(format));
                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Age when " + condition + " is " + result + " and birth date is " + birthDatePastMonth.format(formatWithSlash), "25", String.valueOf(calculateAge(birthDatePastMonth, todaysDate)), String.valueOf(calculateAge(birthDatePastMonth, todaysDate)).equalsIgnoreCase("25"), testContext);
                                                sendKeys(driver, getElement(valueJson, "input", null), birthDateFutureMonth.format(format));
                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Age when " + condition + " is " + result + " and birth date is " + birthDateFutureMonth.format(formatWithSlash), "24", String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)), String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)).equalsIgnoreCase("24"), testContext);
                                            }
                                    }
                                }
                                else
                                    System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                            }
                            break;
                        case "Length":
                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                getAttributeValue(field, valueJson, "(.*?) = (.*)", "FieldValues", rule, "maxLength");
                            break;
                        case "Format":
                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                getAttributeValue(field, valueJson, "(.*?) = (.*)", "FieldValues", rule, "mask");
                            break;
                    }
                } catch (PathNotFoundException e) {
                    System.out.println("Field " + field + " does not have rule \"" + rule + "\"");
                }
            }
        }
        onSoftAssertionHandlerPage.afterScenario(testContext);
        workbook.close();
        fileInputStream.close();
    }


    public int calculateAge(LocalDate dob, LocalDate currentDate) {
        return Period.between(dob, currentDate).getYears();
    }

    public void setDependentCondition(String condition, String valueJson, String result) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        moveToPage(JsonPath.read(valueDependentJson, "$.Page").toString().trim(), JsonPath.read(valueDependentJson, "$.ModuleSectionName").toString().trim());
        setValue(valueDependentJson, result);
        sleepInMilliSeconds(1000);
        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
    }

    public void setValue(String valueDependentJson, String result) {
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
                new Select(getElement(valueDependentJson, "dropdown", null)).selectByVisibleText(result);
                break;
            case "single line textbox":
            case "dob":
                sendKeys(driver, getElement(valueDependentJson, "input", null), result);
                break;
            case "radio button":
                clickElement(driver, getElement(valueDependentJson, "radiofield", result));
                break;
        }
    }
    public void moveToPage(String pageHeader, String formHeader) {
        boolean flag = false;
        if (!(onCommonMethodsPage.getPageHeader().getText().equalsIgnoreCase(pageHeader) & onCommonMethodsPage.getFormHeader().getText().equalsIgnoreCase(formHeader))) {
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
            List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
            int i = 0;
            for (WebElement element : mandetoryFormList) {
                String form = element.getAttribute("innerText");
                if (form.equals(pageHeader)) {
                    element.click();
                    flag = true;
                    break;
                }
            }
            if(!flag)
                clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameCollapse());
        }
    }

    public List<WebElement> getElements(String valueJson, String datatype) {
        switch (datatype) {
            case "radiobutton":
                return findElements(driver, String.format(onCommonMethodsPage.getRadioFieldCheckbox(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
            case "input":
                return findElements(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
        }
        return null;
    }

    public WebElement getElement(String valueJson, String datatype, String optionalValue) {
        switch (datatype) {
            case "dropdown":
                return findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
            case "input":
                return findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
            case "radiofield":
                return findElement(driver, String.format(onCommonMethodsPage.getRadioFieldWithOption(), JsonPath.read(valueJson, "$.CommonTag").toString().trim(), optionalValue));
            case "errortype":
                switch (optionalValue.toLowerCase()) {
                    case "single line textbox":
                    case "tin":
                    case "ssn":
                    case "mm/dd/yyyy":
                    case "dob":
                        return findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
                    case "dropdown":
                        return findElement(driver, String.format(onCommonMethodsPage.getSelectErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
                }
        }
        return null;
    }

    public List<String> getOptions(String valueJson, String dataType) {
        List<String> actualOptions = new ArrayList<>();
        switch (dataType) {
            case "Dropdown":
                sleepInMilliSeconds(2000);
                List<WebElement> dropdownOptions = new Select(getElement(valueJson, "dropdown", null)).getOptions();
                for (WebElement element : dropdownOptions) {
                    if (element.getText().equalsIgnoreCase(""))
                        actualOptions.add("Blank");
                    else
                        actualOptions.add(element.getText());
                }
                break;
            case "Radio Button":
                List<WebElement> radioOptions = getElements(valueJson, "radiobutton");
                for (WebElement element : radioOptions) {
                    actualOptions.add(element.getAttribute("title"));
                }
                break;
        }
        return actualOptions;
    }

    public List<String> getDisplayRuleConditions(String valueJson, String requiredPattern, String parameter, String distinctRule) {
        Pattern pattern = Pattern.compile(requiredPattern);
        Matcher matcher = null;
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

    public void verifyData(String valueJson, String field, String condition, String result, String requiredAttributeValue) {
        String expectedText = "";
        switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
                expectedText = new Select(getElement(valueJson, "dropdown", null)).getFirstSelectedOption().getText().trim();
                printResults(condition, valueJson, field, requiredAttributeValue, expectedText, result);
                break;
            case "single line textbox":
            case "dob":
                expectedText = getElement(valueJson, "input", null).getAttribute("value");
                printResults(condition, valueJson, field, requiredAttributeValue, expectedText, result);
                break;
            case "radio button":
                List<WebElement> radioOptions = getElements(valueJson, "radiobutton");
                for (WebElement element : radioOptions) {
                    expectedText = element.getAttribute("aria-checked");
                    if(expectedText.equals("false"))
                        expectedText = "unchecked";
                    else
                        expectedText = "checked";
                    printResults(field, valueJson, field, requiredAttributeValue, expectedText, element.getAttribute("title"));
                }
                break;
        }
    }

    public void printResults(String condition, String valueJson, String field, String requiredAttributeValue, String expectedText, String result) {
        if(expectedText.equalsIgnoreCase(""))
            expectedText = "blank";
        if (condition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Default Value ", requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.equalsIgnoreCase(expectedText), testContext);
        else if (requiredAttributeValue.equals("TestValue"))
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Prefilled Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.equalsIgnoreCase(expectedText), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Default Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.equalsIgnoreCase(expectedText), testContext);
    }

    public void getAttributeValue(String field, String valueJson, String requiredPattern, String parameter, String rule, String attribute) {
        List<String> listConditions = getDisplayRuleConditions(valueJson, requiredPattern, parameter, "");
        String dependentCondition = listConditions.get(0);
        String dependentResult = listConditions.get(1);
        String expectedText;
        if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
            for (String result : dependentResult.split(", ")) {
                setDependentCondition(dependentCondition, valueJson, result);
                expectedText = getElement(valueJson, "input", null).getAttribute(attribute);
                if (expectedText.equals("99/99/9999"))
                    expectedText = "MM/dd/yyyy";
                if (rule.equalsIgnoreCase("format"))
                    expectedText = expectedText.replaceAll("9", "#");

                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, rule + " when " + dependentCondition + " is " + dependentResult, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
            }
        }
    }

    public void handleValidationRules(String valueJson, String dependentCondition, String dependentResult, String field) {
        for (String distinctRule : JsonPath.read(valueJson, "$.ValidationRules").toString().trim().split((";"))) {
            if (Pattern.compile("[(.*?).] If (.*?) results in an age that is less than (.*?) or greater than (.*?), then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "[(.*?).] If (.*?) results in an age that is less than (.*?) or greater than (.*?), then (.*?): (.*)", "", distinctRule);
                String minValue = listConditions.get(1);
                String maxValue = listConditions.get(2);
                String requiredErrorMessage = listConditions.get(4);
                String error = "";
                LocalDate dob = todaysDate.minusMonths(Long.parseLong(minValue) + 8);

                sendKeys(driver, getElement(valueJson, "input", null), dob.format(format));
                error = clickRedBubble(valueJson);
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) > 0, testContext);

                dob = todaysDate.minusYears(Long.parseLong(maxValue)).minusMonths(1);
                sendKeys(driver, getElement(valueJson, "input", null), dob.format(format));
                error = clickRedBubble(valueJson);
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) < 99, testContext);
            } else if (Pattern.compile("[(.*?).]* If (.*?) (.*?) (.*?), then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "[(.*?).]* If (.*?) (.*?) (.*?), then (.*?): (.*)", "", distinctRule);
                String expectedResult = listConditions.get(2);
                String expectedOperator = listConditions.get(1);
                String requiredErrorMessage = listConditions.get(4);
                handleErrorMessage(expectedOperator, expectedResult, valueJson, requiredErrorMessage, field, dependentCondition, dependentResult);
            } else if (Pattern.compile("If (.*?) = (.*?), then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "If (.*?) = (.*?), then (.*?): (.*)", "", distinctRule);
                String expectedResult = listConditions.get(1);
                String requiredErrorMessage = listConditions.get(3);
                handleErrorMessage("=", expectedResult, valueJson, requiredErrorMessage, field, dependentCondition, dependentResult);
            }
        }
    }

        public void handleErrorMessage(String expectedOperator, String expectedResult, String valueJson, String requiredErrorMessage, String field, String dependentCondition, String dependentResult) {
            String inputValue = "";
            String error = "";
            if (expectedResult.equalsIgnoreCase("Invalid")) {
                switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString()) {
                    case "TIN":
                        inputValue = testContext.getMapTestData().get("InvalidTin").trim();
                        break;
                }
                sendKeys(driver, getElement(valueJson, "input", null), inputValue);
            } else if (expectedResult.equalsIgnoreCase("current date")) {
                switch (expectedOperator) {
                    case "=":
                        inputValue = todaysDate.format(format);
                        break;
                    case ">":
                        inputValue = todaysDate.plusDays(1).format(format);
                        break;
                }
                sendKeys(driver, getElement(valueJson, "input", null), inputValue);
            }

            error = clickRedBubble(valueJson);

            switch (expectedResult.toLowerCase()) {
                case "blank":
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Mandatory Field Validation when " + dependentCondition + " is " + dependentResult, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
                    break;
                case "invalid":
                case "current date":
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), field, "Invalid Value Validation when " + field + " is " + inputValue + "and " + dependentCondition + " is " + dependentResult, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
                    sendKeys(driver, getElement(valueJson, "input", null), "");
                    break;
            }
        }

    public String clickRedBubble(String valueJson) {
        String error = "";
        try {
            if (onCommonMethodsPage.getListErrors().isEmpty())
                clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            WebElement errorElement = getElement(valueJson, "errortype", JsonPath.read(valueJson, "$.WizardControlTypes").toString());
            error = errorElement.getText();
        } catch (NullPointerException e) {
            // Handle null pointer exception
            error = "";
        }
        return error;
    }

}






