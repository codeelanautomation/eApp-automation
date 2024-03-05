package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.SoftAssertionHandlerPage;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import cucumber.api.java.en.Given;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Given("User validate e2e flow for workbook {string} and client {string}")
    public void enterE2ETestData(String excelFileName, String clientName) throws IOException, InterruptedException {
        String excelFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFileName;
        String wizardName = "";
        String dataItemID = "";
        FileInputStream fileInputStream = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet(clientName);
        Iterator<Row> iterator = sheet.iterator();
        
        // Assuming the fifth row contains headers
        Row headerRow = iterator.next();

        int wizardColumnIndex = findColumnIndex(headerRow, EnumsCommon.E2EWIZARDNAME.getText());
        int dataItemIDColumnIndex = findColumnIndex(headerRow, EnumsCommon.E2EDATAITEMID.getText());

        int count = 0;
        for (int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++) {
            wizardName = getExcelColumnValue(excelFilePath, clientName, rowIndex + 3, wizardColumnIndex);
            dataItemID = getExcelColumnValue(excelFilePath, clientName, rowIndex + 3, dataItemIDColumnIndex);
            String valueJson = testContext.getMapTestData().get(wizardName + "|" + dataItemID).trim();

            moveToPage(JsonPath.read(valueJson, "$.WizardName").toString().trim(), JsonPath.read(valueJson, "$.FormName").toString().trim());
            switch (JsonPath.read(valueJson, "$.ControlType").toString().trim().toLowerCase()) {
                case "dropdown":
                    new Select(findElement(driver, String.format(onCommonMethodsPage.getSelectField(), JsonPath.read(valueJson, "$.DataItemID").toString().trim()))).selectByVisibleText(JsonPath.read(valueJson, "$.TestData").toString().trim());
                    break;
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

}






