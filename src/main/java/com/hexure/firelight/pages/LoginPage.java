package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@Data
public class LoginPage extends FLUtilities {
    @FindBy(xpath = "//input[@id='userName' or contains(@name,'userName')]")
    private WebElement txtbox_userName;

    @FindBy(xpath = "//input[@id='password' or contains(@name,'password')]")
    private WebElement txtbox_Password;

    @FindBy(xpath = "//button[@id='login']")
    private WebElement btn_SignIn;

    public LoginPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

}
