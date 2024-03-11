package com.hexure.firelight.libraies;

import com.hexure.firelight.pages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

public class PageObjectManager extends BaseClass {
    private final WebDriver driver;
    private LoginPage onLoginPage;
    private HomePage onHomePage;
    private CreateApplicationPage onCreateApplicationPage;
    private DataEntryPage onDataEntryPage;
    private CommonMethodsPage onCommonMethodsPage;
    private SoftAssertionHandlerPage onSoftAssertionHandlerPage;
    private static final Logger Log = LogManager.getLogger(PageObjectManager.class);

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

    public HomePage getHomePage() {
        try {
            return (onHomePage == null) ? onHomePage = new HomePage(driver) : onHomePage;
        } catch (Exception e) {
            Log.error("Instance creations of HomePage Failed ", e);
            throw new FLException("Instance creations of HomePage Failed " + e.getMessage());
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

    public SoftAssertionHandlerPage getSoftAssertionHandlerPage() {
        try {
            return (onSoftAssertionHandlerPage == null) ? onSoftAssertionHandlerPage = new SoftAssertionHandlerPage(driver) : onSoftAssertionHandlerPage;
        } catch (Exception e) {
            Log.error("Instance creations of SoftAssertionHandlerPage Failed ", e);
            throw new FLException("Instance creations of SoftAssertionHandlerPage Failed " + e.getMessage());
        }
    }
}
