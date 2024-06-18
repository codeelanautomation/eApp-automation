package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateApplicationPage extends FLUtilities {
    public String mandetoryFormList = "//div[@class='navDrawer__bundleName' and text()='%s']/..//span[@class='far fa-exclamation-triangle navDrawer__pageGraphic validation-summary-errors']/../..";
    public String WizardPageNameExpandmvc = "//*[@class='ITWizardPageName']";
    public String optionalFormList = "//*[@class=\"navDrawer__bundleName\" and text()=\"%s\"]/..//*[@class=\"far fa-file navDrawer__pageGraphic \" or @class=\"fas fa-file-alt navDrawer__pageGraphic \"]/../..";
    public String listOfProducts = "//div[@id='divProducts']/a//div[contains(text(),'%s')]";
    private By btnRequestReview = By.id("requestReview");
    private String canvasCurrent = "//canvas[contains(@class,'status_current')]";
    private By signNow = By.id("signNow");
    private By identificationNumber = By.id("txtID");
    private By btnSubmit = By.id("buttonSubmit");
    private By chkAgreeAll = By.id("chkAgreeAll");
    private By chkAgree = By.id("chkAgree");
    private By popOK = By.id("popup_ok");
    private String tblDocuments = "//table[@id='tableNav']//tr";
    private String btnConsent = "//button[@id='buttonConsent' or @id='btnConsent']";
    private By btnDecline = By.id("aDecline");
    private String btnOK = "//img[@alt='OK']";

    @FindBy(xpath = "//td[@class='top']//span[text()='Application']")
    private WebElement btnApplication;

    @FindBy(id = "buildNew")
    private WebElement btnCreate;

    @FindBy(id = "txtAppName")
    private WebElement txtboxNewappname;

    @FindBy(xpath = "//a[@title='Close']")
    private WebElement btnClose;

    @FindBy(xpath = "//a[@title='Close']")
    private List<WebElement> lstBtnClose;

    @FindBy(xpath = "//*[@class='ITWizardPageName']")
    private WebElement wizardPageNameExpand;

    @FindBy(xpath = "//button[@id='cmdSave' or @id='cmdCreate']")
    private WebElement btnCreateActivity;

    @FindBy(id = "Jurisdiction")
    private WebElement ddJurisdiction;

    @FindBy(id = "toolbar__home")
    private WebElement btnHome;

    @FindBy(id = "popup_ok")
    private WebElement btnPopupOK;

    @FindBy(id = "ProductType")
    private WebElement ddProductType;

    @FindBy(xpath = "//button[@class='ITButtonInput  ']//span[text()='Next']")
    private WebElement btnNext;

    @FindBy(xpath = "//div[@id='divOutstandingRequests']//div[@class='largeText']")
    private List<WebElement> lstConfirmationDialog;

    @FindBy(xpath = "//a[@aria-label='[Close]']")
    private WebElement btnCloseApp;

    @FindBy(id = "lnkUse")
    private WebElement lnkUseSignature;

    @FindBy(xpath = "//div[@id='signerListDiv']//a")
    private List<WebElement> lnkUserSignatures;


    public CreateApplicationPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void createApplication(TestContext testContext, WebDriver driver, String product, String productType, String jurisdiction) {
        clickElement(driver, getBtnApplication());
        new Select(getDdJurisdiction()).selectByVisibleText(jurisdiction);
        waitForPageToLoad(driver);
        sleepInMilliSeconds(2000);
        new Select(getDdProductType()).selectByVisibleText(productType);
        sleepInMilliSeconds(2000);
        clickElement(driver, findElement(driver, String.format(listOfProducts, product)));
        waitForPageToLoad(driver);
        clickElement(driver, getBtnCreate());
        String newAppName = "AT " + testContext.getMapTestData().get("product") + " " + getDate("newAppName");
        getTxtboxNewappname().clear();
        getTxtboxNewappname().sendKeys(newAppName);
        getBtnCreateActivity().click();
        waitForPageToLoad(driver);
    }
}
