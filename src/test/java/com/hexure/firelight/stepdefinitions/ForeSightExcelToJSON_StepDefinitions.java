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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.util.Collections;


public class ForeSightExcelToJSON_StepDefinitions {

    JSONObject jsonObject = new JSONObject();
    List<String> requiredColumns = Arrays.asList(EnumsExcelColumns.ENUMSEXCELCOLUMNS.getText().split(", "));
    JSONObject masterJson = new JSONObject();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

            deleteRunnerFeature(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest");
            deleteRunnerFeature(EnumsCommon.FEATUREFILESPATH.getText() + "ForesightTest");
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                String clientName = getCellValue(currentRow.getCell(clientNameIndex));
                String product = getCellValue(currentRow.getCell(productIndex));
                String modules = getCellValue(currentRow.getCell(modulesIndex));
                String filename = getCellValue(currentRow.getCell(filenameIndex));
                String execute = getCellValue(currentRow.getCell(executeIndex));

                if (execute.equalsIgnoreCase("yes")) {
                    if (!masterJson.containsKey(filename.replaceAll(".xlsx", "")))
                        createForesightTestData(filename, product);
                    createFeatureFile(clientName, modules, product, filename);
                    createRunnerFile(clientName, modules);
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

    @Given("Create {string} file for eApp flow with file {string}")
    public void createForesightTestData(String excelFile, String product) {
        String filePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + excelFile;
        String fieldList = "";
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            String states = "";

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
                        if (jsonRows.containsKey(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")))
                            jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), jsonRows.get(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "")).toString() + ", " + excelValue);
                        else
                            jsonRows.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", ""), excelValue);
                    }
                }
            }

            sheet = workbook.getSheet("Ceding Carrier Custom List"); // Assuming data is in the first sheet
            iterator = sheet.iterator();

            // Assuming the first row contains headers
            headerRow = iterator.next().getSheet().getRow(0);

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                // Create input file in json format
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

            sheet = workbook.getSheet("ModulesJurisdictionMapping"); // Assuming data is in the first sheet
            iterator = sheet.iterator();

            // Assuming the first row contains headers
            headerRow = iterator.next().getSheet().getRow(0);
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                JSONObject tempJson = new JSONObject();

                // Create input file in json format
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = currentRow.getCell(i);
                    String excelValue = getCellValue(cell, jsonRows);
                    if (!excelValue.isEmpty())
                        tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
                }
                jsonRows.put(currentRow.getCell(findColumnIndex(headerRow, EnumsCommon.JURISDICTION.getText())).getStringCellValue().trim(), tempJson);
                states += "," + tempJson.get("Jurisdiction").toString().trim();
            }
            jsonRows.put("JurisdictionRules", states.replaceFirst(",", ""));

            sheet = workbook.getSheet("E-App Wizard Spec"); // Assuming data is in the first sheet
            iterator = sheet.iterator();

            // Assuming the first row contains headers
            headerRow = iterator.next().getSheet().getRow(0);
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                JSONObject tempJson = new JSONObject();
                JSONObject tempJsonReplacement;

                // Create input file in json format
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    if (requiredColumns.contains(headerRow.getCell(i).getStringCellValue().trim())) {

                        Cell cell = currentRow.getCell(i);
                        String excelValue = getCellValue(cell, jsonRows);
                        System.out.println(excelValue);
                        if (!excelValue.isEmpty())
                            tempJson.put(headerRow.getCell(i).getStringCellValue().replaceAll(" ", "").replaceAll("\n", ""), excelValue);
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
            masterJson.put(excelFile.replaceAll(".xlsx", ""), jsonRows);

            JSONObject defaultEntry = getJsonObject();

            masterJson.put("commonTestData", defaultEntry);

            jsonObject.put("testData", masterJson);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new FLException("Reading Properties File Failed" + e.getMessage());
        }
    }

    private void deleteRunnerFeature(String folderPath) {
        File folder = new File(folderPath);

        try {
            Path directory = Paths.get(String.valueOf(folder));
            // Recursively delete the directory and its contents
            Files.walk(directory)
                    .sorted(Collections.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            System.out.println("Folder deleted successfully.");
        } catch (IOException e) {
            System.err.println("Failed to delete the folder: " + e.getMessage());
        }
    }

    private String getCellValue(Cell cell, JSONObject jsonRows) {
        String excelValue = "";
        String newValue = "";
        Pattern pattern = Pattern.compile("");

        List<String> listRules = new ArrayList<>();
        if (cell != null && cell.getCellType() == CellType.STRING && !(cell.getStringCellValue().trim().equalsIgnoreCase("None"))) {
            excelValue = cell.getStringCellValue().trim();
            excelValue = excelValue.replaceAll("//", "/").replaceAll("[^\\x00-\\x7F]", "").replaceAll("\n", ";").replaceAll("=", " = ").replaceAll("<>", " <> ").replaceAll("“", "").replaceAll("\"", "").replaceAll("[\\s]+[.]+", ".").replaceAll("[\\s]+", " ").trim();
            if (excelValue.toLowerCase().contains(" or ")) {
                listRules = Arrays.asList(excelValue.split(";"));
                for (String rule : listRules) {
                    JSONObject values = new JSONObject();
                    List<String> resultValue = new ArrayList<>();
                    if (rule.toLowerCase().contains(" or ") & !(rule.toLowerCase().contains("skip for automation"))) {
                        rule = rule.replaceAll("\\[", "").replaceAll("]", "");
                        if (rule.toLowerCase().contains("("))
                            pattern = Pattern.compile("(.*?)\\((.*?)\\)(.*)");
                        else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?").matcher(rule).find())
                            pattern = Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?");

                        if (!pattern.toString().equals("")) {
                            Matcher matcher = pattern.matcher(rule);
                            while (matcher.find()) {
                                List<String> orConditions = Arrays.asList(matcher.group(2).split(" OR "));
                                for (String condition : orConditions)
                                    newValue += rule.replaceAll(matcher.group(2), condition).replaceAll("\\(", "").replaceAll("\\)", "") + ";";
                            }
                        } else
                            newValue += rule + ";";
                    } else
                        newValue += rule + ";";
                }
                excelValue = newValue.substring(0, newValue.length() - 1);
            }
            newValue = "";
            pattern = null;
            if (excelValue.toLowerCase().contains("<>")) {
                listRules = Arrays.asList(excelValue.split(";"));
                for (String rule : listRules) {
                    JSONObject values = new JSONObject();
                    List<String> resultValue = new ArrayList<>();
                    if (rule.toLowerCase().contains("<>") & !(rule.toLowerCase().contains("skip for automation"))) {
                        String conditionAnother = "";
                        String expectedResult = "";
                        if (Pattern.compile("(?i)([^<>\\s]+)\\s*<>(.*?)(?:,|AND|then|$)").matcher(rule).find()) {
                            pattern = Pattern.compile("(?i)([^<>\\s]+)\\s*<>(.*?)(?:,|AND|then|$)");
                            Matcher matcher = pattern.matcher(rule);
                            while (matcher.find()) {
                                conditionAnother = matcher.group(1).trim();
                                expectedResult = matcher.group(2).trim();
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

    public void createFeatureFile(String client, String module, String product, String fileName) {
        ArrayList<String> lines = new ArrayList<>();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.FEATUREFILESPATH.getText() + "End2End/E2EWizardTestFlow.feature"));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("ModuleName", module).replaceAll("ModuleTag", module.replaceAll(" ", "")).replaceAll("Client", client);
                line = line.replaceAll("productName", product).replaceAll("fileName", fileName);
                lines.add(line);
            }  //end if
            reader.close();
            File tempFile = new File(EnumsCommon.FEATUREFILESPATH.getText() + "ForesightTest/" + client + "_" + module.replaceAll(" ", "") + ".feature");
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

    public void createRunnerFile(String client, String module) {
        ArrayList<String> lines = new ArrayList<>();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(EnumsCommon.RUNNERFILESPATH.getText() + "RunFireLightTest.java"));
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("com.hexure.firelight.runner", "com.hexure.firelight.runner.ForeSightTest");
                line = replaceLine(line, "\"json:target/cucumber-html-report\",", "\t\t\"json:target/cucumber" + client + module.replaceAll(" ", "") + "-html-report\",");
                line = replaceLine(line, "\"json:target/cucumber-reports/cucumber.xml\",", "\t\t\"json:target/cucumber-reports/cucumber" + client + module.replaceAll(" ", "") + ".xml\",");
                line = replaceLine(line, "\"html:target/cucumber-reports/cucumber.html\",", "\t\t\"html:target/cucumber-reports/cucumber" + client + module.replaceAll(" ", "") + ".html\",");
                line = replaceLine(line, "\"rerun:target/failedrun.txt\",", "\t\t\"rerun:target/failedrun" + client + module.replaceAll(" ", "") + ".txt\",");
                line = replaceLine(line, "\"json:target/cucumber-reports/cucumber.json\",", "\t\t\"json:target/cucumber-reports/cucumber" + client + module.replaceAll(" ", "") + ".json\",");
                line = replaceLine(line, "features = {", "\t\tfeatures = {\"src/test/resources/features/ForesightTest/" + client + "_" + module.replaceAll(" ", "") + ".feature\"},");
                line = replaceLine(line, "tags = {", "\t\ttags = {\"@" + module.replaceAll(" ", "") + "\"},");
                line = replaceLine(line, "public class RunFireLightTest {", "public class RunForesight" + client + module.replaceAll(" ", "") + "Test" + " {");
                line = replaceLine(line, "core.run(RunFireLightTest.class);", "\t\tcore.run(RunForesight" + client + module.replaceAll(" ", "") + "Test" + ".class);");
                line = replaceLine(line, "String cssFilePath = System.getProperty(\"user.dir\") +", "\t\tString cssFilePath = System.getProperty(\"user.dir\") + \"\\\\target\\\\cucumber-reports\\\\cucumber" + client + module.replaceAll(" ", "") + ".html\\\\style.css\"; // Specify the path to your cucumber.html file");
                lines.add(line);
            }  //end if
            reader.close();
            File tempFile = new File(EnumsCommon.RUNNERFILESPATH.getText() + "ForeSightTest/RunForesight" + client + module.replaceAll(" ", "") + "Test" + ".java");
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

    public String replaceLine(String line, String toBeReplaced, String replacement) {
        if (line.contains(toBeReplaced))
            return replacement;
        return line;
    }
}