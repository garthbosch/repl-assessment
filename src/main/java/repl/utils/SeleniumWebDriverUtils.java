package repl.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static io.github.bonigarcia.wdm.config.DriverManagerType.CHROME;
import static org.awaitility.Awaitility.await;

public class SeleniumWebDriverUtils {
    protected static Logger log = Logging.getLogger(true);
    private WebDriver driver;
    private Proxy proxy = new Proxy();
    private Integer waitTimeOut = 30;
    private static final String XPATH = "xpath";
    private static final String ID = "id";
    private static final String CSS = "css";
    private static final String NAME = "name";
    private static final String CLASSNAME = "classname";
    private static final String LINK_TEXT = "linktext";
    private static final String PARTIAL_LINK_TEXT = "partiallinktext";
    private static final String NO_ELEMENT_ERROR_TEXT = "Unable to find element ";
    private static final String ELEMENT_INF_TEXT = "Element ";
    private String sessionUrl = null;
    private boolean isRemoteWebDriver;
    private SessionId sessionId;

    private void setURL(String baseUrl) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            sessionUrl = baseUrl;
            driver.manage().timeouts().implicitlyWait(waitTimeOut, TimeUnit.SECONDS);
            driver.manage().timeouts().pageLoadTimeout(120, TimeUnit.SECONDS);
            driver.manage().window().maximize();
            driver.manage().deleteAllCookies();
            driver.get(baseUrl);
        } else {
            throw new WebDriverException("====================NO URL SPECIFIED======================");
        }
    }

    /**
     * Starts the selenium session with a new Google Chrome session
     */
    public void startDriver(String baseUrl) {
        try {
            proxy.setNoProxy("takealot*");
            driver = new ChromeDriver(setChromeOptions());

            setURL(baseUrl);
            setSessionId(((RemoteWebDriver) driver).getSessionId());
            log.info("Done selecting Browser");
            log.info("Selenium driver started");
        } catch (Exception e) {
            log.error("Something went wrong while starting up selenium driver - " + e.getMessage());
        }
    }

    /**
     * set and get session Id
     */
    private void setSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public SessionId getSessionId() {
        return this.sessionId;
    }

    /**
     * Configures the variables for the Chromedriver
     */
    private ChromeOptions setChromeOptions() {
        WebDriverManager.getInstance(CHROME).setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setCapability(CapabilityType.OVERLAPPING_CHECK_DISABLED, false);
        chromeOptions.setCapability(CapabilityType.ELEMENT_SCROLL_BEHAVIOR, true);
        chromeOptions.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
        chromeOptions.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
        chromeOptions.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        chromeOptions.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        chromeOptions.setCapability(CapabilityType.SUPPORTS_APPLICATION_CACHE, false);
        chromeOptions.setCapability("idle-timeout", 300000);

        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("download.directory_upgrade", true);
        chromePrefs.put("safebrowsing.enabled", false);
        chromeOptions.setExperimentalOption("prefs", chromePrefs);

        return chromeOptions;
    }

    /**
     * Logs the user out and shutdown the selenium session
     */
    public void shutdown() {
        try {
            driver.quit();
            log.info("Driver shutting down");
        } catch (Exception ex) {
            log.error("Error found while shutting down driver - " + ex.getMessage());
        }
    }

    /**
     * Kills all browser sessions on the host machine
     */
    public void closeBrowserInstances(String operatingSystemName) throws IOException {
        if (operatingSystemName.contains("Windows")) {
            String serviceName = "chrome.exe";
            String driverName = "chromedriver.exe";

            String taskList = "tasklist";
            String kill = "taskkill /F /IM ";
            String line;
            Process p = Runtime.getRuntime().exec(taskList);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            log.info(serviceName + " process being closed");
            while ((line = reader.readLine()) != null) {
                if (line.contains(serviceName)) {
                    Runtime.getRuntime().exec(kill + serviceName);
                }
                if (line.contains(driverName)) {
                    Runtime.getRuntime().exec(kill + driverName);
                }
            }
        }
        if (operatingSystemName.contains("Linux")) {
            String killCommand = "pkill chrome";
            Runtime.getRuntime().exec(killCommand);
            log.info("Successful execution of " + killCommand);
        }
    }

    public boolean waitForElementClickable(String element, String locatorType) {
        boolean isClickable;
        try {
            Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(waitTimeOut))
                    .pollingEvery(Duration.ofMillis(600)).ignoring(NoSuchElementException.class);

            switch (locatorType.toLowerCase()) {
                case XPATH:
                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(element)));
                    break;

                case ID:
                    wait.until(ExpectedConditions.elementToBeClickable(By.id(element)));
                    break;

                case LINK_TEXT:
                    wait.until(ExpectedConditions.elementToBeClickable(By.linkText(element)));
                    break;

                case CSS:
                    wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(element)));
                    break;

                case CLASSNAME:
                    wait.until(ExpectedConditions.elementToBeClickable(By.className(element)));
                    break;

                case NAME:
                    wait.until(ExpectedConditions.elementToBeClickable(By.name(element)));
                    break;

                case PARTIAL_LINK_TEXT:
                    wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText(element)));
                    break;

                default:
                    logNoPropertyError();
            }
            isClickable = true;
        } catch (Exception ex) {
            log.error("Unable to click element " + element + " - " + ex.getMessage());
            isClickable = false;
        }
        return isClickable;
    }

    public boolean waitForVisibilityOfElement(String element, String locatorType) {
        boolean isVisible;
        try {
            Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(waitTimeOut))
                    .pollingEvery(Duration.ofMillis(600)).ignoring(NoSuchElementException.class);
            switch (locatorType.toLowerCase()) {
                case XPATH:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(element)));
                    break;

                case ID:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(element)));
                    break;

                case LINK_TEXT:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(element)));
                    break;

                case CSS:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(element)));
                    break;

                case CLASSNAME:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(element)));
                    break;

                case NAME:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(element)));
                    break;

                case PARTIAL_LINK_TEXT:
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.partialLinkText(element)));
                    break;

                default:
                    logNoPropertyError();
            }
            isVisible = true;
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
            isVisible = false;
        }
        return isVisible;
    }

    public boolean isElementPresent(String element, String locatorType) {
        boolean isPresent;
        try {
            Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(waitTimeOut))
                    .pollingEvery(Duration.ofMillis(600)).ignoring(NoSuchElementException.class);
            switch (locatorType.toLowerCase()) {
                case XPATH:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(element)));
                    break;

                case ID:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id(element)));
                    break;

                case LINK_TEXT:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.linkText(element)));
                    break;

                case CSS:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(element)));
                    break;

                case CLASSNAME:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.className(element)));
                    break;

                case NAME:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.name(element)));
                    break;

                case PARTIAL_LINK_TEXT:
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.partialLinkText(element)));
                    break;

                default:
                    logNoPropertyError();
            }
            isPresent = true;
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
            isPresent = false;
        }
        return isPresent;
    }

    public void uploadFile(String element, String locatorType, String path) {
        try {
            WebElement webElement = findWebElement(element, locatorType);

            if (isRemoteWebDriver) {
                ((RemoteWebElement) webElement).setFileDetector(new LocalFileDetector());
            }
            webElement.sendKeys(path);
            log.info("File uploaded successfully");

        } catch (Exception ex) {
            log.error("Unable to upload file " + element + " - " + ex.getMessage());
        }
    }

    public void uploadFileOld(String element, String locatorType, String path) {
        try {
            WebElement webElement = findWebElement(element, locatorType);
            switch (locatorType.toLowerCase()) {
                case XPATH:
                    driver.findElement(By.xpath(element)).sendKeys(path);
                    break;

                case ID:
                    driver.findElement(By.id(element)).sendKeys(path);
                    break;

                case LINK_TEXT:
                    driver.findElement(By.linkText(element)).sendKeys(path);
                    break;

                case CSS:
                    driver.findElement(By.cssSelector(element)).sendKeys(path);
                    break;

                case CLASSNAME:
                    driver.findElement(By.className(element)).sendKeys(path);
                    break;

                case NAME:
                    driver.findElement(By.name(element)).sendKeys(path);
                    break;

                case PARTIAL_LINK_TEXT:
                    driver.findElement(By.partialLinkText(element)).sendKeys(path);
                    break;

                default:
                    logNoPropertyError();
                    if (isRemoteWebDriver) {
                        ((RemoteWebElement) webElement).setFileDetector(new LocalFileDetector());
                    }
                    webElement.sendKeys(path);
                    log.info("File uploaded successfully");
            }
        } catch (Exception ex) {
            log.error("Unable to upload file " + element + " - " + ex.getMessage());
        }
    }

    public void enterText(String element, String locatorType, String text) {
        Actions actions = new Actions(driver);
        try {
            if (waitForElementClickable(element, locatorType)) {
                clearField(element, locatorType);
                switch (locatorType.toLowerCase()) {
                    case XPATH:
                        actions.doubleClick(driver.findElement(By.xpath(element))).build().perform();
                        driver.findElement(By.xpath(element)).sendKeys(text);
                        break;

                    case ID:
                        actions.doubleClick(driver.findElement(By.id(element))).build().perform();
                        driver.findElement(By.id(element)).sendKeys(text);
                        break;

                    case LINK_TEXT:
                        actions.doubleClick(driver.findElement(By.linkText(element))).build().perform();
                        driver.findElement(By.linkText(element)).sendKeys(text);
                        break;

                    case CSS:
                        actions.doubleClick(driver.findElement(By.cssSelector(element))).build().perform();
                        driver.findElement(By.cssSelector(element)).sendKeys(text);
                        break;

                    case CLASSNAME:
                        actions.doubleClick(driver.findElement(By.className(element))).build().perform();
                        driver.findElement(By.className(element)).sendKeys(text);
                        break;

                    case NAME:
                        actions.doubleClick(driver.findElement(By.name(element))).build().perform();
                        driver.findElement(By.name(element)).sendKeys(text);
                        break;

                    case PARTIAL_LINK_TEXT:
                        actions.doubleClick(driver.findElement(By.partialLinkText(element))).build().perform();
                        driver.findElement(By.partialLinkText(element)).sendKeys(text);
                        break;

                    default:
                        logNoPropertyError();
                }
            }
            enterSuccessLog(element, text);
        } catch (Exception ex) {
            log.error("Unable to select and enter text " + element + " - " + ex.getMessage());
        }
    }

    public void keysTab(String element, String locatorType) {
        if (waitForElementClickable(element, locatorType)) {
            switch (locatorType.toLowerCase()) {
                case XPATH:
                    driver.findElement(By.xpath(element)).sendKeys(Keys.TAB);
                    break;

                case ID:
                    driver.findElement(By.id(element)).sendKeys(Keys.TAB);
                    break;

                case LINK_TEXT:
                    driver.findElement(By.linkText(element)).sendKeys(Keys.TAB);
                    break;

                case CSS:
                    driver.findElement(By.cssSelector(element)).sendKeys(Keys.TAB);
                    break;

                case CLASSNAME:
                    driver.findElement(By.className(element)).sendKeys(Keys.TAB);
                    break;

                case NAME:
                    driver.findElement(By.name(element)).sendKeys(Keys.TAB);
                    break;

                case PARTIAL_LINK_TEXT:
                    driver.findElement(By.partialLinkText(element)).sendKeys(Keys.TAB);
                    break;

                default:
                    logNoPropertyError();
            }
        }
    }

    public void clearField(String element, String locatorType) {
        try {
            if (waitForElementClickable(element, locatorType)) {
                switch (locatorType.toLowerCase()) {
                    case XPATH:
                        driver.findElement(By.xpath(element)).clear();
                        break;

                    case ID:
                        driver.findElement(By.id(element)).clear();
                        break;

                    case LINK_TEXT:
                        driver.findElement(By.linkText(element)).clear();
                        break;

                    case CSS:
                        driver.findElement(By.cssSelector(element)).clear();
                        break;

                    case CLASSNAME:
                        driver.findElement(By.className(element)).clear();
                        break;

                    case NAME:
                        driver.findElement(By.name(element)).clear();
                        break;

                    case PARTIAL_LINK_TEXT:
                        driver.findElement(By.partialLinkText(element)).clear();
                        break;

                    default:
                        logNoPropertyError();
                }
            }
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
        }
    }

    public WebElement findElement(String element, String locatorType) {
        WebElement webElement = null;
        try {
            if (waitForVisibilityOfElement(element, locatorType)) {
                return getElemByLocatorType(element, locatorType);
            }
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
        }
        return webElement;
    }

    // this method does the same thing as findElement except that it does not look for the visibility of an element before returning it
    public WebElement findWebElement(String element, String locatorType) {
        return getElemByLocatorType(element, locatorType);
    }

    private WebElement getElemByLocatorType(String element, String locatorType) {
        switch (locatorType.toLowerCase()) {
            case XPATH:
                return driver.findElement(By.xpath(element));

            case ID:
                return driver.findElement(By.id(element));

            case LINK_TEXT:
                return driver.findElement(By.linkText(element));

            case CSS:
                return driver.findElement(By.cssSelector(element));

            case CLASSNAME:
                return driver.findElement(By.className(element));

            case NAME:
                return driver.findElement(By.name(element));

            case PARTIAL_LINK_TEXT:
                return driver.findElement(By.partialLinkText(element));
            default:
                logNoPropertyError();
                return null;
        }
    }

    public void clickElement(String element, String locatorType) {
        try {
            if (waitForElementClickable(element, locatorType) &&
                    clickByByLocatorType(element, locatorType)) {
                clickSuccessLog(element, locatorType);
            }
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
        }
    }

    public void clickElementJavaScript(String element, String locatorType) {
        try {
            String jsClick = "arguments[0].click()";
            if (waitForElementClickable(element, locatorType)) {
                JavascriptExecutor executor = (JavascriptExecutor) driver;
                switch (locatorType.toLowerCase()) {
                    case XPATH:
                        executor.executeScript(jsClick, driver.findElement(By.xpath(element)));
                        break;

                    case ID:
                        executor.executeScript(jsClick, driver.findElement(By.id(element)));
                        break;

                    case LINK_TEXT:
                        executor.executeScript(jsClick, driver.findElement(By.linkText(element)));
                        break;

                    case CSS:
                        executor.executeScript(jsClick, driver.findElement(By.cssSelector(element)));
                        break;

                    case CLASSNAME:
                        executor.executeScript(jsClick, driver.findElement(By.className(element)));
                        break;

                    case NAME:
                        executor.executeScript(jsClick, driver.findElement(By.name(element)));
                        break;
                    case PARTIAL_LINK_TEXT:
                        executor.executeScript(jsClick, driver.findElement(By.partialLinkText(element)));
                        break;
                    default:
                        logNoPropertyError();
                }
            }
            clickSuccessLog(element, locatorType);
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
        }
    }

    protected boolean isElementDisplayed(String elementLocator, String locatorType) {
        try {
            WebElement element = findWebElement(elementLocator, locatorType);

            while (!element.isDisplayed()) {
                if (element.isDisplayed()) {
                    return true;
                } else {
                    pause(3000);
                    log.info("Element - " + elementLocator + " is not yet visible");
                }
            }
        } catch (Exception e) {
            log.error("Could not determine element visibility - " + e.getMessage());
        }
        return false;
    }

    public boolean isElementPresentInDOM(String element, String locatorType) {
        boolean isElementPresent = false;
        try {
            switch (locatorType.toLowerCase()) {
                case XPATH:
                    isElementPresent = !driver.findElements(By.xpath(element)).isEmpty();
                    break;

                case ID:
                    isElementPresent = !driver.findElements(By.id(element)).isEmpty();
                    break;

                case LINK_TEXT:
                    isElementPresent = !driver.findElements(By.linkText(element)).isEmpty();
                    break;

                case CSS:
                    isElementPresent = !driver.findElements(By.cssSelector(element)).isEmpty();
                    break;

                case CLASSNAME:
                    isElementPresent = !driver.findElements(By.className(element)).isEmpty();
                    break;

                case NAME:
                    isElementPresent = !driver.findElements(By.name(element)).isEmpty();
                    break;

                case PARTIAL_LINK_TEXT:
                    isElementPresent = !driver.findElements(By.partialLinkText(element)).isEmpty();
                    break;

                default:
                    logNoPropertyError();
            }
            if (isElementPresent) {
                successIsElementsPresentInDOMLog(element);
            } else {
                log.warn("Could not locate " + element + " in DOM");
            }
        } catch (Exception ex) {
            log.error("Something went wrong while looking for element " + element + " - " + ex.getMessage());
            isElementPresent = false;
        }
        return isElementPresent;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getText(String element, String locatorType) {
        String textInContext = null;
        try {
            if (waitForElementClickable(element, locatorType)) {
                switch (locatorType.toLowerCase()) {
                    case XPATH:
                        textInContext = driver.findElement(By.xpath(element)).getText();
                        break;

                    case ID:
                        textInContext = driver.findElement(By.id(element)).getText();
                        break;

                    case LINK_TEXT:
                        textInContext = driver.findElement(By.linkText(element)).getText();
                        break;

                    case CSS:
                        textInContext = driver.findElement(By.cssSelector(element)).getText();
                        break;

                    case CLASSNAME:
                        textInContext = driver.findElement(By.className(element)).getText();
                        break;

                    case NAME:
                        textInContext = driver.findElement(By.name(element)).getText();
                        break;

                    case PARTIAL_LINK_TEXT:
                        textInContext = driver.findElement(By.partialLinkText(element)).getText();
                        break;

                    default:
                        logNoPropertyError();
                }
            }
            sucessGetTextLog(element);
        } catch (Exception ex) {
            log.error("Unable to find attribute " + element + " - " + ex.getMessage());
        }
        return textInContext;
    }

    public String getSelectedValue(String element, String locatorType) {
        try {
            if (waitForElementClickable(element, locatorType)) {
                switch (locatorType.toLowerCase()) {
                    case XPATH:
                        return (new Select(driver.findElement(By.xpath(element)))).getFirstSelectedOption().getText();

                    case ID:
                        return (new Select(driver.findElement(By.id(element)))).getFirstSelectedOption().getText();

                    case LINK_TEXT:
                        return (new Select(driver.findElement(By.linkText(element)))).getFirstSelectedOption().getText();

                    case CSS:
                        return (new Select(driver.findElement(By.cssSelector(element)))).getFirstSelectedOption().getText();

                    case CLASSNAME:
                        return (new Select(driver.findElement(By.className(element)))).getFirstSelectedOption().getText();
                    case NAME:
                        return (new Select(driver.findElement(By.name(element)))).getFirstSelectedOption().getText();

                    case PARTIAL_LINK_TEXT:
                        return (new Select(driver.findElement(By.partialLinkText(element)))).getFirstSelectedOption().getText();
                    default:
                        logNoPropertyError();
                }
            }
            successGetSelectedValueLog(element, locatorType);
        } catch (Exception ex) {
            log.error("Unable to find attribute " + element + " - " + ex.getMessage());
        }
        return null;
    }

    public String getAttributeValue(String element, String locatorType, String attribute) {
        String attributeValue = null;
        try {
            if (waitForElementClickable(element, locatorType)) {
                attributeValue = resolveElementAttribute(element, locatorType, attribute);
            }
            clickSuccessLog(element, locatorType);
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + " - " + ex.getMessage());
        }
        return attributeValue;
    }

    // this is added primarily for hidden inputs which the existing getAttributeValue method fails to resolve.
    public String getElementAttribute(String element, String locatorType, String attribute) {
        String attrValue = null;
        try {
            return resolveElementAttribute(element, locatorType, attribute);
        } catch (Exception ex) {
            log.error(NO_ELEMENT_ERROR_TEXT + element + "  by attribute " + attribute + "  - " + ex.getMessage());
        }
        return attrValue;
    }

    private String resolveElementAttribute(String element, String locatorType, String attribute) {
        switch (locatorType.toLowerCase()) {
            case XPATH:
                return driver.findElement(By.xpath(element)).getAttribute(attribute);

            case ID:
                return driver.findElement(By.id(element)).getAttribute(attribute);

            case LINK_TEXT:
                return driver.findElement(By.linkText(element)).getAttribute(attribute);

            case CSS:
                return driver.findElement(By.cssSelector(element)).getAttribute(attribute);

            case CLASSNAME:
                return driver.findElement(By.className(element)).getAttribute(attribute);

            case NAME:
                return driver.findElement(By.name(element)).getAttribute(attribute);

            case PARTIAL_LINK_TEXT:
                return driver.findElement(By.partialLinkText(element)).getAttribute(attribute);

            default:
                logNoPropertyError();
        }
        return null;
    }

    public void selectTextValue(String element, String locatorType, String text) {
        Select select;
        try {
            if (waitForElementClickable(element, locatorType)) {
                switch (locatorType.toLowerCase()) {
                    case XPATH:
                        select = (new Select(driver.findElement(By.xpath(element))));
                        select.selectByVisibleText(text);
                        break;

                    case ID:
                        select = (new Select(driver.findElement(By.id(element))));
                        select.selectByVisibleText(text);
                        break;

                    case LINK_TEXT:
                        select = (new Select(driver.findElement(By.linkText(element))));
                        select.selectByVisibleText(text);
                        break;

                    case CSS:
                        select = (new Select(driver.findElement(By.cssSelector(element))));
                        select.selectByVisibleText(text);
                        break;

                    case CLASSNAME:
                        select = (new Select(driver.findElement(By.className(element))));
                        select.selectByVisibleText(text);
                        break;

                    case NAME:
                        select = (new Select(driver.findElement(By.name(element))));
                        select.selectByVisibleText(text);
                        break;

                    case PARTIAL_LINK_TEXT:
                        select = (new Select(driver.findElement(By.partialLinkText(element))));
                        select.selectByVisibleText(text);
                        break;

                    default:
                        logNoPropertyError();
                }
            }
            successSelectByVisibleTextLog(text);

        } catch (Exception e) {
            String methodName = Object.class.getEnclosingMethod().getName();
            log.error("Something went wrong while executing " + methodName + " due to - " + e.getMessage());

        }
    }

    public void selectOptionByIndex(String element, String locatorType, int index) {
        try {
            if (waitForElementClickable(element, locatorType)) {
                selectByLocatorType(element, locatorType, index);
            }
            successSelectByIndexLog(element, index);
        } catch (Exception e) {
            log.error("Something went wrong while selecting index " + index + " from element " + element);
        }
    }

    public void clickAndSelectOptionByIndex(String element, String locatorType, int index) {
        try {
            if (clickByByLocatorType(element, locatorType)) {
                selectByLocatorType(element, locatorType, index);
                successSelectByIndexLog(element, index);
            }

        } catch (Exception e) {
            log.error("Failed to select element " + element + " by index " + index + " - " + e.getMessage());
        }
    }

    public List<WebElement> findElements(String element, String locatorType) {
        List<WebElement> findElements = null;
        try {
            switch (locatorType.toLowerCase()) {
                case ID:
                    findElements = driver.findElements(By.id(element));
                    break;

                case NAME:
                    findElements = driver.findElements(By.name(element));
                    break;

                case CSS:
                    findElements = driver.findElements(By.cssSelector(element));
                    break;

                case XPATH:
                    findElements = driver.findElements(By.xpath(element));
                    break;

                case LINK_TEXT:
                    findElements = driver.findElements(By.linkText(element));
                    break;

                case PARTIAL_LINK_TEXT:
                    findElements = driver.findElements(By.partialLinkText(element));
                    break;

                default:
                    logNoPropertyError();
            }
        } catch (Exception e) {
            log.error("Something went wrong while finding the elements");
        }
        return findElements;
    }

    public void setImplicitWaitTimeout(int timeoutInSeconds) {
        try {
            driver.manage().timeouts().implicitlyWait(timeoutInSeconds, TimeUnit.SECONDS);
            log.info("Successfully set the driver implicit wait value to " + timeoutInSeconds + " seconds");
        } catch (Exception e) {
            log.error("Error occurred while trying to set the driver implicit wait value to " + timeoutInSeconds + " seconds - " + e.getMessage());
        }
    }

    public void switchToFrame(String frame, String locatorType) {
        try {
            driver.switchTo().frame(findElement(frame, locatorType));
            iframeSwitchSuccessLog(frame);

        } catch (Exception e) {
            log.error("Unable to find frame - " + frame + " with message " + e.getMessage());

        }
    }

    public void switchToParentFrame() {
        try {
            driver.switchTo().parentFrame();
            log.info("Successfully switched to parent frame");
        } catch (Exception e) {
            log.error(("Unable to switch to frame - " + e.getMessage()));
        }
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public String getAlertTextAndAccept() {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            log.info("Alert data: " + alertText);
            alert.accept();
            return alertText;
        } catch (NoAlertPresentException e) {
            log.error("Something went wrong while getting the alert text - " + e.getMessage());
        }
        return null;
    }

    public String switchToNewWindow() {
        try {
            String firstWindow = driver.getWindowHandle();
            Set<String> windowHandles = driver.getWindowHandles();
            log.info("Number of windows: " + windowHandles.size());
            for (String windowHandle : windowHandles) {
                log.info("Handle: " + windowHandle);
                if (!windowHandle.equalsIgnoreCase(firstWindow)) {
                    driver.switchTo().window(windowHandle);
                    log.info("Switched to window - " + windowHandle);
                    return windowHandle;
                }
            }
        } catch (Exception e) {
            log.error("Something went wrong while switching to new window " + e.getMessage());
        }
        return null;
    }

    public void switchToWindow(String windowHandle) {
        try {
            driver.switchTo().window(windowHandle);
            log.info("Switched to window - " + windowHandle);
        } catch (Exception e) {
            log.error("Something went wrong while switching to window " + e.getMessage());
        }
    }

    public String getCurrentWindowHandle() {
        try {
            String windowHandle = driver.getWindowHandle();
            log.info("Current window handle - " + windowHandle);
            return windowHandle;
        } catch (Exception e) {
            log.error("Something went wrong while getting window " + e.getMessage());
            return null;
        }
    }

    public void closeWindow(String windowHandle) {
        try {
            driver.switchTo().window(windowHandle).close();
            log.info("Closed window - " + windowHandle);
        } catch (Exception e) {
            log.error("Something went wrong while closing window " + e.getMessage());
        }
    }

    public void scrollToTopOfPage() {
        log.info("Scrolling to top of page");
        ((JavascriptExecutor) driver).executeScript("scroll(0, -250)");
    }

    public void scrollToBottomOfPage() {
        log.info("Scrolling to bottom of page");
        ((JavascriptExecutor) driver).executeScript("scroll(0, 250)");
    }

    public void scrollToView(WebElement element) {
        log.info("Scrolling to view of element");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public void scrollToElement(WebElement parentElement, WebElement actualElement) {
        log.info("Scrolling to element : " + actualElement);
        Actions actions = new Actions(driver);
        actions.moveToElement(parentElement).moveToElement(actualElement);
        actions.perform();
    }

    public void sendKeys(WebElement webElement, String s) {
        webElement.sendKeys(s);
    }

    public void openNewTab(String url) {
        try {
            ((JavascriptExecutor) driver).executeScript("window.open()");
            ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
            driver.switchTo().window(tabs.get(1));
            openURL(url);
        } catch (Exception ex) {
            log.error("Unable to open new tab - " + ex.getMessage());
            log.error(ex.getStackTrace());
        }
    }

    public void openURL(String url) {
        try {
            driver.get(url);
        } catch (Exception ex) {
            log.error("Unable to open url - " + ex.getMessage());
            log.error(ex.getStackTrace());
        }
    }

    public void pause(long t) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(t).toMillis());
    }

    public int getChartRowsSize(String element) {
        List<WebElement> rows = driver.findElements(By.xpath(element));
        return rows.size();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getLogoutUrl() {
        String logoutURL = null;
        try {
            int index = sessionUrl.indexOf("Xiriuz/Controller?");
            String toBeReplaced = sessionUrl.substring(index);
            String replacement = "Xiriuz/logoff.do";

            return sessionUrl.replace(toBeReplaced, replacement);
        } catch (Exception e) {
            log.error("Error occurred while getting logout URL - " + e.getMessage());
        }
        return logoutURL;
    }

    private void logNoPropertyError() {
        log.info("No or incorrect element type was specified, please specify which element attribute you want to interact with");
    }

    private void enterSuccessLog(String element, String value) {
        log.info(value + " successfully entered into " + element);
    }

    private void clickSuccessLog(String element, String locatorType) {
        log.info(ELEMENT_INF_TEXT + element + " successfully clicked with attribute type " + locatorType);
    }

    private void successIsElementsPresentInDOMLog(String element) {
        log.info(ELEMENT_INF_TEXT + element + " is present in the DOM");
    }

    private void sucessGetTextLog(String element) {
        log.info("Successful retrival of text from element " + element);
    }

    private void successGetSelectedValueLog(String element, String locatorType) {
        log.info(ELEMENT_INF_TEXT + element + " get Selected Value attribute type " + locatorType);
    }

    private void successSelectByVisibleTextLog(String text) {
        log.info(text + " successfully selected from dropdown");
    }

    private void successSelectByIndexLog(String element, int index) {
        log.info(index + " successfully selected by for element " + element);
    }

    private void iframeSwitchSuccessLog(String frame) {
        log.info("Successfully switched to frame - " + frame);
    }

    private void selectByLocatorType(String element, String locatorType, int index) {
        Select select;
        switch (locatorType) {
            case XPATH:
                select = (new Select(driver.findElement(By.xpath(element))));
                select.selectByIndex(index);
                break;

            case ID:
                select = (new Select(driver.findElement(By.id(element))));
                select.selectByIndex(index);
                break;

            case LINK_TEXT:
                select = (new Select(driver.findElement(By.linkText(element))));
                select.selectByIndex(index);
                break;

            case CSS:
                select = (new Select(driver.findElement(By.cssSelector(element))));
                select.selectByIndex(index);
                break;

            case CLASSNAME:
                select = (new Select(driver.findElement(By.className(element))));
                select.selectByIndex(index);
                break;

            case NAME:
                select = (new Select(driver.findElement(By.name(element))));
                select.selectByIndex(index);
                break;

            case PARTIAL_LINK_TEXT:
                select = (new Select(driver.findElement(By.partialLinkText(element))));
                select.selectByIndex(index);
                break;

            default:
                logNoPropertyError();
        }
    }

    private boolean clickByByLocatorType(String element, String locatorType) {
        switch (locatorType.toLowerCase()) {
            case XPATH:
                driver.findElement(By.xpath(element)).click();
                return true;
            case ID:
                driver.findElement(By.id(element)).click();
                return true;

            case LINK_TEXT:
                driver.findElement(By.linkText(element)).click();
                return true;
            case CSS:
                driver.findElement(By.cssSelector(element)).click();
                return true;

            case CLASSNAME:
                driver.findElement(By.className(element)).click();
                return true;

            case NAME:
                driver.findElement(By.name(element)).click();
                return true;

            case PARTIAL_LINK_TEXT:
                driver.findElement(By.partialLinkText(element)).click();
                return true;
            default:
                logNoPropertyError();
                return false;
        }
    }

    /* This method runs javascript to check if the page is ready. If it returns false it will wait 2 seconds and do another attempt (pollinterval of 2 secs).
    It will continue to do this for 90 seconds. If the page is ready it will continue with the test */
    public void checkPageIsReady() {
        await().pollInterval(2, TimeUnit.SECONDS).atMost(90, TimeUnit.SECONDS).until(isPageReady());
    }

    private Callable<Boolean> isPageReady() {
        return () -> {
            JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
            boolean readyStateOfPage = javascriptExecutor.executeScript("return document.readyState").toString().equals("complete");
            if (!readyStateOfPage) {
                log.warn("The page with title " + driver.getTitle() + " is not ready yet. In 2 seconds another attempt will be made.");
            }
            return readyStateOfPage;
        };
    }
}
