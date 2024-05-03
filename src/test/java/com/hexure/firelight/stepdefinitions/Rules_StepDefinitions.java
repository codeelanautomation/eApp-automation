package com.hexure.firelight.stepdefinitions;

import com.hexure.firelight.pages.CommonMethodsPage;
import com.hexure.firelight.pages.CreateApplicationPage;
import com.hexure.firelight.pages.LoginPage;
import com.hexure.firelight.pages.SoftAssertionHandlerPage;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import cucumber.api.java.en.Given;
import org.apache.commons.lang3.RandomStringUtils;
import com.hexure.firelight.libraies.*;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Rules_StepDefinitions extends FLUtilities {
    private final TestContext testContext;
    private final WebDriver driver;
    private final CommonMethodsPage onCommonMethodsPage;
    private final SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    private final CreateApplicationPage onCreateApplicationPage;
    private final LoginPage onLoginPage;
    int countValidation = 1;
    DateTimeFormatter format = DateTimeFormatter.ofPattern(("MMddyyyy"));
    DateTimeFormatter formatWithSlash = DateTimeFormatter.ofPattern(("MM/dd/yyyy"));
    LocalDate todaysDate = LocalDate.now();
    String prefilledValue = "";
    String difference;
    String moduleName;
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    List<List<String>> combinationConditions = new ArrayList<>();
    List<List<String>> combinationConditionsValidations = new ArrayList<>();
    Map<String, String> howManyOperator = new HashMap<>();
    Map<String, String> howManyOperatorEnableCondition = new HashMap<>();
    Map<String, String> howManyOperatorValidations = new HashMap<>();
    String jurisdictionStatesCode = "";
    List<String> displayEnableConditions = new ArrayList<>();
    List<String> skippedInvalidElements = new ArrayList<>();
    int fieldsEvaluated = 0;
    String executedJurisdiction = "";


    public Rules_StepDefinitions(TestContext context) {
        testContext = context;
        driver = context.getDriver();
        onCommonMethodsPage = testContext.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
        onLoginPage = testContext.getPageObjectManager().getLoginPage();
        onCreateApplicationPage = testContext.getPageObjectManager().getCreateApplicationPage();
    }

    @Given("User clicks application for Product {string} and Product Type {string} and validate wizard fields for module {string}")
    public void createAppAndValidateWizard(String product, String productType, String module) {
        captureScreenshot(driver, testContext, false);

        // Extract states from the test context
        String[] states = testContext.getMapTestData().get("JurisdictionRules").split(",");

        // Loop through each state (jurisdiction)
        for (String jurisdiction : states) {
            skippedInvalidElements.clear();
            if (shouldExecuteForJurisdiction(jurisdiction, module)) {
                createApplication(jurisdiction, product, productType);
                executePostCreationTasks(module);
            }
        }
        // Print final results after all operations
        printFinalResults();
    }

    private boolean shouldExecuteForJurisdiction(String jurisdiction, String module) {
        String moduleJurisdictionMapping = testContext.getMapTestData().get(jurisdiction).trim();
        jurisdictionStatesCode = JsonPath.read(moduleJurisdictionMapping, "$.State").toString().trim();
        List<String> moduleValues = Arrays.asList(
                JsonPath.read(moduleJurisdictionMapping, "$.Module").toString().trim().split(", ")
        );
        return moduleValues.contains(module) || moduleValues.contains("All");
    }

    private void createApplication(String jurisdiction, String product, String productType) {
        executedJurisdiction = jurisdiction;
        clickElement(driver, onCreateApplicationPage.getBtnApplication());
        selectDropdownValue(onCreateApplicationPage.getDd_Jurisdiction(), jurisdiction);
        waitForPageToLoad(driver);
        sleepInMilliSeconds(2000);

        waitUntilDropDownListPopulated(driver, new Select(onCreateApplicationPage.getDd_ProductType()));
        selectDropdownValue(onCreateApplicationPage.getDd_ProductType(), productType);
        sleepInMilliSeconds(2000);

        clickProductInDropdown(product);
        waitForPageToLoad(driver);

        createNewApplication(product);
    }

    private void selectDropdownValue(WebElement dropdown, String visibleText) {
        new Select(dropdown).selectByVisibleText(visibleText);
    }

    private void clickProductInDropdown(String product) {
        syncElement(driver, findElement(driver, String.format(onCreateApplicationPage.list_OfProducts, product)), EnumsCommon.TOCLICKABLE.getText());
        try {
            clickElement(driver, findElement(driver, String.format(onCreateApplicationPage.list_OfProducts, product)));
        } catch (StaleElementReferenceException e) {
            clickElement(driver, findElement(driver, String.format(onCreateApplicationPage.list_OfProducts, product)));
        }
    }

    private void createNewApplication(String product) {
        clickElement(driver, onCreateApplicationPage.getBtn_Create());
        String newAppName = "AT " + product + " " + getDate("newAppName");
        onCreateApplicationPage.getTxtBox_newAppName().clear();
        onCreateApplicationPage.getTxtBox_newAppName().sendKeys(newAppName);
        onCreateApplicationPage.getBtn_CreateActivity().click();
        waitForPageToLoad(driver);
    }

    private void executePostCreationTasks(String module) {
        if (!onCreateApplicationPage.getLstBtnClose().isEmpty()) {
            clickElement(driver, onCreateApplicationPage.getBtnClose());
        }
        verifyFormDataWithInboundXml(module);
        waitForPageToLoad(driver);
        clickElement(driver, onCreateApplicationPage.getBtnHome());
        clickElement(driver, onCreateApplicationPage.getBtnPopupOK());
        waitForPageToLoad(driver);
    }

    public void verifyFormDataWithInboundXml(String module) {
        Set<String> fieldNames = getFieldNamesFromContext();
        for (String fieldName : fieldNames) {
            if (isModuleMatching(fieldName, module)) {
                if(fieldName.equalsIgnoreCase("ContingentBeneficiary1_Primary_EmailAddress"))
                    System.out.println(1);
                wizardTesting(fieldName);
            }
        }
    }

    // Helper method to get the set of field names from the test context
    private Set<String> getFieldNamesFromContext() {
        String fieldListRaw = testContext.getMapTestData().get("fieldList").trim();
        return new LinkedHashSet<>(Arrays.asList(fieldListRaw.split(", ")));
    }

    // Helper method to check if the module section name matches the provided module
    private boolean isModuleMatching(String fieldName, String module) {
        String fieldData = testContext.getMapTestData().get(fieldName).trim();
        String moduleSectionName = JsonPath.read(fieldData, "$.ModuleSectionName").toString().trim();
        return module.equalsIgnoreCase(moduleSectionName);
    }

    public String findKeyExistsJSON(String condition) {
        if (!testContext.getMapTestData().containsKey(condition))
            return condition;
        return "";
    }

    public void wizardTesting(String field) {
        String section = "";
        String condition;
        String expectedOperator;
        String expectedResult;
        String requiredFirstAttribute;
        String requiredSecondAttribute;
        String requiredAttributeValue;
        boolean expectedFlag;
        List<String> actualOptions;
        List<String> expectedOptions = new ArrayList<>();
        List<String> listConditions;
        List<String> expectedResults;
        List<String> listFieldValueConditions;
        String dataType = "";
        String wizardControlType;
        String reason;
        String order;
        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys;
        List<String> rulesList = Arrays.asList("ListOptions", "ValidationRules", "RulesWizard", "Length", "Format");
        String displayedText;
        String key;
        String values;
        String[] conditionValues;
        mapConditions.clear();
        String listConditionkeys;
        List<String> invalidTag = new ArrayList<>();
        boolean conditionFlag = true;

        String valueJson = testContext.getMapTestData().get(field).trim();
        if (valueJson.contains("CommonTag")) {
            order = JsonPath.read(valueJson, "$.Order").toString().trim();
            moduleName = JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim();
            String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
            reason = "";
            if (valueJson.contains("Reason:Skipforautomation"))
                reason = JsonPath.read(valueJson, "$.Reason:Skipforautomation").toString().trim();
            combinationConditions = new ArrayList<>();
            howManyOperator = new HashMap<>();

            if (valueJson.contains("WizardControlTypes") & !(field.toLowerCase().contains("lookup") | valueJson.toLowerCase().contains("hide for day") | commonTag.equalsIgnoreCase("No Tag") | commonTag.isEmpty())) {
                if (valueJson.contains("Section\""))
                    section = JsonPath.read(valueJson, "$.Section").toString().trim();
                wizardControlType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
                verifyAndMoveToPage(valueJson);
                if (verifyElementExists(valueJson, skippedInvalidElements, order, field)) {
                    fieldsEvaluated++;
                    combinationConditions.clear();
                    howManyOperator.clear();
                    for (String rule : rulesList) {
                        displayEnableConditions.clear();
                        try {
                            switch (rule) {
                                case "ListOptions":
                                    String options = JsonPath.read(valueJson, "$.ListOptions").toString().trim();
                                    if (options.contains(";"))
                                        expectedOptions = Arrays.asList(options.split(";"));
                                    else if (testContext.getMapTestData().containsKey(options.replaceAll(" ", "")))
                                        expectedOptions = Arrays.asList(testContext.getMapTestData().get(options.replaceAll(" ", "")).split(", "));
                                    else
                                        conditionFlag = false;

                                    if (conditionFlag) {
                                        if (valueJson.contains("DisplayRules")) {
                                            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]);
                                            setCombinationConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)");
                                        }
                                        if (invalidTag.isEmpty()) {
                                            if (combinationConditions.isEmpty()) {
                                                verifyOptions(valueJson, field, expectedOptions, "", "List Options");
                                            } else {
                                                for (List<String> result : combinationConditions) {
                                                    List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, "List Options");
                                                    conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                                                    listConditionkeys = flagInvalidKeys.get(0);
                                                    displayedText = flagInvalidKeys.get(2);

                                                    if (conditionFlag) {
                                                        if (listConditionkeys.isEmpty()) {
                                                            verifyAndMoveToPage(valueJson);
                                                            verifyOptions(valueJson, field, expectedOptions, displayedText, "List Options");
                                                        } else
                                                            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "List Options", "Key " + listConditionkeys + " does not exists in JSON", testContext);
                                                    }
                                                }
                                                combinationConditions.clear();
                                                howManyOperator.clear();
                                            }
                                        } else
                                            onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
                                    } else
                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "List Options", "Data list does not contain \"" + options + "\"", testContext);
                                    break;
                                case "RulesWizard":
                                    for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split(";")) {
                                        distinctRule = distinctRule.replaceFirst("(\\d+\\.\\s*)?", "").trim().replaceFirst("\\.$", "").trim();
                                        System.out.println(order + ". " + field + " -> " + distinctRule);
                                        if (!(distinctRule.toLowerCase().contains("lookup") | distinctRule.toLowerCase().contains("not required to use") | distinctRule.toLowerCase().contains("implemented then specify") | distinctRule.toLowerCase().contains("skip for automation"))) {
                                            invalidTag = getInvalidTags(skippedInvalidElements, distinctRule);
                                            if (invalidTag.isEmpty()) {
                                                if (Pattern.compile("(\\d+\\.\\s*)?If (.*?),? then (?i)(SHOW|HIDE) (Options|Label as) (.*)\\.?").matcher(distinctRule).find()) {
                                                    listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?),? then (?i)(SHOW|HIDE) (Options|Label as) (.*)\\.?", "", distinctRule);
                                                    condition = listConditions.get(1);
                                                    expectedResult = listConditions.get(2);
                                                    requiredSecondAttribute = listConditions.get(4);
                                                    mapConditions.clear();
                                                    conditionFlag = true;

                                                    for (String eachCondition : condition.trim().split(("(?i)(AND)"))) {
                                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)", "", eachCondition.trim());
                                                        key = listFieldValueConditions.get(0).trim();
                                                        expectedOperator = listFieldValueConditions.get(1).trim();
                                                        conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
                                                        howManyOperator.put(key, expectedOperator);
                                                        // Add the key-value pairs to the map
                                                        mapConditions = setMapConditions(conditionValues, key, mapConditions);
                                                    }
                                                    allKeys = new ArrayList<>(mapConditions.keySet());
                                                    generateCombinations(allKeys, new ArrayList<>(), mapConditions, "ListOptions");

                                                    for (List<String> result : combinationConditions) {
                                                        List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, distinctRule);
                                                        conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                                                        listConditionkeys = flagInvalidKeys.get(0);
                                                        displayedText = flagInvalidKeys.get(2);

                                                        if (conditionFlag) {
                                                            if (listConditionkeys.isEmpty()) {
                                                                verifyAndMoveToPage(valueJson);
                                                                switch (wizardControlType) {
                                                                    case "Dropdown":
                                                                    case "State Dropdown":
                                                                        expectedOptions = Arrays.asList(requiredSecondAttribute.split(", "));
                                                                        actualOptions = getOptions(valueJson, wizardControlType);
                                                                        switch (expectedResult.toLowerCase()) {
                                                                            case "show":
                                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, dataType + " Options" + displayedText, actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                                                            case "hide":
                                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, dataType + " Options" + displayedText, actualOptions, expectedOptions, !(actualOptions.containsAll(expectedOptions)), testContext);
                                                                        }
                                                                        break;
                                                                    default:
                                                                        String expectedValue = getElementLabel(valueJson, wizardControlType).getText();
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, "Label Information", "Field is displayed under label" + displayedText, expectedValue, requiredSecondAttribute, expectedValue.replaceAll("’", "").equals(requiredSecondAttribute), testContext);
                                                                }
                                                            } else
                                                                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                                                        }
                                                    }
                                                    combinationConditions.clear();
                                                    howManyOperator.clear();
                                                } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?").matcher(distinctRule).find()) {
                                                    List<String> listExpectedConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*)\\.?", "", distinctRule);
                                                    condition = listExpectedConditions.get(1);
                                                    expectedResults = Arrays.asList(listExpectedConditions.get(2).split(" (?i)AND "));
                                                    mapConditions.clear();
                                                    boolean hidden = false;
                                                    conditionFlag = true;
                                                    String secondParamter = "";
                                                    if (expectedResults.size() > 1)
                                                        secondParamter = expectedResults.get(1);
                                                    if (secondParamter.equalsIgnoreCase("hide"))
                                                        hidden = true;

                                                    for (String eachCondition : condition.trim().split(("AND"))) {
                                                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)", "", eachCondition.trim());
                                                        key = listFieldValueConditions.get(0).trim();
                                                        expectedOperator = listFieldValueConditions.get(1).trim();
                                                        conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
                                                        howManyOperator.put(key, expectedOperator);
                                                        // Add the key-value pairs to the map
                                                        mapConditions = setMapConditions(conditionValues, key, mapConditions);
                                                    }
                                                    allKeys = new ArrayList<>(mapConditions.keySet());
                                                    generateCombinations(allKeys, new ArrayList<>(), mapConditions, "RulesWizard");

                                                    for (List<String> result : combinationConditions) {
                                                        List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, distinctRule);
                                                        conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                                                        listConditionkeys = flagInvalidKeys.get(0);
                                                        displayedText = flagInvalidKeys.get(2);
                                                        if (secondParamter.equalsIgnoreCase("enable") & displayEnableConditions.isEmpty()) {
                                                            displayEnableConditions.addAll(result);
                                                            howManyOperatorEnableCondition = new HashMap<>(howManyOperator);
                                                        }

                                                        if (conditionFlag) {
                                                            if (listConditionkeys.isEmpty()) {
                                                                if (verifyAndMoveToPage(valueJson)) {
                                                                    if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                                        setVisibilityRules(expectedResults.get(0).trim(), valueJson, wizardControlType, order, field, secondParamter, distinctRule, displayedText);
                                                                        if (!secondParamter.isEmpty())
                                                                            setVisibilityRules(secondParamter.trim(), valueJson, wizardControlType, order, field, "", distinctRule, displayedText);
                                                                    } else
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field does not exists" + displayedText, true, "true", true, testContext);
                                                                } else
                                                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, hidden, hidden, testContext);
                                                            } else
                                                                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                                                        }
                                                    }
                                                    combinationConditions.clear();
                                                    howManyOperator.clear();
