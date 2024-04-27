package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

@Data
public class CommonMethodsPage extends FLUtilities {
    public String selectField = "//select[@data-dataitemid='%s']";
    public String labelField = "//div[@data-dataitemid='%s']";
    public String inputField = "//input[@data-dataitemid='%s']";
    public String radioField = "//div[@data-dataitemid='%s']//div[@title='%s']";
    public String chkBoxField = "//div[@data-dataitemid='%s']//div[@role]";
    public String txtField = "//div[@title='%s']//ancestor::div[@class='dataGrid__row']//input";
    public String radioFieldCheckbox = "//div[@data-dataitemid='%s']//div[@role='checkbox']";
    public String sectionSelect = "//div[text()='%s']//ancestor::body//select[@data-dataitemid='%s']";
    public String sectionInput = "//div[text()='%s']//ancestor::body//input[@data-dataitemid='%s']";
    public String sectionRadio = "//div[text()='%s']//ancestor::body//div[@data-dataitemid='%s']";
    public String radioFieldWithOption = "//div[@data-dataitemid='%s']//div[@role='checkbox' and @title='%s']";
    public String radioErrorField = "//div[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String inputErrorField = "//input[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String selectErrorField = "//select[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String fieldWithTitleAttribute = "//*[@title=\"%s\"]|//*[@aria-label=\"%s\"]";
    public String mandatoryFormElement = "//*[@class=\"navDrawer__bundleName\" and text()=\"%s\"]/..//*[@class=\"far fa-exclamation-triangle navDrawer__pageGraphic validation-summary-errors\"]/../..";

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


    public CommonMethodsPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }
}

