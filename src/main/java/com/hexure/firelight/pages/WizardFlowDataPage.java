package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
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

@Data
public class WizardFlowDataPage extends FLUtilities {
    public CommonMethodsPage onCommonMethodsPage;
    public LoginPage onLoginPage;
    public SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    public TestContext testContext;
    public WebDriver driver;
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
    Map<String, String> howManyOperatorDisplayCondition = new HashMap<>();
    Map<String, String> howManyOperatorValidations = new HashMap<>();
    String jurisdictionStatesCode = "";
    List<String> displayEnableConditions = new ArrayList<>();
    List<String> displayConditions = new ArrayList<>();
    List<String> skippedInvalidElements = new ArrayList<>();
    String executedJurisdiction = "";

    public WizardFlowDataPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void setPageObjects(TestContext testContext, WebDriver driver) {
        onCommonMethodsPage = testContext.getPageObjectManager().getCommonMethodPage();
        onSoftAssertionHandlerPage = testContext.getPageObjectManager().getSoftAssertionHandlerPage();
        onLoginPage = testContext.getPageObjectManager().getLoginPage();
        this.testContext = testContext;
        this.driver = driver;
    }

    /**
     * @param field This will validate following sections if available : List Options, Rules Wizard, Length and Format.
     *              Under RulesWizard, while testing default values, it validates placeholder and validation rules too.
     *              For each rule, combinations of conditions are created along with logical operator.
     *              Then each dependednt condition is set to validate the field under test.
     *              <p>
     *              Each rule will validate following parts:
     *              A) Whether element with given commonTag exists. If it doesn't, it is added to invalidTags list. This is checked for the field under test and its dependent conditions
     *              B) If commonTag exists in JSON.
     *              C) If display rule is specified, it will first set all those dependent conditions.
     *              D) Based on this display rule, whether Page for field under test is displayed.
     *              E) Based on this display rule, whether field under test is displayed.
     *              F) Based on this display rule, whether field under test is enabled.
     *              G) If the field under test is radio button/dropdown, whether it contains given value to be set
     *              H) All these checks C - F are checked for dependent conditions too
     */
    public void wizardTesting(String field) {
        String section = "";
        String wizardControlType;
        String reason;
        String order;
        List<String> invalidTag;
        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> rulesList = Arrays.asList("ListOptions", "RulesWizard", "Length", "Format");

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
                if (valueJson.contains("Section\"")) section = JsonPath.read(valueJson, "$.Section").toString().trim();
                wizardControlType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
                onCommonMethodsPage.moveToPage(driver, JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
                if (verifyElementExists(valueJson, order, field, true, "")) {
                    for (String rule : rulesList) {
                        combinationConditions.clear();
                        howManyOperator.clear();
                        displayEnableConditions.clear();
                        displayConditions.clear();
                        try {
                            switch (rule) {
                                /** Verify dropdown/radio button options in UI matches with the ones provided in spec. **/
                                case "ListOptions":
                                    mapConditions.clear();
                                    verifyListOptions(valueJson, field, order);
                                    break;
                                case "RulesWizard":
                                    /** Iterate over each rule of Rules Wizard column **/
                                    for (String distinctRule : JsonPath.read(valueJson, "$." + rule).toString().trim().split(";")) {
                                        distinctRule = distinctRule.replaceFirst("(\\d+\\.\\s*)?", "").trim().replaceFirst("\\.$", "").trim();
                                        System.out.println(order + ". " + field + " -> " + distinctRule);
                                        combinationConditions.clear();
                                        combinationConditionsValidations.clear();
                                        howManyOperatorValidations.clear();
                                        howManyOperator.clear();
                                        List<String> skippedRules = Arrays.asList("lookup", "not required to use", "implemented then specify", "skip for automation");
                                        if (!(skippedRules.stream().anyMatch(distinctRule::contains) | distinctRule.isEmpty())) {
                                            enableDisplayCondition(valueJson, displayConditions, howManyOperatorDisplayCondition);
                                            invalidTag = getInvalidTags(skippedInvalidElements, distinctRule);
                                            if (invalidTag.isEmpty()) {
                                                /** Validate dropdown options or label of any text depending on given conditions **/
                                                if (Pattern.compile("(\\d+\\.\\s*)?(?i)If (.*?),? (?i)then (?i)(SHOW|HIDE) (Options|Option|Label as) (.*)\\.?").matcher(distinctRule).find()) {
                                                    mapConditions.clear();
                                                    handleShowHideOptionsLabel(valueJson, mapConditions, order, field, distinctRule, wizardControlType);
                                                } else if (Pattern.compile("(\\d+\\.\\s*)?(?i)If (.*?)(?:,)? (?i)then (.*)\\.?").matcher(distinctRule).find()) {
                                                    /** Validates visibility coditions for a field based on some given conditions  **/
                                                    handleVisibilityRules(valueJson, mapConditions, order, field, distinctRule, wizardControlType);
                                                } else if (Pattern.compile("(\\d+\\.\\s*)?(?i)(Default|Placeholder) = (.*)\\.?").matcher(distinctRule).find()) {
                                                    /** Validates Default value of a field
                                                     * Also validate Placeholder value if given in spec and validation rules for each field **/
                                                    handleDefaultPlaceholderRules(valueJson, mapConditions, order, field, distinctRule, wizardControlType, section);
                                                } else if (Pattern.compile("(\\d+\\.\\s*)?(Age is calculated on age last birth date|(?i)Always enabled|(?i)Always disable|Optional)\\.?").matcher(distinctRule).find()) {
                                                    /** validates all other cases for a field **/
                                                    handleAllOtherRulesWizards(valueJson, mapConditions, order, field, distinctRule, wizardControlType);
                                                } else {
                                                    System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule does not match any criteria for field", testContext);
                                                }
                                            } else
                                                onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
                                        } else
                                            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, reason, testContext);
                                    }
                                    enableDisplayCondition(valueJson, displayEnableConditions, howManyOperatorEnableCondition);
                                    break;
                                /** Validates length and Format of a field **/
                                case "Length":
                                case "Format":
                                    handleLengthFormatRules(testContext, driver, valueJson, order, field, rule, wizardControlType, executedJurisdiction, moduleName);
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + rule);
                            }
                        } catch (PathNotFoundException e) {
                            System.out.println("Field " + field + " does not have rule \"" + rule + "\"");
                        }
                    }
                } else {
                    if (!field.isEmpty())
                        skippedInvalidElements.add(field);
                    System.out.println("The skipped invalid elements" + skippedInvalidElements);
                }
            } else {
                if (!field.isEmpty()) skippedInvalidElements.add(field);
                onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Either field is Lookup, hide for day 1 or has no wizard control type", testContext);
            }
        } else
            onSoftAssertionHandlerPage.assertSkippedElement(driver, "", executedJurisdiction, moduleName, field, "Field has No Tag", testContext);
    }


    public void handleLengthFormatRules(TestContext testContext, WebDriver driver, String valueJson, String order, String field, String rule, String wizardControlType, String executedJurisdiction, String moduleName) {
        List<Object> invalidFlag = setInvalidTags(valueJson, "", "(\\S+)\\s*(=|<>|<|>)\\s*(.*)");
        List<String> invalidTag = (List<String>) invalidFlag.get(0);
        String attribute = "maxLength";
        if (rule.equalsIgnoreCase("format"))
            attribute = "mask";
        if (invalidTag.isEmpty()) {
            if (!JsonPath.read(valueJson, "$." + rule).toString().trim().equalsIgnoreCase("blank")) {
                getAttributeValue(field, valueJson, order, wizardControlType, rule, attribute);
            }
        } else
            onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
    }

    public void handleAllOtherRulesWizards(String valueJson, LinkedHashMap<String, List<String>> mapConditions, String order, String field, String distinctRule, String wizardControlType) {
        List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(Age is calculated on age last birth date|(?i)Always enabled|(?i)Always disable|Optional)\\.?", "", distinctRule);
        String requiredAttributeValue = listConditions.get(1);
        mapConditions.clear();
        boolean expectedFlag;
        List<Object> invalidFlag = setInvalidTags(valueJson, "", "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)");
        boolean conditionFlag = (Boolean) invalidFlag.get(1);

        if (conditionFlag) {
            switch (requiredAttributeValue.toLowerCase()) {
                case "age is calculated on age last birth date":
                    for (List<String> result : combinationConditions) {
                        List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, distinctRule);
                        conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                        String listConditionkeys = flagInvalidKeys.get(0);
                        String displayedText = flagInvalidKeys.get(2);

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
                                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON " + displayedText, testContext);
                        }
                    }
                    combinationConditions.clear();
                    howManyOperator.clear();
                    break;
                case "always enabled":
                    verifyAndMoveToPage(valueJson);
                    expectedFlag = getElement(valueJson, wizardControlType, null).isEnabled();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is always enabled", expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "always disable":
                    verifyAndMoveToPage(valueJson);
                    for (WebElement element : getElements(valueJson, wizardControlType)) {
                        expectedFlag = element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput");
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is always disabled", expectedFlag, "true", expectedFlag, testContext);
                    }
                    break;
                case "optional":
                    verifyAndMoveToPage(valueJson);
                    String error = clickRedBubble(valueJson);
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Optional Field Validation", error, "", error.isEmpty(), testContext);
                    break;
            }
        } else
            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "There must be consecutive ANDs in display rule", testContext);
    }

    /**
     * get all options of a dropdown or a radio button
     *
     * @param valueDependentJson, conditionalWizard, result
     * @return true if value exists
     */
    public boolean verifyValueExists(String valueDependentJson, String conditionalWizard, String result) {
        return !getOptions(valueDependentJson, conditionalWizard).contains(result);
    }

    /**
     * Verify if key (commontag) exists in an input JSON
     *
     * @param condition
     * @return Blank if key exists else key itself
     */
    public String findKeyExistsJSON(String condition) {
        if (!testContext.getMapTestData().containsKey(condition)) return condition;
        return "";
    }

    public void handleDefaultPlaceholderRules(String valueJson, LinkedHashMap<String, List<String>> mapConditions, String order, String field, String distinctRule, String wizardControlType, String section) {
        List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(?i)(Default|Placeholder) = (.*)\\.?", "", distinctRule);
        String requiredFirstAttribute = listConditions.get(1);
        String requiredAttributeValue = listConditions.get(2);
        String key = "";
        String values = "";
        mapConditions.clear();
        String displayedText = "";

        List<Object> invalidFlag = setInvalidTags(valueJson, "", "(.*?) (=|<>|<|>) (.*)");
        boolean conditionFlag = (Boolean) invalidFlag.get(1);

        if (conditionFlag) {
            if (combinationConditions.isEmpty()) {   //Display rules is not available for this field
                setDefaultPlaceholderConditions(valueJson, wizardControlType, requiredAttributeValue, requiredFirstAttribute, field, distinctRule, displayedText, order, key, values, section);
            } else {
                for (List<String> result : combinationConditions) {
                    List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*:\\s*(.*?)(?:;|$)", valueJson, order, field, distinctRule);
                    conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                    String listConditionkeys = flagInvalidKeys.get(0);
                    displayedText = flagInvalidKeys.get(2);

                    if (conditionFlag) {
                        if (listConditionkeys.isEmpty()) {
                            setDefaultPlaceholderConditions(valueJson, wizardControlType, requiredAttributeValue, requiredFirstAttribute, field, distinctRule, displayedText, order, key, values, section);
                            if (displayConditions.isEmpty()) {
                                displayConditions.addAll(result);
                                howManyOperatorDisplayCondition = new HashMap<>(howManyOperator);
                            }
                        } else
                            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON " + displayedText, testContext);
                    }
                }
                combinationConditions.clear();
                howManyOperator.clear();
            }
        } else
            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "There must be consecutive ANDs in display rule", testContext);
    }

    public void setDefaultPlaceholderConditions(String valueJson, String wizardControlType, String requiredAttributeValue, String requiredFirstAttribute, String field, String distinctRule, String displayedText, String order, String key, String values, String section) {
        boolean conditionFlag = true;
        if (verifyAndMoveToPage(valueJson)) {
                if (!getElements(valueJson, wizardControlType).isEmpty()) {
                    if (requiredFirstAttribute.equalsIgnoreCase("Placeholder"))
                        handlePlaceholderRules(valueJson, field, requiredAttributeValue, distinctRule, displayedText);
                    else {
                        verifyData(valueJson, field, requiredAttributeValue, "", distinctRule, displayedText, order);
                        if(!key.isEmpty())
                            conditionFlag = verifyStateCodeRules(key, howManyOperator.get(key), values);
                        if (!conditionFlag)
                            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule is applicable " + displayedText + " not when state code is " + executedJurisdiction, testContext);
                        else {
                            if (valueJson.contains("ValidationRules"))
                                handleValidationRules(valueJson, key, values, field, order, displayedText);
                            handleSectionRules(valueJson, wizardControlType, section, order, field, displayedText);
                        }
                    }
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field does not exists" + displayedText, true, "true", true, testContext);
            } else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, "true", true, testContext);
    }

    public void handleVisibilityRules(String valueJson, LinkedHashMap<String, List<String>> mapConditions, String order, String field, String distinctRule, String wizardControlType) {
        List<String> listExpectedConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(?i)If (.*?)(?:,)? (?i)then (.*)\\.?", "", distinctRule);
        String condition = listExpectedConditions.get(1);
        List<String> expectedResults = Arrays.asList(listExpectedConditions.get(2).split(" (?i)AND "));
        mapConditions.clear();
        boolean hidden = false;
        boolean conditionFlag = true;
        String secondParamter = "";
        if (expectedResults.size() > 1)
            secondParamter = expectedResults.get(1);
        if (secondParamter.equalsIgnoreCase("hide")) hidden = true;

        for (String eachCondition : condition.trim().split(("AND"))) {
            List<String> listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)", "", eachCondition.trim());
            conditionFlag = !(listFieldValueConditions.isEmpty());
            if (!conditionFlag) {
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "There must be consecutive ANDs in display rule", testContext);
                break;
            }
            String key = listFieldValueConditions.get(0).trim();
            conditionFlag = setKeyDisplay(valueJson, key, order, field, distinctRule, "");
            String expectedOperator = listFieldValueConditions.get(1).trim();
            String[] conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
            howManyOperator.put(key, expectedOperator);
            // Add the key-value pairs to the map
            mapConditions = setMapConditions(conditionValues, key, mapConditions);
        }
        combinationConditions.clear();
        if (conditionFlag) {
            List<String> allKeys = new ArrayList<>(mapConditions.keySet());
            generateCombinations(allKeys, new ArrayList<>(), mapConditions, "RulesWizard");

            for (List<String> result : combinationConditions) {
                List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, distinctRule);
                conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                String listConditionkeys = flagInvalidKeys.get(0);
                String displayedText = flagInvalidKeys.get(2);

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
                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON " + displayedText, testContext);
                }
            }
            combinationConditions.clear();
            howManyOperator.clear();
        }
    }

    public void handleShowHideOptionsLabel(String valueJson, LinkedHashMap<String, List<String>> mapConditions, String order, String field, String distinctRule, String wizardControlType) {
        List<String> listConditions;
        listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(?i)If (.*?),? (?i)then (?i)(SHOW|HIDE) (Options|Option|Label as) (.*)\\.?", "", distinctRule);
        String condition = listConditions.get(1);
        String expectedResult = listConditions.get(2);
        String requiredSecondAttribute = listConditions.get(4);
        boolean conditionFlag = true;
        List<String> listFieldValueConditions;

        for (String eachCondition : condition.trim().split(("(?i)(AND)"))) {
            listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*(=|<>|<|>)\\s*(.*)", "", eachCondition.trim());
            conditionFlag = !(listFieldValueConditions.isEmpty());
            if (!conditionFlag)
                break;
            String key = listFieldValueConditions.get(0).trim();
            String expectedOperator = listFieldValueConditions.get(1).trim();
            String[] conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
            howManyOperator.put(key, expectedOperator);
            mapConditions = setMapConditions(conditionValues, key, mapConditions);
        }
        if (conditionFlag) {
            List<String> allKeys = new ArrayList<>(mapConditions.keySet());
            generateCombinations(allKeys, new ArrayList<>(), mapConditions, "");

            for (List<String> result : combinationConditions) {
                List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, distinctRule);
                conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                String listConditionkeys = flagInvalidKeys.get(0);
                String displayedText = flagInvalidKeys.get(2);

                if (conditionFlag) {
                    if (listConditionkeys.isEmpty()) {
                        verifyAndMoveToPage(valueJson);
                        switch (wizardControlType) {
                            case "Dropdown":
                            case "State Dropdown":
                                List<String> expectedOptions = Arrays.asList(requiredSecondAttribute.split(", "));
                                List<String> actualOptions = getOptions(valueJson, wizardControlType);
                                if (expectedResult.equalsIgnoreCase("show"))
                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " Options" + displayedText, actualOptions, expectedOptions, actualOptions.equals(expectedOptions), testContext);
                                else
                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " Options" + displayedText, actualOptions, expectedOptions, !(actualOptions.containsAll(expectedOptions)), testContext);
                                break;
                            default:
                                String expectedValue = getElementLabel(valueJson, wizardControlType).getText();
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field is displayed under label" + displayedText, expectedValue, requiredSecondAttribute, expectedValue.replaceAll("’", "").equals(requiredSecondAttribute), testContext);
                        }
                    } else
                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON " + displayedText, testContext);
                }
            }
            combinationConditions.clear();
            howManyOperator.clear();
        } else
            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "There must be consecutive ANDs in display rule", testContext);
    }


    /**
     * It sets precondition to display or enable any field based on its given Display rule.
     *
     * @param valueJson
     * @param conditions
     * @param operators
     */
    public void enableDisplayCondition(String valueJson, List<String> conditions, Map<String, String> operators) {
        if (!conditions.isEmpty()) {
            for (String condition1 : conditions) {
                List<String> listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                String key = listFieldValueConditions.get(0).trim();
                String values = listFieldValueConditions.get(1).trim();
                String conditionalOperator = operators.get(key);
                setDisplayEnableConditions(key, values, conditionalOperator, valueJson, operators);
            }
        }
    }

    public void verifyListOptions(String valueJson, String field, String order) {
        String options = JsonPath.read(valueJson, "$.ListOptions").toString().trim();
        List<String> expectedOptions;

        expectedOptions = getExpectedOptions(options);
        if (!expectedOptions.isEmpty()) {
            List<Object> invalidFlag = setInvalidTags(valueJson, "", "(\\S+)\\s*(=|<>|<|>)\\s*(.*)");
            List<String> invalidTag = (List<String>) invalidFlag.get(0);
            boolean conditionFlag = (Boolean) invalidFlag.get(1);

            if (conditionFlag) {
                if (invalidTag.isEmpty()) {
                    if (combinationConditions.isEmpty()) {
                        verifyOptions(valueJson, field, expectedOptions, "", "List Options");
                    } else {
                        for (List<String> result : combinationConditions) {
                            List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: (.*)", valueJson, order, field, "List Options");
                            conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                            String listConditionkeys = flagInvalidKeys.get(0);
                            String displayedText = flagInvalidKeys.get(2);

                            if (conditionFlag) {
                                if (listConditionkeys.isEmpty()) {
                                    verifyAndMoveToPage(valueJson);
                                    verifyOptions(valueJson, field, expectedOptions, displayedText, "List Options");
                                } else
                                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "List Options", "Key " + listConditionkeys + " does not exists in JSON " + displayedText, testContext);
                            }
                        }
                        combinationConditions.clear();
                        howManyOperator.clear();
                    }
                } else
                    onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
            } else
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "List Options", "There must be consecutive ANDs in display rule", testContext);
        } else
            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "List Options", "Data list does not contain \"" + options + "\"", testContext);
    }

    /**
     * Perform action to Display or Enable a field
     *
     * @param key
     * @param values
     * @param conditionalOperator
     * @param valueJson
     * @param operators
     */
    public void setDisplayEnableConditions(String key, String values, String conditionalOperator, String valueJson, Map<String, String> operators) {
        if (!key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
            List<String> valuesOperator = adjustValuesAndOperator(key, values, operators);
            values = valuesOperator.get(0);
            conditionalOperator = (valuesOperator.get(1).equalsIgnoreCase("")) ? conditionalOperator : valuesOperator.get(1);
            if (!(onCommonMethodsPage.getListErrors().isEmpty()))
                clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            setDependentCondition(key, conditionalOperator, valueJson, values, "", "");
        }
    }

    /**
     * A field may display or enable based on another fields. This method display/enable those dependent fields based on corresponding display rules.
     *
     * @param valueJson
     * @param key
     * @param order
     * @param field
     * @param distinctRule
     * @param conditionCombination
     * @return
     */
    public boolean setKeyDisplay(String valueJson, String key, String order, String field, String distinctRule, String conditionCombination) {
        if (!(key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE") | key.matches("[0-9]+"))) {
            boolean conditionFlag = findKeyExistsJSON(key).isEmpty();
            if (!conditionFlag) {
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + key + " does not exists in JSON ", testContext);
                return false;
            }
            String tempJson = testContext.getMapTestData().get(key);
            if (verifyAndMoveToPage(tempJson)) {
                if (getElements(tempJson, JsonPath.read(tempJson, "$.WizardControlTypes").toString().trim()).isEmpty())
                    conditionFlag = verifyElementExists(tempJson, JsonPath.read(tempJson, "$.Order").toString().trim(), JsonPath.read(tempJson, "$.CommonTag").toString().trim(), false, conditionCombination);
                if (!conditionFlag) {
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + key + " does not exists in UI ", testContext);
                    return false;
                }
            }
            verifyAndMoveToPage(valueJson);
        }
        return true;
    }

    /**
     * Verify if a given page exists.
     * If page exists, move to that page
     *
     * @param valueJson
     * @return
     */
    public boolean verifyAndMoveToPage(String valueJson) {
        if (onCommonMethodsPage.verifyPage(driver, JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim())) {
            onCommonMethodsPage.moveToPage(driver, JsonPath.read(valueJson, "$.Page").toString().trim(), JsonPath.read(valueJson, "$.ModuleSectionName").toString().trim());
            waitForPageToLoad(driver);
            return true;
        }
        return false;
    }

    /**
     * This sets precondition for each field to set dependent conditions while validating Rules
     * Also checks if a rule is applicable to given Jurisdiction based on key "FLI_ISSUED_STATE_CODE:.
     * Also sets text to be displayed in cucumber report - displayedText
     * Also sets value to be set on a field based on logical operators specified in dependent conditions.
     * If element is enabled, it sets the value else mark that rule as a skipped rule
     * it also sets invalid value for a field
     *
     * @param result
     * @param pattern
     * @param valueJson
     * @param order
     * @param field
     * @param distinctRule
     * @return conditionFlag - false if any dependent condition fails such as page/field does not exists or rule not applicable else true
     * displayedText - Text to be displayed in cucumber report
     * listConditionkeys - if any key/commonTag does not exists in JSON or is skipped previously as it is not available in UI
     */
    public List<String> setDependentConditions(List<String> result, String pattern, String valueJson, String order, String field, String distinctRule) {
        String displayedText = " when ";
        List<String> listFieldValueConditions;
        String conditionalOperator;
        String listConditionkeys = "";
        boolean conditionFlag = true;

        for (String condition1 : result) {
            listFieldValueConditions = getDisplayRuleConditions(valueJson, pattern, "", condition1.trim());
            String key = listFieldValueConditions.get(0).trim();
            String values = listFieldValueConditions.get(1).trim();
            listConditionkeys = findKeyExistsJSON(key);

            if (!(listConditionkeys.isEmpty() | listConditionkeys.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")))
                break;
            listConditionkeys = listConditionkeys.replaceAll("FLI_ISSUED_STATE_CODE", "");
            conditionalOperator = howManyOperator.get(key);
            List<String> valuesOperator = adjustValuesAndOperator(key, values, howManyOperator);
            values = valuesOperator.get(0);
            conditionalOperator = (valuesOperator.get(1).equalsIgnoreCase("")) ? conditionalOperator : valuesOperator.get(1);
            conditionFlag = verifyStateCodeRules(key, howManyOperator.get(key), values);
            displayedText += key + " " + howManyOperator.get(key) + " " + values + " and ";
            if (!conditionFlag)
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule is applicable " + displayedText + " not when state code is " + executedJurisdiction, testContext);
            else if (!key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
                String valueDependentJson = testContext.getMapTestData().get(key);
                if (verifyAndMoveToPage(valueDependentJson)) {
                    if (verifyElementDisabled(valueDependentJson, values)) {
                        if (!setDependentCondition(key, conditionalOperator, valueJson, values, distinctRule, displayedText))
                            conditionFlag = false;
                    } else {
                        conditionFlag = false;
                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Field " + key + " is disabled " + displayedText, testContext);
                    }
                } else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, false, false, testContext);
            }
        }
        if (displayedText.trim().endsWith("and"))
            displayedText = displayedText.substring(0, displayedText.length() - 4);
        return Arrays.asList(listConditionkeys, String.valueOf(conditionFlag), displayedText);
    }

    /**
     * Verify if element is disabled
     *
     * @param valueDependentJson
     * @param result
     * @return true if element is enabled else false
     */
    public boolean verifyElementDisabled(String valueDependentJson, String result) {
        String conditionalWizard = JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString();
        WebElement elem;
        if (conditionalWizard.equalsIgnoreCase("radio button"))
            elem = getElement(valueDependentJson, conditionalWizard, result);
        else
            elem = getElement(valueDependentJson, conditionalWizard, "");
        return (!(isAttributePresent(elem, "readonly") | isAttributePresent(elem, "disabled")));
    }

    /**
     * This validates visibility rules for a field.
     * If based on multiple conditions, value for a field is set to another field or given value, it validates the same.
     *
     * @param requiredAttribute
     * @param valueJson
     * @param wizardControlType
     * @param order
     * @param field
     * @param secondAttribute
     * @param distinctRule
     * @param displayedText
     */
    public void setVisibilityRules(String requiredAttribute, String valueJson, String wizardControlType, String order, String field, String secondAttribute, String distinctRule, String displayedText) {
        boolean expectedFlag;
        WebElement elem;

        if (Pattern.compile("SET (\\S+)\\s* = (.*)").matcher(requiredAttribute).find()) {
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
                            verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, prefilledValue, testData, distinctRule, displayedText, order);
                            setDependentCondition(expectedResultFirst, "=", valueJson, "", distinctRule, displayedText);
                        } else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Field " + conditionFirst + " does not exists" + displayedText, true, "true", true, testContext);
                    } else
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, "true", true, testContext);
                } else {
                    if (verifyAndMoveToPage(valueJson)) {
                        if (!getElements(valueJson, wizardControlType).isEmpty()) {
                            verifyData(testContext.getMapTestData().get(conditionFirst).trim(), field, expectedResultFirst, "", distinctRule, displayedText, order);
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
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " " + element.getAttribute("title") + " is enabled " + displayedText, element.isEnabled(), "true", element.isEnabled(), testContext);
                    }
                    break;
                case "disable":
                    if (!secondAttribute.equalsIgnoreCase("hide")) {
                        for (WebElement element : getElements(valueJson, wizardControlType)) {
                            expectedFlag = element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput") | isAttributePresent(element, "readonly") | isAttributePresent(element, "disabled");
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " \"" + element.getAttribute("title") + "\" disabled " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                        }
                    }
                    break;
                case "set to no":
                    elem = getElement(valueJson, "radioField", "No");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " No is selected" + displayedText, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to yes":
                    elem = getElement(valueJson, "radioField", "Yes");
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " Yes is selected " + displayedText, elem.getAttribute("aria-checked"), "true", elem.getAttribute("aria-checked").equalsIgnoreCase("true"), testContext);
                    break;
                case "set to self":
                case "set to united states":
                    verifyData(valueJson, field, getDisplayRuleConditions(valueJson, "set to (.*)", "", requiredAttribute.toLowerCase()).get(0), "", distinctRule, displayedText, order);
                    break;
                case "hide":
                    expectedFlag = getElements(valueJson, wizardControlType).isEmpty();
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Hidden Rule " + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    break;
                case "read only":
                    for (WebElement element : getElements(valueJson, wizardControlType)) {
                        expectedFlag = (element.getAttribute("class").contains("disabled") | element.getAttribute("class").contains("readOnlyInput"));
                        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, wizardControlType + " \"" + element.getAttribute("title") + "\" read only" + displayedText, expectedFlag, "true", expectedFlag, testContext);
                    }
                    break;
                default:
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule does not match any visibility condition", testContext);
            }
        }
    }

    /**
     * It creates multiple combinations of given conditions
     * Example, For given rule " If Owner_Type = Individual, UTMA/UGMA AND Owner_DOB <60 then HIDE options Yes, No.". this will create following combinations:
     * Owner_Type: Individual, Owner_DOB<60
     * Owner_Yype: UTMA/UGMA, Owner_DOB<60
     *
     * @param valueJson
     * @param pattern
     * @param conditionCombination
     * @return true if conditions are specified correctly else false
     */
    public boolean setCombinationConditions(String valueJson, String pattern, String conditionCombination) {
        List<String> listFieldValueConditions;
        String key;
        String[] conditionValues;
        String expectedOperator;
        boolean conditionFlag;
        LinkedHashMap<String, List<String>> mapConditions = new LinkedHashMap<>();
        List<String> allKeys;

        for (String eachCondition : JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0].split(("AND"))) {
            listFieldValueConditions = getDisplayRuleConditions(valueJson, pattern, "", eachCondition.trim());
            conditionFlag = !(listFieldValueConditions.isEmpty());
            if (!conditionFlag) return false;

            key = listFieldValueConditions.get(0).trim();
            expectedOperator = listFieldValueConditions.get(1).trim();
            conditionValues = listFieldValueConditions.get(2).trim().trim().split(", ");
            howManyOperator.put(key, expectedOperator);
            // Add the key-value pairs to the map
            mapConditions = setMapConditions(conditionValues, key, mapConditions);
        }
        allKeys = new ArrayList<>(mapConditions.keySet());
        generateCombinations(allKeys, new ArrayList<>(), mapConditions, conditionCombination);
        return true;
    }

    /**
     * validates given section for each field
     *
     * @param valueJson
     * @param wizardControlType
     * @param section
     * @param order
     * @param field
     * @param displayedText
     */
    public void handleSectionRules(String valueJson, String wizardControlType, String section, String order, String field, String displayedText) {
        section = section.replaceAll("X", "1");
        boolean expectedFlag = getElementSection(valueJson, wizardControlType, section);
        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, "Section Information", "Field is displayed under section " + section + displayedText, expectedFlag, "true", expectedFlag, testContext);
    }

    /**
     * Calculates age based on given DOB from currentDate
     *
     * @param dob
     * @param currentDate
     * @return age as integer
     */
    public int calculateAge(LocalDate dob, LocalDate currentDate) {
        return Period.between(dob, currentDate).getYears();
    }

    /**
     * Verify options of a dropdown/radio button
     *
     * @param valueJson
     * @param field
     * @param expectedOptions
     * @param displayedText
     * @param distinctRule
     */
    public void verifyOptions(String valueJson, String field, List<String> expectedOptions, String displayedText, String distinctRule) {
        String dataType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
        List<String> actualOptions = getOptions(valueJson, dataType);
        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, dataType + " Options" + displayedText, actualOptions, expectedOptions, actualOptions.containsAll(expectedOptions), testContext);
    }

    /**
     * Removes failed conditions from displayedText so that cucumber report shows correct dependent conditions
     *
     * @param displayedText
     * @return displayedText
     */
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

    /**
     * setDependentConditions sets the condition and its values. It
     * This method actually sets the value for each condition.
     * It checks if page exists, or value exists for given dropdown/radio button.
     *
     * @param condition
     * @param expectedOperator
     * @param valueJson
     * @param result
     * @param distinctRule
     * @param displayedText
     * @return true if a value is set for a given field
     */
    public boolean setDependentCondition(String condition, String expectedOperator, String valueJson, String result, String distinctRule, String displayedText) {
        String valueDependentJson = testContext.getMapTestData().get(condition).trim();
        String conditionalWizard = JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim();
        if (verifyAndMoveToPage(valueDependentJson)) {
            if (getElements(valueDependentJson, conditionalWizard).isEmpty()) {
                displayedText = removeUnsetCondition(displayedText);
                onSoftAssertionHandlerPage.assertSkippedRules(driver, JsonPath.read(valueJson, "$.Order").toString().trim(), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag").toString().trim(), distinctRule, "Key " + condition + " does not exists in UI " + displayedText, testContext);
                return false;
            }

            if (result.isEmpty() | result.equalsIgnoreCase("blank")) {
                result = "Blank";
                if (conditionalWizard.equalsIgnoreCase("dropdown")) {
                    if (verifyValueExists(valueDependentJson, conditionalWizard, result)) {
                        onSoftAssertionHandlerPage.assertSkippedRules(driver, JsonPath.read(valueJson, "$.Order").toString().trim(), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag").toString().trim(), distinctRule, "Value " + result + " does not exist for " + conditionalWizard + " " + condition, testContext);
                        return false;
                    }
                }
            } else if (conditionalWizard.equalsIgnoreCase("dropdown") | conditionalWizard.equalsIgnoreCase("radio button")) {
                if (verifyValueExists(valueDependentJson, conditionalWizard, result)) {
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, JsonPath.read(valueJson, "$.Order").toString().trim(), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag").toString().trim(), distinctRule, "Value " + result + " does not exist for " + conditionalWizard + " " + condition, testContext);
                    return false;
                }
            }
            if (expectedOperator.equalsIgnoreCase("=")) {
                if (verifyElementDisabled(valueDependentJson, result)) {
                    if ((result.equalsIgnoreCase("selected") | result.equalsIgnoreCase("unselected")) && conditionalWizard.equalsIgnoreCase("checkbox"))
                        return verifyCheckBoxSelectYesNO(result, getElement(valueDependentJson, conditionalWizard, result));
                    else
                        setValue(valueDependentJson, result);
                }
            } else if (expectedOperator.equalsIgnoreCase("<>")) {
                if (verifyElementDisabled(valueDependentJson, result))
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

    /**
     * Set value in a given field
     *
     * @param valueDependentJson
     * @param result
     */
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
                    if (prefilledValue.equals("0")) elem.selectByIndex(Integer.parseInt(result) + 1);
                    prefilledValue = elem.getFirstSelectedOption().getText().trim();
                } else new Select(getElement(valueDependentJson, "dropdown", null)).selectByVisibleText(result);
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
            case "multiline textbox":
            case "multi line text box":
                sendKeys(driver, getElement(valueDependentJson, "multiline textbox", null), result);
                syncElement(driver, getElement(valueDependentJson, "multiline textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            default:
                sendKeys(driver, getElement(valueDependentJson, "single line textbox", null), result);
                syncElement(driver, getElement(valueDependentJson, "single line textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(1000);
    }

    /**
     * Reset/Clear each field
     *
     * @param valueDependentJson
     * @param result
     */
    public void resetValue(String valueDependentJson, String result) {
        waitForPageToLoad(driver);
        if (result.equalsIgnoreCase("Blank")) {
            result = "";
        }
        switch (JsonPath.read(valueDependentJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                new Select(getElement(valueDependentJson, "dropdown", null)).selectByVisibleText(result);
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
            case "multiline textbox":
            case "multi line text box":
                sendKeys(driver, getElement(valueDependentJson, "multiline textbox", null), "");
                syncElement(driver, getElement(valueDependentJson, "multiline textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
            default:
                sendKeys(driver, getElement(valueDependentJson, "single line textbox", null), "");
                syncElement(driver, getElement(valueDependentJson, "single line textbox", null), EnumsCommon.TOCLICKABLE.getText());
                break;
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(1000);
    }

    /**
     * Verify if rule is applicable to a given jurisdiction
     *
     * @param key
     * @param operator
     * @param values
     * @return true if rule is applicable else false
     */
    private boolean verifyStateCodeRules(String key, String operator, String values) {
        if (key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
            boolean isEqualOperator = operator.equals("=");
            boolean isNotEqualOperator = operator.equals("<>");

            return ((isEqualOperator && values.equalsIgnoreCase(jurisdictionStatesCode)) || (isNotEqualOperator && !values.equalsIgnoreCase(jurisdictionStatesCode)));
        }
        return true;
    }

    /**
     * Set test data for a field
     *
     * @param valueDependentJson
     * @return testdata
     */
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

    /**
     * verify if an element exists
     *
     * @param valueJson
     * @param datatype
     * @return list of web element
     */
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
            case "multiline textbox":
            case "multi line text box":
                return findElements(driver, String.format(onCommonMethodsPage.getTextareaField(), commonTag));
            default:
                return findElements(driver, String.format(onCommonMethodsPage.getInputField(), commonTag));
        }
    }

    /**
     * Create map for each key and values pair
     *
     * @param conditionValues
     * @param key
     * @param mapConditions
     * @return map
     */
    public LinkedHashMap<String, List<String>> setMapConditions(String[] conditionValues, String key, LinkedHashMap<String, List<String>> mapConditions) {
        for (String value : conditionValues) {
            List<String> valuesList = mapConditions.getOrDefault(key, new ArrayList<>());
            valuesList.add(value.trim());
            mapConditions.put(key, valuesList);
        }
        return mapConditions;
    }

    /**
     * @param valueJson
     * @param datatype
     * @param optionalValue
     * @return webelement
     */
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
                    case "multiline textbox":
                    case "multi line text box":
                        return findElement(driver, String.format(onCommonMethodsPage.getTextareaErrorField(), commonTag));
                    default:
                        return findElement(driver, String.format(onCommonMethodsPage.getInputErrorField(), commonTag));
                }
            case "multiline textbox":
            case "multi line text box":
                return findElement(driver, String.format(onCommonMethodsPage.getTextareaField(), commonTag));
            default:
                return findElement(driver, String.format(onCommonMethodsPage.getInputField(), commonTag));
        }
    }

    /**
     * @param valueJson
     * @param datatype
     * @param section
     * @return section of each webelement
     */
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
            case "multiline textbox":
            case "multi line text box":
                return !findElements(driver, String.format(onCommonMethodsPage.getSectionTextarea(), section, commonTag)).isEmpty();
            default:
                return !findElements(driver, String.format(onCommonMethodsPage.getSectionInput(), section, commonTag)).isEmpty();
        }
    }

    /**
     * @param valueJson
     * @param datatype
     * @return label of each webelement
     */
    public WebElement getElementLabel(String valueJson, String datatype) {
        String commonTag = JsonPath.read(valueJson, "$.CommonTag").toString().trim();
        switch (datatype.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                return findElement(driver, String.format(onCommonMethodsPage.getLabelSelect(), commonTag));
            case "multiline textbox":
            case "multi line text box":
                return findElement(driver, String.format(onCommonMethodsPage.getLabelTextarea(), commonTag));
            default:
                return findElement(driver, String.format(onCommonMethodsPage.getLabelInput(), commonTag));
        }
    }

    /**
     * @param valueJson
     * @param dataType
     * @return dropdown or radio button options
     */
    public List<String> getOptions(String valueJson, String dataType) {
        List<String> actualOptions = new ArrayList<>();
        switch (dataType.toLowerCase()) {
            case "dropdown":
            case "state dropdown":
                List<WebElement> dropdownOptions = new Select(getElement(valueJson, "dropdown", null)).getOptions();
                for (WebElement element : dropdownOptions) {
                    if (element.getText().isEmpty()) actualOptions.add("Blank");
                    else actualOptions.add(element.getText());
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

    /**
     * @param valueJson
     * @param requiredPattern
     * @param parameter
     * @param distinctRule
     * @return list of conditions based on given pattern and rule
     */
    public List<String> getDisplayRuleConditions(String valueJson, String requiredPattern, String parameter, String distinctRule) {
        Pattern pattern = Pattern.compile(requiredPattern);
        Matcher matcher;
        if (parameter.isEmpty()) matcher = pattern.matcher(distinctRule);
        else matcher = pattern.matcher(JsonPath.read(valueJson, "$." + parameter).toString().trim());

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

    /**
     * Verify default/invalid value of a field
     *
     * @param valueJson
     * @param field
     * @param requiredAttributeValue
     * @param attribute
     * @param distinctRule
     * @param displayedText
     * @param order
     */
    public void verifyData(String valueJson, String field, String requiredAttributeValue, String attribute, String distinctRule, String displayedText, String order) {
        String expectedText;

        switch (JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()) {
            case "state dropdown":
            case "dropdown":
                expectedText = new Select(getElement(valueJson, "dropdown", null)).getFirstSelectedOption().getText().trim();
                if (expectedText.isEmpty())
                    expectedText = requiredAttributeValue;
                printResults(field, requiredAttributeValue, expectedText, attribute, distinctRule, displayedText, order);
                break;
            case "radio button":
                if (!(requiredAttributeValue.toLowerCase().contains("checked") | requiredAttributeValue.equalsIgnoreCase("blank"))) {
                    expectedText = getElement(valueJson, "Radio Button", requiredAttributeValue).getAttribute("aria-checked");
                    if (expectedText.trim().equalsIgnoreCase("true"))
                        requiredAttributeValue = "true";
                    printResults(field, requiredAttributeValue, expectedText, attribute, distinctRule, displayedText, order);
                } else {
                    List<WebElement> radioOptions = getElements(valueJson, "Radio Button");
                    for (WebElement element : radioOptions) {
                        expectedText = element.getAttribute("aria-checked");
                        expectedText = getExpectedText(expectedText, requiredAttributeValue);
                        displayedText += " and " + field + " is " + element.getAttribute("title");
                        printResults(field, requiredAttributeValue, expectedText, attribute, distinctRule, displayedText, order);
                    }
                }
                break;
            case "checkbox":
                expectedText = getElement(valueJson, "checkbox", null).getAttribute("aria-checked");
                expectedText = getExpectedText(expectedText, requiredAttributeValue);
                printResults(field, requiredAttributeValue, expectedText, attribute, distinctRule, displayedText, order);
                break;
            case "multiline textbox":
            case "multi line text box":
                expectedText = getElement(valueJson, "multiline textbox", null).getAttribute("value");
                if (expectedText.isEmpty())
                    expectedText = requiredAttributeValue;
                printResults(field, requiredAttributeValue, expectedText, attribute, distinctRule, displayedText, order);
                break;
            default:
                expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                if (expectedText.isEmpty() && requiredAttributeValue.equalsIgnoreCase("blank"))
                    expectedText = requiredAttributeValue;
                printResults(field, requiredAttributeValue, expectedText, attribute, distinctRule, displayedText, order);
                break;
        }
    }

    public String getExpectedText(String expectedText, String requiredAttributeValue) {
        switch (expectedText + "_" + requiredAttributeValue.toLowerCase()) {
            case "false_unchecked":
            case "false_unselected":
            case "true_checked":
            case "true_selected":
                expectedText = requiredAttributeValue;
                break;
            case "false_checked":
                expectedText = "Unchecked";
                break;
            case "false_selected":
                expectedText = "Unselected";
                break;
            case "true_unchecked":
                expectedText = "Checked";
                break;
            case "true_unselected":
                expectedText = "Selected";
                break;
        }
        return expectedText;
    }

    /**
     * Add element to skippedInvalidElements list if it does not exist in UI
     *
     * @param skippedInvalidElements
     * @param valueJson
     * @return
     */
    public List<String> getInvalidTags(List<String> skippedInvalidElements, String valueJson) {
        return skippedInvalidElements.stream().filter(valueJson::contains).collect(Collectors.toList());
    }

    /**
     * add assertions
     *
     * @param field
     * @param requiredAttributeValue
     * @param expectedText
     * @param attribute
     * @param distinctRule
     * @param displayedText
     * @param order
     */
    public void printResults(String field, String requiredAttributeValue, String expectedText, String attribute, String distinctRule, String displayedText, String order) {
        boolean flag = requiredAttributeValue.trim().equals(expectedText.trim());
        if (attribute.equalsIgnoreCase("prefilled with"))
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Prefilled Value " + displayedText, expectedText, requiredAttributeValue, flag, testContext);
        else
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, distinctRule, "Default Value " + displayedText, expectedText, requiredAttributeValue, flag, testContext);
    }

    /**
     * get value of an attribute maxlength or a mask for a field
     *
     * @param field
     * @param valueJson
     * @param order
     * @param wizardControlType
     * @param rule
     * @param attribute
     */
    public void getAttributeValue(String field, String valueJson, String order, String wizardControlType, String rule, String attribute) {
        String listConditionkeys;
        String displayedText;
        boolean conditionFlag;

        if (!valueJson.contains("DisplayRules")) {
            if (verifyAndMoveToPage(valueJson)) {
                if (!getElements(valueJson, wizardControlType).isEmpty())
                    getLength(valueJson, attribute, rule, field, null);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, rule, rule + " Validations -> Field does not exists", true, "true", true, testContext);
            } else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, rule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists", true, false, false, testContext);
        } else {
            conditionFlag = setCombinationConditions(valueJson, "([^\\s]+)\\s* (=|<>|>|<) (.*)", "");
            if (conditionFlag) {
                for (List<String> result : combinationConditions) {
                    List<String> flagInvalidKeys = setDependentConditions(result, "([^\\s]+)\\s*: \\s*(.*?)(?:;|$)", valueJson, order, field, rule);
                    conditionFlag = Boolean.parseBoolean(flagInvalidKeys.get(1));
                    listConditionkeys = flagInvalidKeys.get(0);
                    displayedText = flagInvalidKeys.get(2);

                    if (conditionFlag) {
                        if (listConditionkeys.isEmpty()) {
                            if (verifyAndMoveToPage(valueJson)) {
                                if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                    if (verifyElementDisabled(valueJson, ""))
                                        getLength(valueJson, attribute, rule, field, displayedText);
                                    else
                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, "Length", "Field is disabled", testContext);
                                } else
                                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, rule, rule + " Validations -> Field does not exists" + displayedText, true, "true", true, testContext);
                            } else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), order, executedJurisdiction, moduleName, field, rule, rule + " Validations -> Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists" + displayedText, true, false, false, testContext);
                        } else
                            onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, rule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
                    }
                }
                combinationConditions.clear();
                howManyOperator.clear();
            } else
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, rule, "There must be consecutive ANDs in display rule", testContext);
        }
    }

    /**
     * Enter digits and verify format for a field
     *
     * @param valueJson
     * @param attribute
     * @param rule
     * @param field
     * @param displayedText
     */
    public void getLength(String valueJson, String attribute, String rule, String field, String displayedText) {
        boolean expectedFlag;

        if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().equalsIgnoreCase("email")) {
            sendKeys(driver, getElement(valueJson, "input", null), JsonPath.read(valueJson, "$.Format").toString());
            expectedFlag = findElements(driver, String.format(onCommonMethodsPage.getInputErrorField(), JsonPath.read(valueJson, "$.CommonTag").toString().trim())).isEmpty();
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, "Format of " + field + " is " + JsonPath.read(valueJson, "$.Format").toString(), expectedFlag, "true", expectedFlag, testContext);
        } else {
            try {
                String expectedText = getElement(valueJson, "input", null).getAttribute(attribute);

                if (expectedText.equals("99/99/9999")) expectedText = "MM/dd/yyyy";
                if (rule.equalsIgnoreCase("format")) expectedText = expectedText.replaceAll("9", "#");

                if (combinationConditions.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + displayedText, expectedText, JsonPath.read(valueJson, "$." + rule).toString().trim(), expectedText.equalsIgnoreCase(JsonPath.read(valueJson, "$." + rule).toString().trim()), testContext);

                if (verifyElementDisabled(valueJson, ""))
                    handleTextLengthFields(valueJson, rule, field, displayedText, combinationConditions);
            } catch (NullPointerException e) {
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, "Field does not have attribute " + attribute, "", JsonPath.read(valueJson, "$." + rule).toString().trim(), false, testContext);
            }
        }
    }

    /**
     * Enter digits of different length to test length and format of each field
     *
     * @param valueJson
     * @param rule
     * @param field
     * @param displayedText
     * @param combinationConditions
     */
    public void handleTextLengthFields(String valueJson, String rule, String field, String displayedText, List<List<String>> combinationConditions) {
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
                    if (length == attributeValue + 1) temp = temp.substring(0, temp.length() - 1);

                    if (valueJson.contains("\"Format\"")) {
                        format = JsonPath.read(valueJson, "$.Format").toString().trim().replaceAll("[a-zA-Z]", "#");
                        formatter = new MaskFormatter(format);
                        formatter.setValueContainsLiteralCharacters(false);
                        temp = formatter.valueToString(temp);
                    }

                    if (length == attributeValue - 1) {
                        if (error.isEmpty() & expectedText.isEmpty()) {
                            if (combinationConditions.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations when length is " + length, "Not a mandatory field or value less than given length", temp, true, testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations " + displayedText + " and length is " + length, "Not a mandatory field or value less than given length", temp, true, testContext);
                        } else if (error.isEmpty()) {
                            if (combinationConditions.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations when length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations " + displayedText + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        } else {
                            if (combinationConditions.isEmpty())
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations when length is " + length, error, error, true, testContext);
                            else
                                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations " + displayedText + " and length is " + length, error, error, true, testContext);
                        }
                    } else {
                        if (combinationConditions.isEmpty())
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations when length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                        else
                            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations " + displayedText + " and length is " + length, expectedText, temp, expectedText.equalsIgnoreCase(temp), testContext);
                    }
                }
            } else {
                temp = RandomStringUtils.random(attributeValue, allowedChars);
                setValue(valueJson, temp);
                expectedText = getElement(valueJson, "single line textbox", null).getAttribute("value");
                expectedFormat = getElement(valueJson, "single line textbox", null).getAttribute("mask");
                if (combinationConditions.isEmpty())
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + "validations when length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
                else
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, rule, rule + " validations " + displayedText + " and length is " + attributeValue, expectedText, expectedFormat, expectedText.equalsIgnoreCase(expectedFormat), testContext);
            }
            setValue(valueJson, "");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify placeholder for a given field
     *
     * @param valueJson
     * @param field
     * @param requiredAttributeValue
     * @param distinctRule
     * @param displayedText
     */
    public void handlePlaceholderRules(String valueJson, String field, String requiredAttributeValue, String distinctRule, String displayedText) {
        String expectedAttribute = getElement(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes"), "").getAttribute("placeholder");
        onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Placeholder value " + displayedText, expectedAttribute, requiredAttributeValue, expectedAttribute.equalsIgnoreCase(requiredAttributeValue), testContext);

    }

    /**
     * Handle validation rules.
     * Iterate over each validation rule and validate the same
     *
     * @param valueJson
     * @param dependentCondition
     * @param dependentResult
     * @param field
     * @param order
     * @param displayedText
     */
    public void handleValidationRules(String valueJson, String dependentCondition, String dependentResult, String field, String order, String displayedText) {
        String displayedTextNew = displayedText;
        for (String distinctRule : JsonPath.read(valueJson, "$.ValidationRules").toString().trim().split((";"))) {
            displayedText = displayedTextNew;
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
            String conditionalOperator;
            String wizardControlType = JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim();
            String requiredErrorMessage;

            if (!(dependentCondition.isEmpty() | dependentCondition.equals("FLI_ISSUED_STATE_CODE")))
                setDependentCondition(dependentCondition, howManyOperator.get(dependentCondition), valueJson, dependentResult, distinctRule, displayedText);
            verifyAndMoveToPage(valueJson);

            if (Pattern.compile("(\\d+\\.\\s*)?(?i)If (.*?) results in an age that is less than (.*?) or greater than (.*?),? (?i)then (.*?): (.*)").matcher(distinctRule).find()) {
                List<String> listConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(?i)If (.*?) results in an age that is less than (.*?) or greater than (.*?),? (?i)then (.*?): (.*)", "", distinctRule);
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
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> Error message when " + displayedText + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) > Integer.parseInt(minValue), testContext);

                    dob = todaysDate.minusYears(Long.parseLong(maxValue) + 1).minusMonths(1);
                    sendKeys(driver, getElement(valueJson, "single line textbox", null), dob.format(format));
                    error = clickRedBubble(valueJson);
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> Error message when " + displayedText + " and age is " + calculateAge(dob, todaysDate) + " and DOB is " + dob.format(formatWithSlash), error, requiredErrorMessage, calculateAge(dob, todaysDate) < Integer.parseInt(maxValue), testContext);
                } else
                    onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Key " + listConditionkeys + " does not exists in JSON", testContext);
            } else if (Pattern.compile("(\\d+\\.\\s*)?(?i)If (.*?)(?:,)? (?i)then (.*?): (.*)\\.?").matcher(distinctRule).find()) {
                /** Validate error message based on dependent conditions **/
                List<String> listExpectedConditions = getDisplayRuleConditions(valueJson, "(\\d+\\.\\s*)?(?i)If (.*?)(?:,)? (?i)then (.*?): (.*)\\.?", "", distinctRule);
                condition = listExpectedConditions.get(1).trim().replaceAll("FLI_EFFECTIVE_DATE - ", "");
                requiredErrorMessage = listExpectedConditions.get(3);
                listConditionkeys = "";
                boolean hidden = false;
                conditionFlag = true;
                boolean conditionAnotherFlag = true;
                String tempCondition = "";
                String tempOperator = "";
                String[] tempValues = new String[0];
                boolean conditionFlagCompare = true;
                List<String> dateFields = Arrays.asList("dob", "date", "mm/dd/yyyy");

                for (String eachCondition : condition.trim().split(("AND"))) {
                    if (Pattern.compile("(\\S+)\\s*(=|>=|<=|<>|<|>)\\s*(.*)").matcher(eachCondition.trim()).find()) {
                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "(\\S+)\\s*(=|>=|<=|<>|<|>)\\s*(.*)", "", eachCondition.trim());
                        key = listFieldValueConditions.get(0).trim();
                        conditionAnotherFlag = setKeyDisplay(valueJson, key, order, field, distinctRule, "ValidationRules");
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
                combinationConditionsValidations.clear();
                if (conditionAnotherFlag) {
                    if (conditionFlag) {
                        allKeys = new ArrayList<>(mapConditions.keySet());
                        generateCombinations(allKeys, new ArrayList<>(), mapConditions, "ValidationRules");

                        for (List<String> result : combinationConditionsValidations) {
                            displayedText = displayedTextNew;
                            for (String condition1 : result) {
                                listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                                key = listFieldValueConditions.get(0).trim();
                                values = listFieldValueConditions.get(1).trim();
                                listConditionkeys = findKeyExistsJSON(key);  // This listConditionkeys is verifying whether key exists in JSON
                                conditionFlag = true;

                                if (findKeyExistsJSON(values).isEmpty() && dateFields.contains(JsonPath.read(testContext.getMapTestData().get(key).trim(), "$.WizardControlTypes").toString().trim().toLowerCase())) {
                                    if (!setDependentCondition(values, "=", valueJson, todaysDate.format(format), distinctRule, displayedText))
                                        conditionFlag = false;
                                    values = "current date";
                                }
                                if (listConditionkeys.matches("[0-9]+")) {
                                    conditionFlagCompare = compareValues(key, values);
                                    listConditionkeys = listConditionkeys.replaceAll("[0-9]+", "");
                                } else {
                                    if (!(listConditionkeys.isEmpty() | listConditionkeys.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")))
                                        break;
                                    listConditionkeys = listConditionkeys.replaceAll("FLI_ISSUED_STATE_CODE", "");
                                    conditionalOperator = howManyOperatorValidations.get(key);
                                    List<String> valuesOperator = adjustValuesAndOperator(key, values, howManyOperatorValidations);
                                    values = valuesOperator.get(0);
                                    conditionalOperator = (valuesOperator.get(1).equalsIgnoreCase("")) ? conditionalOperator : valuesOperator.get(1);
                                    conditionFlag = verifyStateCodeRules(key, howManyOperatorValidations.get(key), values);
                                    displayedText += " and " + key + howManyOperatorValidations.get(key) + values;
                                    if (!conditionFlag)
                                        onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Rule is applicable " + displayedText + " not when state code is " + executedJurisdiction, testContext);
                                    else if (!key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
                                        String inputValue;
                                        if (!(values.equalsIgnoreCase("blank")) & (values.equalsIgnoreCase("invalid") | values.equalsIgnoreCase("current date") | dateFields.contains(JsonPath.read(testContext.getMapTestData().get(key).trim(), "$.WizardControlTypes").toString().trim().toLowerCase()))) {
                                            inputValue = getInvalidValue(values, expectedOperator, valueJson, field, distinctRule, requiredErrorMessage, testContext);
                                            displayedText += " and input is " + inputValue;
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

                            if (conditionFlag) {
                                if (listConditionkeys.isEmpty()) {
                                    if (displayedText.trim().endsWith("and"))
                                        displayedText = displayedText.substring(0, displayedText.length() - 4);
                                    if (verifyAndMoveToPage(valueJson)) {
                                        if (!getElements(valueJson, wizardControlType).isEmpty()) {
                                            handleErrorMessage(valueJson, requiredErrorMessage, field, distinctRule, displayedText, conditionFlagCompare);
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
                }
            } else {
                System.out.println("Rule " + distinctRule + " does not match any criteria for field " + field);
                onSoftAssertionHandlerPage.assertSkippedRules(driver, order, executedJurisdiction, moduleName, field, distinctRule, "Validation rules does not match any criteria", testContext);
            }
        }
    }

    /**
     * Compare integer values
     *
     * @param key
     * @param values
     * @return true if condition is satisfied else false
     */
    public boolean compareValues(String key, String values) {
        if (howManyOperatorValidations.get(key).equalsIgnoreCase(">"))
            return Integer.parseInt(key) > Integer.parseInt(values);
        else if (howManyOperatorValidations.get(key).equalsIgnoreCase("<"))
            return Integer.parseInt(key) < Integer.parseInt(values);
        else if (howManyOperatorValidations.get(key).equalsIgnoreCase("="))
            return Integer.parseInt(key) == Integer.parseInt(values);
        else if (howManyOperatorValidations.get(key).equalsIgnoreCase("<>"))
            return Integer.parseInt(key) != Integer.parseInt(values);
        return true;
    }

    /**
     * validates error message
     *
     * @param valueJson
     * @param requiredErrorMessage
     * @param field
     * @param distinctRule
     * @param displayedText
     * @param conditionFlagCompare
     */
    public void handleErrorMessage(String valueJson, String requiredErrorMessage, String field, String distinctRule, String displayedText, boolean conditionFlagCompare) {
        String inputValue = "";
        List<String> dateFields = Arrays.asList("dob", "date", "mm/dd/yyyy");
        String error;

        error = clickRedBubble(valueJson);
        if (dateFields.stream().anyMatch(JsonPath.read(valueJson, "$.WizardControlTypes").toString().trim().toLowerCase()::contains)) {
            if (displayedText.equalsIgnoreCase(" when "))
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> Value Validation", error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            else
                onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> Value Validation " + displayedText, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            sendKeys(driver, getElement(valueJson, "single line textbox", null), "");
        } else {
            if (!conditionFlagCompare) requiredErrorMessage = "";
            onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, field, distinctRule, "Validation Rule -> " + displayedText, error, requiredErrorMessage, error.contains(requiredErrorMessage), testContext);
            if (JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Radio Button")) {
                resetValue(valueJson, inputValue);
            } else {
                if (verifyElementDisabled(valueJson, "")) resetValue(valueJson, "");
            }
        }
    }

    /**
     * @param expectedResult
     * @param expectedOperator
     * @param valueJson
     * @param field
     * @param distinctRule
     * @param requiredErrorMessage
     * @param testContext
     * @return invalidValue for a given field
     */
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
                    default:
                        return "TestValue";
                }
            } else if (expectedResult.equalsIgnoreCase("current date")) {
                switch (expectedOperator) {
                    case "=":
                        return todaysDate.format(format);
                    case ">":
                        return todaysDate.plusDays(1).format(format);
                    case "<":
                        return todaysDate.minusDays(1).format(format);
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

    /**
     * Click red bubble
     *
     * @param valueJson
     * @return error message
     */
    public String clickRedBubble(String valueJson) {
        waitForPageToLoad(driver);
        String error;
        waitForPageToLoad(driver);
        try {
            if (onCommonMethodsPage.getListErrors().isEmpty())
                clickElement(driver, onCommonMethodsPage.getRedColorErrorValidationBubble());
            if (!(JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Checkbox") | JsonPath.read(valueJson, "$.WizardControlTypes").toString().equals("Radio Button"))) {
                if (verifyElementDisabled(valueJson, ""))
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

    /**
     * validate invalid values for an email field
     *
     * @param valueJson
     * @param field
     * @param requiredErrorMessage
     * @param distinctRule
     * @return invalidValue
     */
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

    /**
     * Read options from given spec
     *
     * @param options
     * @return
     */
    public List<String> getExpectedOptions(String options) {
        List<String> expectedOptions = new ArrayList<>();
        if (options.contains(";")) expectedOptions = Arrays.asList(options.split(";"));
        else if (testContext.getMapTestData().containsKey(options.replaceAll(" ", "")))
            expectedOptions = Arrays.asList(testContext.getMapTestData().get(options.replaceAll(" ", "")).split(", "));
        return expectedOptions;
    }

    /**
     * calculate execution time for cucumber report
     */
    public void printFinalResults(int fieldsEvaluated) {
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

    /**
     * Find if commonTag exists in skipped elements
     *
     * @param valueJson
     * @param conditionCombination
     * @param pattern
     * @return invalid common tag. Return true if display rule is given in specified format else false
     */
    public List<Object> setInvalidTags(String valueJson, String conditionCombination, String pattern) {
        List<String> invalidTag = new ArrayList<>();
        boolean conditionFlag = true;
        if (valueJson.contains("DisplayRules")) {
            invalidTag = getInvalidTags(skippedInvalidElements, JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]);
            if (Pattern.compile(pattern).matcher(JsonPath.read(valueJson, "$.DisplayRules").toString().trim().split(";")[0]).find())
                conditionFlag = setCombinationConditions(valueJson, pattern, conditionCombination);
        }
        return Arrays.asList(invalidTag, conditionFlag);
    }

    /**
     * Verify if an element exists on a UI based on display rules
     *
     * @param valueJson
     * @param order
     * @param field
     * @param assertionFlag
     * @param conditionCombination
     * @return true if exists else false
     */
    public boolean verifyElementExists(String valueJson, String order, String field, boolean assertionFlag, String conditionCombination) {
        List<String> listFieldValueConditions;
        String key;
        String listConditionkeys = "";
        String values;
        List<String> invalidTag;
        String conditionalOperator;
        String displayedText = "";
        boolean conditionFlag;

        List<Object> invalidFlag = setInvalidTags(valueJson, conditionCombination, "(\\S+)\\s*(=|<>|<|>)\\s*(.*)");
        invalidTag = (List<String>) invalidFlag.get(0);
        conditionFlag = (Boolean) invalidFlag.get(1);

        List<List<String>> tempCombinationConditions = (conditionCombination.equalsIgnoreCase("ValidationRules")) ? combinationConditionsValidations : combinationConditions;
        if (conditionFlag) {
            if (invalidTag.isEmpty()) {
                if (!tempCombinationConditions.isEmpty()) {
                    List<String> result = tempCombinationConditions.get(0);
                    for (String condition1 : result) {
                        listFieldValueConditions = getDisplayRuleConditions(valueJson, "([^\\s]+)\\s*: (.*)", "", condition1.trim());
                        key = listFieldValueConditions.get(0).trim();
                        values = listFieldValueConditions.get(1).trim();
                        displayedText += key + " " + howManyOperator.get(key) + " " + values + " and ";
                        listConditionkeys = findKeyExistsJSON(key);
                        if (!(listConditionkeys.isEmpty() | listConditionkeys.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")))
                            break;
                        listConditionkeys = listConditionkeys.replaceAll("FLI_ISSUED_STATE_CODE", "");
                        conditionalOperator = howManyOperator.get(key);
                        List<String> valuesOperator = adjustValuesAndOperator(key, values, howManyOperator);
                        values = valuesOperator.get(0);
                        conditionalOperator = (valuesOperator.get(1).equalsIgnoreCase("")) ? conditionalOperator : valuesOperator.get(1);
                        if (key.equalsIgnoreCase("FLI_ISSUED_STATE_CODE")) {
                            if (!((howManyOperator.get(key).equalsIgnoreCase("=") & values.equalsIgnoreCase(jurisdictionStatesCode)) | (howManyOperator.get(key).equalsIgnoreCase("<>") & !(values.equalsIgnoreCase(jurisdictionStatesCode))))) {
                                if (assertionFlag)
                                    onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Either field does not exists or not applicable for jurisdiction " + executedJurisdiction, testContext);
                                return false;
                            }
                        } else {
                            setDependentCondition(key, conditionalOperator, valueJson, values, "Display rule -> " + JsonPath.read(valueJson, "$.DisplayRules").toString().trim(), displayedText);
                        }
                    }
                }
                if (listConditionkeys.isEmpty()) {
                    if (verifyAndMoveToPage(valueJson)) {
                        if (!getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).isEmpty())
                            return !getElements(valueJson, JsonPath.read(valueJson, "$.WizardControlTypes")).isEmpty();
                        else if (assertionFlag)
                            onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Either Wizard Control type or Common tag is incorrect", testContext);
                    } else if (assertionFlag)
                        onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Page " + JsonPath.read(valueJson, "$.Page").toString().trim() + " does not exists " + displayedText, testContext);
                } else if (assertionFlag)
                    onSoftAssertionHandlerPage.assertTrue(driver, String.valueOf(countValidation++), JsonPath.read(valueJson, "$.Order"), executedJurisdiction, moduleName, JsonPath.read(valueJson, "$.CommonTag"), "Display rule -> " + JsonPath.read(valueJson, "$.DisplayRules").toString().trim(), "Key " + listConditionkeys + " does not exists in JSON", false, "true", false, testContext);
                if (conditionCombination.equalsIgnoreCase("ValidationRules"))
                    combinationConditionsValidations.clear();
                else
                    combinationConditions.clear();
                howManyOperator.clear();
            } else if (assertionFlag)
                onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "Key " + invalidTag + " not a valid tag", testContext);
        } else if (assertionFlag)
            onSoftAssertionHandlerPage.assertSkippedElement(driver, order, executedJurisdiction, moduleName, field, "There must be consecutive ANDs in display rule", testContext);
        return false;
    }

    /**
     * set value based on a given operator
     *
     * @param key
     * @param values
     * @param operators
     * @return list of values and updated logical operator
     */
    private List<String> adjustValuesAndOperator(String key, String values, Map<String, String> operators) {
        String operator = operators.get(key);
        String conditionalOperator = "";
        if (values.matches("\\d+")) {
            switch (operator) {
                case ">":
                    values = String.valueOf(Integer.parseInt(values) + 1);
                    conditionalOperator = "=";
                    break;
                case "<":
                    values = String.valueOf(Integer.parseInt(values) - 1);
                    conditionalOperator = "=";
                    break;
            }
        }
        return Arrays.asList(values, conditionalOperator);
    }
}

