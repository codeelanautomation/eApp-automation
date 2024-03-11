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
        boolean expectedFlag = false;
        String inputValue = "";
        List<String> actualOptions = new ArrayList<>();
        List<String> expectedOptions = new ArrayList<>();
        List<WebElement> radioOptions = new ArrayList<>();
        List<String> listConditions = new ArrayList<>();
        String dataType = "";

        // Assuming the fifth row contains headers
        Row headerRow = iterator.next();

        int fieldColumnIndex = findColumnIndex(headerRow, EnumsCommon.FIELD.getText());
        int sectionColumnIndex = findColumnIndex(headerRow, EnumsCommon.SECTION.getText());
        List<String> rulesList = Arrays.asList("Options", "ValidationRules", "RulesforWizard", "Length", "Format");

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
                            expectedOptions = Arrays.asList(JsonPath.read(valueJson, "$.Options").toString().trim().split(";"));
                            dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
                            actualOptions = getOptions(valueJson, dataType);
                            onSoftAssertionHandlerPage.assertTrue(field, dataType + " Options", actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                            break;
                        case "RulesforWizard":
                            for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split((";"))) {
                                if (Pattern.compile("If (.*?) = (.*?) then (.*?) and (.*?) else (.*?) = (.*?) then (.*?) and (.*)").matcher(distinctRule).find()) {
                                    listConditions = getDisplayRuleConditions(valueJson, "If (.*?) = (.*?) then (.*?) and (.*?) else (.*?) = (.*?) then (.*?) and (.*)", "", distinctRule);
                                    condition = listConditions.get(0);
                                    expectedResult = listConditions.get(1);
                                    requiredFirstAttribute = listConditions.get(2);
                                    requiredSecondAttribute = listConditions.get(3);
                                    conditionElse = listConditions.get(4);
                                    expectedResultElse = listConditions.get(5);
                                    requiredFirstAttributeElse = listConditions.get(6);
                                    requiredSecondAttributeElse = listConditions.get(7);

                                    for (String result : expectedResult.split(",")) {
                                        setDependentCondition(condition, valueJson, result);
                                        if (requiredFirstAttribute.equalsIgnoreCase("display")) {
                                            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim()) {
                                                case "Single Line Textbox":
                                                    expectedFlag = !findElements(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
                                                    break;
                                                case "Radio Button":
                                                    expectedFlag = !findElements(driver, String.format(onCommonMethodsPage.getRadioField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
                                                    break;
                                            }
                                            onSoftAssertionHandlerPage.assertTrue(field, "Field is displayed when " + condition + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                        }
                                        if (requiredSecondAttribute.equalsIgnoreCase("enable")) {
                                            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim()) {
                                                case "Single Line Textbox":
                                                    expectedFlag = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEnabled();
                                                    onSoftAssertionHandlerPage.assertTrue(field, "Field is enabled when " + condition + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                                    break;
                                                case "Radio Button":
                                                    radioOptions = findElements(driver, String.format(onCommonMethodsPage.getRadioField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
                                                    for (WebElement element : radioOptions)
                                                        onSoftAssertionHandlerPage.assertTrue(field, "Radio button " + element.getAttribute("title") + " is enabled when " + condition + " is " + result, element.isEnabled(), "true", element.isEnabled(), testContext);
                                                    break;
                                            }
                                        }
                                    }
                                    for (String result : expectedResultElse.split(",")) {
                                        setDependentCondition(conditionElse, valueJson, result);
                                        switch (requiredFirstAttributeElse.toLowerCase()) {
                                            case "hide":
                                                expectedFlag = findElements(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
                                                onSoftAssertionHandlerPage.assertTrue(field, "Hidden Rule when " + conditionElse + " is " + result, expectedFlag, expectedFlag, expectedFlag, testContext);
                                                break;
                                            case "set to no":
                                                WebElement element = findElement(driver, String.format(onCommonMethodsPage.getRadioFieldWithOption(), JsonPath.read(valueJson, "$.CommonTag").toString().trim(), "No"));
                                                onSoftAssertionHandlerPage.assertTrue(field, "Radio button No is selected when " + conditionElse + " is " + result, element.getAttribute("aria-checked"), "true", element.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                                                break;

                                        }
                                        switch (requiredSecondAttributeElse.toLowerCase()) {
                                            case "disable":
                                                if(!requiredFirstAttributeElse.equalsIgnoreCase("hide")) {
                                                    radioOptions = findElements(driver, String.format(onCommonMethodsPage.getRadioField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
                                                    for (WebElement element : radioOptions)
                                                        onSoftAssertionHandlerPage.assertTrue(field, "Radio button \"" + element.getAttribute("title") + "\" disabled when " + condition + " is " + result, element.getAttribute("aria-checked"), "false", element.getAttribute("aria-checked").equalsIgnoreCase("false"), testContext);
                                                }
                                                break;
                                        }
                                    }
                                } else if (Pattern.compile("(.*?) = (.*)").matcher(distinctRule).find()) {
                                    listConditions = getDisplayRuleConditions(valueJson, "(.*?) = (.*)", "", distinctRule);
                                    requiredAttributeValue = listConditions.get(1);

                                    listConditions = getDisplayRuleConditions(valueJson, "If (.*?) = (.*)", "DisplayRule", "");
                                    condition = listConditions.get(0);
                                    expectedResult = listConditions.get(1);

                                    if(expectedResult.isEmpty())
                                        verifyData(valueJson, field, "", "", requiredAttributeValue);
                                    else {
                                        for (String result : expectedResult.split(",")) {
                                            setDependentCondition(condition, valueJson, result);
                                            switch (requiredAttributeValue.toLowerCase().trim()) {
                                                case "blank":
                                                    expectedText = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getAttribute("value");
                                                    if (expectedText.equalsIgnoreCase(""))
                                                        expectedText = "blank";
                                                    verifyData(valueJson, field, condition, result, requiredAttributeValue);
                                                    break;
                                                case "unselected":
                                                    radioOptions = findElements(driver, String.format(onCommonMethodsPage.getRadioField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
                                                    for (WebElement element : radioOptions)
                                                        onSoftAssertionHandlerPage.assertTrue(field, "Radio button \"" + element.getAttribute("title") + "\" unselected by default when " + condition + " is " + result, element.getAttribute("aria-checked"), "false", element.getAttribute("aria-checked").equalsIgnoreCase("false"), testContext);
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        case "ValidationRules":
                            for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split((";"))) {
                                listConditions = getDisplayRuleConditions(valueJson, "If (.*?) = (.*?) then (.*?): (.*)", "", distinctRule);
                                condition = listConditions.get(0);
                                expectedResult = listConditions.get(1);
                                requiredErrorCondition = listConditions.get(2);
                                requiredErrorMessage = listConditions.get(3);

                                listConditions = getDisplayRuleConditions(valueJson, "If (.*?) = (.*)", "DisplayRule", "");
                                dependentCondition = listConditions.get(0);
                                dependentResult = listConditions.get(1);

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
                                    error = findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getText();
                                    switch (expectedResult.toLowerCase()) {
                                        case "blank":
                                            onSoftAssertionHandlerPage.assertTrue(field, "Mandatory Field Validation when " + condition + " is " + result, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
                                            break;
                                        case "invalid":
                                            onSoftAssertionHandlerPage.assertTrue(field, "Invalid Value Validation when " + field + " is " + inputValue, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
                                            sendKeys(driver, findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())), "");
                                            break;
                                    }
                                }
                            }
                            break;
                        case "Length":
                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                getAttributeValue(field, valueJson, "If (.*?) = (.*)", "DisplayRule", rule, "maxLength");
                            break;
                        case "Format":
                            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank"))
                                getAttributeValue(field, valueJson, "If (.*?) = (.*)", "DisplayRule", rule, "mask");
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
            List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
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

    public List<String> getOptions(String valueJson, String dataType) {
        List<String> actualOptions = new ArrayList<>();
        switch (dataType) {
            case "Dropdown":
                List<WebElement> dropdownOptions = new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()))).getOptions();
                for (WebElement element : dropdownOptions) {
                    if (element.getText().equalsIgnoreCase(""))
                        actualOptions.add("Blank");
                    else
                        actualOptions.add(element.getText());
                }
                break;
            case "Radio Button":
                List<WebElement> radioOptions = findElements(driver, String.format(onCommonMethodsPage.getRadioField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim()));
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
        if(parameter.equalsIgnoreCase(""))
            matcher = pattern.matcher(distinctRule);
        else
            matcher = pattern.matcher(JsonPath.read(valueJson, "$." + parameter).toString().trim());

        int count = 1;
        List<String> options = new ArrayList<>();
        while(matcher.find()) {
            while (count <= matcher.groupCount()) {
                options.add(matcher.group(count));
                count++;
            }
        }
        return options;
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

    public void getAttributeValue(String field, String valueJson, String requiredPattern, String parameter, String rule, String attribute) {
        List<String> listConditions = getDisplayRuleConditions(valueJson, requiredPattern, parameter, "");
        String dependentCondition = listConditions.get(0);
        String dependentResult = listConditions.get(1);
        String expectedText;
        if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
            for (String result : dependentResult.split(",")) {
                setDependentCondition(dependentCondition, valueJson, result);
                expectedText = findElement(driver, String.format(onCommonMethodsPage.getInputField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).getAttribute(attribute);
                if(rule.equalsIgnoreCase("format"))
                    expectedText = expectedText.replaceAll("9", "#");
                onSoftAssertionHandlerPage.assertTrue(field, rule + " when " + dependentCondition + " is " + dependentResult, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
            }
        }
    }

}






