package com.hexure.firelight.libraies;

import cucumber.api.Scenario;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

@Data
public class TestContext
{
    private WebDriver driver = null;
    private PageObjectManager pageObjectManager = null;
    private Scenario scenario = null;
    private String testCaseID = null;
    private String moduleName = null;
    private String captureScreenshot = null;
    private String appType = null;
    private HashMap<String,String> mapTestData = null;
    private String environment = null;
    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultSet = null;
    private String currentTestUserName = null;
    private String uiType = null;
    private String browser = null;
    private String adminCacheTime = null;
    private String VM_Name = null;
    private String pdfName = null;
}
