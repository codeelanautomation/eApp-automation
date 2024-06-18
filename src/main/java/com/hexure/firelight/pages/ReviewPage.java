package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

@Data
public class ReviewPage extends FLUtilities
{
    private By link_requestReviewReact = By.id("idSendReminder");
    private By toolBar_Home = By.id("toolbar__home");

    @FindBy(id="aDecline")
    private WebElement btn_DeclineReview;

    @FindBy(xpath = "//button[contains(text(),'More Info')]/preceding-sibling::button[contains(text(),'Approve')]")
    private WebElement btn_Approve;

    @FindBy(id = "Passcode")
    private WebElement txtBox_Passcode;

    @FindBy(xpath="//input[contains(@id,'reviewbtn')]")
    private WebElement btn_Review;

    @FindBy(id = "cmdLoginPasscode")
    private WebElement btn_enterPasscode;

    @FindBy(id="SSN")
    private WebElement txtBox_MailerSSN;

    @FindBy(xpath="//button[@aria-label='Send Email Request']")
    private WebElement btnSendEmailRequest;

    @FindBy(xpath = "//strong[.='Back To App']")
    private WebElement btn_backToapp;

    @FindBy(xpath = "//strong/parent::span/span")
    private WebElement passcode_recipient;

    @FindBy(id="BirthDate")
    private WebElement txtBox_SignerDOB;

    @FindBy(id="aSendRequestToReviewer")
    private WebElement btn_SendRequestToReviewer;

    @FindBy(xpath="(//button[@type='button'])[1]")
    private WebElement btn_OK;

    @FindBy(xpath="//div[@class='panelContent']/span/h3")
    private WebElement msg_ElectronicReviewDecline;

    @FindBy(xpath="//span[contains(@class,'minus-circle')]")
    private WebElement image_ReviewMinusCircle;

    @FindBy(xpath="//span[text()='REVIEW']")
    private WebElement tab_REVIEW;

    @FindBy(xpath="//span[text()='REVIEW']")
    private List<WebElement> list_TabREVIEW;

    @FindBy(xpath="//div[@id='InitialMessage']/following-sibling::a")
    private WebElement btn_InitialMessagePopupClose;

    @FindBy(xpath = "//div[@id='divTopBar']//div[contains(text(),'Easy Signing Form')]")
    private  WebElement txt_EasySigningForm;

    @FindBy(xpath = "//button[@id='imgNext']")
    private WebElement btn_Continue;

    private By link_SubmitMVC = By.xpath("//a[@id='submitModal']");
    private By link_SubmitReact = By.xpath("//ul/li/a[.='Submit']");

    @FindBy(xpath = "//div[@id='popup_message']//b[text()='Application will be submitted.']")
    private WebElement msg_Submit_Confirmation1;

    @FindBy(xpath = "//div[@id='popup_message']//b/p[text()=' No further edits will be allowed.']")
    private WebElement msg_Submit_Confirmation2;

    @FindBy(xpath = "//div[@id='popup_message']//p/b[text()=' Are you sure?']")
    private WebElement msg_Submit_Confirmation3;

    private By btn_Ok_ConfPopupMVC = By.id("popup_ok");
    private By btn_Ok_ConfPopupReact = By.xpath("//span[contains(text(),'Yes')]");

    @FindBy(id="popup_cancel")
    private WebElement btn_Cancel_ConfPopup;

    private By canvas_ReviewMVC = By.id("REVIEW");
    private By canvas_ReviewReact = By.xpath("//span[text()='REVIEW']");

    @FindBy(id="toolbar_OtherActions")
    private WebElement otherAction_tab;

    @FindBy(xpath = "//div[@id='divOtherActions']/a")
    private List<WebElement> otherActionOptions;

    @FindBy(xpath = "//div[@class='auditDisplayStatus']")
    private List<WebElement> auditStatuses;

    @FindBy(xpath = "//div[@class='auditDisplayStatus']/following-sibling::strong")
    private List<WebElement> auditStatusmsgs;

    @FindBy(xpath = "//span[@id='ui-id-1']/following-sibling::button")
    private WebElement btn_Historyclose;

    @FindBy(id="DATA ENTRY")
    private WebElement tab_DATAENTRY;

    @FindBy(xpath="//ul/li/a[.='Application']")
    private WebElement link_Application;

    @FindBy(id="SIGNATURES")
    private WebElement tab_SIGNATURES;

    @FindBy(xpath="//img[@alt='SIGNATURES']")
    private WebElement image_SignatureTick;

    @FindBy(xpath="//img[@alt='DATA ENTRY']")
    private WebElement image_DataEntryTick;

    @FindBy(xpath="//img[@alt='REVIEW']")
    private WebElement image_ReviewCircle;

    @FindBy(xpath = "//a[contains(text(),'Add Reviewer')]")
    private WebElement link_AddReviewer;

    @FindBy(xpath = "//a[contains(text(),'Reset')]")
    private WebElement link_Reset;

    @FindBy(xpath = "//input[contains(@id,'ToName')]")
    private WebElement txtBox_ReviewerName;

    @FindBy(xpath = "//input[contains(@id,'ToEmail')]")
    private WebElement txtBox_ReviewerEmail;

    @FindBy(xpath = "//input[contains(@id,'Checked')]")
    private WebElement CheckBox_Reviewer;

    @FindBy(id = "buttonSend")
    private WebElement btn_sendEmailRequest;

