package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.Enums.EnumsExcelColumns;
import com.hexure.firelight.libraies.Enums.EnumsJSONProp;
import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLException;
import cucumber.api.java.en.Given;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ForeSightE2EToJSON_StepDefinitions {

    JSONObject jsonObject = new JSONObject();
    List<String> requiredColumns = Arrays.asList(EnumsExcelColumns.ENUMSEXCELCOLUMNS.getText().split(", "));

    @Given("Create {string} file for client {string} for eApp E2E flow")
    public void createForesightTestData(String jsonFile, String clientName) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + "E2EFlow.xlsx";
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {
                 Sheet sheet = workbook.getSheet(clientName); // Assuming data is in the first sheet

                 Iterator<Row> iterator = sheet.iterator();

                 // Assuming the first row contains headers
                 Row headerRow = iterator.next().getSheet().getRow(0);
                 JSONObject jsonRows = new JSONObject();
                 while (iterator.hasNext()) {
                     Row currentRow = iterator.next();
                     JSONObject tempJson = new JSONObject();

                     // Create input file in json format
                     if(!(currentRow.getCell(2).getStringCellValue().equalsIgnoreCase("jurisdiction") | currentRow.getCell(2).getStringCellValue().equalsIgnoreCase("ProductType"))) {
                         for (int i = 0; i < headerRow.getLastCellNum(); i++) {

                             Cell cell = currentRow.getCell(i);
                             String excelValue = getCellValue(cell);
                             if (!excelValue.isEmpty())
                                 tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), excelValue);
                         }
                         jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2EWIZARDNAME.getText())).getStringCellValue().trim() + "|" + currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2EDATAITEMID.getText())).getStringCellValue().trim(), tempJson);
                     }
                     else
                         jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2EDATAITEMID.getText())).getStringCellValue().trim(), currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2ETESTDATA.getText())).getStringCellValue().trim());
                 }
                 JSONObject masterJson = new JSONObject();
                 masterJson.put(clientName, jsonRows);

                 JSONObject defaultEntry = getJsonObject();

                 masterJson.put("commonTestData", defaultEntry);

                 jsonObject.put("testData", masterJson);
                 FileWriter jsonTestData = new FileWriter(EnumsCommon.ABSOLUTE_FILES_PATH.getText() + jsonFile);
                 BufferedWriter writer = new BufferedWriter(jsonTestData);
                 writer.write(String.valueOf(jsonObject));
                 writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(Exception e)        {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    private static String getCellValue(Cell cell) {
        String excelValue;

        if (cell != null && cell.getCellType() == CellType.STRING) {
            excelValue = cell.getStringCellValue().trim();
            if (excelValue.contains("//"))
                excelValue = excelValue.replaceAll("//", "/");
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
        } else {
            excelValue = "";
        }
        return excelValue;
    }

    private static JSONObject getJsonObject() {
        JSONObject defaultEntry = new JSONObject();
        defaultEntry.put("InvalidTin", "123456789");
        return defaultEntry;
    }

    private static int findColumnIndex(Row headerRow, String columnName) {
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (columnName.equalsIgnoreCase(getCellColumnValue(cell))) {
                return cell.getColumnIndex();
            }
        }
        return -1; // Column not found
    }

    private static String getCellColumnValue(Cell cell) {
        return cell == null ? "" : cell.toString().trim();
    }

}