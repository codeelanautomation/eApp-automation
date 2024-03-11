package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@Data
public class CreateApplicationPage extends FLUtilities {
    public static String mandetoryFormList = "//div[@class='navDrawer__bundleName' and text()='%s']/..//span[@class='far fa-exclamation-triangle navDrawer__pageGraphic validation-summary-errors']/../..";
    public By WizardPageNameExpandmvc = By.xpath("//*[@class='ITWizardPageName']");
    public String optionalFormList = "//*[@class=\"navDrawer__bundleName\" and text()=\"%s\"]/..//*[@class=\"far fa-file navDrawer__pageGraphic \" or @class=\"fas fa-file-alt navDrawer__pageGraphic \"]/../..";
    public String list_OfProducts = "//div[@id='divProducts']/a//div[contains(text(),'%s')]";

    @FindBy(id = "buildNew")
    private WebElement btn_Create;

    @FindBy(id = "txtAppName")
    private WebElement txtBox_newAppName;

    @FindBy(xpath = "//*[@class='ITWizardPageName']")
    private WebElement WizardPageNameExpand;

    @FindBy(xpath = "//button[@id='cmdSave' or @id='cmdCreate']")
    private WebElement btn_CreateActivity;

    @FindBy(id = "Jurisdiction")
    private WebElement dd_Jurisdiction;

    @FindBy(id = "ProductType")
    private WebElement dd_ProductType;

    public CreateApplicationPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }
}
