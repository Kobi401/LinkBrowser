package com.kobi401.browser.engine;

import com.kobi401.browser.Launch;
import com.kobi401.browser.devtools.InspectElementTool;
import com.kobi401.browser.encryption.EncryptionUtils;
import com.kobi401.browser.jsbridge.JavaScriptBridge;
import com.kobi401.browser.security.AdBlocker;
import com.kobi401.browser.security.SecurityUtils;
import com.kobi401.browser.ui.BrowserUI;
import javafx.beans.value.ChangeListener;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserEngine {
    private static WebView webView;
    private WebEngine webEngine;
    private String currentUrl;
    private double loadProgress;
    private boolean isLoading;
    private static final Logger logger = Logger.getLogger(BrowserEngine.class.getName());
    private CookieManager cookieManager;
    private EncryptionUtils encryptionUtils;
    private Stack<String> webHistory = new Stack<>();
    private SecurityUtils securityUtils;
    private AdBlocker adBlocker;

    private static BrowserEngine instance;
    private InspectElementTool inspectElementTool;
    private Launch launch;

    public static BrowserEngine getInstance() {
        if (instance == null) {
            instance = new BrowserEngine();
        }
        return instance;
    }

    public BrowserEngine() {
        initialize();
    }

    private void initialize() {
        webView = new WebView();
        this.securityUtils = new SecurityUtils();
        this.encryptionUtils = new EncryptionUtils();
        webEngine = webView.getEngine();
        cookieManager = new CookieManager();
        java.net.CookieHandler.setDefault(cookieManager);

        String userHome = System.getProperty("user.home");
        String userDataDirectoryPath = userHome + File.separator + "LinkBrowser" + File.separator + "User";
        File userDataDirectory = new File(userDataDirectoryPath);
        if (!userDataDirectory.exists()) {
            boolean created = userDataDirectory.mkdirs();
            if (created) {
                System.out.println("User data directory created at: " + userDataDirectoryPath);
            } else {
                System.err.println("Failed to create user data directory at: " + userDataDirectoryPath);
            }
        }
        webView.getEngine().setUserDataDirectory(userDataDirectory);

        initializeJavaScriptBridge();

        if (this.adBlocker == null) {
            this.adBlocker = new AdBlocker();
        }

        loadLocalHtml("welcome.html");

        // Event Listeners
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            loadProgress = newVal.doubleValue();
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            isLoading = newState == Worker.State.RUNNING;
        });

        webEngine.locationProperty().addListener((obs, oldVal, newVal) -> {
            currentUrl = newVal;
        });

        webEngine.titleProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Page Title Changed: " + newVal);
        });

        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.err.println("Error loading page: " + newVal.getMessage());
            }
        });

        applyDefaultSettings();
    }

    private void initializeJavaScriptBridge() {
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject jsBridge = (JSObject) webEngine.executeScript("window");
                    jsBridge.setMember("java", this);
                    jsBridge.setMember("javaInspectorBridge", inspectElementTool);
                    logger.info("JavaScript bridge initialized successfully.");
                } catch (Exception e) {
                    logger.severe("Failed to initialize JavaScript bridge: " + e.getMessage());
                }
            }
        });
    }

    public void setUserAgent(String userAgent) {
        webEngine.setUserAgent(userAgent);
    }

    public void setCookiesEnabled(boolean enabled) {
        if (enabled) {
            System.out.println("Cookies enabled.");
            java.net.CookieHandler.setDefault(cookieManager);
        } else {
            System.out.println("Cookies disabled.");
            java.net.CookieHandler.setDefault(null);
        }
    }

    public void setCustomCookies(URI uri, List<String> cookies) {
        cookies.forEach(cookie -> {
            System.out.println("Setting cookie for " + uri + ": " + cookie);
        });
    }

    public void setFontSize(String fontSize) {
        String size = "16px";
        switch (fontSize) {
            case "small":
                size = "12px";
                break;
            case "large":
                size = "20px";
                break;
            case "medium":
            default:
                size = "16px";
                break;
        }
        webEngine.executeScript("document.body.style.fontSize = '" + size + "';");
    }

    private void applyDefaultSettings() {
        setJavaScriptEnabled(true);
        setCookiesEnabled(true);
        setFontSize("medium");
    }


    public void loadPage(String url) {
        try {
            if (!securityUtils.isSecureUrl(url) && !isLocalHtml(url)) {
                System.err.println("Insecure URL: " + url);
                return;
            }

            if (url.startsWith("http://") || url.startsWith("https://")) {
                String encryptedUrl = encryptionUtils.encrypt(url);
                webEngine.load(url);
                webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        adBlocker.blockAdsWithJS();
                    }
                });
            } else {
                File localFile = new File(url);
                if (localFile.exists() || new File(System.getProperty("user.dir"), url).exists()) {
                    loadLocalHtml(url);
                } else {
                    System.err.println("Invalid URL: " + url);
                }
            }

            webHistory.push(url);

        } catch (Exception e) {
            System.err.println("Error loading page: " + e.getMessage());
        }
    }

    private boolean isLocalHtml(String url) {
        // Check if the URL is a local file (starts with "file://" or a relative path)
        if (url.startsWith("file://")) {
            return new File(url.substring(7)).exists(); // Check for file:// URLs
        } else {
            // Check for relative paths or absolute file paths
            File localFile = new File(url);
            return localFile.exists() || new File(System.getProperty("user.dir"), url).exists(); // Check for relative file paths
        }
    }


    private void loadLocalHtml(String fileName) {
        try {
            URL resourceUrl = getClass().getClassLoader().getResource(fileName);
            if (resourceUrl != null) {
                URI uri = resourceUrl.toURI();
                webEngine.load(uri.toString());
            } else {
                System.err.println("Local HTML file not found: " + fileName);
            }
        } catch (Exception e) {
            System.err.println("Error loading local HTML file: " + e.getMessage());
        }
    }

    public void goBack() {
        if (!webHistory.isEmpty()) {
            String previousUrl = webHistory.pop();
            loadPage(previousUrl);
        } else {
            System.out.println("No history available to go back.");
        }
    }

    public void goForward() {
        if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
            webEngine.getHistory().go(1);
        }
    }

    public void reloadPage() {
        webEngine.reload();
    }

    public void stopLoading() {
        webEngine.getLoadWorker().cancel();
    }

    public static WebView getWebView() {
        return webView;
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }

    public void executeJavaScript(String script, Consumer<Object> callback) {
        Object result = webEngine.executeScript(script);
        if (callback != null) {
            callback.accept(result);
        }
    }

    public void setJavaScriptEnabled(boolean enabled) {
        webEngine.setJavaScriptEnabled(enabled);
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public double getLoadProgress() {
        return loadProgress;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void clearCache() {
        webEngine.getHistory().getEntries().clear();
    }

    public String getPageTitle() {
        return webEngine.getTitle();
    }

    public void addUrlChangeListener(ChangeListener<String> listener) {
        webEngine.locationProperty().addListener(listener);
    }

    public void addTitleChangeListener(ChangeListener<String> listener) {
        webEngine.titleProperty().addListener(listener);
    }

    public void addLoadStateListener(ChangeListener<Worker.State> listener) {
        webEngine.getLoadWorker().stateProperty().addListener(listener);
    }
}
