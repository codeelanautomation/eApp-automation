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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rules_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;

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
        String sheetName = "Annuity Owner Module";
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
        String requiredErrorCondition = "";
        String requiredErrorMessage = "";
        String requiredFirstAttribute = "";
        String requiredSecondAttribute = "";
        String requiredFirstAttributeElse = "";
        String requiredSecondAttributeElse = "";
        String requiredAttributeValue = "";
        Pattern pattern;
        Matcher matcher;
        String error;
        String dependentCondition = "";
        String dependentResult = "";
        String expectedText = "";
        boolean expectedFlag;
        String inputValue = "";

        // Assuming the fifth row contains headers
        Row headerRow = iterator.next();

        int fieldColumnIndex = findColumnIndex(headerRow, EnumsCommon.FIELD.getText());
        int sectionColumnIndex = findColumnIndex(headerRow, EnumsCommon.SECTION.getText());
        List<String> rulesList = Arrays.asList("Options", "ValidationRules", "RulesforWizard", "Length", "Format");
//         "Mask", "Validation")
        int count = 0;
        for (int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++) {
            field = getExcelColumnValue(excelFilePath, sheetName, rowIndex + 1, fieldColumnIndex);
            section = getExcelColumnValue(excelFilePath, sheetName, rowIndex + 1, sectionColumnIndex);
            String valueJson = testContext.getMapTestData().get(field).trim();
            expectedResult= "";

            moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());

            for (String rule : rulesList) {
                try {
                    switch (rule) {
                        case "Options":
                            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim()) {
                                case "Dropdown":
                                    List<String> actualOptions = new ArrayList<>();
                                    List<WebElement> dropdownOptions = new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()))).getOptions();
                                    for (WebElement element : dropdownOptions) {
                                        if (element.getText().equalsIgnoreCase(""))
                                            actualOptions.add("Blank");
                                        else
                                            actualOptions.add(element.getText());
                                    }
                                    List<String> expectedOptions = Arrays.asList(JsonPath.read(valueJson, "$.Options").toString().trim().split(";"));
                                    onSoftAssertionHandlerPage.assertTrue(field, "Dropdown Options", actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
//                                    onSoftAssertionHandlerPage.assertTrue("Field " + field + " does not contains expected values: Actual: " + actualOptions + " , Expected: " + expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                    break;
                                //                            case "String":
                                //                                assertTrue(ActualValue.matches("[a-zA-Z]+"), "Expected " + FSEField + " contains alpha numeric values too or is blank, Actual: " + ActualValue);
                                //                                break;
                            }
                            break;
                        case "RulesforWizard":
                            for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split((";"))) {
                                if (Pattern.compile("If (.*?) = (.*?) then (.*?) and (.*?) else (.*?) = (.*?) then (.*?) and (.*)").matcher(distinctRule).find()) {
                                    pattern = Pattern.compile("If (.*?) = (.*?) then (.*?) and (.*?) else (.*?) = (.*?) then (.*?) and (.*)");
                                    matcher = pattern.matcher(distinctRule);
                                    while (matcher.find()) {
                                        condition = matcher.group(1);
                                        expectedResult = matcher.group(2);
                                        requiredFirstAttribute = matcher.group(3);
                                        requiredSecondAttribute = matcher.group(4);
                                        conditionElse = matcher.group(5);
                                        expectedResultElse = matcher.group(6);
                                        requiredFirstAttributeElse = matcher.group(7);
                                        requiredSecondAttributeElse = matcher.group(8);
                                    }
                                    for (String result : expectedResult.split(",")) {
                                        setDependentCondition(condition, valueJson, result);
                                        if (requiredFirstAttribute.equalsIgnoreCase("display")) {
                                            expectedFlag = !findElements(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
                                            onSoftAssertionHandlerPage.assertTrue(field, "Field is displayed when " + condition + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                        }
                                        if (requiredSecondAttribute.equalsIgnoreCase("enable")) {
                                            expectedFlag = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEnabled();
                                            onSoftAssertionHandlerPage.assertTrue(field, "Field is enabled when " + condition + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                        }
                                    }
                                    for (String result : expectedResultElse.split(",")) {
                                        setDependentCondition(conditionElse, valueJson, result);
                                        expectedFlag = findElements(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
                                        onSoftAssertionHandlerPage.assertTrue(field, "Hidden Rule when " + conditionElse + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                    }
                                } else if (Pattern.compile("(.*?) = (.*)").matcher(distinctRule).find()) {
                                    pattern = Pattern.compile("(.*?) = (.*)");
                                    matcher = pattern.matcher(distinctRule);
                                    while (matcher.find()) {
                                        requiredFirstAttribute = matcher.group(1);
                                        requiredAttributeValue = matcher.group(2);
                                    }
                                    pattern = Pattern.compile("If (.*?) = (.*)");
                                    matcher = pattern.matcher(JsonPath.read(valueJson, "$.DisplayRule").toString().trim());
                                    while (matcher.find()) {
                                        condition = matcher.group(1);
                                        expectedResult = matcher.group(2);
                                    }
                                    if(expectedResult.isEmpty())
                                        verifyData(valueJson, field, "", "", requiredAttributeValue);
                                    else {
                                        for (String result : expectedResult.split(",")) {
                                            setDependentCondition(condition, valueJson, result);
                                            expectedText = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getAttribute("value");
                                            if (expectedText.equalsIgnoreCase(""))
                                                expectedText = "blank";
                                            verifyData(valueJson, field, condition, result, requiredAttributeValue);
                                        }
                                    }
                                }
                            }
                            break;
                        case "ValidationRules":
                            for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split((";"))) {
                                pattern = Pattern.compile("If (.*?) = (.*?) then (.*?): (.*)");
                                matcher = pattern.matcher(distinctRule);
                                while (matcher.find()) {
                                    condition = matcher.group(1);
                                    expectedResult = matcher.group(2);
                                    requiredErrorCondition = matcher.group(3);
                                    requiredErrorMessage = matcher.group(4);
                                }
                                pattern = Pattern.compile("If (.*?) = (.*)");
                                matcher = pattern.matcher(JsonPath.read(valueJson, "$.DisplayRule").toString().trim());
                                while (matcher.find()) {
                                    dependentCondition = matcher.group(1);
                                    dependentResult = matcher.group(2);
                                }
                                for (String result : dependentResult.split(",")) {
                                    setDependentCondition(dependentCondition, valueJson, result);
                                    if (expectedResult.toLowerCase().equalsIgnoreCase("Invalid")) {
                                        switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString()) {
                                            case "TIN":
                                                inputValue = testContext.getMapTestData().get("InvalidTin").toString().trim();
                                                break;
                                        }
                                        sendKeys(driver, findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())), inputValue);
                                    }
                                    if (onCommonMethodsPage.getListErrors().isEmpty())
                                        clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
                                    switch (expectedResult.toLowerCase()) {
                                        case "blank":
                                            error = findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getText();
                                            onSoftAssertionHandlerPage.assertTrue(field, "Mandatory Field Validation when " + condition + " is " + result, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
                                            break;
                                        case "invalid":
                                            error = findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getText();
                                            onSoftAssertionHandlerPage.assertTrue(field, "Invalid Value Validation when " + field + " is " + inputValue, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
                                            sendKeys(driver, findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())), "");
                                            break;

                                    }
                                }
                            }
                            break;
                        case "Length":
                                pattern = Pattern.compile("If (.*?) = (.*)");
                                matcher = pattern.matcher(JsonPath.read(valueJson, "$.DisplayRule").toString().trim());
                                while (matcher.find()) {
                                    dependentCondition = matcher.group(1);
                                    dependentResult = matcher.group(2);
                                }
                                if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
                                    for (String result : dependentResult.split(",")) {
                                        setDependentCondition(dependentCondition, valueJson, result);
                                        expectedText = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getAttribute("maxlength");
                                        onSoftAssertionHandlerPage.assertTrue(field, "Length when " + dependentCondition + " is " + dependentResult, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
                                    }
                                }
                            break;
                        case "Format":
                            pattern = Pattern.compile("If (.*?) = (.*)");
                            matcher = pattern.matcher(JsonPath.read(valueJson, "$.DisplayRule").toString().trim());
                            while (matcher.find()) {
                                dependentCondition = matcher.group(1);
                                dependentResult = matcher.group(2);
                            }
                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
                                for (String result : dependentResult.split(",")) {
                                    setDependentCondition(dependentCondition, valueJson, result);
                                    expectedText = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getAttribute("mask").replaceAll("9", "#");
                                    onSoftAssertionHandlerPage.assertTrue(field, "Format when " + dependentCondition + " is " + dependentResult, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
                                }
                            }
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

    public void setDependentCondition(String condition, String valueJson, String result) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        moveToPage(JsonPath.read(valueDependentJson, "$.Page").toString().trim(), JsonPath.read(valueDependentJson, "$.ModuleSectionName").toString().trim());
        new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueDependentJson, "$.CommonTag").toString().trim()))).selectByVisibleText(result);
        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
    }


    public void moveToPage(String pageHeader, String formHeader) {
        if (!(onCommonMethodsPage.getPageHeader().getText().equalsIgnoreCase(pageHeader) & onCommonMethodsPage.getFormHeader().getText().equalsIgnoreCase(formHeader))) {
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
            List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandetoryFormElement(), formHeader));
            int i = 0;
            for (WebElement element : mandetoryFormList) {
                String form = element.getAttribute("innerText");
                if (form.equals(pageHeader)) {
                    element.click();
                    break;
                }
            }
        }
    }

    public void verifyData(String valueJson, String field, String condition, String result, String requiredAttributeValue) {
        String expectedText = "";
        if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().equalsIgnoreCase("dropdown"))
            expectedText = new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()))).getFirstSelectedOption().getText().trim();
        else if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().equalsIgnoreCase("single line textbox"))
            expectedText = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getAttribute("value");
        if (expectedText.equalsIgnoreCase(""))
            expectedText = "blank";
        if(condition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(field, "Default Value ", requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.equalsIgnoreCase(expectedText), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(field, "Default Value when " + condition + " is " + result, requiredAttributeValue.toLowerCase(), expectedText.toLowerCase(), requiredAttributeValue.equalsIgnoreCase(expectedText), testContext);
    }

}






