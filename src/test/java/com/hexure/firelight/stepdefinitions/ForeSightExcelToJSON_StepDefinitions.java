package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.libraies.Enums.EnumsExcelColumns;
import com.hexure.firelight.libraies.Enums.EnumsJSONProp;
import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLException;
import cucumber.api.java.en.Given;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.json.simple.JSONObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ForeSightExcelToJSON_StepDefinitions {

    JSONObject jsonObject = new JSONObject();
    List<String> requiredColumns = Arrays.asList(EnumsExcelColumns.ENUMSEXCELCOLUMNS.getText().split(", "));

    @Given("Create {string} file for eApp flow with file {string}")
    public void createForesightTestData(String jsonFile, String excelFile) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFile;
        try (FileInputStream file = new FileInputStream(filePath);
            XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheet("E-App Wizard Spec"); // Assuming data is in the first sheet
            Iterator<Row> iterator = sheet.iterator();

            // Assuming the first row contains headers
            Row headerRow = iterator.next().getSheet().getRow(0);
            JSONObject jsonRows = new JSONObject();
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                JSONObject tempJson = new JSONObject();

                // Create input file in json format
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    if (requiredColumns.contains(headerRow.getCell(i).getStringCellValue().trim())) {

                        Cell cell = currentRow.getCell(i);
                        String excelValue = getCellValue(cell);
                        if (!excelValue.isEmpty())
                            tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
                    }
                }
                jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim(), tempJson);
            }

            sheet = workbook.getSheet("Data List"); // Assuming data is in the first sheet
            iterator = sheet.iterator();

            // Assuming the first row contains headers
            headerRow = iterator.next().getSheet().getRow(0);

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                // Create input file in json format
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                        Cell cell = currentRow.getCell(i);
                        String excelValue = getCellValue(cell);
                        if (!excelValue.isEmpty()) {
                            if(jsonRows.containsKey(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")))
                                jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), jsonRows.get(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")).toString() + ", " + excelValue);
                            else
                                jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), excelValue);
                        }
                }
            }
            JSONObject masterJson = new JSONObject();
            masterJson.put(excelFile.replaceAll(".xlsx", ""), jsonRows);

            JSONObject defaultEntry = getJsonObject();

            masterJson.put("commonTestData", defaultEntry);

            jsonObject.put("testData", masterJson);
            FileWriter jsonTestData = new FileWriter(EnumsCommon.ABSOLUTE_FILES_PATH.getText() + jsonFile);
            BufferedWriter writer = new BufferedWriter(jsonTestData);
            writer.write(String.valueOf(jsonObject));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    private static String getCellValue(Cell cell) {
        String excelValue = "";

        if (cell != null && cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().equalsIgnoreCase("None")) {
            excelValue = cell.getStringCellValue().trim();
            excelValue = excelValue.replaceAll("//", "/").replaceAll("\n", ";").replaceAll("[\\s]+[.]+", ".").replaceAll("[\\s]+", " ");
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
        }
        return excelValue;
    }

    private static JSONObject getJsonObject() {
        JSONObject defaultEntry = new JSONObject();
        defaultEntry.put("InvalidTin", "123456789");
        defaultEntry.put("InvalidSSN", "123456789");
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