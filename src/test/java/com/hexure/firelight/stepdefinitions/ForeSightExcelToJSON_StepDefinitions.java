package com.hexure.firelight.stepdefinitions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hexure.firelight.libraies.Enums.EnumsExcelColumns;
import com.hexure.firelight.libraies.Enums.EnumsJSONProp;
import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLException;
import com.jayway.jsonpath.JsonPath;
import cucumber.api.java.en.Given;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.json.simple.JSONObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.util.Collections;


public class ForeSightExcelToJSON_StepDefinitions {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JSONObject jsonObject = new JSONObject();
    List<String> requiredColumns = Arrays.asList(EnumsExcelColumns.ENUMSEXCELCOLUMNS.getText().split(", "));
    JSONObject masterJson = new JSONObject();

    /**
     * Create input JSON, runner and feature files from given spec and created flow interface
     *
     * @param jsonFile
     * @param excelFile
     */
    @Given("Create {string} file for eApp flow with interface file {string}")
    public void createForesightTestDataInterface(String jsonFile, String excelFile) {
        String filePath = EnumsCommon.ABSOLUTE_FILES_PATH.getText() + excelFile;
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheet("Interface"); // Assuming data is in the first sheet
            Iterator<Row> iterator = sheet.iterator();
            Row headerRow = iterator.next().getSheet().getRow(0);

            int clientNameIndex = findColumnIndex(headerRow, "Client Name");
            int productIndex = findColumnIndex(headerRow, "Product");
            int modulesIndex = findColumnIndex(headerRow, "Modules");
            int filenameIndex = findColumnIndex(headerRow, "FileName");
            int executeIndex = findColumnIndex(headerRow, "Execute");
            int JurisdictionWiseReportIndex = findColumnIndex(headerRow, "JurisdictionWiseReport");

            deleteRunnerFeature(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest");
            deleteRunnerFeature(EnumsCommon.FEATUREFILESPATH.getText() + "ForesightTest");
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                String clientName = getCellValue(currentRow.getCell(clientNameIndex));
                String product = getCellValue(currentRow.getCell(productIndex));
                String modules = getCellValue(currentRow.getCell(modulesIndex));
                String filename = getCellValue(currentRow.getCell(filenameIndex));
                String execute = getCellValue(currentRow.getCell(executeIndex));
                String jurisdictionWiseReport = getCellValue(currentRow.getCell(JurisdictionWiseReportIndex));

                if (execute.equalsIgnoreCase("yes")) {
                    if (!masterJson.containsKey(filename.replaceAll(".xlsx", "")))
                        createForesightTestData(filename, product);
                    JSONObject jsonTemp = JsonPath.read(masterJson, "$." + clientName);
                    if (jsonTemp.containsKey(modules) && jurisdictionWiseReport.equalsIgnoreCase("Yes")) {
                        for (String state : jsonTemp.get(modules).toString().trim().split(", ")) {
                            createFeatureFile(clientName, modules, product, filename, state);
                            createRunnerFile(clientName, modules, state);
                        }
                    } else {
                        createFeatureFile(clientName, modules, product, filename, "All");
                        createRunnerFile(clientName, modules, "All");
                    }
                }
            }
            FileWriter jsonTestData = new FileWriter(EnumsCommon.ABSOLUTE_FILES_PATH.getText() + jsonFile);
            BufferedWriter writer = new BufferedWriter(jsonTestData);
            writer.write(gson.toJson(jsonObject));
            writer.close();
            createUniqueCounter();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    /**
     * Create input JSON for multiple sheets
     * @param excelFile
     * @param product
     */
    public void createForesightTestData(String excelFile, String product) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFile;
        JSONObject jsonRows = new JSONObject();
        String fieldList = "";

        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {

            processSheet(workbook.getSheet("Data List"), jsonRows);
            processSheet(workbook.getSheet("Ceding Carrier Custom List"), jsonRows);
            processJurisdictionMappingSheet(workbook.getSheet("ModulesJurisdictionMapping"), jsonRows);
            processEAppWizardSpecSheet(workbook.getSheet("E-App Wizard Spec"), jsonRows, fieldList, product);

            masterJson.put(excelFile.replaceAll(".xlsx", ""), jsonRows);
            masterJson.put("commonTestData", getJsonObject());
            jsonObject.put("testData", masterJson);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    private void processSheet(Sheet sheet, JSONObject jsonRows) {
        Iterator<Row> iterator = sheet.iterator();
        Row headerRow = iterator.next().getSheet().getRow(0);

        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            extractRowData(headerRow, currentRow, jsonRows);
        }
    }

    /**
     * Create key value pair with columnName as key and cell value as value for a JSONObject jsonRows
     *
     * @param headerRow
     * @param currentRow
     * @param jsonRows
     */
    private void extractRowData(Row headerRow, Row currentRow, JSONObject jsonRows) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = currentRow.getCell(i);
            String excelValue = getCellValue(cell, jsonRows);
            if (!excelValue.isEmpty()) {
                if (jsonRows.containsKey(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")))
                    jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), jsonRows.get(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")).toString() + ", " + excelValue);
                else
                    jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), excelValue);
            }
        }
    }

    /**
     * Read ModuleJurisdcitionMapping sheet and create input json and put entries in JSONObject jsonRows
     *
     * @param sheet
     * @param jsonRows
     */
    private void processJurisdictionMappingSheet(Sheet sheet, JSONObject jsonRows) {
        Iterator<Row> iterator = sheet.iterator();
        Row headerRow = iterator.next().getSheet().getRow(0);
        String states = "";

        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            JSONObject tempJson = new JSONObject();

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = currentRow.getCell(i);
                String excelValue = getCellValue(cell, jsonRows);
                if (!excelValue.isEmpty()) {
                    tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
                }
            }
            jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.JURISDICTION.getText())).getStringCellValue().trim(), tempJson);

            String[] jurisdictionModules = tempJson.get("Module").toString().trim().split(", ");
            for (String jurisdictionModule : jurisdictionModules) {
                jsonRows.putIfAbsent(jurisdictionModule, "Alabama");
                jsonRows.put(jurisdictionModule, jsonRows.get(jurisdictionModule).toString().contains(tempJson.get("Jurisdiction").toString()) ? jsonRows.get(jurisdictionModule) : jsonRows.get(jurisdictionModule) + ", " + tempJson.get("Jurisdiction"));
            }
            states += "," + tempJson.get("Jurisdiction").toString().trim();
        }
        jsonRows.put("JurisdictionRules", states.replaceFirst(",", ""));
    }

    /**
     * Read wizard spec sheet and create input json
     *
     * @param sheet
     * @param jsonRows
     * @param fieldList
     * @param product
     */
    private void processEAppWizardSpecSheet(Sheet sheet, JSONObject jsonRows, String fieldList, String product) {
        Row headerRow;
        Iterator<Row> iterator = sheet.iterator();

        // Assuming the first row contains headers
        headerRow = iterator.next().getSheet().getRow(0);

        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            JSONObject tempJson = new JSONObject();
            JSONObject tempJsonReplacement;

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                if (requiredColumns.contains(headerRow.getCell(i).getStringCellValue().trim())) {
                    Cell cell = currentRow.getCell(i);
                    String excelValue = getCellValue(cell, jsonRows);
                    if (!excelValue.isEmpty()) {
                        tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
                    }
                }
            }
            tempJsonReplacement = new JSONObject(tempJson);
            if (tempJson.get("ModuleSectionName").equals("Replacements Module")) {
                List<String> numberExchanges = new ArrayList<>(Arrays.asList(jsonRows.get("NumberofExchanges/Transfers/Rollovers").toString().trim().split(", ")));
                numberExchanges.removeAll(Arrays.asList("Blank"));
                for (String exchange : numberExchanges) {
                    tempJson.replaceAll((key, value) -> value.toString().replaceAll("X", exchange).replaceAll("Number_Transfers > 1", "Number_Transfers = " + exchange));
                    jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim().replaceAll("X", exchange), tempJson);
                    fieldList += ", " + currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim().replaceAll("X", exchange);
                    tempJson = new JSONObject(tempJsonReplacement);
                }
            } else {
                jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim(), tempJson);
                fieldList += ", " + currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim();
            }
        }
        jsonRows.put("product", product);
        jsonRows.put("fieldList", fieldList.replaceFirst(", ", ""));
    }

    /**
     * Delete existing feature and runner files
     *
     * @param folderPath
     */
    private void deleteRunnerFeature(String folderPath) {
        File folder = new File(folderPath);

        try {
            Path directory = Paths.get(String.valueOf(folder));
            // Recursively delete the directory and its contents
            Files.walk(directory)
                    .sorted(Collections.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            System.out.println("Runner & Feature Folder deleted successfully.");
        } catch (IOException e) {
            System.err.println("Failed to delete the folder: " + e.getMessage());
        }
    }

    /**
     * get value of a cell and make necessary adjustments to each cell.
     * handle "or" and create separate rule
     * handle "<>" operator
     * handle "+" operator
     * handle ">=" and "<=" operators
     *
     * @param cell
     * @param jsonRows
     * @return cell value
     */
    private String getCellValue(Cell cell, JSONObject jsonRows) {
        String excelValue = "";
        String newValue = "";
        Pattern pattern = Pattern.compile("");

        List<String> listRules = new ArrayList<>();
        if (cell != null && cell.getCellType() == CellType.STRING && !(cell.getStringCellValue().trim().equalsIgnoreCase("None"))) {
            excelValue = cell.getStringCellValue().trim();
            excelValue = excelValue.replaceAll("//", "/").replaceAll("[^\\x00-\\x7F^–]", "").replaceAll("–", "-").replaceAll("\n", ";").replaceAll("=", " = ").replaceAll("<>", " <> ").replaceAll("“", "").replaceAll("\"", "").replaceAll("[\\s]+[.]+", ".").replaceAll("[\\s]+", " ").replaceAll("> =", ">=").replaceAll("< =", "<=").trim();
            if (excelValue.contains(" OR ")) {
                listRules = Arrays.asList(excelValue.split(";"));
                for (String rule : listRules) {
                    JSONObject values = new JSONObject();
                    List<String> resultValue = new ArrayList<>();
                    if (rule.contains(" OR ") & !(rule.toLowerCase().contains("skip for automation"))) {
                        rule = rule.replaceAll("\\[", "").replaceAll("]", "");
                        if (rule.toLowerCase().contains("("))
                            pattern = Pattern.compile("(.*?)\\((.*?)\\)(.*)");
                        else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?").matcher(rule).find())
                            pattern = Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?");
                        else if (Pattern.compile("(\\d+\\.\\s*)?(.*?)").matcher(rule).find())
                            pattern = Pattern.compile("(\\d+\\.\\s*)?(.*?)");

                        if (!pattern.toString().equals("")) {
                            Matcher matcher = pattern.matcher(rule);

                            while (matcher.find()) {
                                List<String> orConditions = Arrays.asList(matcher.group(2).split(" OR "));
                                if (orConditions.size() > 1) {
                                    for (String condition : orConditions)
                                        newValue += rule.replaceAll(matcher.group(2), condition).replaceAll("\\(", "").replaceAll("\\)", "") + ";";
                                } else {
                                    for (String condition : rule.split(" OR "))
                                        newValue += condition + ";";
                                    break;
                                }
                            }
                        } else
                            newValue += rule + ";";
                    } else
                        newValue += rule + ";";
                }
                excelValue = newValue.substring(0, newValue.length() - 1);
            }
            newValue = "";
            if (excelValue.toLowerCase().contains("<>")) {
                listRules = Arrays.asList(excelValue.split(";"));
                for (String rule : listRules) {
                    JSONObject values = new JSONObject();
                    List<String> resultValue = new ArrayList<>();
                    if (rule.toLowerCase().contains("<>") & !(rule.toLowerCase().contains("skip for automation"))) {
                        String conditionAnother = "";
                        String expectedResult = "";
                        if (Pattern.compile("(?i)([^<>\\s]+)\\s*<>(.*?)(?:AND|then|$)").matcher(rule).find()) {
                            pattern = Pattern.compile("(?i)([^<>\\s]+)\\s*<>(.*?)(?:AND|then|$)");
                            Matcher matcher = pattern.matcher(rule);
                            while (matcher.find()) {
                                conditionAnother = matcher.group(1).trim();
                                expectedResult = matcher.group(2).trim();
                                expectedResult = (expectedResult.endsWith(",")) ? expectedResult.substring(0, expectedResult.length() - 1) : expectedResult;
                                if (jsonRows.containsKey(conditionAnother)) {
                                    values = (JSONObject) jsonRows.get(conditionAnother);
                                    if (values.containsKey("ListOptions") && !(values.get("ListOptions").toString().trim().contains("Number"))) {
                                        resultValue = new ArrayList<>(Arrays.asList(jsonRows.get(values.get("ListOptions").toString().trim().replaceAll(" ", "")).toString().trim().split(", ")));

                                        Set<String> valuesToRemove = new HashSet<>(Arrays.asList("Blank", expectedResult));
                                        // Remove elements that match the given condition
                                        resultValue.removeIf(valuesToRemove::contains);
                                        newValue += rule.replace(expectedResult, String.join(", ", resultValue)).replace("<>", "=") + ";";
                                    } else
                                        newValue += rule + ";";
                                } else
                                    newValue += rule + ";";
                            }
                        } else
                            newValue += rule + ";";
                    } else
                        newValue += rule + ";";
                }
                excelValue = newValue.substring(0, newValue.length() - 1);
            }
            newValue = "";
            if (excelValue.toLowerCase().contains("+")) {
                String conditionAnother = "";
                String expectedResult = "";
                String expectedOperator = "";
                float value;
                for (String rule : excelValue.split(";")) {
                    String temp = "";
                    if (Pattern.compile("(?i)\\((.*?)\\)\\s*(<|>|<>|=)(.*?)(?:,|AND|then|$)").matcher(rule).find()) {
                        pattern = Pattern.compile("(?i)\\((.*?)\\)\\s*(<|>|<>|=)(.*?)(?:,|AND|then|$)");
                        Matcher matcher = pattern.matcher(rule);
                        while (matcher.find()) {
                            conditionAnother = matcher.group(1).trim();
                            expectedOperator = matcher.group(2).trim();
                            expectedResult = matcher.group(3).trim();
                        }
                        listRules = Arrays.asList(conditionAnother.split("\\+"));
                        value = Float.parseFloat(expectedResult);
                        if (expectedOperator.equals(">"))
                            value = value + 1;
                        else if (expectedOperator.equals("<"))
                            value = value - 1;
                        value = value / listRules.size();
                        for (String eachCondition : listRules)
                            temp += " AND " + eachCondition.trim() + " = " + value;
                        newValue += rule.replaceAll("(?i)\\((.*?)\\)\\s*(<|>|<>|=)(.*?)", temp.replaceFirst(" AND ", "")).replaceAll(" " + expectedResult, "") + ";";
                    } else
                        newValue += rule + ";";
                }
                excelValue = newValue.substring(0, newValue.length() - 1);
            }
            newValue = "";
            if (excelValue.toLowerCase().contains(">=") | excelValue.toLowerCase().contains("<=")) {
                String expectedOperator = "";
                for (String rule : excelValue.split(";")) {
                    if (Pattern.compile("(.*?)\\s*(>=|<=)\\s*(.*)").matcher(rule).find()) {
                        pattern = Pattern.compile("(.*?)\\s*(>=|<=)\\s*(.*)");
                        Matcher matcher = pattern.matcher(rule);
                        while (matcher.find()) {
                            expectedOperator = matcher.group(2).trim();
                        }
                        for(int i =0;i<expectedOperator.length();i++)
                            newValue += rule.replaceAll(expectedOperator, expectedOperator.substring(i, i+1)) + ";";
                    } else
                        newValue += rule + ";";
                }
                excelValue = newValue.substring(0, newValue.length() - 1);
            }
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC)
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
        return excelValue;
    }

    private String getCellValue(Cell cell) {
        String excelValue = "";

        if (cell != null && cell.getCellType() == CellType.STRING) {
            excelValue = cell.getStringCellValue().trim();
            if (excelValue.contains("//"))
                excelValue = excelValue.replaceAll("//", "/");
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
        }
        return excelValue;
    }

    /**
     * common test data in a json
     * @return JSONObject
     */
    private JSONObject getJsonObject() {
        JSONObject defaultEntry = new JSONObject();
        defaultEntry.put("InvalidTin", "123456789");
        defaultEntry.put("InvalidSSN", "123456789");
        defaultEntry.put("InvalidDate", "02292023");
        defaultEntry.put("InvalidEmail", "test,test@,testgmail.com,%%@test.c,test@gmail .com,example.com,example@@example.com");
        return defaultEntry;
    }

    /**
     * index of a given column in excel
     *
     * @param headerRow
     * @param columnName
     * @return
     */
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

    /**
     * Create feature file based on module and jurisdiction
     * @param client
     * @param module
     * @param product
     * @param fileName
     * @param state
     */
    public void createFeatureFile(String client, String module, String product, String fileName, String state) {
        ArrayList<String> lines = new ArrayList<>();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.FEATUREFILESPATH.getText() + "End2End/E2EWizardTestFlow.feature"));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("ModuleName", module).replaceAll("ModuleTag", module.replaceAll(" ", "")).replaceAll("Client", client);
                line = line.replaceAll("productName", product).replaceAll("fileName", fileName).replaceAll("state", state);
                lines.add(line);
            }  //end if
            reader.close();
            File tempFile = new File(EnumsCommon.FEATUREFILESPATH.getText() + "ForesightTest/" + client + "_" + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + ".feature");
            tempFile.getParentFile().mkdirs();
            FileWriter featureFile = new FileWriter(tempFile);
            BufferedWriter writer = new BufferedWriter(featureFile);
            for (String line1 : lines)
                writer.write(line1 + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create runner file based on module and jurisdiction
     * @param client
     * @param module
     * @param state
     */
    public void createRunnerFile(String client, String module, String state) {
        ArrayList<String> lines = new ArrayList<>();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.RUNNERFILESPATH.getText() + "RunFireLightTest.java"));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("com.hexure.firelight.runner", "com.hexure.firelight.runner.ForeSightTest");
                line = line.replaceAll("ModuleName", module).replaceAll("ModuleTag", module.replaceAll(" ", "")).replaceAll("Client", client).replaceAll("State", state.replaceAll(" ", ""));
                line = replaceLine(line, "features = {", "\t\tfeatures = {\"src/test/resources/features/ForesightTest/" + client + "_" + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + ".feature\"},");
                line = replaceLine(line, "tags = {", "\t\ttags = {\"@" + module.replaceAll(" ", "") + "\"},");
                line = replaceLine(line, "public class RunFireLightTest {", "public class RunForesight" + client + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + "Test" + " {");
                line = replaceLine(line, "core.run(RunFireLightTest.class);", "\t\tcore.run(RunForesight" + client + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + "Test" + ".class);");
                lines.add(line);
            }  //end if
            reader.close();
            File tempFile = new File(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest/RunForesight" + client + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + "Test" + ".java");
            tempFile.getParentFile().mkdirs();
            FileWriter runnerFile = new FileWriter(tempFile);
            BufferedWriter writer = new BufferedWriter(runnerFile);
            for (String line1 : lines)
                writer.write(line1 + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create runner file for each re-run
     */
    public void createUniqueCounter() {
        ArrayList<String> lines = new ArrayList<>();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.RUNNERFILESPATH.getText() + "UniqueTestCounter.java"));
            while ((line = reader.readLine()) != null) {
                line = replaceLine(line, "package com.hexure.firelight.runner;", "package com.hexure.firelight.runner.ForeSightTest;");
                lines.add(line);
            }  //end if
            reader.close();
            File tempFile = new File(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest/UniqueTestCounter.java");
            tempFile.getParentFile().mkdirs();
            FileWriter runnerFile = new FileWriter(tempFile);
            BufferedWriter writer = new BufferedWriter(runnerFile);
            for (String line1 : lines)
                writer.write(line1 + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Replace line with given parameters
     * @param line
     * @param toBeReplaced
     * @param replacement
     * @return
     */
    public String replaceLine(String line, String toBeReplaced, String replacement) {
        return line.contains(toBeReplaced) ? replacement : line;
    }
}