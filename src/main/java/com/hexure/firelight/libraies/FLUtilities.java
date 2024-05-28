package com.hexure.firelight.libraies;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;

public class FLUtilities extends BaseClass {
    private static final Logger Log = LogManager.getLogger(FLUtilities.class);

    protected void syncElement(WebDriver driver, WebElement element, String conditionForWait) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            switch (conditionForWait) {
                case "ToVisible":
                    wait.until((WebDriver d) -> ExpectedConditions.visibilityOf(element).apply(d));
                    break;
                case "ToClickable":
                    wait.until((WebDriver d) -> ExpectedConditions.elementToBeClickable(element).apply(d));
                    break;
                case "ToInvisible":
                    wait.until((WebDriver d) -> ExpectedConditions.invisibilityOf(element).apply(d));
                    break;
                default:
                    throw new FLException("Invalid Condition " + conditionForWait);
            }
        } catch (StaleElementReferenceException | NoSuchElementException e) {
            System.out.println("No Such Element Exception is showing on searching element " + element);
        } catch (Exception e) {
            Log.error("Could Not Sync WebElement ", e);
            throw new FLException("Could Not Sync WebElement " + e.getMessage());
        }
    }

    protected void clickElement(WebDriver driver, WebElement element) {
        int retryCount = 4;
        for (int attempt = 0; attempt < retryCount; attempt++) {
            syncElement(driver, element, EnumsCommon.TOCLICKABLE.getText());
            try {
                if (!element.isDisplayed()) {
                    scrollToWebElement(driver, element);
                }
                element.click();
                return; // Exit the method if click is successful
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                // Retry after a brief delay
                try {
                    Thread.sleep(500); // Add a delay of 500 milliseconds between retries
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                Log.error("Could not click WebElement ", e);
                throw new FLException("Could not click WebElement: " + e.getMessage());
            }
        }
        // If all retry attempts fail, throw an exception
        throw new FLException("Failed to click WebElement after " + retryCount + " attempts");
    }

    protected void scrollToWebElement(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        } catch (Exception e) {
            Log.error("Could Not Scroll WebElement ", e);
            throw new FLException("Could Not Scroll WebElement " + e.getMessage());
        }
    }

    protected void clickElementByJSE(WebDriver driver, WebElement element) {
        syncElement(driver, element, EnumsCommon.TOCLICKABLE.getText());
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        } catch (Exception e) {
            Log.error("Clicking WebElement By JavaScriptExecutor Failed ", e);
            throw new FLException("Clicking WebElement By JavaScriptExecutor Failed " + e.getMessage());
        }
    }

    protected void sendKeys(WebDriver driver, WebElement element, String stringToInput) {
        syncElement(driver, element, EnumsCommon.TOVISIBLE.getText());
        try {
            element.clear();
            clickElement(driver, element);
            element.sendKeys(stringToInput);
            element.sendKeys(Keys.TAB);
        } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
            syncElement(driver, element, EnumsCommon.TOCLICKABLE.getText());
            // Scroll the element into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            element.clear();
            // Move to the element using Actions class
            new Actions(driver).moveToElement(element).sendKeys(stringToInput).perform();
            element.sendKeys(Keys.TAB);
        } catch (Exception e) {
            Log.error("SendKeys Failed ", e);
            throw new FLException(stringToInput + " could not be entered in element" + e.getMessage());
        }
        waitForPageToLoad(driver);
        sleepInMilliSeconds(1000);
    }

    protected void sleepInMilliSeconds(int milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            Log.error("Explicit Sleep Failed ", e);
            throw new FLException("Explicit Sleep Failed " + e.getMessage());
        }
    }

    protected void waitUntilDropDownListPopulated(WebDriver driver, Select dropdown) {
        try {
            FluentWait<WebDriver> wait = new FluentWait<>(driver)
                    .pollingEvery(Duration.ofMillis(250))
                    .withTimeout(Duration.ofSeconds(15))
                    .ignoring(NoSuchElementException.class);

            wait.until(driver1 -> dropdown.getOptions().size() > 3);
        } catch (StaleElementReferenceException se) {
            throw new FLException("Element is not available in DOM " + se.getMessage());
        } catch (Exception e) {
            throw new FLException("Error in populating dropdown " + e.getMessage());
        }
    }

    protected void clickElement(WebDriver driver, String stringXpath) {
        WebElement element = driver.findElement(By.xpath(stringXpath));
        syncElement(driver, element, EnumsCommon.TOCLICKABLE.getText());
        try {
            if (!element.isDisplayed()) {
                scrollToWebElement(driver, element);
                element.click();
            } else {
                element.click();
            }
        } catch (Exception e) {
            Log.error("Could Not Click WebElement ", e);
            throw new FLException("Could Not Click WebElement " + e.getMessage());
        }
    }

    public WebElement findElement(WebDriver driver, String stringXpath) {
        try {
            syncElement(driver, driver.findElement(By.xpath(stringXpath)), EnumsCommon.TOVISIBLE.getText());
            return driver.findElement(By.xpath(stringXpath));
        } catch (NoSuchElementException e) {
            System.out.println("No Such Element Exception is showing on searching element " + stringXpath);
            return null;
        }
    }

    public List<WebElement> findElements(WebDriver driver, String stringXpath) {
        return driver.findElements(By.xpath(stringXpath));
    }

    protected void checkBoxSelectYesNO(String userAction, WebElement element) {
        if (getCheckBoxAction(userAction)) {
            if (element.getAttribute("aria-checked").equals("false"))
                element.click();
        } else {
            if (element.getAttribute("aria-checked").equals("true"))
                element.click();
        }
    }

    protected boolean verifyCheckBoxSelectYesNO(String userAction, WebElement element) {
        boolean flag = true;
        checkBoxSelectYesNO(userAction, element);
        if (getCheckBoxAction(userAction)) {
            if (element.getAttribute("aria-checked").equals("false"))
                flag = false;
        } else {
            if (element.getAttribute("aria-checked").equals("true"))
                flag = false;
        }
        return flag;
    }

    private boolean getCheckBoxAction(String action) {
        return action.equalsIgnoreCase("yes") || action.equalsIgnoreCase("check") || action.equalsIgnoreCase("checked") || action.equalsIgnoreCase("selected");
    }


    /**
     * index of a given column in Excel
     *
     * @param headerRow  - Header row of an Excel sheet
     * @param columnName - Column name of header row
     * @return int, index of that Column
     */
    public int findColumnIndex(Row headerRow, String columnName) {
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (columnName.equalsIgnoreCase(getCellColumnValue(cell))) {
                return cell.getColumnIndex();
            }
        }
        return -1; // Column not found
    }

    public String getCellColumnValue(Cell cell) {
        return cell == null ? "" : cell.toString().trim();
    }
}