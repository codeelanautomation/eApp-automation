package com.hexure.firelight.libraies;

import com.hexure.firelight.pages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

public class PageObjectManager extends BaseClass {
    private static final Logger Log = LogManager.getLogger(PageObjectManager.class);
    private final WebDriver driver;
    private LoginPage onLoginPage;
    private CreateApplicationPage onCreateApplicationPage;
    private DataEntryPage onDataEntryPage;
    private CommonMethodsPage onCommonMethodsPage;
    private ExcelHandlerPage onExcelHandlerPage;
    private E2EFlowDataPage onE2EFlowDataPage;
    private WizardFlowDataPage onWizardFlowDataPage;
    private SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    private XmlUtilityPage onXmlUtilityPage;
    private ImportXMLDataPage onImportXMLDataPage;
    private ReviewPage onReviewPage;
    private XmlDataPage onXmlDataPage;

    public PageObjectManager(WebDriver driver) {
        this.driver = driver;
    }

    public LoginPage getLoginPage() {
        try {
            return (onLoginPage == null) ? onLoginPage = new LoginPage(driver) : onLoginPage;
        } catch (Exception e) {
            Log.error("Instance creations of LoginPage Failed ", e);
            throw new FLException("Instance creations of LoginPage Failed " + e.getMessage());
        }
    }

    public CreateApplicationPage getCreateApplicationPage() {
        try {
            return (onCreateApplicationPage == null) ? onCreateApplicationPage = new CreateApplicationPage(driver) : onCreateApplicationPage;
        } catch (Exception e) {
            Log.error("Instance creations of CreateApplicationPage Failed ", e);
            throw new FLException("Instance creations of CreateApplicationPage Failed " + e.getMessage());
        }
    }

    public DataEntryPage getDataEntryPage() {
        try {
            return (onDataEntryPage == null) ? onDataEntryPage = new DataEntryPage(driver) : onDataEntryPage;
        } catch (Exception e) {
            Log.error("Instance creations of DataEntryPage Failed ", e);
            throw new FLException("Instance creations of DataEntryPage Failed " + e.getMessage());
        }
    }

    public CommonMethodsPage getCommonMethodPage() {
        try {
            return (onCommonMethodsPage == null) ? onCommonMethodsPage = new CommonMethodsPage(driver) : onCommonMethodsPage;
        } catch (Exception e) {
            Log.error("Instance creations of CommonMethodsPage Failed ", e);
            throw new FLException("Instance creations of CommonMethodsPage Failed " + e.getMessage());
        }
    }

    public ExcelHandlerPage getExcelHandlerPage() {
        try {
            return (onExcelHandlerPage == null) ? onExcelHandlerPage = new ExcelHandlerPage(driver) : onExcelHandlerPage;
        } catch (Exception e) {
            Log.error("Instance creations of ExcelHandlerPage Failed ", e);
            throw new FLException("Instance creations of ExcelHandlerPage Failed " + e.getMessage());
        }
    }

    public E2EFlowDataPage getE2EFlowDataPage() {
        try {
            return (onE2EFlowDataPage == null) ? onE2EFlowDataPage = new E2EFlowDataPage(driver) : onE2EFlowDataPage;
        } catch (Exception e) {
            Log.error("Instance creations of E2EFlowDataPage Failed ", e);
            throw new FLException("Instance creations of E2EFlowDataPage Failed " + e.getMessage());
        }
    }

    public WizardFlowDataPage getWizardFlowDataPage() {
        try {
            return (onWizardFlowDataPage == null) ? onWizardFlowDataPage = new WizardFlowDataPage(driver) : onWizardFlowDataPage;
        } catch (Exception e) {
            Log.error("Instance creations of WizardFlowDataPage Failed ", e);
            throw new FLException("Instance creations of WizardFlowDataPage Failed " + e.getMessage());
        }
    }

    public SoftAssertionHandlerPage getSoftAssertionHandlerPage() {
        try {
            return (onSoftAssertionHandlerPage == null) ? onSoftAssertionHandlerPage = new SoftAssertionHandlerPage(driver) : onSoftAssertionHandlerPage;
        } catch (Exception e) {
            Log.error("Instance creations of SoftAssertionHandlerPage Failed ", e);
            throw new FLException("Instance creations of SoftAssertionHandlerPage Failed " + e.getMessage());
        }
    }

    public XmlUtilityPage getXmlUtilityPage() {
        try {
            return (onXmlUtilityPage == null) ? onXmlUtilityPage = new XmlUtilityPage(driver) : onXmlUtilityPage;
        } catch (Exception e) {
            Log.error("Instance creations of XmlUtilityPage Failed ", e);
            throw new FLException("Instance creations of XmlUtilityPage Failed " + e.getMessage());
        }
    }

    public ImportXMLDataPage getImportXMLDataPage() {
        try {
            return (onImportXMLDataPage == null) ? onImportXMLDataPage = new ImportXMLDataPage(driver) : onImportXMLDataPage;
        } catch (Exception e) {
            Log.error("Instance creations of ImportXMLDataPage Failed ", e);
            throw new FLException("Instance creations of ImportXMLDataPage Failed " + e.getMessage());
        }
    }

    public ReviewPage getReviewPage() {
        try {
            return (onReviewPage == null) ? onReviewPage = new ReviewPage(driver) : onReviewPage;
        } catch (Exception e) {
            Log.error("Instance creations of ReviewPage Failed ", e);
            throw new FLException("Instance creations of ReviewPage Failed " + e.getMessage());
        }
    }

    public XmlDataPage getXmlDataPage() {
        try {
            return (onXmlDataPage == null) ? onXmlDataPage = new XmlDataPage(driver) : onXmlDataPage;
        } catch (Exception e) {
            Log.error("Instance creations of XmlDataPage Failed ", e);
            throw new FLException("Instance creations of XmlDataPage Failed " + e.getMessage());
        }
    }
}