    private By link_SendReminderMailMVC = By.xpath("//div[contains(@id,'divOutStandingRequestActions')]/a[contains(text(),'Reminder')]");
    private By link_SendReminderMailReact = By.xpath("//div[contains(@id,'divOutStandingRequestActions')]//div[contains(text(),'Reminder')]");

    @FindBy(id = "Message")
    private WebElement emailMsg;

    //********************************************************

    @FindBy(xpath="//*[@class='reviewerEmail__passCode']//span")
    private WebElement sendEmailPasscode;

    @FindBy(xpath="//button[@aria-label=\"Send Email Request\"]")
    private WebElement buttonSendEmailRequest;

    @FindBy(id = "buttonCancel")
    private WebElement btn_Cancel;

    @FindBy(xpath="//*[@class='outstandingRequestsDialog__requestDetails linkFocus']/*[@class=\"linkFocus\"]")
    private List<WebElement> pendingRequestDetails;

    @FindBy(xpath = "//tbody/tr/td/span")
    private WebElement pendingRequestDetails_mvc;

    @FindBy(xpath="//*[@class=' ITNavLink linkFocus']")
    private List<WebElement> pendingRequestLinks;

    @FindBy(xpath = "//a[contains(@class,'blueLink')]")
    private List<WebElement> pendingRequestLinks_mvc;

    @FindBy(xpath = "//div[contains(@id,'divOutStandingRequestActions')]/a")
    private List<WebElement> list_PendingRequestLinks_mvc;

    @FindBy(xpath = "//div[contains(@id,'divOutStandingRequestActions')]//div[contains(@class,'null ITLink')]//div[@class='ITNavLinkText']")
    private List<WebElement> list_PendingRequestLinks_React;

    @FindBy(xpath = "//a[@class=' ITNavLink linkFocus']/div")
    private WebElement link_sendReminder;

    @FindBy(xpath="//*[@class='roundCornersTop heading']")
    private WebElement pageHeadingEmailRequestsSent;

    @FindBy(xpath="//*[@class='panelContent']//h3")
    private WebElement panelContent;

    @FindBy(xpath="//*[@class='roundCorners heading']")
    private WebElement passCodeText;

    @FindBy(id="buttonBack")
    private WebElement buttonBack;

    @FindBy(id="Message")
    private WebElement Message;

    @FindBy(xpath = "//span[contains(.,'Passcode')]/span | //div[contains(text(),'Passcode')]/span")
    private WebElement passcode_Reviewer;

    @FindBy(id = "popup_message")
    private WebElement alert_MessageSent;

    @FindBy(xpath = "//div[@class='toastTitle']/following-sibling::div")
    private WebElement alert_MessageSentReact;

    @FindBy(xpath = "//div[@class='roundCornersTop heading']")
    private WebElement heading_Reviewer;

    @FindBy(xpath = "//div[@class='reviewerEmail']/div[@tabindex]")
    private WebElement messageText_SendEmailtoReviewerPage;

    @FindBy(xpath = "//input[contains(@id,'FromName')]")
    private WebElement txtBox_Yourname;

    @FindBy(xpath = "//input[contains(@id,'FromEmail')]")
    private WebElement txtBox_YourEmail;

    @FindBy(id = "txtSubject")
    private WebElement txtBox_Subject;

    @FindBy(id = "txtFromName-error")
    private WebElement txtMsg_ErrorYourname;

    @FindBy(id = "txtFromEmail-error")
    private WebElement txtMsg_ErrorYourEmail;

    @FindBy(xpath = "//input[contains(@aria-label,'Reviewer Name')]/..//span")
    private WebElement txtMsg_ErrorReviewerName;

    @FindBy(xpath = "//input[contains(@aria-label,'Reviewer Email')]/..//span")
    private WebElement txtMsg_ErrorReviewerEmail;

    private By link_closeMVC = By.xpath("//div[@id='lnkClose']/a");
    private By link_closeReact = By.xpath("//div[@class='ITDialog__box ']//span[@class='ITButtonText ']/span[text()='Close']");

    @FindBy(xpath = "//button[@aria-label='Cancel']")
    private WebElement btn_cancelMessage;

   @FindBy(xpath = "//span[@id='divOutstandingRequestBody']/div/h2")
   private WebElement heading_ReviewQueuePopup;

   @FindBy(xpath = "//span[@id='divOutstandingRequestBody']//div/strong[contains(text(),'User Name')]/following-sibling::p")
   private WebElement txtMsg_ReviewQueuePopup;

   @FindBy(xpath = "//div[@id='divReply']//textarea")
   private WebElement txtBox_Reply;

   @FindBy(xpath = "//a[text()='Reply']")
   private WebElement link_Reply;

   @FindBy(id = "cmdSubmit")
   private WebElement btn_Send;

   @FindBy(xpath = "//a[contains(text(),'Send Reminder to Secondary')]")
   private WebElement btn_SecondaryRemainder;

    @FindBy(xpath = "//div[contains(@id,'divOutStandingRequestActions')]//div[contains(text(),'Reminder')]")
    private List <WebElement> list_LinkSendReminder;

    private String tabNumber = "//span[text()='%s']/..//div/span";
    private String tabNumber2 = "//canvas[contains(@id,'%s')]/parent::div";

    public ReviewPage(WebDriver driver)
    {
        initElements(driver);
    }

    private void initElements(WebDriver driver)
    {
		PageFactory.initElements(driver, this);
    }
}
