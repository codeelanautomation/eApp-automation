package com.hexure.firelight.stepdefinitions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForeSightExcelToJSON_StepDefinitions {

    JSONObject jsonObject = new JSONObject();
    List<String> requiredColumns = Arrays.asList(EnumsExcelColumns.ENUMSEXCELCOLUMNS.getText().split(", "));
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Given("Create {string} file for eApp flow with file {string}")
    public void createForesightTestData(String jsonFile, String excelFile) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFile;
        try (FileInputStream file = new FileInputStream(filePath);
            XSSFWorkbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheet("Data List"); // Assuming data is in the first sheet
            Iterator<Row> iterator = sheet.iterator();

            // Assuming the first row contains headers
            Row headerRow = iterator.next().getSheet().getRow(0);
            JSONObject jsonRows = new JSONObject();

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                // Create input file in json format
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = currentRow.getCell(i);
                    String excelValue = getCellValue(cell, jsonRows);
                    if (!excelValue.isEmpty()) {
                        if(jsonRows.containsKey(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")))
                            jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), jsonRows.get(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")).toString() + ", " + excelValue);
                        else
                            jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), excelValue);
                    }
                }
            }
            int count = 0;
            sheet = workbook.getSheet("E-App Wizard Spec"); // Assuming data is in the first sheet
            iterator = sheet.iterator();

            // Assuming the first row contains headers
            headerRow = iterator.next().getSheet().getRow(0);
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                JSONObject tempJson = new JSONObject();

                // Create input file in json format
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
//                    System.out.println(headerRow.getCell(i).getStringCellValue());
                    if (requiredColumns.contains(headerRow.getCell(i).getStringCellValue().trim())) {

                        Cell cell = currentRow.getCell(i);
                        String excelValue = getCellValue(cell, jsonRows);
                        System.out.println(excelValue);
                        if (!excelValue.isEmpty())
                            tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
                    }
                }
                jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim(), tempJson);
            }

            JSONObject masterJson = new JSONObject();
            masterJson.put(excelFile.replaceAll(".xlsx", ""), jsonRows);

            JSONObject defaultEntry = getJsonObject();

            masterJson.put("commonTestData", defaultEntry);

            jsonObject.put("testData", masterJson);
            FileWriter jsonTestData = new FileWriter(EnumsCommon.ABSOLUTE_FILES_PATH.getText() + jsonFile);
            BufferedWriter writer = new BufferedWriter(jsonTestData);
            writer.write(gson.toJson(jsonObject));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    private static String getCellValue(Cell cell, JSONObject jsonRows) {
        String excelValue = "";
        String newValue = "";
        Pattern pattern = null;
        List<String> listRules = new ArrayList<>();
        if (cell != null && cell.getCellType() == CellType.STRING && !(cell.getStringCellValue().trim().equalsIgnoreCase("None"))) {
            excelValue = cell.getStringCellValue().trim();
            excelValue = excelValue.replaceAll("//", "/").replaceAll("[^\\x00-\\x7F]", "").replaceAll("\n", ";").replaceAll("=", " = ").replaceAll("<>", " <> ").replaceAll("“", "").replaceAll("\"", "").replaceAll("[\\s]+[.]+", ".").replaceAll("[\\s]+", " ").trim();
            if (excelValue.toLowerCase().contains(" or ") & !(excelValue.toLowerCase().contains("skip for automation"))) {
                listRules = Arrays.asList(excelValue.split(";"));
                for (String rule : listRules) {
                    JSONObject values = new JSONObject();
                    List<String> resultValue = new ArrayList<>();
                    if (rule.toLowerCase().contains(" or ")) {
                        if(rule.toLowerCase().contains("("))
                            pattern = Pattern.compile("(.*?)\\((.*?)\\)(.*)");
                        else
                            pattern = Pattern.compile("(\\d+\\.\\s*)?If (.*?) (.*?)(?:,)? then (.*)\\.?");
                        Matcher matcher = pattern.matcher(rule);
                        while (matcher.find()) {
                            List<String> orConditions = Arrays.asList(matcher.group(2).split(" OR "));
                            for (String condition : orConditions)
                                newValue += rule.replaceAll(matcher.group(2), condition).replaceAll("\\(", "").replaceAll("\\)", "") + ";";
                        }
                    } else
                        newValue += rule + ";";
                }
                excelValue = newValue;
            }
            newValue = "";
            if (excelValue.toLowerCase().contains("<>") & !(excelValue.toLowerCase().contains("skip for automation"))) {
                listRules = Arrays.asList(excelValue.split(";"));
                for (String rule : listRules) {
                    JSONObject values = new JSONObject();
                    List<String> resultValue = new ArrayList<>();
                    if (rule.toLowerCase().contains("<>")) {
                        String conditionAnother = "";
                        String expectedResult = "";
                        if (Pattern.compile("([^<>\\s]+)\\s*<>\\s*([^<>\\s]+)").matcher(rule).find())
                            pattern = Pattern.compile("([^<>\\s]+)\\s*<>\\s*([^<>\\s]+)");
                        Matcher matcher = pattern.matcher(rule);
                        while (matcher.find()) {
                            conditionAnother = matcher.group(1);
                            expectedResult = matcher.group(2);
                        }
                        if (jsonRows.containsKey(conditionAnother)) {
                            values = (JSONObject) jsonRows.get(conditionAnother);
                            if (values.containsKey("ListOptions")) {
                                resultValue = new ArrayList<>(Arrays.asList(jsonRows.get(values.get("ListOptions").toString().trim().replaceAll(" ", "")).toString().trim().split(", ")));

                                Set<String> valuesToRemove = new HashSet<>(Arrays.asList("Blank", expectedResult));
                                // Remove elements that match the given condition
                                resultValue.removeIf(valuesToRemove::contains);

                                for (String value : resultValue) {
                                    newValue += rule.replace(expectedResult, value).replace("<>", "=") + ";";
                                }
                            } else
                                newValue = excelValue;
                        }
                    } else
                        newValue += rule + ";";
                }
                    excelValue = newValue;
            }
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC)
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
        return excelValue;
    }

    private JSONObject getJsonObject() {
        JSONObject defaultEntry = new JSONObject();
        defaultEntry.put("InvalidTin", "123456789");
        defaultEntry.put("InvalidSSN", "123456789");
        defaultEntry.put("InvalidDate", "02292023");
        defaultEntry.put("InvalidEmail", "test,test@,testgmail.com,%%@test.c,test@gmail .com,example.com,example@@example.com");
        return defaultEntry;
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (columnName.equalsIgnoreCase(getCellColumnValue(cell))) {
                return cell.getColumnIndex();
            }
        }
        return -1; // Column not found
    }

    private String getCellColumnValue(Cell cell) {
        return cell == null ? "" : cell.toString().trim();
    }

}