package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@Data
public class LoginPage extends FLUtilities {
    @FindBy(id = "userName")
    private WebElement txtbox_userName;

    @FindBy(id = "password")
    private WebElement txtbox_Password;

    @FindBy(id = "login")
    private WebElement btn_SignIn;

    public LoginPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

}
