package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.SoftAssertionHandlerPage;
import com.jayway.jsonpath.JsonPath;

import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bytedeco.opencv.presets.opencv_core;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class Flow_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;

    public Flow_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCommonMethodsPage = testContext.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (columnName.equalsIgnoreCase(getCellValue(cell))) {
                return cell.getColumnIndex();
            }
        }
        return -1; // Column not found
    }

    private String getCellValue(Cell cell) {
        return cell == null ? "" : cell.toString().trim();
    }

    public String getExcelColumnValue(String excelFilePath, String sheetName, int rowIndex, int columnIndex) {
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

    @Given("User validate e2e flow for workbook {string} and client {string}")
    public void enterE2ETestData(String excelFileName, String clientName) throws IOException, InterruptedException {
        String excelFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFileName;
        String wizardName = "";
        String dataItemID = "";
        String titleName = "";
        String formName = "";
        String testData = "";

        FileInputStream fileInputStream = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet(clientName);
        Iterator<Row> iterator = sheet.iterator();
        List<List<String>> listInputFields = new ArrayList<>();
        // Assuming the fifth row contains headers
        Row headerRow = iterator.next();

        int wizardColumnIndex = findColumnIndex(headerRow, EnumsCommon.E2EWIZARDNAME.getText());
        int dataItemIDColumnIndex = findColumnIndex(headerRow, EnumsCommon.E2EDATAITEMID.getText());
        int titleColumnIndex = findColumnIndex(headerRow, EnumsCommon.E2ETITLE.getText());


        int count = 1;
        for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            wizardName = getExcelColumnValue(excelFilePath, clientName, rowIndex, wizardColumnIndex);
            dataItemID = getExcelColumnValue(excelFilePath, clientName, rowIndex, dataItemIDColumnIndex);
            titleName = getExcelColumnValue(excelFilePath, clientName, rowIndex, titleColumnIndex);
            String valueJson = "";

            if (!dataItemID.toLowerCase().contains("lookup")) {
                if (!dataItemID.isEmpty())
                    valueJson = testContext.getMapTestData().get(wizardName + "|" + dataItemID).trim();
                else
                    valueJson = testContext.getMapTestData().get(wizardName + "|" + titleName).trim();
                formName = JsonPath.read(valueJson, "$.FormName").toString().trim();
                testData = JsonPath.read(valueJson, "$.TestData").toString().trim();

                moveToPage(formName, wizardName);
                String controlType = JsonPath.read(valueJson, "$.ControlType").toString().trim().toLowerCase();
                switch (controlType) {
                    case "dropdown":
                        new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), dataItemID))).selectByVisibleText(testData);
                        findElement(driver, String.format(onCommonMethodsPage.getSelectField(), dataItemID)).sendKeys(Keys.TAB);
                        listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                        break;
                    case "radio":
                        clickElement(driver, findElement(driver, String.format(onCommonMethodsPage.getRadioField(), dataItemID, testData)));
                        listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                        break;
                    case "textbox":
                        if (valueJson.contains("DataItemID")) {
                            sendKeys(driver, findElement(driver, String.format(onCommonMethodsPage.getInputField(), dataItemID)), testData);
                            if (dataItemID.toLowerCase().contains("date")) {
                                new Actions(driver).moveToElement(onCommonMethodsPage.getFormHeader()).click().perform();
                            }
                            listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                        } else {
                            sendKeys(driver, findElement(driver, String.format(onCommonMethodsPage.getTxtField(), titleName)), testData);
                            listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, titleName, controlType, testData));
                        }
                        break;
                    case "checkbox":
                        checkBoxSelectYesNO(testData, findElement(driver, String.format(onCommonMethodsPage.getChkBoxField(), dataItemID)));
                        listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                        break;
                    case "phone":
                        sendKeys(driver, findElement(driver, String.format(onCommonMethodsPage.getInputField(), dataItemID)), testData);
                        listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                        break;
                }
            }
        }
        printTabularReport(listInputFields, testContext);
        workbook.close();
        fileInputStream.close();
    }

    public void setDependentCondition(String condition, String valueJson, String result) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        moveToPage(JsonPath.read(valueDependentJson, "$.Page").toString().trim(), JsonPath.read(valueDependentJson, "$.ModuleSectionName").toString().trim());
        new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueDependentJson, "$.CommonTag").toString().trim()))).selectByVisibleText(result);
        moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
    }

    public void moveToPage(String formHeader, String pageHeader) {
        if (!(onCommonMethodsPage.getPageHeader().getText().equalsIgnoreCase(pageHeader) & onCommonMethodsPage.getFormHeader().getText().equalsIgnoreCase(formHeader))) {
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
            List<WebElement> mandatoryFormsList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
            for (WebElement element : mandatoryFormsList) {
                String form = element.getAttribute("innerText");
                if (form.equals(pageHeader)) {
                    element.click();
                    break;
                }
            }
        }
    }

    private void printTabularReport(List<List<String>> entries, TestContext testContext) {
        Scenario scenario = testContext.getScenario();
        String resultSet = "";
        resultSet += "<table border=\"1\" width=\"90%\"> <tr style='color: blue; font-weight: bold; background-color: #C5D88A;'> <th>S.No</th> <th>Form Name</th> <th>Wizard Name</th> <th>Common Tag</th> <th>Control Type</th> <th>Test Data</th> </tr>";

        for (List<String> entry : entries)
            resultSet += "<tr style='color: green; font-weight: bold; background-color: #C5D88A;'> <td>" + entry.get(0) + "</td> <td>" + entry.get(1) + "</td> <td>" + entry.get(2) + "</td> <td>" + entry.get(3) + "</td> <td>" + entry.get(4) + "</td> <td>" + entry.get(5) + "</td> </tr>";
        resultSet += "</table>";
        scenario.log(resultSet);
    }
}






