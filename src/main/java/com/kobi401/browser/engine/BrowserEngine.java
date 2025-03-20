package com.kobi401.browser.engine;

import com.kobi401.browser.encryption.EncryptionUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserEngine {
    private static WebView webView;
    private final WebEngine webEngine;
    private String currentUrl;
    private double loadProgress;
    private boolean isLoading;

    private CookieManager cookieManager;
    private boolean trackingProtectionEnabled = false;
    private Set<String> blockedTrackingUrls;
    private EncryptionUtils encryptionUtils;
    private Stack<String> webHistory = new Stack<>();

    public BrowserEngine() {
        webView = new WebView();
        this.encryptionUtils = new EncryptionUtils();
        webEngine = webView.getEngine();
        cookieManager = new CookieManager();
        blockedTrackingUrls = new HashSet<>();
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

        JSObject jsObject = (JSObject) webEngine.executeScript("window");
        jsObject.setMember("java", this);

        loadPage("welcome.html");

        // Listen for page load progress
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            loadProgress = newVal.doubleValue();
        });

        // Listen for changes in loading state
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            isLoading = newState == Worker.State.RUNNING;
        });

        // Listen for URL changes
        webEngine.locationProperty().addListener((obs, oldVal, newVal) -> {
            currentUrl = newVal;
        });

        // Listen for page title updates
        webEngine.titleProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Page Title Changed: " + newVal);
        });

        // Listen for errors
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.err.println("Error loading page: " + newVal.getMessage());
            }
        });

        fetchTrackingUrlsFromEasyList();
        applyDefaultSettings();
    }

    private void fetchTrackingUrlsFromEasyList() {
        String easyListUrl = "https://easylist.to/easylist/easyprivacy.txt";

        try {
            URL url = new URL(easyListUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (!inputLine.startsWith("!") && !inputLine.isEmpty()) {
                    String domainPattern = "(?<=//)([a-zA-Z0-9.-]+)";
                    Pattern pattern = Pattern.compile(domainPattern);
                    Matcher matcher = pattern.matcher(inputLine);
                    while (matcher.find()) {
                        String domain = matcher.group(1);
                        blockedTrackingUrls.add(domain);
                    }
                }
            }
            in.close();
            System.out.println("Successfully fetched and parsed tracking URLs from EasyList.");
        } catch (IOException e) {
            System.out.println("Error fetching EasyList: " + e.getMessage());
        }
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

    public void setTrackingProtectionEnabled(boolean enabled) {
        trackingProtectionEnabled = enabled;
        if (enabled) {
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    blockTrackingScripts(webEngine.getLocation());
                }
            });
            System.out.println("Tracking Protection enabled.");
        } else {
            System.out.println("Tracking Protection disabled.");
        }
    }

    private void blockTrackingScripts(String currentUrl) {
        if (currentUrl.startsWith("file://")) {
            System.out.println("Ignoring local file: " + currentUrl);
            return;
        }

        for (String blockedUrl : blockedTrackingUrls) {
            if (currentUrl.contains(blockedUrl)) {
                System.out.println("Blocking tracking script from: " + currentUrl);
                webEngine.executeScript("document.write('This content is blocked due to tracking protection')");
                return;
            }
        }
    }

    public void filterRequests(String requestUrl) {
        if (trackingProtectionEnabled && isTrackingUrl(requestUrl)) {
            System.out.println("Blocking request to tracking URL: " + requestUrl);
        }
    }

    private boolean isTrackingUrl(String url) {
        for (String blockedUrl : blockedTrackingUrls) {
            if (url.contains(blockedUrl)) {
                return true;
            }
        }
        return false;
    }

    public void setCustomCookies(URI uri, List<String> cookies) {
        cookies.forEach(cookie -> {
            System.out.println("Setting cookie for " + uri + ": " + cookie);
        });
    }

    public void setTheme(String theme) {
        if ("dark".equalsIgnoreCase(theme)) {
            webEngine.executeScript("document.body.style.backgroundColor = '#121212';");
            webEngine.executeScript("document.body.style.color = '#ffffff';");
        } else {
            webEngine.executeScript("document.body.style.backgroundColor = '#ffffff';");
            webEngine.executeScript("document.body.style.color = '#000000';");
        }
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
        setTrackingProtectionEnabled(false);
        setTheme("dark");
        setFontSize("medium");
    }

    public void loadPage(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String encryptedUrl = encryptionUtils.encrypt(url);
                webEngine.load(url);
            } else {
                loadLocalHtml(url);
            }

            webHistory.push(url);
        } catch (Exception e) {
            System.err.println("Error loading page: " + e.getMessage());
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
