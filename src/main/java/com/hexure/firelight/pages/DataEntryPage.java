package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.FLUtilities;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@EqualsAndHashCode(callSuper = true)
@Data
public class DataEntryPage extends FLUtilities {

    @FindBy(id = "root__wizardName")
    private WebElement dataEntryPageHeader;

    @FindBy(className = "ITWizardPageName")
    private WebElement formName;

    public DataEntryPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }
}