package com.hexure.firelight.pages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hexure.firelight.libraies.Enums.EnumsExcelColumns;
import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLException;
import com.hexure.firelight.libraies.FLUtilities;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class E2EFlowDataPage extends FLUtilities {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    List<String> requiredColumns = Arrays.asList(EnumsExcelColumns.ENUMSEXCELCOLUMNS.getText().split(", "));
    JSONObject jsonObject = new JSONObject();
    JSONObject masterJson = new JSONObject();

    public E2EFlowDataPage(WebDriver driver) {
        initElements(driver);
    }

    public static String getCellValue(Cell cell) {
        // Initialize a string to store the cell value
        String excelValue ="";

        // Check if the cell is not null and its type is STRING
        if (cell != null && cell.getCellType() == CellType.STRING) {
            // Get the string value of the cell and trim any leading or trailing whitespace
            excelValue = cell.getStringCellValue().trim();

            // If the string contains double slashes ("//"), replace them with a single slash ("/")
            if (excelValue.contains("//")) {
                excelValue = excelValue.replaceAll("//", "/");
            }
            // Check if the cell is not null and its type is NUMERIC
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            // Convert the numeric value of the cell to a string and trim any leading or trailing whitespace
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
            // If the cell is null or neither STRING nor NUMERIC
        }
        return excelValue;
    }

    /**
     * common test data in a json
     *
     * @return JSONObject
     */
    public static JSONObject getJsonObject() {
        JSONObject defaultEntry = new JSONObject();
        defaultEntry.put("InvalidTin", "123456789");
        defaultEntry.put("InvalidSSN", "123456789");
        defaultEntry.put("InvalidDate", "02292023");
        defaultEntry.put("InvalidEmail", "test,test@,testgmail.com,%%@test.c,test@gmail .com,example.com,example@@example.com");
        return defaultEntry;
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public JSONObject addCellValueToJson(Row headerRow, Row currentRow, JSONObject tempJson) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            // Get the corresponding cell from the current row
            Cell cell = currentRow.getCell(i);
            String excelValue = getCellValue(cell);
            // Check if the cell value is not empty
            if (!excelValue.isEmpty())
                // Get the header name for the current column, remove spaces from it, and use it as the JSON key
                tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), excelValue);
        }
        return tempJson;
    }

    public void createForesightTestDataInterface(String jsonFile, String excelFile) {
        // Define the file path using a common absolute path and the provided Excel file name
        String filePath = EnumsCommon.ABSOLUTE_FILES_PATH.getText() + excelFile;

        // Use try-with-resources to ensure the FileInputStream and XSSFWorkbook are closed properly
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {

            // Get the sheet named "Interface" from the workbook
            Sheet sheet = workbook.getSheet("Interface");
            Iterator<Row> iterator = sheet.iterator();

            // Retrieve the header row
            Row headerRow = iterator.next().getSheet().getRow(0);

            // Find the column indexes for specific headers in the header row
            int clientNameIndex = findColumnIndex(headerRow, "Client Name");
            int productIndex = findColumnIndex(headerRow, "Product");
            int modulesIndex = findColumnIndex(headerRow, "Modules");
            int filenameIndex = findColumnIndex(headerRow, "FileName");
            int executeIndex = findColumnIndex(headerRow, "Execute");
            int jurisdictionWiseReportIndex = findColumnIndex(headerRow, "JurisdictionWiseReport");
            int inboundTypeIndex = findColumnIndex(headerRow, "Inbound Type");
            int inboundIndex = findColumnIndex(headerRow, "Inbound");
            int outboundIndex = findColumnIndex(headerRow, "Outbound");

            // Delete existing runner and feature files
            deleteRunnerFeature(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest");
            deleteRunnerFeature(EnumsCommon.FEATUREFILESPATH.getText() + "ForesightTest");

            // Iterate through the rows of the sheet, starting from the second row
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                // Retrieve cell values from the current row based on the header indexes
                String clientName = getCellValue(currentRow.getCell(clientNameIndex));
                String product = getCellValue(currentRow.getCell(productIndex));
                String modules = getCellValue(currentRow.getCell(modulesIndex));
                String filename = getCellValue(currentRow.getCell(filenameIndex));
                String execute = getCellValue(currentRow.getCell(executeIndex));
                String jurisdictionWiseReport = getCellValue(currentRow.getCell(jurisdictionWiseReportIndex));
                String inboundType = getCellValue(currentRow.getCell(inboundTypeIndex));
                String inbound = getCellValue(currentRow.getCell(inboundIndex));
                String outbound = getCellValue(currentRow.getCell(outboundIndex));

                // Check if the 'Execute' column value is 'yes'
                if (execute.equalsIgnoreCase("yes")) {
                    // If the filename is not already a key in masterJson, create foresight test data
                    if (!masterJson.containsKey(filename.replaceAll(".xlsx", ""))) {
                        createForesightTestData(filename, product);
                    }

                    // Read the JSON data for the current client
                    JSONObject jsonTemp = JsonPath.read(masterJson, "$." + clientName);

                    // If the module exists in JSON test data for a client and JurisdictionWiseReport is 'Yes'
                    if (jsonTemp.containsKey(modules) && jurisdictionWiseReport.equalsIgnoreCase("Yes")) {
                        // Iterate through the states and create feature and runner files for each state
                        for (String state : jsonTemp.get(modules).toString().trim().split(", ")) {
                            createFeatureFile(clientName, modules, product, filename, state);
                            createRunnerFile(clientName, modules, state);
                        }
                    } else {
                        if(inboundType.equalsIgnoreCase("Direct Login"))
                        // Create feature and runner files for all jurisdictions
                        createFeatureFile(clientName, modules, product, filename, "All");
                        createRunnerFile(clientName, modules, "All");
                    }
                }
            }

            // Write the JSON test data to the specified JSON file
            FileWriter jsonTestData = new FileWriter(EnumsCommon.ABSOLUTE_FILES_PATH.getText() + jsonFile);
            BufferedWriter writer = new BufferedWriter(jsonTestData);
            writer.write(gson.toJson(jsonObject));
            writer.close();

            // Create a unique counter which will keep track of rerun count
            createUniqueCounter();

        } catch (IOException e) {
            // Handle exceptions related to file access
            throw new FLException("File is inaccessible: " + e.getMessage());
        } catch (Exception e) {
            // Handle any other exceptions that may occur
            throw new FLException("Reading Properties File Failed: " + e.getMessage());
        }
    }

    /**
     * Create input JSON for multiple sheets
     *
     * @param excelFile - Spec file
     * @param product   - Product to be used to create FL application
     */
    public void createForesightTestData(String excelFile, String product) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFile;
        JSONObject jsonRows = new JSONObject();
        String fieldList = "";

        // Read excel file to create test data
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {

            // Verify if sheet exists, create test data for that sheet
            if (verifySheetExists(workbook, "Data List"))
                processSheet(workbook.getSheet("Data List"), jsonRows);
            if (verifySheetExists(workbook, "Ceding Carrier Custom List"))
                processSheet(workbook.getSheet("Ceding Carrier Custom List"), jsonRows);
            if (verifySheetExists(workbook, "ModulesJurisdictionMapping"))
                processJurisdictionMappingSheet(workbook.getSheet("ModulesJurisdictionMapping"), jsonRows);
            if (verifySheetExists(workbook, "E-App Wizard Spec"))
                processEAppWizardSpecSheet(workbook.getSheet("E-App Wizard Spec"), jsonRows, fieldList, product);

            // JSON entry for a client with all processed sheets
            masterJson.put(excelFile.replaceAll(".xlsx", ""), jsonRows);
            masterJson.put("commonTestData", getJsonObject());
            jsonObject.put("testData", masterJson);

        } catch (IOException e) {
            throw new FLException("File is inaccessible" + e.getMessage());
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    /**
     * Verify if sheet exists in a workbook
     * @param workbook - Excel file name
     * @param sheetName - Sheet name
     * @return true if sheet exists
     */
    public boolean verifySheetExists(XSSFWorkbook workbook, String sheetName) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.getSheetAt(i).getSheetName().contentEquals(sheetName))
                //use  Writeableworkbookobj.getSheet(); to return the specific sheet and write the data into the sheet
                return true;
        }
        return false;
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
     * @param headerRow  - Header row of an Excel Spec
     * @param currentRow - Pointer to a row under observation
     * @param jsonRows   - JSONObject jsonRows
     */
    private void extractRowData(Row headerRow, Row currentRow, JSONObject jsonRows) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = currentRow.getCell(i);
            String excelValue = getCellValue(cell, jsonRows);

            // If key exists, add value to existing key else create new key value pair
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
     * Key - jurisdicition and values - All modules eligible for that jurisdcition
     *
     * @param sheet    - Sheet name
     * @param jsonRows - JSONObject jsonRows
     */
    private void processJurisdictionMappingSheet(Sheet sheet, JSONObject jsonRows) {
        Iterator<Row> iterator = sheet.iterator();
        Row headerRow = iterator.next().getSheet().getRow(0);
        StringBuilder states = new StringBuilder();

        // iterating over all rows of a sheet
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            JSONObject tempJson = new JSONObject();

            // Create key value pair of Jurisdicton and corresponding modules
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = currentRow.getCell(i);
                String excelValue = getCellValue(cell, jsonRows);
                if (!excelValue.isEmpty()) {
                    tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
                }
            }
            jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.JURISDICTION.getText())).getStringCellValue().trim(), tempJson);

            // All modules are valid for Alabama, so another key value pair with key as module and value as list of valid jurisdictions are created with Alabama as one of the value
            String[] jurisdictionModules = tempJson.get("Module").toString().trim().split(", ");
            for (String jurisdictionModule : jurisdictionModules) {
                jsonRows.putIfAbsent(jurisdictionModule, "Alabama");
                jsonRows.put(jurisdictionModule, jsonRows.get(jurisdictionModule).toString().contains(tempJson.get("Jurisdiction").toString()) ? jsonRows.get(jurisdictionModule) : jsonRows.get(jurisdictionModule) + ", " + tempJson.get("Jurisdiction"));
            }
            states.append(",").append(tempJson.get("Jurisdiction").toString().trim());
        }
        jsonRows.put("JurisdictionRules", states.toString().replaceFirst(",", ""));
    }

    /**
     * Read wizard spec sheet and create input json
     *
     * @param sheet     - Sheet name
     * @param jsonRows  - JSONObject jsonRows
     * @param fieldList - list of all the validated fields
     * @param product   - Product to create FL application
     */
    private void processEAppWizardSpecSheet(Sheet sheet, JSONObject jsonRows, String fieldList, String product) {
        Row headerRow;
        Iterator<Row> iterator = sheet.iterator();

        headerRow = iterator.next().getSheet().getRow(0);

        StringBuilder fieldListBuilder = new StringBuilder(fieldList);
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
            if (!tempJson.containsKey("ModuleSectionName"))
                tempJson.put("ModuleSectionName", "");
            if (!tempJson.containsKey("CommonTag"))
                tempJson.put("CommonTag", "");
            if (!tempJson.containsKey("Order"))
                tempJson.put("Order", "");

            // For replacements module, create fields for given number of transfers/exchanges
            if (tempJson.get("ModuleSectionName").equals("Replacements Module")) {
                List<String> numberExchanges = new ArrayList<>(Arrays.asList(jsonRows.get("NumberofExchanges/Transfers/Rollovers").toString().trim().split(", ")));
                numberExchanges.removeAll(Collections.singletonList("Blank"));
                for (String exchange : numberExchanges) {
                    tempJson.replaceAll((key, value) -> value.toString().replaceAll("X", exchange).replaceAll("Number_Transfers > 1", "Number_Transfers = " + exchange));
                    jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim().replaceAll("X", exchange), tempJson);
                    fieldListBuilder.append(", ").append(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim().replaceAll("X", exchange));
                    tempJson = new JSONObject(tempJsonReplacement);
                }
            } else {
                jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim(), tempJson);
                fieldListBuilder.append(", ").append(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.FIELD.getText())).getStringCellValue().trim());
            }
        }
        fieldList = fieldListBuilder.toString();
        jsonRows.put("product", product);
        jsonRows.put("fieldList", fieldList.replaceFirst(", ", ""));
    }

    /**
     * Delete existing feature and runner files
     *
     * @param folderPath - Folder in which reports are saved
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
     * @param cell     - Cell of an Excel
     * @param jsonRows - JSONObject jsonRows
     * @return cell value - Cell value of a cell under test
     */
    private String getCellValue(Cell cell, JSONObject jsonRows) {
        String excelValue = "";

        if (cell != null && cell.getCellType() == CellType.STRING && !(cell.getStringCellValue().trim().equalsIgnoreCase("None"))) {
            excelValue = cell.getStringCellValue().trim();
            excelValue = excelValue.replaceAll("[^\\x00-\\x7F^–]", "");
            excelValue = excelValue.replaceAll("–", "-");
            excelValue = excelValue.replaceAll("\n", ";");
            excelValue = excelValue.replaceAll("=", " = ");
            excelValue = excelValue.replaceAll("<>", " <> ");
            excelValue = excelValue.replaceAll("“", "");
            excelValue = excelValue.replaceAll("\"", "");
            excelValue = excelValue.replaceAll("\\s+[.]+", ".");
            excelValue = excelValue.replaceAll("\\s+", " ");
            excelValue = excelValue.replaceAll("> =", ">=");
            excelValue = excelValue.replaceAll("< =", "<=");
            excelValue.trim();
            if (excelValue.contains(" OR "))
                excelValue = handleOrConditions(excelValue);
            if (excelValue.toLowerCase().contains("<>"))
                excelValue = handleNotEqual(excelValue, jsonRows);
            if (excelValue.toLowerCase().contains("+"))
                excelValue = handleAddition(excelValue);
            if (excelValue.toLowerCase().contains(">=") | excelValue.toLowerCase().contains("<="))
                excelValue = handleComparisonOperators(excelValue);
        } else if (cell != null && cell.getCellType() == CellType.NUMERIC)
            excelValue = String.valueOf(((XSSFCell) cell).getRawValue()).trim();
        return excelValue;
    }

    /**
     * Handle >= and <= operators. Using boundary value analysis, create multiple values needed to test
     *
     * @param excelValue
     * @return
     */
    public String handleComparisonOperators(String excelValue) {
        String expectedOperator = "";
        Pattern pattern;
        StringBuilder newValue = new StringBuilder();

        for (String rule : excelValue.split(";")) {
            if (Pattern.compile("(.*?)\\s*(>=|<=)\\s*(.*)").matcher(rule).find()) {
                pattern = Pattern.compile("(.*?)\\s*(>=|<=)\\s*(.*)");
                Matcher matcher = pattern.matcher(rule);
                while (matcher.find()) {
                    expectedOperator = matcher.group(2).trim();
                }
                for (int i = 0; i < expectedOperator.length(); i++)
                    newValue.append(rule.replaceAll(expectedOperator, expectedOperator.substring(i, i + 1))).append(";");
            } else
                newValue.append(rule).append(";");
        }
        excelValue = newValue.substring(0, newValue.length() - 1);
        return excelValue;
    }

    /**
     * Handle addition operator.
     *
     * @param excelValue
     * @return
     */
    public String handleAddition(String excelValue) {
        String conditionAnother = "";
        String expectedResult = "";
        String expectedOperator = "";
        List<String> listRules;
        StringBuilder newValue = new StringBuilder();
        Pattern pattern;

        float value;
        for (String rule : excelValue.split(";")) {
            StringBuilder temp = new StringBuilder();
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
                    temp.append(" AND ").append(eachCondition.trim()).append(" = ").append(value);
                newValue.append(rule.replaceAll("(?i)\\((.*?)\\)\\s*(<|>|<>|=)(.*?)", temp.toString().replaceFirst(" AND ", "")).replaceAll(" " + expectedResult, "")).append(";");
            } else
                newValue.append(rule).append(";");
        }
        excelValue = newValue.substring(0, newValue.length() - 1);
        return excelValue;
    }

    /**
     * Handle OR conditions
     *
     * Split rules in multiple rules if OR exists in the rules
     * @param excelValue
     * @return
     */
    public String handleOrConditions(String excelValue) {
        String[] listRules = excelValue.split(";");
        StringBuilder newValue = new StringBuilder();
        Pattern pattern = Pattern.compile("");

        for (String rule : listRules) {
            if (rule.contains(" OR ") & !(rule.toLowerCase().contains("skip for automation"))) {
                rule = rule.replaceAll("\\[", "").replaceAll("]", "");
                if (rule.toLowerCase().contains("("))
                    pattern = Pattern.compile("(.*?)\\((.*?)\\)(.*)");
                else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?").matcher(rule).find())
                    pattern = Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?");
                else if (Pattern.compile("(\\d+\\.\\s*)?(.*?)").matcher(rule).find())
                    pattern = Pattern.compile("(\\d+\\.\\s*)?(.*?)");

                if (!pattern.toString().isEmpty()) {
                    Matcher matcher = pattern.matcher(rule);

                    while (matcher.find()) {
                        List<String> orConditions = Arrays.asList(matcher.group(2).split(" OR "));
                        if (orConditions.size() > 1) {
                            for (String condition : orConditions)
                                newValue.append(rule.replaceAll(matcher.group(2), condition).replaceAll("\\(", "").replaceAll("\\)", "")).append(";");
                        } else {
                            for (String condition : rule.split(" OR "))
                                newValue.append(condition).append(";");
                            break;
                        }
                    }
                } else
                    newValue.append(rule).append(";");
            } else
                newValue.append(rule).append(";");
        }
        excelValue = newValue.substring(0, newValue.length() - 1);
        return excelValue;
    }

    /**
     * Handle Not Equal to operator and convert not equal operator to equal to operator.
     *
     * @param excelValue
     * @param jsonRows
     * @return
     */
    public String handleNotEqual(String excelValue, JSONObject jsonRows) {
        String[] listRules = excelValue.split(";");
        StringBuilder newValue = new StringBuilder();
        Pattern pattern;

        for (String rule : listRules) {
            JSONObject values;
            List<String> resultValue;
            if (rule.toLowerCase().contains("<>") & !(rule.toLowerCase().contains("skip for automation"))) {
                String conditionAnother;
                String expectedResult;
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
                                newValue.append(rule.replace(expectedResult, String.join(", ", resultValue)).replace("<>", "=")).append(";");
                            } else
                                newValue.append(rule).append(";");
                        } else
                            newValue.append(rule).append(";");
                    }
                } else
                    newValue.append(rule).append(";");
            } else
                newValue.append(rule).append(";");
        }
        excelValue = newValue.substring(0, newValue.length() - 1);
        return excelValue;
    }

    /**
     * Create feature file based on module and jurisdiction
     *
     * @param client   - Client name
     * @param module   - module name
     * @param product  - product name
     * @param fileName - Spec from client
     * @param state    - Jurisdiction to create FL application
     */
    public void createFeatureFile(String client, String module, String product, String fileName, String state) {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.FEATUREFILESPATH.getText() + "End2End/E2EWizardTestFlow.feature"));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("ModuleName", module).replaceAll("ModuleTag", module.replaceAll(" ", "")).replaceAll("Client", client);
                line = line.replaceAll("productName", product).replaceAll("fileName", fileName).replaceAll("state", state);
                lines.add(line);
            }
            reader.close();
            File tempFile = new File(EnumsCommon.FEATUREFILESPATH.getText() + "ForesightTest/" + client + "_" + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + ".feature");
            tempFile.getParentFile().mkdirs();
            FileWriter featureFile = new FileWriter(tempFile);
            BufferedWriter writer = new BufferedWriter(featureFile);
            for (String line1 : lines)
                writer.write(line1 + "\n");
            writer.close();
        } catch (IOException e) {
            throw new FLException("File is inaccessible" + e.getMessage());
        }
    }

    /**
     * Create runner file based on module and jurisdiction
     *
     * @param client - Client name
     * @param module - module name
     * @param state  - Jurisdiction to create FL application
     */
    public void createRunnerFile(String client, String module, String state) {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.RUNNERFILESPATH.getText() + "RunFireLightTest.java"));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("com.hexure.firelight.runner", "com.hexure.firelight.runner.ForeSightTest");
                line = line.replaceAll("ModuleName", module).replaceAll("ModuleTag", module.replaceAll(" ", "")).replaceAll("Client", client).replaceAll("State", state.replaceAll(" ", ""));
                line = replaceLine(line, "features = {", "\t\tfeatures = {\"src/test/resources/features/ForesightTest/" + client + "_" + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + ".feature\"},");
                line = replaceLine(line, "tags = ", "\t\ttags = \"@" + module.replaceAll(" ", "") + "\",");
                line = replaceLine(line, "public class RunFireLightTest {", "public class RunForesight" + client + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + "Test" + " {");
                line = replaceLine(line, "core.run(RunFireLightTest.class);", "\t\tcore.run(RunForesight" + client + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + "Test" + ".class);");
                lines.add(line);
            }
            reader.close();
            File tempFile = new File(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest/RunForesight" + client + module.replaceAll(" ", "") + "_" + state.replaceAll(" ", "") + "Test" + ".java");
            tempFile.getParentFile().mkdirs();
            FileWriter runnerFile = new FileWriter(tempFile);
            BufferedWriter writer = new BufferedWriter(runnerFile);
            for (String line1 : lines)
                writer.write(line1 + "\n");
            writer.close();
        } catch (IOException e) {
            throw new FLException("File is inaccessible" + e.getMessage());
        }
    }

    /**
     * Create runner file for each re-run
     */
    public void createUniqueCounter() {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.RUNNERFILESPATH.getText() + "UniqueTestCounter.java"));
            while ((line = reader.readLine()) != null) {
                line = replaceLine(line, "package com.hexure.firelight.runner;", "package com.hexure.firelight.runner.ForeSightTest;");
                lines.add(line);
            }
            reader.close();
            File tempFile = new File(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest/UniqueTestCounter.java");
            tempFile.getParentFile().mkdirs();
            FileWriter runnerFile = new FileWriter(tempFile);
            BufferedWriter writer = new BufferedWriter(runnerFile);
            for (String line1 : lines)
                writer.write(line1 + "\n");
            writer.close();
        } catch (IOException e) {
            throw new FLException("File is inaccessible" + e.getMessage());
        }
    }

    /**
     * Replace line with given parameters
     *
     * @param line         - original line
     * @param toBeReplaced - Substring which needs to be replaced
     * @param replacement  - Replace by given string
     * @return Replaced string - Replaced string
     */
    public String replaceLine(String line, String toBeReplaced, String replacement) {
        return line.contains(toBeReplaced) ? replacement : line;
    }

    public void createForesightE2ETestData(String jsonFile, String clientName) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + "E2EFlow.xlsx";
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheet(clientName);

            Iterator<Row> iterator = sheet.iterator();
            Row headerRow = iterator.next().getSheet().getRow(0);
            JSONObject jsonRows = new JSONObject();
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                JSONObject tempJson = new JSONObject();
                String secondData;

                List<String> rowValue = Arrays.asList("jurisdiction", "ProductType");
                if (rowValue.stream().noneMatch(currentRow.getCell(2).getStringCellValue()::equalsIgnoreCase)) {
                    tempJson.putAll(addCellValueToJson(headerRow, currentRow, tempJson));
                    secondData = currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2EDATAITEMID.getText())).getStringCellValue().trim();
                    if (secondData.isEmpty())
                        secondData = currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2ETITLE.getText())).getStringCellValue().trim();
                    jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.E2EWIZARDNAME.getText())).getStringCellValue().trim() + "|" + secondData, tempJson);
                } else
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
            throw new FLException("File is inaccessible" + e.getMessage());
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }
}

