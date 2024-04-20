package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.time.LocalTime;

@Data
public class LoginPage extends FLUtilities {
    @FindBy(id = "ctl00_content_txtUserName")
    private WebElement txtbox_userName;

    @FindBy(id = "ctl00_content_txtPassword")
    private WebElement txtbox_Password;

    @FindBy(id = "ctl00_content_cmdLogin")
    private WebElement btn_SignIn;

    public long startTime;
    public LocalTime startLocalTime ;
    public LoginPage(WebDriver driver) {
        initElements(driver);
        startTime = System.currentTimeMillis();
        startLocalTime = LocalTime.now();
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

}
