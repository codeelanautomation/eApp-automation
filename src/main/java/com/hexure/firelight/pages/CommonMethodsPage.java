package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openqa.selenium.By;
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

@EqualsAndHashCode(callSuper = true)
@Data
public class CommonMethodsPage extends FLUtilities {
    public String selectField = "//select[@data-dataitemid='%s']";
    public String textareaField = "//textarea[@data-dataitemid='%s']";
    public String labelField = "//div[@data-dataitemid='%s']";
    public String inputField = "//input[@data-dataitemid='%s']";
    public String radioField = "//div[@data-dataitemid='%s']//div[@title='%s']";
    public String chkBoxField = "//div[@data-dataitemid='%s']//div[@role='checkbox']";
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
    private By errorMessageElement = By.xpath("//span[@class='validationText']");
    private By formPages = By.className("fa-exclamation-triangle");
    private By inputElement = By.xpath("(//span[@class='validationText'])[1]//ancestor::div[@class='ITComponent']//input");
    private By selectElement = By.xpath("(//span[@class='validationText'])[1]//ancestor::div[@class='ITComponent']//select");
    private By checkboxElement = By.xpath("(//span[@class='validationText'])[1]//ancestor::div[@class='ITComponent']//div[@class='ITCheckBoxCheck']");
    private By componentClass = By.xpath("(//span[@class='validationText'])[1]//ancestor::div[contains(@class,'ITCheckBoxWrapper')]");
    private By btnLogOff = By.xpath("//a[text()='Log Off']");

    int count = 1;
    List<List<String>> listInputFields = new ArrayList<>();

    @FindBy(xpath = "//button[@id='popup_ok']")
    private WebElement popup_Yes;

    @FindBy(id = "ToggleMessagesLink")
    private WebElement redColorErrorValidationBubble;

    @FindBy(id = "ToggleMessagesLink")
    private List<WebElement> lstRedColorErrorValidationBubble;

    @FindBy(xpath = "//span[@class='validationText']")
    private List<WebElement> listErrors;

    @FindBy(id = "root__wizardName")
    private WebElement formHeader;

    @FindBy(className = "ITWizardPageName")
    private WebElement pageHeader;

    @FindBy(id = "imgOpenWiz")
    private WebElement wizardPageNameExpand;

    @FindBy(id = "imgCloseWiz")
    private WebElement wizardPageNameCollapse;

    @FindBy(id = "imgExpand")
    private List<WebElement> listWizardPageNameExpand;

    @FindBy(id = "imgCloseWiz")
    private List<WebElement> listWizardPageNameCollapse;

    @FindBy(xpath = "//div[@class='react-datepicker']")
    private List<WebElement> gridDatePicker;

    @FindBy(xpath = "//a[@title='Close']")
    private WebElement btnClose;

    @FindBy(xpath = "//a[@title='Close']")
    private List<WebElement> lstBtnClose;

    @FindBy(xpath = "//div[@id='lblPercent']")
    private WebElement lblPercent;

    @FindBy(id = "imgNext")
    private WebElement btnContinue;

    @FindBy(xpath = "//div[@class='listRecent__name']")
    private WebElement listRecentApplication;

    public CommonMethodsPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    /**
     * move to a given page
     *
     * @param pageHeader - Page name
     * @param formHeader - Form name
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
     * @param pageHeader - Page name
     * @param formHeader - Form name
     * @return true if page exists else false
     */
    public boolean verifyPage(WebDriver driver, String pageHeader, String formHeader) {
        boolean flag = false;
        waitForPageToLoad(driver);
        if (!getListWizardPageNameExpand().isEmpty())
            clickElementByJSE(driver, getWizardPageNameExpand());
        List<WebElement> mandetoryFormList = findElements(driver, String.format(getMandatoryFormElement(), formHeader));
        for (WebElement element : mandetoryFormList) {
            String form = element.getAttribute("innerText");
            if (form.equals(pageHeader)) {
                flag = true;
                break;
            }
        }
        if (!getListWizardPageNameCollapse().isEmpty())
            clickElementByJSE(driver, getWizardPageNameCollapse());
        return flag;
    }

    /**
     * Set the value for E2E flow
     * @param driver
     * @param formName
     * @param wizardName
     * @param valueJson
     * @param dataItemID
     * @param testData
     * @param titleName
     */
    public void setE2EValue(WebDriver driver, String formName, String wizardName, String valueJson, String dataItemID, String testData, String titleName) {
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