//                                                } else if (Pattern.compile("(\\d+\\.\\s*)?If ([^\\s]+)\\s* = (.*?) (?i)AND ([^\\s]+)\\s* = (.*?),? then (.*)\\.?$").matcher(distinctRule).find()) {
//                                                    listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If ([^\\s]+)\\s* = (.*?) (?i)AND ([^\\s]+)\\s* = (.*),? then (.*?)\\.?$", "", distinctRule);
//                                                    condition = listConditions.get(1);
//                                                    expectedResult = listConditions.get(2);
//                                                    conditionAnother = listConditions.get(3);
//                                                    expectedResultAnother = listConditions.get(4);
//                                                    String[] requiredPrefilledAttribute = listConditions.get(6).split(", ");
//                                                    displayedText = "";
//                                                    for (String result : expectedResult.split(", ")) {
//                                                        setConditions(condition, valueJson, result, conditionAnother, expectedResultAnother, "=", "=", distinctRule, displayedText);
//                                                        for (String prefilledAttribute : requiredPrefilledAttribute) {
//                                                            testData = setTestData(testContext.getMapTestData().get(prefilledAttribute.split(" = ")[1]).trim());
//                                                            setDependentCondition(prefilledAttribute.split(" = ")[1], "=", valueJson, testData, distinctRule, displayedText);
//                                                            verifyData(testContext.getMapTestData().get(prefilledAttribute.split(" = ")[0]).trim(), prefilledAttribute.split(" = ")[0], condition, result, testData, "", distinctRule);
//                                                        }
//                                                    }
                                                } else if (Pattern.compile("(\\d+\\.\\s*)?(Default|Placeholder) = (.*)\\.?").matcher(distinctRule).find()) {
                                                    listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(.*?) = (.*)\\.?", "", distinctRule);
                                                    requiredFirstAttribute = listConditions.get(1);
                                                    requiredAttributeValue = listConditions.get(2);
                                                    key = values = "";
                                                    mapConditions.clear();
                                                    displayedText = "";
                                                    conditionFlag = true;

                                                    if (valueJson.contains("DisplayRules"))
                                                        setCombinationConditions(valueJson, "(.*?) (=|<>|<|>) (.*)");

                                                    if (combinationConditions.isEmpty()) {   //Display rules is not available for this field
                                                        if (verifyAndMoveToPage(valueJson)) {
                                                            if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                                if (requiredFirstAttribute.equalsIgnoreCase("Placeholder"))
                                                                    handlePlaceholderRules(valueJson, "", "", field, requiredAttributeValue, distinctRule);
                                                                else {
                                                                    verifyData(valueJson, field, displayedText, requiredAttributeValue, "", distinctRule);
                                                                    if (valueJson.contains("ValidationRules"))
                                                                        handleValidationRules(valueJson, "", "", field, order);
                                                                    handleSectionRules(valueJson, wizardControlType, section, order, field, "");
                                                                }
                                                            } else
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field " + field + " does not exists", true, "true", true, testContext);
                                                        } else
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, "true", true, testContext);
                                                    } else {
                                                        for (List<String> result : combinationConditions) {
                                                            List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*:\\s*(.*?)(?:;|$)", valueJson, order, field, distinctRule);
                                                            conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                                                            listConditionkeys = flagInvalidKeys.get(0);
                                                            displayedText = flagInvalidKeys.get(2);

                                                            if (conditionFlag) {
                                                                if (listConditionkeys.isEmpty()) {
                                                                    if (verifyAndMoveToPage(valueJson)) {
                                                                        if (requiredFirstAttribute.equalsIgnoreCase("Placeholder"))
                                                                            handlePlaceholderRules(valueJson, "", "", field, requiredAttributeValue, distinctRule);
                                                                        else {
                                                                            if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                                                                switch (requiredAttributeValue.toLowerCase().trim()) {
                                                                                    case "unselected":
                                                                                    case "unchecked":
                                                                                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                                                                                            expectedFlag = element.getAttribute("aria-checked").equalsIgnoreCase("false");
                                                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " \"" + element.getAttribute("title") + "\" " + requiredAttributeValue.toLowerCase().trim() + " by default" + displayedText, expectedFlag, "true", expectedFlag, testContext);
                                                                                        }
                                                                                        break;
                                                                                    default:
                                                                                        verifyData(valueJson, field, displayedText, requiredAttributeValue, "", distinctRule);
                                                                                }
                                                                                conditionFlag = verifyStateCodeRules(key, howManyOperator.get(key), values);
                                                                                if (!conditionFlag)
                                                                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "List Options", "Rule is applicable " + displayedText + " and not when state code is " + executedJurisdiction, testContext);
                                                                                else {
                                                                                    if (valueJson.contains("ValidationRules"))
                                                                                        handleValidationRules(valueJson, key, values, field, order);
                                                                                    handleSectionRules(valueJson, wizardControlType, section, order, field, displayedText);
                                                                                }
                                                                            } else
                                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field does not exists" + displayedText, true, "true", true, testContext);
                                                                        }
                                                                    } else
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, "true", true, testContext);
                                                                } else
                                                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                                                            }
                                                        }
                                                        combinationConditions.clear();
                                                        howManyOperator.clear();
                                                    }
                                                } else if (Pattern.compile("(\\d+\\.\\s*)?(Age is calculated on age last birth date|(?i)Always enabled|(?i)Always disable|Optional)\\.?").matcher(distinctRule).find()) {
                                                    listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(Age is calculated on age last birth date|(?i)Always enabled|(?i)Always disable|Optional)\\.?", "", distinctRule);
                                                    requiredAttributeValue = listConditions.get(1);
                                                    mapConditions.clear();
                                                    conditionFlag = true;

                                                    if (valueJson.contains("DisplayRules"))
                                                        setCombinationConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)");

                                                    switch (requiredAttributeValue.toLowerCase()) {
                                                        case "age is calculated on age last birth date":
                                                            for (List<String> result : combinationConditions) {
                                                                List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, distinctRule);
                                                                conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                                                                listConditionkeys = flagInvalidKeys.get(0);
                                                                displayedText = flagInvalidKeys.get(2);

                                                                if (conditionFlag) {
                                                                    if (listConditionkeys.isEmpty()) {
                                                                        verifyAndMoveToPage(valueJson);
                                                                        LocalDate birthDatePastMonth = todaysDate.minusYears(25).plusMonths(-1);
                                                                        LocalDate birthDateFutureMonth = todaysDate.minusYears(25).plusMonths(1);
                                                                        sendKeys(driver, getElement(valueJson, wizardControlType, null), birthDatePastMonth.format(format));
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Age" + displayedText + " and birth date is " + birthDatePastMonth.format(formatWithSlash), "25", String.valueOf(calculateAge(birthDatePastMonth, todaysDate)), String.valueOf(calculateAge(birthDatePastMonth, todaysDate)).equalsIgnoreCase("25"), testContext);
                                                                        sendKeys(driver, getElement(valueJson, wizardControlType, null), birthDateFutureMonth.format(format));
                                                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Age" + displayedText + " and birth date is " + birthDateFutureMonth.format(formatWithSlash), "24", String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)), String.valueOf(calculateAge(birthDateFutureMonth, todaysDate)).equalsIgnoreCase("24"), testContext);
                                                                    } else
                                                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                                                                }
                                                            }
                                                            combinationConditions.clear();
                                                            howManyOperator.clear();
                                                            break;
                                                        case "always enabled":
                                                            expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is always enabled", expectedFlag, "true", expectedFlag, testContext);
                                                            break;
                                                        case "always disable":
                                                            for (WebElement element : getElements(valueJson, wizardControlType)) {
                                                                expectedFlag = element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput");
                                                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is always disabled", expectedFlag, "true", expectedFlag, testContext);
                                                            }
                                                            break;
                                                        case "optional":
                                                            String error = clickRedBubble(valueJson);
                                                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Optional Field Validation", error, "", error.isEmpty(), testContext);
                                                            break;
                                                    }
                                                } else {
                                                    System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule does not match any criteria for field", testContext);
                                                }
                                            } else
                                                onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
                                        } else
                                            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, reason, testContext);
                                    }
                                    break;
                                case "Length":
                                    invalidTag = new ArrayList<>();
                                    if (valueJson.contains("DisplayRules"))
                                        invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]);
                                    if (invalidTag.isEmpty()) {
                                        if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
                                            getAttributeValue(field, valueJson, order, wizardControlType, rule, "maxLength", "Length");
                                        }
                                    } else
                                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
                                    break;
                                case "Format":
                                    invalidTag = new ArrayList<>();
                                    if (valueJson.contains("DisplayRules"))
                                        invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]);
                                    if (invalidTag.isEmpty()) {
                                        if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
                                            getAttributeValue(field, valueJson, order, wizardControlType, rule, "mask", "Format");
                                        }
                                    } else
                                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
                                    break;
                            }
                            if (!displayEnableConditions.isEmpty()) {
                                howManyOperator = howManyOperatorEnableCondition;
                                setDependentConditions(displayEnableConditions, "([^\\s]+)\\s*: (.*)", valueJson, order, field, "");
                                if (!(onCommonMethodsPage.getListErrors().isEmpty()))
                                    clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
                                howManyOperatorEnableCondition.clear();
                                howManyOperator.clear();
                            }
                        } catch (PathNotFoundException e) {
                            System.out.println("Field " + field + " does not have rule \"" + rule + "\"");
                        }
                    }
                } else {
                    if (!field.isEmpty())
                        skippedInvalidElements.add(field);
                }
            } else {
                if (!field.isEmpty())
                    skippedInvalidElements.add(field);
                onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Either field is Lookup, hide for day 1 or has no wizard control type", testContext);
            }
        } else
            onSoftAssertionHandlerPage.assertSkippedElement(driver, "", executedJurisdiction, moduleName, field, "Field has No Tag", testContext);
    }

    public LinkedHashMap<String, List<String>> setMapConditions(String[] conditionValues, String key, LinkedHashMap<String, List<String>> mapConditions) {
        for (String value : conditionValues) {
            List<String> valuesList = mapConditions.getOrDefault(key, new ArrayList<>());
            valuesList.add(value.trim());
            mapConditions.put(key, valuesList);
        }
        return mapConditions;
    }

    private List<String> setDependentConditions(List<String> result, String pattern, String valueJson, String order, String field, String distinctRule) {
        String conditionKeys = "";
        String displayedText = " when ";
        boolean conditionFlag = true;
        String conditionalOperator;

        for (String condition : result) {
            List<String> fieldValueConditions = getDisplayRuleConditions(valueJson, pattern, "", condition.trim());
            String key = fieldValueConditions.get(0).trim();
            String values = fieldValueConditions.get(1).trim();

            displayedText += key + " " + getOperator(howManyOperator, key) + " " + values + " and ";
            conditionKeys = findKeyExistsJSON(key);

            conditionFlag = checkStateCode(conditionKeys);
            if (!conditionFlag)
                break;
            conditionKeys = conditionKeys.replaceAll("FLI_ISSUED_STATE_CODE", "");
            conditionalOperator = howManyOperator.get(key);
            List<String> valuesOperator = adjustValuesAndOperator(key, values, howManyOperator);
            values = valuesOperator.get(0);
            conditionalOperator = (valuesOperator.get(1).equalsIgnoreCase("")) ? conditionalOperator : valuesOperator.get(1);
            conditionFlag = verifyStateCodeRules(key, getOperator(howManyOperator, key), values);

            if (!conditionFlag) {
                handleSkippedRules(order, field, distinctRule, "Rule is applicable " + displayedText + " not when state code is " + executedJurisdiction);
            } else if (!key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
                conditionFlag = validateElementAccessibility(order, field, key, values, valueJson, distinctRule, displayedText, conditionalOperator);
            }

            if (!conditionFlag) {
                break; // Break early if any condition fails
            }
        }

        // Remove trailing 'and'
        if (displayedText.trim().endsWith(" and")) {
            displayedText = displayedText.substring(0, displayedText.length() - 4);
        }
        return Arrays.asList(conditionKeys, String.valueOf(conditionFlag), displayedText);
    }

    private boolean checkStateCode(String conditionKeys) {
        return conditionKeys.isEmpty() || conditionKeys.equalsIgnoreCase("FLI_ISSUED_STATE_CODE");
    }

    private String getOperator(Map<String, String> operators, String key) {
        return operators.get(key);
    }

    private List<String> adjustValuesAndOperator(String key, String values, Map<String, String> operators) {
        String operator = getOperator(operators, key);
        String conditionalOperator = "";
        if (operator.equals(">") && values.matches("\\d+")) {
            values = String.valueOf(Integer.parseInt(values) + 1);
            conditionalOperator = "=";
        } else if (operator.equals("<") && values.matches("\\d+")) {
            values = String.valueOf(Integer.parseInt(values) - 1);
            conditionalOperator = "=";
        }
        return Arrays.asList(values, conditionalOperator);
    }

    private boolean verifyStateCodeRules(String key, String operator, String values) {

        if (key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
            boolean isEqualOperator = operator.equals("=");
            boolean isNotEqualOperator = operator.equals("<>");

            return !((isEqualOperator && values.equalsIgnoreCase(jurisdictionStatesCode)) ||
                    (isNotEqualOperator && !values.equalsIgnoreCase(jurisdictionStatesCode)));
        }
        return true;
    }

    private void handleSkippedRules(String order, String field, String distinctRule, String reason) {
        onSoftAssertionHandlerPage.assertSkippedRules(
                driver,
                order,
                executedJurisdiction,
                moduleName,
                field,
                distinctRule,
                reason,
                testContext
        );
    }

    private boolean validateElementAccessibility(String order, String field, String key, String values, String valueJson, String distinctRule, String displayedText, String conditionalOperator) {
        boolean conditionFlag;
        String valueDependentJson = testContext.getMapTestData().get(key);
        String wizardControlType = JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString();
        WebElement elem;
        verifyAndMoveToPage(valueDependentJson);

        elem = (wizardControlType.equalsIgnoreCase("radio button")) ? getElement(valueDependentJson, JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString(), values) : getElement(valueDependentJson, JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString(),"");
        boolean isReadOnlyOrDisabled = isAttributePresent(elem, "readonly") || isAttributePresent(elem, "disabled");
        if (isReadOnlyOrDisabled) {
            conditionFlag = false;
            handleSkippedRules(order, field, distinctRule, "Field " + key + " is disabled");
        } else {
            conditionFlag = setDependentCondition(key, conditionalOperator, valueJson, values, distinctRule, displayedText);
        }

        return conditionFlag;
    }

    public void setVisibilityRules(String requiredAttribute, String valueJson, String wizardControlType, String order, String field, String secondAttribute, String distinctRule, String displayedText) {
        boolean expectedFlag;
        WebElement elem;

        if (Pattern.compile("SET ([^\\s]+)\\s* = (.*)").matcher(requiredAttribute).find()) {
            List<String> listConditions = getDisplayRuleConditions(valueJson, "SET ([^\\s]+)\\s* = (.*)", "", requiredAttribute);
            String conditionFirst = listConditions.get(0);
            String expectedResultFirst = listConditions.get(1);
            String listConditionkeys = findKeyExistsJSON(conditionFirst);
            if (listConditionkeys.isEmpty()) {
                if (testContext.getMapTestData().containsKey(expectedResultFirst)) {
                    String testData = setTestData(testContext.getMapTestData().get(expectedResultFirst).trim());
                    if (verifyAndMoveToPage(valueJson)) {
                        if (!getElements(valueJson, wizardControlType).isEmpty()) {
                            setDependentCondition(expectedResultFirst, "=", valueJson, testData, distinctRule, displayedText);
                            verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, displayedText, prefilledValue, testData, distinctRule);
                            setDependentCondition(expectedResultFirst, "=", valueJson, "", distinctRule, displayedText);
                        } else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field " + conditionFirst + " does not exists" + displayedText, true, "true", true, testContext);
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, "true", true, testContext);
                } else {
                    if (verifyAndMoveToPage(valueJson)) {
                        if (!getElements(valueJson, wizardControlType).isEmpty()) {
                            verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, displayedText, expectedResultFirst, "", distinctRule);
                        } else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field " + conditionFirst + " does not exists" + displayedText, true, "true", true, testContext);
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, "true", true, testContext);
                }
            } else
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
        } else {
            switch (requiredAttribute.toLowerCase()) {
                case "show":
                    expectedFlag = !getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is shown" + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "enable":
                    if (!(wizardControlType.equals("Radio Button"))) {
                        expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is enabled " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    } else {
                        for (WebElement element : getElements(valueJson, wizardControlType))
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Radio button " + element.getAttribute("title") + " is enabled " + displayedText, element.isEnabled(), "true", element.isEnabled(), testContext);
                    }
                    break;
                case "disable":
                    if (!secondAttribute.equalsIgnoreCase("hide")) {
                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                            expectedFlag = element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput");
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" disabled " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                        }
                    }
                    break;
                case "set to no":
                    elem = getElement(valueJson, "radioField", "No");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Radio button No is selected" + displayedText, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to yes":
                    elem = getElement(valueJson, "radioField", "Yes");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Radio button Yes is selected " + displayedText, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to self":
                case "set to united states":
                    verifyData(valueJson, field, displayedText, getDisplayRuleConditions(valueJson, "set to (.*)", "", requiredAttribute.toLowerCase()).get(0), "", distinctRule);
                    break;
                case "hide":
                    expectedFlag = getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Hidden Rule " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "read only":
                    for (WebElement element : getElements(valueJson, wizardControlType)) {
                        expectedFlag = (element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput"));
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Radio button \"" + element.getAttribute("title") + "\" read only" + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    }
                    break;
                default:
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule does not match any visibility condition", testContext);
            }
        }
    }

    public boolean verifyAndMoveToPage(String valueJson) {
        if (verifyPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
            moveToPage(JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
            waitForPageToLoad(driver);
            return true;
        }
        return false;
    }

    public void setCombinationConditions(String valueJson, String pattern) {
        List<String> listFieldValueConditions;
        String key;
        String[] conditionValues;
        String expectedOperator;

        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys;
        for (String eachCondition : JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0].split(("AND"))) {
            listFieldValueConditions = getDisplayRuleConditions(valueJson, pattern, "", eachCondition.trim());
            key = listFieldValueConditions.get(0).trim();
            expectedOperator = listFieldValueConditions.get(1).trim();
            conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
            howManyOperator.put(key, expectedOperator);
            // Add the key-value pairs to the map
            mapConditions = setMapConditions(conditionValues, key, mapConditions);
        }
        allKeys = new ArrayList<>(mapConditions.keySet());
        generateCombinations(allKeys, new ArrayList<>(), mapConditions, "");
    }

    public void handleSectionRules(String valueJson, String wizardControlType, String section, String order, String field, String displayedText) {
        section = section.replaceAll("X", "1");
        boolean expectedFlag = getElementSection(valueJson, wizardControlType, section);
        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, "Section Information", "Field is displayed under section " + section + displayedText, expectedFlag, "true", expectedFlag, testContext);
    }

    public int calculateAge(LocalDate dob, LocalDate currentDate) {
        return Period.between(dob, currentDate).getYears();
    }

    public void verifyOptions(String valueJson, String field, List<String> expectedOptions, String displayedText, String distinctRule) {
        String dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
        List<String> actualOptions = getOptions(valueJson, dataType);
        if (displayedText.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, dataType + " Options", actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, dataType + " Options" + displayedText, actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
    }

    public String removeUnsetCondition(String displayedText) {
        // Split the input string by "and" keyword
        String[] parts = displayedText.split("\\band\\b");

        // Concatenate the parts, excluding the last two "and" conditions
        StringBuilder output = new StringBuilder();
        for (int part = 0; part < Math.max(parts.length - 2, 0); part++) {
            output.append(parts[part].trim());
            if (part < Math.max(parts.length - 3, 0)) {
                output.append(" and ");
            }
        }
        return output.toString();
    }

    public boolean setDependentCondition(String condition, String expectedOperator, String valueJson, String result, String distinctRule, String displayedText) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        String wizardControlType = JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim();
        if (verifyAndMoveToPage(valueDependentJson)) {
            if (getElements(valueDependentJson, wizardControlType).isEmpty()) {
                displayedText = removeUnsetCondition(displayedText);
                onSoftAssertionHandlerPage.assertSkippedRules(driver, JsonPath.read(valueJson, "$.Order").toString().trim(), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag").toString().trim(), distinctRule, "Key " + condition + " does not exists in UI " + displayedText, testContext);
                return false;
            }

            if (result.isEmpty() | result.equalsIgnoreCase("blank")) {
                if (wizardControlType.equalsIgnoreCase("dropdown")) {
                    result = "Blank";
                    if (!isValueInOptions(valueJson, condition, result, distinctRule))
                        return isValueInOptions(valueJson, condition, result, distinctRule);
                }
            } else if (wizardControlType.equalsIgnoreCase("dropdown") | wizardControlType.equalsIgnoreCase("radio button")) {
                if (!isValueInOptions(valueJson, condition, result, distinctRule))
                    return false;
            }
            if (expectedOperator.equalsIgnoreCase("=")) {
                if (isReadonlyDisabled(valueDependentJson, result)) {
                    WebElement elem = (wizardControlType.equalsIgnoreCase("radio button")) ? getElement(valueDependentJson, wizardControlType, result) : getElement(valueDependentJson, wizardControlType,"");
                    if ((result.equalsIgnoreCase("selected") | result.equalsIgnoreCase("unselected")) && JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().equalsIgnoreCase("checkbox"))
                        return verifyCheckBoxSelectYesNO(result, elem);
                    else
                        setValue(valueDependentJson, result);
                }
            } else if (expectedOperator.equalsIgnoreCase("<>")) {
                if (isReadonlyDisabled(valueDependentJson, result))
                    setValue(valueDependentJson, "1");
            }
            waitForPageToLoad(driver);
            sleepInMilliSeconds(2000);
        } else {
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order").toString().trim(), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag").toString().trim(), "", "Page " + JsonPath.read(valueDependentJson, "$.Page").toString().trim() + " does not exists " + displayedText, true, false, false, testContext);
            return false;
        }
        return true;
    }

    private boolean isValueInOptions(String valueJson, String condition, String result, String distinctRule) {
        if (!getOptions(testContext.getMapTestData().get(condition).trim(), JsonPath.read(testContext.getMapTestData().get(condition).trim(), "$.WizardControlTypes").toString().trim()).contains(result)) {
            onSoftAssertionHandlerPage.assertSkippedRules(driver, JsonPath.read(valueJson, "$.Order").toString().trim(), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag").toString().trim(), distinctRule, "Value " + result + " does not exist for field " + condition, testContext);
            return false;
        }
        return true;
    }

    public void setValue(String valueDependentJson, String result) {
        waitForPageToLoad(driver);
        if (result.equalsIgnoreCase("Blank")) {
            result = "";
        }
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                if (result.equalsIgnoreCase("1")) {
                    Select elem = new Select(getElement(valueDependentJson, "dropdown", null));
                    elem.selectByIndex(Integer.parseInt(result));
                    prefilledValue = elem.getFirstSelectedOption().getText().trim();
                    if (prefilledValue.equals("0"))
                        elem.selectByIndex(Integer.parseInt(result) + 1);
                    prefilledValue = elem.getFirstSelectedOption().getText().trim();
                } else
                    selectDropdownValue(getElement(valueDependentJson, "dropdown", null), result);
                syncElement(driver, getElement(valueDependentJson, "dropdown", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "checkbox":
                checkBoxSelectYesNO(result, getElement(valueDependentJson, "checkbox", null));
                syncElement(driver, getElement(valueDependentJson, "checkbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "radio button":
                if (!result.isEmpty()) {
                    checkBoxSelectYesNO("check", getElement(valueDependentJson, "radioField", result));
                    syncElement(driver, getElement(valueDependentJson, "radioField", result), EnumsCommon.TOCLICKABLE.getText());
                }
                break;
            default:
                sendKeys(driver, getElement(valueDependentJson, "single line textbox", null), result);
                syncElement(driver, getElement(valueDependentJson, "single line textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(1000);
    }

    public void resetValue(String valueDependentJson, String result) {
        waitForPageToLoad(driver);
        if (result.equalsIgnoreCase("Blank")) {
            result = "";
        }
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                selectDropdownValue(getElement(valueDependentJson, "dropdown", null), result);
                syncElement(driver, getElement(valueDependentJson, "dropdown", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "checkbox":
                checkBoxSelectYesNO("uncheck", getElement(valueDependentJson, "checkbox", null));
                syncElement(driver, getElement(valueDependentJson, "checkbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            case "radio button":
                if (!result.isEmpty()) {
                    checkBoxSelectYesNO("unselected", getElement(valueDependentJson, "radioField", result));
                    syncElement(driver, getElement(valueDependentJson, "radioField", result), EnumsCommon.TOCLICKABLE.getText());
                }
                break;
            default:
                sendKeys(driver, getElement(valueDependentJson, "single line textbox", null), "");
                syncElement(driver, getElement(valueDependentJson, "single line textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(1000);
    }

    public String setTestData(String valueDependentJson) {
        String wizardControlType = JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase();
        switch (wizardControlType) {
            case "dropdown":
            case "state dropdown":
                return "1";
            case "zip":
                return "12345";
            case "tin":
            case "ssn":
                return "1234567890";
            case "dob":
            case "date":
            case "mm/dd/yyyy":
                return todaysDate.format(format);
            default:
                return "TestValue";
        }
    }

    public void moveToPage(String pageHeader, String formHeader) {
        waitForPageToLoad(driver);
        boolean flag = false;
        if (!(onCommonMethodsPage.getPageHeader().getText().equalsIgnoreCase(pageHeader) & onCommonMethodsPage.getFormHeader().getText().equalsIgnoreCase(formHeader))) {
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
            List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
            for (WebElement element : mandetoryFormList) {
                String form = element.getAttribute("innerText");
                if (form.equals(pageHeader)) {
                    clickElement(driver, element);
                    flag = true;
                    break;
                }
            }
            if (!flag)
                clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameCollapse());
        }
        waitForPageToLoad(driver);
    }

    public boolean verifyPage(String pageHeader, String formHeader) {
        boolean flag = false;
        waitForPageToLoad(driver);
        if (!onCommonMethodsPage.getList_WizardPageNameExpand().isEmpty())
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameExpand());
        List<WebElement> mandetoryFormList = findElements(driver, String.format(onCommonMethodsPage.getMandatoryFormElement(), formHeader));
        for (WebElement element : mandetoryFormList) {
            String form = element.getAttribute("innerText");
            if (form.equals(pageHeader)) {
                flag = true;
                break;
            }
        }
        if (!onCommonMethodsPage.getList_WizardPageNameCollapse().isEmpty())
            clickElementByJSE(driver, onCommonMethodsPage.getWizardPageNameCollapse());
        return flag;
    }

    public List<WebElement> getElements(String valueJson, String datatype) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "radio button":
            case "checkbox":
                return findElements(driver, String.format(onCommonMethodsPage.getRadioFieldCheckbox(), commonTag));
            case "dropdown":
            case "state dropdown":
                return findElements(driver, String.format(onCommonMethodsPage.getSelectField(), commonTag));
            case "label":
                return findElements(driver, String.format(onCommonMethodsPage.getLabelField(), commonTag));
            default:
                return findElements(driver, String.format(onCommonMethodsPage.getInputField(), commonTag));
        }
    }

    public WebElement getElement(String valueJson, String datatype, String optionalValue) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return findElement(driver, String.format(onCommonMethodsPage.getSelectField(), commonTag));
            case "checkbox":
                return findElement(driver, String.format(onCommonMethodsPage.getRadioFieldCheckbox(), commonTag));
            case "label":
                return findElement(driver, String.format(onCommonMethodsPage.getLabelField(), commonTag));
            case "radiofield":
            case "radio button":
                return findElement(driver, String.format(onCommonMethodsPage.getRadioFieldWithOption(), commonTag, optionalValue));
            case "errortype":
                switch (optionalValue.toLowerCase()) {
                    case "dropdown":
                    case "state dropdown":
                        return findElement(driver, String.format(onCommonMethodsPage.getSelectErrorField(), commonTag));
                    case "radio button":
                    case "checkbox":
                        return findElement(driver, String.format(onCommonMethodsPage.getRadioErrorField(), commonTag));
                    default:
                        return findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), commonTag));
                }
            default:
                return findElement(driver, String.format(onCommonMethodsPage.getInputField(), commonTag));
        }
    }

    public boolean getElementSection(String valueJson, String datatype, String section) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return !findElements(driver, String.format(onCommonMethodsPage.getSectionSelect(), section, commonTag)).isEmpty();
            case "checkbox":
            case "radio button":
            case "label":
                return !findElements(driver, String.format(onCommonMethodsPage.getSectionRadio(), section, commonTag)).isEmpty();
            default:
                return !findElements(driver, String.format(onCommonMethodsPage.getSectionInput(), section, commonTag)).isEmpty();
        }
    }

    public WebElement getElementLabel(String valueJson, String datatype) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return findElement(driver, String.format(onCommonMethodsPage.getLabelSelect(), commonTag));
            default:
                return findElement(driver, String.format(onCommonMethodsPage.getLabelInput(), commonTag));
        }
    }

    public List<String> getOptions(String valueJson, String dataType) {
        List<String> actualOptions = new ArrayList<>();
        switch (dataType.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                List<WebElement> dropdownOptions = new Select(getElement(valueJson, "dropdown", null)).getOptions();
                for (WebElement element : dropdownOptions) {
                    if (element.getText().isEmpty())
                        actualOptions.add("Blank");
                    else
                        actualOptions.add(element.getText());
                }
                break;
            case "radio button":
                List<WebElement> radioOptions = getElements(valueJson, "Radio Button");
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
        if (parameter.isEmpty())
            matcher = pattern.matcher(distinctRule);
        else
            matcher = pattern.matcher(JsonPath.read(valueJson, "$." + parameter).toString().trim());

        int count = 1;
        List<String> options = new ArrayList<>();
        while (matcher.find()) {
            while (count <= matcher.groupCount()) {
                options.add(matcher.group(count));
                count++;
            }
        }
        return options;
    }

    public void verifyData(String valueJson, String field, String displayedText, String requiredAttributeValue, String attribute, String distinctRule) {
        String expectedText;
        try {
            switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
                case "state dropdown":
                case "dropdown":
                    expectedText = new Select(getElement(valueJson, "dropdown", null)).getFirstSelectedOption().getText().trim();
                    if (expectedText.isEmpty())
                        expectedText = "blank";
                    printResults(valueJson, field, requiredAttributeValue, expectedText, displayedText, attribute, distinctRule);
                    break;
                case "radio button":
                    List<WebElement> radioOptions = getElements(valueJson, "Radio Button");
                    for (WebElement element : radioOptions) {
                        expectedText = element.getAttribute("aria-checked");
                        if (expectedText.equals("false"))
                            expectedText = "unchecked";
                        else
                            expectedText = "checked";
                        printResults(valueJson, field, requiredAttributeValue, expectedText, displayedText, attribute, distinctRule);
                    }
                    break;
                case "checkbox":
                    expectedText = getElement(valueJson, "checkbox", null).getAttribute("aria-checked");
                    if (expectedText.equals("false"))
                        expectedText = "unchecked";
                    else
                        expectedText = "checked";
                    printResults(valueJson, field, requiredAttributeValue, expectedText, displayedText, attribute, distinctRule);
                    break;
                default:
                    expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                    if (expectedText.isEmpty())
                        expectedText = "blank";
                    printResults(valueJson, field, requiredAttributeValue, expectedText, displayedText, attribute, distinctRule);
                    break;
            }
        } catch (NullPointerException e) {
            onSoftAssertionHandlerPage.assertNoElement(driver, field, displayedText, testContext);
        }
    }

    public List<String> getInvalidTags(List<String> skippedInvalidElements, String valueJson) {
        return skippedInvalidElements.stream().filter(valueJson::contains).collect(Collectors.toList());
    }

    public void printResults(String valueJson, String field, String requiredAttributeValue, String expectedText, String displayedText, String attribute, String distinctRule) {
        boolean flag = requiredAttributeValue.trim().equalsIgnoreCase(expectedText.trim());
        if (attribute.equalsIgnoreCase("prefilled with"))
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Prefilled Value " + displayedText, expectedText.toLowerCase(), requiredAttributeValue.toLowerCase(), flag, testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Default Value " + displayedText, expectedText.toLowerCase(), requiredAttributeValue.toLowerCase(), flag, testContext);
    }

    public void getAttributeValue(String field, String valueJson, String order, String wizardControlType, String rule, String attribute, String distinctRule) {
        String listConditionkeys;
        String displayedText;
        boolean conditionFlag;

        if (!valueJson.contains("DisplayRules")) {
            if (verifyAndMoveToPage(valueJson)) {
                if (!getElements(valueJson, wizardControlType).isEmpty())
                    getLength(valueJson, attribute, rule, field, distinctRule, null);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, rule + " Validations -> Field does not exists", true, "true", true, testContext);
            } else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, false, false, testContext);
        } else {
            setCombinationConditions(valueJson, "([^\\s]+)\\s* (=|<>|>|<) (.*)");

            for (List<String> result : combinationConditions) {
                List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: \\s*(.*?)(?:;|$)", valueJson, order, field, distinctRule);
                conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                listConditionkeys = flagInvalidKeys.get(0);
                displayedText = flagInvalidKeys.get(2);

                if (conditionFlag) {
                    if (listConditionkeys.isEmpty()) {
                        if (verifyAndMoveToPage(valueJson)) {
                            if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                if (isReadonlyDisabled(valueJson, ""))
                                    getLength(valueJson, attribute, rule, field, distinctRule, displayedText);
                                else
                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "Length", "Field is disabled", testContext);
                            } else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, rule + " Validations -> Field does not exists" + displayedText, true, "true", true, testContext);
                        } else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, false, false, testContext);
                    } else
                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                }
            }
            combinationConditions.clear();
            howManyOperator.clear();
        }
    }

    public void getLength(String valueJson, String attribute, String rule, String field, String distinctRule, String displayedText) {
        boolean expectedFlag;

        if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().equalsIgnoreCase("email")) {
            sendKeys(driver, getElement(valueJson, "input", null), JsonPath.read(valueJson, "$.Format").toString());
            expectedFlag = findElements(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Format of " + field + " is " + JsonPath.read(valueJson, "$.Format").toString(), expectedFlag, "true", expectedFlag, testContext);
        } else {
            try {
                String expectedText = getElement(valueJson, "input", null).getAttribute(attribute);

                if (expectedText.equals("99/99/9999"))
                    expectedText = "MM/dd/yyyy";
                if (rule.equalsIgnoreCase("format"))
                    expectedText = expectedText.replaceAll("9", "#");

                if (combinationConditions.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + displayedText, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);

                if (isReadonlyDisabled(valueJson, ""))
                    handleTextLengthFields(valueJson, rule, field, distinctRule, displayedText, combinationConditions);
            } catch (NullPointerException e) {
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Field does not have attribute " + attribute, false, true, false, testContext);
            }
        }
    }

    public void handleTextLengthFields(String valueJson, String rule, String field, String distinctRule, String displayedText, List<List<String>> combinationConditions) {
        int attributeValue = Integer.parseInt(JsonPath.read(valueJson, "$.Length").toString().trim());
        String allowedChars = "9";
        String expectedText;
        String expectedFormat;
        String temp;
        MaskFormatter formatter;
        String error;
        String format;
        try {
            if (rule.equalsIgnoreCase("length")) {
                for (int length = attributeValue - 1; length <= attributeValue + 1; length++) {
                    temp = RandomStringUtils.random(length, allowedChars);
                    setValue(valueJson, temp);
                    expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                    error = clickRedBubble(valueJson);
                    if (length == attributeValue + 1)
                        temp = temp.substring(0, temp.length() - 1);

                    if (valueJson.contains("\"Format\"")) {
                        format = JsonPath.read(valueJson, "$.Format").toString().trim().replaceAll("[a-zA-Z]", "#");
                        formatter = new MaskFormatter(format);
                        formatter.setValueContainsLiteralCharacters(false);
                        temp = formatter.valueToString(temp);
                    }

                    if (length == attributeValue - 1) {
                        if (error.isEmpty() & expectedText.isEmpty())
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, "Not a mandatory field or value less than given length", temp, true, testContext);
                        else if (error.isEmpty())
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, error, error, true, testContext);

                    } else {
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                    }
                }
                setValue(valueJson, "");
            } else {
                temp = RandomStringUtils.random(attributeValue, allowedChars);
                setValue(valueJson, temp);
                expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                expectedFormat = getElement(valueJson, "single line textbox", null).getAttribute("mask");
                if (combinationConditions.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + "validations when length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, rule + " validations " + displayedText + " and length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePlaceholderRules(String valueJson, String dependentCondition, String dependentResult, String field, String requiredAttributeValue, String distinctRule) {
        String expectedAttribute = getElement(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes"), "").getAttribute("placeholder");
        if (dependentCondition.isEmpty())
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Placeholder value", expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Placeholder value when " + dependentCondition + " is " + dependentResult, expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);

    }

    public void handleValidationRules(String valueJson, String dependentCondition, String dependentResult, String field, String order) {
        for (String distinctRule : JsonPath.read(valueJson, "$.ValidationRules").toString().trim().split((";"))) {
            distinctRule = distinctRule.replaceFirst("(\\d+\\.\\s*)?", "").trim();
            System.out.println(order + ". " + field + " -> " + distinctRule);
            String condition, key, values;
            String expectedOperator = "";
            String listConditionkeys;
            LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
            List<String> allKeys;
            boolean conditionFlag;
            List<String> listFieldValueConditions;
            String[] conditionValues;
            String displayedText = "";
            String conditionalOperator;
            String wizardControlType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
            String requiredErrorMessage;

            if (!(dependentCondition.isEmpty() | dependentCondition.equals("FLI_ISSUED_STATE_CODE")))
                setDependentCondition(dependentCondition, howManyOperator.get(dependentCondition), valueJson, dependentResult, distinctRule, displayedText);
            verifyAndMoveToPage(valueJson);

            if (Pattern.compile("(\\d+\\.\\s*)?If (.*?) results in an age that is less than (.*?) or greater than (.*?),? then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?) results in an age that is less than (.*?) or greater than (.*?),? then (.*?): (.*)", "", distinctRule);
                String minValue = listConditions.get(2);
                String maxValue = listConditions.get(3);
                requiredErrorMessage = listConditions.get(5);
                condition = listConditions.get(1);

                String error;
                LocalDate dob = todaysDate.minusYears(Long.parseLong(minValue)).minusMonths(8);

                listConditionkeys = findKeyExistsJSON(condition);
                if (!(listConditionkeys.isEmpty() | listConditionkeys.equalsIgnoreCase("FLI_ISSUED_STATE_CODE"))) {
                    sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                    error = clickRedBubble(valueJson);
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) > Integer.parseInt(minValue), testContext);

                    dob = todaysDate.minusYears(Long.parseLong(maxValue) + 1).minusMonths(1);
                    sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                    error = clickRedBubble(valueJson);
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> Error message when " + dependentCondition + " is " + dependentResult + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) < Integer.parseInt(maxValue), testContext);
                } else
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
            } else if (Pattern.compile("(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*?): (.*)\\.?").matcher(distinctRule).find()) {
                List<String> listExpectedConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?If (.*?)(?:,)? then (.*?): (.*)\\.?", "", distinctRule);
                condition = listExpectedConditions.get(1).trim().replaceAll("FLI_EFFECTIVE_DATE - ", "");
                requiredErrorMessage = listExpectedConditions.get(3);
                listConditionkeys = "";
                boolean hidden = false;
                conditionFlag = true;
                String tempCondition = "";
                String tempOperator = "";
                String[] tempValues = new String[0];
                boolean conditionFlagCompare = true;

                for (String eachCondition : condition.trim().split(("AND"))) {
                    if (Pattern.compile("([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)").matcher(eachCondition.trim()).find()) {
                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)", "", eachCondition.trim());
                        key = listFieldValueConditions.get(0).trim();
                        expectedOperator = listFieldValueConditions.get(1).trim();
                        conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
                        if (tempCondition.equalsIgnoreCase(key) & tempOperator.equals(">") & expectedOperator.equals("<")) {
                            howManyOperatorValidations.put(key, "=");
                            mapConditions.put(key, Arrays.asList(String.valueOf(Integer.parseInt(tempValues[0]) + 1), String.valueOf(Integer.parseInt(conditionValues[0]) - 1)));
                        } else {
                            tempCondition = key;
                            tempValues = conditionValues;
                            tempOperator = expectedOperator;

                            howManyOperatorValidations.put(key, expectedOperator);
                            // Add the key-value pairs to the map
                            mapConditions = setMapConditions(conditionValues, key, mapConditions);
                        }
                    } else
                        conditionFlag = false;
                }

                if (conditionFlag) {
                    allKeys = new ArrayList<>(mapConditions.keySet());
                    generateCombinations(allKeys, new ArrayList<>(), mapConditions, "ValidationRules");

                    for (List<String> result : combinationConditionsValidations) {
                        displayedText = " when ";
                        for (String condition1 : result) {
                            listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                            key = listFieldValueConditions.get(0).trim();
                            values = listFieldValueConditions.get(1).trim();
                            displayedText += key + howManyOperatorValidations.get(key) + values + " and ";
                            listConditionkeys = findKeyExistsJSON(key);  // This listConditionkeys is verifying whether key exists in JSON
                            conditionFlag = true;

                            if (listConditionkeys.matches("\\d+")) {
                                conditionFlagCompare = comparekeyValues(key, values);
                                listConditionkeys = listConditionkeys.replaceAll("\\d+", "");
                            } else {
                                if (!(listConditionkeys.isEmpty() | listConditionkeys.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")))
                                    break;
                                listConditionkeys = listConditionkeys.replaceAll("FLI_ISSUED_STATE_CODE", "");
                                conditionalOperator = howManyOperatorValidations.get(key);
                                List<String> valuesOperator = adjustValuesAndOperator(key, values, howManyOperatorValidations);
                                values = valuesOperator.get(0);
                                conditionalOperator = (valuesOperator.get(1).equalsIgnoreCase("")) ? conditionalOperator : valuesOperator.get(1);
                                conditionFlag = verifyStateCodeRules(key, howManyOperatorValidations.get(key), values);
                                if (!conditionFlag)
                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule is applicable " + displayedText + " and not when state code is " + executedJurisdiction, testContext);
                                else if (!key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
                                    List<String> dateFields = Arrays.asList("dob", "date", "mm/dd/yyyy");
                                    String inputValue;

                                    if (!(values.equalsIgnoreCase("blank")) & (values.equalsIgnoreCase("invalid") | values.equalsIgnoreCase("current date") | dateFields.contains(JsonPath.read(testContext.getMapTestData().get(key).trim(), "$.WizardControlTypes").toString().trim().toLowerCase()))) {
                                        inputValue = getInvalidValue(values, expectedOperator, valueJson, field, distinctRule, requiredErrorMessage, testContext);
                                        displayedText += "input is " + inputValue;
                                        conditionalOperator = "=";
                                    } else
                                        inputValue = values;
                                    if (inputValue.equalsIgnoreCase("fail")) {
                                        conditionFlag = false;
                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "No value found for " + values + displayedText, testContext);
                                    } else if (!setDependentCondition(key, conditionalOperator, valueJson, inputValue, distinctRule, displayedText))
                                        conditionFlag = false;
                                }
                            }
                        }
                        if (displayedText.trim().endsWith("and"))
                            displayedText = displayedText.substring(0, displayedText.length() - 4);

                        if (conditionFlag) {
                            if (listConditionkeys.isEmpty()) {
                                if (verifyAndMoveToPage(valueJson)) {
                                    if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                        handleErrorMessage(valueJson, requiredErrorMessage, field, distinctRule, order, displayedText, conditionFlagCompare);
                                    } else
                                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field does not exists" + displayedText, true, "true", true, testContext);
                                } else
                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, hidden, hidden, testContext);
                            } else
                                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                        }
                    }
                    combinationConditionsValidations.clear();
                    mapConditions.clear();
                    howManyOperatorValidations.clear();
                } else
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Condition " + condition + " does not match expected criteria", testContext);
            } else {
                System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Validation rules does not match any criteria", testContext);
            }
        }
    }

    private boolean comparekeyValues(String key, String values) {
        String operator = howManyOperatorValidations.get(key);
        switch (operator) {
            case ">":
                return Integer.parseInt(key) > Integer.parseInt(values);
            case "<":
                return Integer.parseInt(key) < Integer.parseInt(values);
            case "=":
                return Integer.parseInt(key) == Integer.parseInt(values);
            case "<>":
                return Integer.parseInt(key) != Integer.parseInt(values);
        }
        return false;
    }

    public void handleErrorMessage(String valueJson, String requiredErrorMessage, String field, String distinctRule, String order, String displayedText, boolean conditionFlagCompare) {
        String inputValue = "";
        List<String> dateFields = Arrays.asList("dob", "date", "mm/dd/yyyy");
        String error;

        error = clickRedBubble(valueJson);
        if (dateFields.stream().anyMatch(JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()::contains)) {
            if (displayedText.equalsIgnoreCase(" when "))
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> " + " Value Validation", error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> " + " Value Validation " + displayedText, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            sendKeys(driver, getElement(valueJson, "single line textbox", null), "");
        } else {
            if (!conditionFlagCompare)
                requiredErrorMessage = "";
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> " + displayedText, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Radio Button")) {
                resetValue(valueJson, inputValue);
            } else {
                if (isReadonlyDisabled(valueJson, ""))
                    resetValue(valueJson, "");
            }
        }
    }

    public String getInvalidValue(String expectedResult, String expectedOperator, String valueJson, String field, String distinctRule, String requiredErrorMessage, TestContext testContext) {
        try {
            List<String> dateCondition = new ArrayList<>();

            List<String> dateFields = Arrays.asList("dob", "date", "mm/dd/yyyy");
            List<String> expectedValues = Arrays.asList("invalid", "yes", "husband", "wife", "spouse");
            if (expectedValues.stream().anyMatch(expectedResult.toLowerCase()::contains)) {
                switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().toLowerCase()) {
                    case "tin":
                        return testContext.getMapTestData().get("InvalidTin").trim();
                    case "ssn":
                        return testContext.getMapTestData().get("InvalidSSN").trim();
                    case "date":
                    case "dob":
                    case "mm/dd/yyyy":
                        return testContext.getMapTestData().get("InvalidDate").trim();
                    case "email":
                    case "single line textbox":
                        return validateInvalidEmail(valueJson, field, requiredErrorMessage, distinctRule);
                    case "radio button":
                    case "dropdown":
                    case "state dropdown":
                        return expectedResult;
                }
            } else if (expectedResult.equalsIgnoreCase("current date")) {
                switch (expectedOperator) {
                    case "=":
                        return todaysDate.format(format);
                    case ">":
                        return todaysDate.plusDays(1).format(format);
                }
            } else if (dateFields.contains(JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase())) {
                if (expectedResult.toLowerCase().contains(" "))
                    dateCondition = Arrays.asList(expectedResult.split(" "));

                switch (expectedOperator) {
                    case "<":
                        return todaysDate.minusYears(Long.parseLong(expectedResult)).plusDays(1).format(format);
                    case "=":
                        if (!expectedResult.equalsIgnoreCase("blank"))
                            return todaysDate.minusYears(Long.parseLong(expectedResult)).format(format);
                    case ">":
                        if (!dateCondition.isEmpty() && dateCondition.get(1).equalsIgnoreCase("months"))
                            return todaysDate.minusMonths(Long.parseLong(dateCondition.get(0))).minusDays(1).format(format);
                        else
                            return todaysDate.minusYears(Long.parseLong(expectedResult) + 1).minusDays(1).format(format);
                }
            }
        } catch (NumberFormatException e) {
            return "fail";
        }
        return "";
    }

    public String clickRedBubble(String valueJson) {
        waitForPageToLoad(driver);
        String error;
        waitForPageToLoad(driver);
        try {
            if (onCommonMethodsPage.getListErrors().isEmpty())
                clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            if (!(JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Checkbox") | JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Radio Button"))) {
                if (isReadonlyDisabled(valueJson, ""))
                    clickElement(driver, getElement(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes").toString(), JsonPath.read(valueJson, "$.WizardControlTypes").toString()));
            }
            WebElement errorElement = getElement(valueJson, "errortype", JsonPath.read(valueJson, "$.WizardControlTypes").toString());
            error = errorElement.getText();
        } catch (NullPointerException e) {
            // Handle null pointer exception
            error = "";
        }
        return error;
    }

    public boolean isReadonlyDisabled(String valueJson, String result) {
        String wizardControlType = JsonPath.read(valueJson, "$.WizardControlTypes").toString();
        WebElement elem = (wizardControlType.equalsIgnoreCase("radio button")) ? getElement(valueJson, wizardControlType, result) : getElement(valueJson, wizardControlType,"");
        return !(isAttributePresent(elem, "readonly") | isAttributePresent(elem, "disabled") | isAttributePresent(elem, "class"));
    }

    public String validateInvalidEmail(String valueJson, String field, String requiredErrorMessage, String distinctRule) {
        List<String> invalidEmails = Arrays.asList(testContext.getMapTestData().get("InvalidEmail").trim().split(","));
        String lastInvalidEmail = invalidEmails.get(invalidEmails.size() - 1);
        for (String invalidEmail : invalidEmails) {
            sendKeys(driver, getElement(valueJson, "input", null), invalidEmail);
            String error = clickRedBubble(valueJson);
            String validationMessage = "Invalid Value Validation when " + field + " is " + invalidEmail;
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, validationMessage, error, requiredErrorMessage, error.equalsIgnoreCase(requiredErrorMessage), testContext);
        }
        return lastInvalidEmail;
    }

    public void printFinalResults() {
        long endTime;
        LocalTime endLocalTime;
        endTime = System.currentTimeMillis();
        endLocalTime = LocalTime.now();
        long durationMillis = endTime - onLoginPage.getStartTime();
        long hours = durationMillis / (1000 * 60 * 60);
        long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = ((durationMillis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        difference = String.format("%dh %dm %ds", hours, minutes, seconds);
        testContext.getScenario().write("<div width='100%' style='font-size:1.6vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>Cucumber Report : " + LocalDate.now() + "</div>");
        testContext.getScenario().write("<div width='100%' style='font-size:1.2vw; border: none; color: green; font-weight: bold; background-color: #C5D88A;'>" + timeFormat.format(onLoginPage.getStartLocalTime()) + " - " + timeFormat.format(endLocalTime) + "(" + difference + ")</div>");
        onSoftAssertionHandlerPage.afterScenario(testContext, fieldsEvaluated);
    }

    private void generateCombinations(List<String> keys, List<String> combination, Map<String, List<String>> keyValuesMap, String conditionCombination) {
        if (keys.isEmpty()) {
            // Print the combination
            if (conditionCombination.equals("ValidationRules"))
                combinationConditionsValidations.add(combination);
            else
                combinationConditions.add(combination);
            return;
        }
        String currentKey = keys.get(0);
        List<String> currentValues = keyValuesMap.get(currentKey);
        for (String value : currentValues) {
            // Add the current key-value pair to the combination
            List<String> newCombination = new ArrayList<>(combination);
            newCombination.add(currentKey + ": " + value);
            // Recur with the remaining keys
            generateCombinations(keys.subList(1, keys.size()), newCombination, keyValuesMap, conditionCombination);
        }
    }

    public boolean verifyElementExists(String valueJson, List<String> skippedInvalidElements, String order, String field) {
        String listConditionkeys;
        List<String> invalidTag = new ArrayList<>();
        String displayedText;

        if (valueJson.contains("DisplayRules")) {
            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]);
            if (Pattern.compile("([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)").matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]).find())
                setCombinationConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)");
        }

        if (invalidTag.isEmpty()) {
            if (combinationConditions.isEmpty()) {
                if (verifyAndMoveToPage(valueJson)) {
                    if (!getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).isEmpty())
                        return !getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).isEmpty();
                    else
                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Either Wizard Control type or Common tag is incorrect", testContext);
                } else {
                    onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", testContext);
                }
            } else {
                List<String> flagInvalidKeys = setDependentConditions(combinationConditions.get(0), "([^\\s]+)\\s*: (.*)", valueJson, order, field, "");
                listConditionkeys = flagInvalidKeys.get(0);
                displayedText = flagInvalidKeys.get(2);

                if (listConditionkeys.isEmpty()) {
                    if (verifyAndMoveToPage(valueJson)) {
                        if (!getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).isEmpty())
                            return !getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).isEmpty();
                        else
                            onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Either Wizard Control type or Common tag is incorrect", testContext);
                    } else {
                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists " + displayedText, testContext);
                    }
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag"), "", "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                combinationConditions.clear();
                howManyOperator.clear();
            }
        } else
            onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
        return false;
    }
}




