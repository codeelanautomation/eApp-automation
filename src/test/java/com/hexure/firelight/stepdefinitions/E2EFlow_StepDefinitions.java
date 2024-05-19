package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.ExcelHandlerPage;
import com.hexure.firelight.pages.SoftAssertionHandlerPage;
import com.jayway.jsonpath.JsonPath;
import cucumber.api.java.en.Given;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;


public class E2EFlow_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final ExcelHandlerPage onExcelHandlerPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    private static final Logger Log = LogManager.getLogger(E2EFlow_StepDefinitions.class);

    public E2EFlow_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCommonMethodsPage = testContext.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
        onExcelHandlerPage = testContext.getPageObjectManager().getExcelHandlerPage();
    }

    @Given("User validate e2e flow for workbook {string} and client {string}")
    public void enterE2ETestData(String excelFileName, String clientName) {
        String excelFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFileName;
        String wizardName;
        String dataItemID;
        String titleName;
        String formName;
        String testData;
        try {
            FileInputStream fileInputStream = new FileInputStream(excelFilePath);
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheet(clientName);
            Iterator<Row> iterator = sheet.iterator();
            List<List<String>> listInputFields;
            // Assuming the fifth row contains headers
            Row headerRow = iterator.next();

            int wizardColumnIndex = onExcelHandlerPage.findColumnIndex(headerRow, EnumsCommon.E2EWIZARDNAME.getText());
            int dataItemIDColumnIndex = onExcelHandlerPage.findColumnIndex(headerRow, EnumsCommon.E2EDATAITEMID.getText());
            int titleColumnIndex = onExcelHandlerPage.findColumnIndex(headerRow, EnumsCommon.E2ETITLE.getText());

            int count = 1;
            for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                wizardName = onExcelHandlerPage.getExcelColumnValue(excelFilePath, clientName, rowIndex, wizardColumnIndex);
                dataItemID = onExcelHandlerPage.getExcelColumnValue(excelFilePath, clientName, rowIndex, dataItemIDColumnIndex);
                titleName = onExcelHandlerPage.getExcelColumnValue(excelFilePath, clientName, rowIndex, titleColumnIndex);
                String valueJson;

                if (!dataItemID.toLowerCase().contains("lookup")) {
                    if (!dataItemID.isEmpty())
                        valueJson = testContext.getMapTestData().get(wizardName + "|" + dataItemID).trim();
                    else
                        valueJson = testContext.getMapTestData().get(wizardName + "|" + titleName).trim();
                    formName = JsonPath.read(valueJson, "$.FormName").toString().trim();
                    testData = JsonPath.read(valueJson, "$.TestData").toString().trim();
                    onCommonMethodsPage.setE2EValue(driver, formName, wizardName, valueJson, dataItemID, testData, count, titleName);
                }
            }
            listInputFields = onCommonMethodsPage.getListInputFields();
            onSoftAssertionHandlerPage.printTabularReport(listInputFields, testContext);
            workbook.close();
            fileInputStream.close();
        } catch (IOException e) {
            Log.error("File {} is not accessible", excelFileName);
        }
    }
}






