package com.archinsurance.ss.adapter.driver;

import com.archinsurance.config.Config;
import com.archinsurance.config.constants.Browser;
import com.archinsurance.system.util.SystemUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WebDriverFactory {
    public static WebDriver getDriver() {
        if (Config.getBrowserForExecution().equals(Browser.CHROME)) {
            return getChromeDriver();
        } else if (Config.getBrowserForExecution().equals(Browser.EDGE)) {
            return getEdgeDriver();
        }
        return null;
    }

    public static MutableCapabilities getSauceOptions() throws ConfigurationException {
        MutableCapabilities sauceOptions = new MutableCapabilities();
        PropertiesConfiguration config = new PropertiesConfiguration(Config.getConfigPath());
        String sauceUserName = config.getString("sauceUserName", "vinodjoshi");
        String accessKey = config.getString("sauceAccessKey", "14d39a8c-8054-4811-a0a3-7ed0ff00d5ab");
        String parentTunnel = config.getString("parentTunnel", "jcherry87");
        String tunnelId = config.getString("tunnelId", "shared_dev_tunnel");

        sauceOptions.setCapability("username", sauceUserName);
        sauceOptions.setCapability("accessKey", accessKey);
        sauceOptions.setCapability("tunnelIdentifier", tunnelId);
        sauceOptions.setCapability("parentTunnel", parentTunnel);
        return sauceOptions;
    }

    public static ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized"); // open Browser in maximized mode
        options.addArguments("disable-infobars"); // disabling infobars
        options.addArguments("--disable-extensions"); // disabling extensions
        options.addArguments("--disable-gpu"); // applicable to windows os only
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--test-type");
        options.addArguments("--disable-popup-blocking");

        if (StringUtils.isNotBlank(Config.getLocalChromeDriverPath())) {
            System.setProperty("webdriver.chrome.driver", Config.getLocalChromeDriverPath());

            String chromeBinary = getChromeBinary();
            if (StringUtils.isNotBlank(chromeBinary)) {
                options.setBinary(chromeBinary);
            }

            if (Config.isProxyEnabled()) {
                options.setCapability("proxy", buildProxy());
            }
        }
        return options;
    }

    public static EdgeOptions getEdgeOptions() {
        EdgeOptions options = new EdgeOptions();

        if (StringUtils.isNotBlank(Config.getLocalEdgeDriverPath())) {
            System.setProperty("webdriver.edge.driver", Config.getLocalChromeDriverPath());

            if (Config.isProxyEnabled()) {
                options.setCapability("proxy", buildProxy());
            }
        }
        return options;
    }

    public static WebDriver getChromeDriver() {
        WebDriver driver;

        System.setProperty("webdriver.chrome.logfile", "logs/chromelog.log");
        System.setProperty("webdriver.chrome.verboseLogging", "true");

        if (StringUtils.isBlank(Config.getLocalChromeDriverPath())) {
            WebDriverManager.chromedriver().setup();
        }

        ChromeOptions options = getChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.CONTROL, "0"));
        driver.manage().timeouts().implicitlyWait(Config.getImplicitTimeout(), TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(Config.getPageLoadTimeout(), TimeUnit.SECONDS);
        return driver;
    }

    public static WebDriver getEdgeDriver() {
        WebDriver driver;
        if (StringUtils.isNotBlank(Config.getLocalEdgeDriverPath())) {
            driver = new EdgeDriver(getEdgeOptions());
        } else {
            WebDriverManager.edgedriver().setup();
            driver = new EdgeDriver();
        }

        driver.manage().window().maximize();
        driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.CONTROL, "0"));
        return driver;
    }

    private static Proxy buildProxy() {
        Proxy proxy = null;

        if (Config.isProxyEnabled()) {
            String proxyDetails = String.format("%s:%d", Config.getProxyHost(), Config.getProxyPort());
            proxy = new Proxy();
            proxy.setProxyType(Proxy.ProxyType.MANUAL);
            proxy.setHttpProxy(proxyDetails);
            proxy.setSslProxy(proxyDetails);
        }

        return proxy;
    }

    private static String getChromeBinary() {
        String chromeBinary = "";

        try {
            if (SystemUtil.isWindows()) {
                chromeBinary = new File(Config.getWindowsChromeBinary()).getCanonicalPath();
            } else if (SystemUtil.isUnix() || SystemUtil.isLinux()) {
                chromeBinary = new File(Config.getLinuxChromeBinary()).getCanonicalPath();
            }
        } catch (IOException e) {
            // do nothing
        }
        return chromeBinary;
    }
}