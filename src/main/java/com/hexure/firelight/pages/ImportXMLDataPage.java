package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import javax.swing.text.MaskFormatter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImportXMLDataPage extends FLUtilities {

    public WebDriver driver;

    @FindBy(id = "cboCarrier")
    private WebElement cboCarrier;

    @FindBy(id = "cboHost")
    private WebElement cboHostName;

    @FindBy(id = "txtLoginUserName")
    private WebElement edtUsername;

    @FindBy(id = "txtPassword")
    private WebElement edtPassword;

    @FindBy(id = "txt1228ProducerName")
    private WebElement edtProducerName;

    @FindBy(id = "txt1228ProducerId")
    private WebElement edtProducerID;

    @FindBy(id = "txt103Xml")
    private WebElement edtXMLDocument;

    @FindBy(id = "btnProcess")
    private WebElement btnProcess;

    public ImportXMLDataPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void setCredentialsAndXmlData(WebDriver driver, TestContext testContext) {
            WebElement anchorLink = driver.findElement(By.linkText("QE Test Page for 1228 and 103 Transactions"));
            anchorLink.click();

            Select dropdown = new Select(cboCarrier);
            dropdown.selectByValue(testContext.getMapTestData().get("org"));

            Select cboHost = new Select(cboHostName);
            cboHost.selectByValue(testContext.getMapTestData().get("host"));

            sendKeys(driver, edtUsername, testContext.getMapTestData().get("username"));
            sendKeys(driver, edtPassword, testContext.getMapTestData().get("password"));
            sendKeys(driver, edtProducerName, testContext.getMapTestData().get("producerName"));
            sendKeys(driver, edtProducerID, testContext.getMapTestData().get("producerID"));
            String xmlFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + testContext.getMapTestData().get("inboundXmlFileName");
            String xmlData = readFileAsString(xmlFilePath);
            sendKeys(driver, edtXMLDocument, xmlData.trim());
            clickElement(driver, btnProcess);
    }

    private String readFileAsString(String filePath) {
        try {
            Path path = Paths.get(filePath);
            byte[] encoded = Files.readAllBytes(path);
            return new String(encoded, StandardCharsets.UTF_16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}