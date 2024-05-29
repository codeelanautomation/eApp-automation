package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.time.LocalTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginPage extends FLUtilities {
    public long startTime;
    public LocalTime startLocalTime;

    @FindBy(id = "ctl00_content_txtUserName")
    @CacheLookup
    private WebElement txtboxUserName;

    @FindBy(id = "ctl00_content_txtPassword")
    @CacheLookup
    private WebElement txtboxPassword;

    @FindBy(id = "ctl00_content_cmdLogin")
    @CacheLookup
    private WebElement btnSignIn;

    public LoginPage(WebDriver driver) {
        initElements(driver);
        startTime = System.currentTimeMillis();
        startLocalTime = LocalTime.now();
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

}
