package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class CommonMethodsPage extends FLUtilities {
    List<List<String>> listInputFields = new ArrayList<>();

    public String selectField = "//select[@data-dataitemid='%s']";
    public String textareaField = "//textarea[@data-dataitemid='%s']";
    public String labelField = "//div[@data-dataitemid='%s']";
    public String inputField = "//input[@data-dataitemid='%s']";
    public String radioField = "//div[@data-dataitemid='%s']//div[@title='%s']";
    public String chkBoxField = "//div[@data-dataitemid='%s']//div[@role]";
    public String txtField = "//div[@title='%s']//ancestor::div[@class='dataGrid__row']//input";
    public String radioFieldCheckbox = "//div[@data-dataitemid='%s']//div[@role='checkbox']";
    public String sectionSelect = "//div[text()='%s']//ancestor::body//select[@data-dataitemid='%s']";
    public String sectionTextarea = "//div[text()='%s']//ancestor::body//textarea[@data-dataitemid='%s']";
    public String sectionInput = "//div[text()='%s']//ancestor::body//input[@data-dataitemid='%s']";
    public String sectionRadio = "//div[text()='%s']//ancestor::body//div[@data-dataitemid='%s']";
    public String labelInput = "//input[@data-dataitemid='%s']/ancestor::div[@class='row baselineAlign']//p";
    public String labelSelect = "//select[@data-dataitemid='%s']/ancestor::div[@class='row baselineAlign']//label";
    public String labelTextarea = "//textarea[@data-dataitemid='%s']/ancestor::div[@class='row baselineAlign']//label";
    public String radioFieldWithOption = "//div[@data-dataitemid='%s']//div[@role='checkbox' and @title='%s']";
    public String radioErrorField = "//div[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String inputErrorField = "//input[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String selectErrorField = "//select[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String textareaErrorField = "//textarea[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String fieldWithTitleAttribute = "//*[@title=\"%s\"]|//*[@aria-label=\"%s\"]";
    public String mandatoryFormElement = "//*[@class='navDrawer__bundleName' and text()=\"%s\"]//..//a";

    @FindBy(xpath = "//*[@id='ToggleMessagesLink']")
    private WebElement redColorErrorValidationBubble;

    @FindBy(xpath = "//span[@class='validationText']")
    private List<WebElement> listErrors;

    @FindBy(id = "root__wizardName")
    private WebElement formHeader;

    @FindBy(xpath = "//*[@class='ITWizardPageName']")
    private WebElement pageHeader;

    @FindBy(id = "imgOpenWiz")
    private WebElement WizardPageNameExpand;

    @FindBy(id = "imgCloseWiz")
    private WebElement WizardPageNameCollapse;

    @FindBy(id = "imgExpand")
    private List<WebElement> List_WizardPageNameExpand;

    @FindBy(id = "imgCloseWiz")
    private List<WebElement> List_WizardPageNameCollapse;

    @FindBy(xpath = "//div[@class='react-datepicker']")
    private List<WebElement> grid_DatePicker;

    public CommonMethodsPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }


    /**
     * move to a given page
     *
     * @param pageHeader
     * @param formHeader
     */
    public void moveToPage(WebDriver driver, String pageHeader, String formHeader) {
        waitForPageToLoad(driver);
        boolean flag = false;
        if (!(getPageHeader().getText().equalsIgnoreCase(pageHeader) & getFormHeader().getText().equalsIgnoreCase(formHeader))) {
            clickElementByJSE(driver, getWizardPageNameExpand());
            List<WebElement> mandetoryFormList = findElements(driver, String.format(getMandatoryFormElement(), formHeader));
            for (WebElement element : mandetoryFormList) {
                String form = element.getAttribute("innerText");
                if (form.equals(pageHeader)) {
                    clickElement(driver, getFormHeader());
                    clickElement(driver, element);
                    flag = true;
                    break;
                }
            }
            if (!flag) clickElementByJSE(driver, getWizardPageNameCollapse());
        }
        waitForPageToLoad(driver);
    }

    /**
     * Verify if a page exists
     *
     * @param pageHeader
     * @param formHeader
     * @return true if page exists else false
     */
    public boolean verifyPage(WebDriver driver, String pageHeader, String formHeader) {
        boolean flag = false;
        waitForPageToLoad(driver);
        if (!getList_WizardPageNameExpand().isEmpty())
            clickElementByJSE(driver, getWizardPageNameExpand());
        List<WebElement> mandetoryFormList = findElements(driver, String.format(getMandatoryFormElement(), formHeader));
        for (WebElement element : mandetoryFormList) {
            String form = element.getAttribute("innerText");
            if (form.equals(pageHeader)) {
                flag = true;
                break;
            }
        }
        if (!getList_WizardPageNameCollapse().isEmpty())
            clickElementByJSE(driver, getWizardPageNameCollapse());
        return flag;
    }

    public void setE2EValue(WebDriver driver, String formName, String wizardName, String valueJson, String dataItemID, String testData, int count, String titleName) {
        moveToPage(driver, formName, wizardName);
        String controlType = JsonPath.read(valueJson, "$.ControlType").toString().trim().toLowerCase();
        switch (controlType) {
            case "dropdown":
                new Select(findElement(driver, String.format(getSelectField(), dataItemID))).selectByVisibleText(testData);
                findElement(driver, String.format(getSelectField(), dataItemID)).sendKeys(Keys.TAB);
                listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                break;
            case "radio":
                clickElement(driver, findElement(driver, String.format(getRadioField(), dataItemID, testData)));
                listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                break;
            case "textbox":
                if (valueJson.contains("DataItemID")) {
                    sendKeys(driver, findElement(driver, String.format(getInputField(), dataItemID)), testData);
                    if (dataItemID.toLowerCase().contains("date")) {
                        new Actions(driver).moveToElement(getFormHeader()).click().perform();
                    }
                    listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                } else {
                    sendKeys(driver, findElement(driver, String.format(getTxtField(), titleName)), testData);
                    listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, titleName, controlType, testData));
                }
                break;
            case "checkbox":
                checkBoxSelectYesNO(testData, findElement(driver, String.format(getChkBoxField(), dataItemID)));
                listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                break;
            case "phone":
                sendKeys(driver, findElement(driver, String.format(getInputField(), dataItemID)), testData);
                listInputFields.add(Arrays.asList(String.valueOf(count++), formName, wizardName, dataItemID, controlType, testData));
                break;
        }
    }

}

