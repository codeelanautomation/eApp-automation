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
    public String inputField = "//input[@data-dataitemid='%s']";
    public String radioField = "//div[@data-dataitemid='%s']//div[@title='%s']";
    public String inputErrorField = "//input[@data-dataitemid='%s']//ancestor::div[@class='ITComponent']//span[@class='validationText']";
    public String fieldWithTitleAttribute = "//*[@title=\"%s\"]|//*[@aria-label=\"%s\"]";
    public String mandetoryFormElement="//*[@class=\"navDrawer__bundleName\" and text()=\"%s\"]/..//*[@class=\"far fa-exclamation-triangle navDrawer__pageGraphic validation-summary-errors\"]/../..";

    @FindBy(xpath = "//*[@id='ToggleMessagesLink']")
    private WebElement redColorErrorValidationBubble;

    @FindBy(xpath = "//span[@class='validationText']")
    private List<WebElement> listErrors;

    @FindBy(id = "root__wizardName")
    private WebElement PageHeader;

    @FindBy(xpath="//*[@class='ITWizardPageName']")
    private WebElement formHeader;

    @FindBy(id = "imgExpand")
    private WebElement WizardPageNameExpand;


    public CommonMethodsPage(WebDriver driver) {
        initElements(driver);
    }
    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }
}

