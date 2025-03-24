package com.kobi401.browser.engine;

import com.kobi401.browser.Launch;
import com.kobi401.browser.devtools.InspectElementTool;
import com.kobi401.browser.download.Download;
import com.kobi401.browser.download.DownloadTask;
import com.kobi401.browser.encryption.EncryptionUtils;
import com.kobi401.browser.jsbridge.JSInjectionSystem;
import com.kobi401.browser.security.AdBlocker;
import com.kobi401.browser.security.SecurityUtils;
import javafx.beans.value.ChangeListener;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

import java.io.File;
import java.net.CookieManager;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class BrowserEngine {
    private static WebView webView;
    private WebEngine webEngine;
    private String currentUrl;
    private double loadProgress;
    private boolean isLoading;
    private static final Logger logger = Logger.getLogger(BrowserEngine.class.getName());
    private CookieManager cookieManager;
    private EncryptionUtils encryptionUtils;
    private final Stack<String> webHistory = new Stack<>();
    private SecurityUtils securityUtils;
    //private AdBlocker adBlocker;

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
        webEngine = webView.getEngine();

        this.securityUtils = new SecurityUtils();
        this.encryptionUtils = new EncryptionUtils();
        cookieManager = new CookieManager();
        java.net.CookieHandler.setDefault(cookieManager);
        File userDataDirectory = new File(System.getProperty("user.home") + File.separator + "LinkBrowser" + File.separator + "User");
        if (userDataDirectory.mkdirs()) {
            System.out.println("User data directory created at: " + userDataDirectory.getAbsolutePath());
        }
        webEngine.setUserDataDirectory(userDataDirectory);

        JSInjectionSystem jsInjectionSystem = new JSInjectionSystem(webEngine);

        loadLocalHtml("welcome.html");

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            isLoading = newState == Worker.State.RUNNING;
            if (newState == Worker.State.SUCCEEDED) {
                injectRightClickListener(webEngine);
            }
        });

        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> loadProgress = newVal.doubleValue());

        webEngine.locationProperty().addListener((obs, oldVal, newVal) -> currentUrl = newVal);

        webEngine.titleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.out.println("Page Title Changed: " + newVal);
            }
        });

        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.err.println("Error loading page: " + newVal.getMessage());
            }
        });

        applyDefaultSettings();
    }

    private void injectRightClickListener(WebEngine webEngine) {
        String script =
                "document.addEventListener('contextmenu', function(event) { " +
                        "    let element = event.target; " +
                        "    if (element.tagName === 'IMG') { " +
                        "        window.javaBridge.setLastClickedElement(element.src); " +
                        "    } else if (element.tagName === 'A' && element.href) { " +
                        "        window.javaBridge.setLastClickedElement(element.href); " +
                        "    } else { " +
                        "        window.javaBridge.setLastClickedElement(''); " +
                        "    } " +
                        "}, true);";

        webEngine.executeScript(script);
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
        String size = switch (fontSize) {
            case "small" -> "12px";
            case "large" -> "20px";
            default -> "16px";
        };
        webEngine.executeScript("document.body.style.fontSize = '" + size + "';");
    }

    private void applyDefaultSettings() {
        setJavaScriptEnabled(true);
        setCookiesEnabled(true);
        setFontSize("medium");
    }

    //Should be alot more optimized now?
    public void loadPage(String url) {
        try {
            //we ain't handling insecure stuff. Period.
            if (!securityUtils.isSecureUrl(url) && !isLocalHtml(url)) {
                System.err.println("Stop right there! Insecure URL: " + url);
                return;
            }

            if (isDownloadLink(url)) {
                handleDownload(url);
                return;
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                webEngine.load(url);
            }
            else {
                loadLocalFile(url);
            }
            webHistory.push(url);

        } catch (Exception e) {
            System.err.println("Error loading page: " + e.getMessage());
        }
    }

    private void handleDownload(String url) {
        String fileName = getFileNameFromUrl(url);
        File destinationFile = new File(System.getProperty("user.home"), fileName);
        System.out.println("Downloading: " + fileName);
        new Thread(new DownloadTask(new Download(fileName), url, destinationFile)).start();
    }

    private void loadLocalFile(String url) {
        File localFile = new File(url);
        if (localFile.exists() || new File(System.getProperty("user.dir"), url).exists()) {
            loadLocalHtml(url);
        } else {
            System.err.println("Invalid URL, again: " + url);
        }
    }

    private boolean isDownloadLink(String url) {
        String[] downloadExtensions = {
                // Archives & Compressed Files
                ".zip", ".rar", ".tar", ".gz", ".7z", ".bz2", ".xz", ".tar.gz", ".tar.xz", ".tar.bz2",

                // Executables & Installers
                ".exe", ".msi", ".apk", ".dmg", ".bin", ".run", ".sh", ".deb", ".rpm", ".iso", ".img",

                // Documents & Text Files
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf", ".odt", ".ods", ".odp",

                // Audio Files
                ".mp3", ".wav", ".aac", ".flac", ".ogg", ".m4a", ".wma", ".mid", ".midi", ".opus", ".aiff",

                // Video Files
                ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mpeg", ".mpg", ".m4v", ".ts",

                // Image Files
                ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".tif", ".svg", ".webp", ".ico", ".heic", ".heif",

                // Code & Scripts
                ".java", ".py", ".js", ".html", ".css", ".php", ".c", ".cpp", ".h", ".cs", ".swift", ".rb", ".go", ".sh",

                // Fonts
                ".ttf", ".otf", ".woff", ".woff2",

                // Others
                ".torrent", ".crx", ".dll", ".bat", ".log", ".cfg", ".dat"
        };
        String lowerUrl = url.toLowerCase();
        for (String ext : downloadExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private String getFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        return fileName;
    }

    private boolean isLocalHtml(String url) {
        if (url.startsWith("file://")) {
            return new File(url.substring(7)).exists();
        } else {
            File localFile = new File(url);
            return localFile.exists() || new File(System.getProperty("user.dir"), url).exists();
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
}
