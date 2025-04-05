package com.kobi401.browser.engine.main;

import com.kobi401.browser.Launch;
import com.kobi401.browser.devtools.inspectElement.InspectElementTool;
import com.kobi401.browser.download.downloadManager.Download;
import com.kobi401.browser.download.downloadTask.DownloadTask;
import com.kobi401.browser.encryption.EncryptionUtils;
import com.kobi401.browser.jsbridge.linkWebBridge.LinkWebBridge;
import com.kobi401.browser.jsbridge.Flash.RuffleFlashPlayer;
import com.kobi401.browser.jsbridge.webGL.WebGL;
import com.kobi401.browser.security.utils.SecurityUtils;
import com.kobi401.browser.utils.tracking.BrowserTracker;
import com.kobi401.browser.utils.debug.Debugger;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

import java.io.File;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

public class BrowserEngine {
    private WebEngine webEngine;
    private static WebView webView;
    private String currentUrl;
    private double loadProgress;
    private boolean isLoading;
    private static final Logger logger = Logger.getLogger(BrowserEngine.class.getName());
    private CookieManager cookieManager;
    private EncryptionUtils encryptionUtils;
    private final Stack<String> webHistory = new Stack<>();
    private SecurityUtils securityUtils;
    //private AdBlocker adBlocker;
    private WebGL webGL;
    private static BrowserEngine instance;
    private InspectElementTool inspectElementTool;
    private Launch launch;

    private LinkWebBridge linkWebBridge;
    private static BrowserTracker browserTracker;

    public static BrowserEngine getInstance() {
        if (instance == null) {
            instance = new BrowserEngine();
        }
        return instance;
    }

    public BrowserEngine() {
        initialize();
    }

    public static BrowserTracker getTracker() {
        return browserTracker;
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
            Debugger.println("User data directory created at: " + userDataDirectory.getAbsolutePath());
        }
        webEngine.setUserDataDirectory(userDataDirectory);

        webGL = new WebGL(webEngine);
        webGL.downloadWebGLLibrary();
        linkWebBridge = new LinkWebBridge(webEngine);
        browserTracker = new BrowserTracker(webEngine);

        webEngine.locationProperty().addListener((obs, oldVal, newVal) -> {
            currentUrl = newVal;
            browserTracker.getVisitedWebsites();
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            isLoading = newState == Worker.State.RUNNING;

            if (newState == Worker.State.SUCCEEDED) {
                webGL.injectWebGLIfNeeded();
                injectRightClickListener(webEngine);
            }
        });

        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> loadProgress = newVal.doubleValue());

        webEngine.titleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Debugger.println("Page Title Changed: " + newVal);
            }
        });

        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.err.println("Error loading page: " + newVal.getMessage());
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                RuffleFlashPlayer flashPlayer = new RuffleFlashPlayer(webEngine, linkWebBridge);
                flashPlayer.loadFlashContentFromExternalPage(currentUrl);
            }
        });

        applyDefaultSettings();
        loadLocalHtml("welcome.html");
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
            Debugger.println("Cookies enabled.");
            java.net.CookieHandler.setDefault(cookieManager);
        } else {
            Debugger.println("Cookies disabled.");
            java.net.CookieHandler.setDefault(null);
        }
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

    public void loadPage(String url) {
        try {
            if (!securityUtils.isSecureUrl(url) && !isLocalHtml(url)) {
                System.err.println("Stop right there! Insecure URL: " + url);
                return;
            }

            if (isDownloadLink(url)) {
                handleDownload(url);
                return;
            }

            if (isFlashFile(url)) {
                RuffleFlashPlayer flashPlayer = new RuffleFlashPlayer(webEngine, linkWebBridge);
                flashPlayer.loadFlashContentFromUrl(url);
                return;
            }

            if (url.startsWith("http://") || url.startsWith("https://")) {
                webEngine.load(url);
            } else {
                loadLocalFile(url);
            }

            webHistory.push(url);

        } catch (Exception e) {
            System.err.println("Error loading page: " + e.getMessage());
        }
    }

    public boolean isFlashFile(String url) {
        if (url.toLowerCase().endsWith(".swf")) {
            return true;
        }
        String[] flashGameSites = {
                "newgrounds.com",
                "kongregate.com",
                "armorgames.com",
                "addictinggames.com",
                "miniclip.com"
        };

        for (String site : flashGameSites) {
            if (url.toLowerCase().contains(site)) {
                return true;
            }
        }

        return false;
    }

    private void handleDownload(String url) {
        String fileName = getFileNameFromUrl(url);
        File destinationFile = new File(System.getProperty("user.home"), fileName);
        Debugger.println("Downloading: " + fileName);
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
                ".zip", ".rar", ".tar", ".gz", ".7z", ".bz2", ".xz", ".tar.gz", ".tar.xz", ".tar.bz2",
                ".exe", ".msi", ".apk", ".dmg", ".bin", ".run", ".sh", ".deb", ".rpm", ".iso", ".img",
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf", ".odt", ".ods", ".odp",
                ".mp3", ".wav", ".aac", ".flac", ".ogg", ".m4a", ".wma", ".mid", ".midi", ".opus", ".aiff",
                ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mpeg", ".mpg", ".m4v", ".ts",
                ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".tif", ".svg", ".webp", ".ico", ".heic", ".heif",
                ".java", ".py", ".js", ".html", ".css", ".php", ".c", ".cpp", ".h", ".cs", ".swift", ".rb", ".go", ".sh",
                ".ttf", ".otf", ".woff", ".woff2",
                ".torrent", ".crx", ".dll", ".bat", ".log", ".cfg", ".dat"
        };

        String lowerUrl = url.toLowerCase();
        for (String ext : downloadExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        if (url.contains("%download") || url.contains("/download/")) {
            return true;
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
        try {
            if (url.startsWith("file://")) {
                File file = new File(new URI(url));
                return file.exists() && file.isFile();
            }
            File localFile = new File(url);
            if (localFile.exists() && localFile.isFile()) {
                return true;
            }
            File workingDirFile = new File(System.getProperty("user.dir"), url);
            if (workingDirFile.exists() && workingDirFile.isFile()) {
                return true;
            }
            URL resourceUrl = getClass().getClassLoader().getResource(url);
            return resourceUrl != null;

        } catch (Exception e) {
            System.err.println("Error checking local HTML file: " + e.getMessage());
            return false;
        }
    }

    private void loadLocalHtml(String fileName) {
        try {
            File localFile = new File(fileName);
            if (localFile.exists() && localFile.isFile()) {
                webEngine.load(localFile.toURI().toString());
                Debugger.println("Loaded local HTML file: " + localFile.toURI());
                return;
            }
            File workingDirFile = new File(System.getProperty("user.dir"), fileName);
            if (workingDirFile.exists() && workingDirFile.isFile()) {
                webEngine.load(workingDirFile.toURI().toString());
                Debugger.println("Loaded from working directory: " + workingDirFile.toURI());
                return;
            }
            URL resourceUrl = getClass().getClassLoader().getResource(fileName);
            if (resourceUrl != null) {
                URI uri = resourceUrl.toURI();
                webEngine.load(uri.toString());
                Debugger.println("Loaded from classpath resources: " + uri);
                return;
            }
            System.err.println("Local HTML file not found: " + fileName);
        } catch (Exception e) {
            System.err.println("Error loading local HTML file: " + e.getMessage());
        }
    }

    public static WebView getWebView() {
        return webView;
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }

    public void setJavaScriptEnabled(boolean enabled) {
        webEngine.setJavaScriptEnabled(enabled);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getPageTitle() {
        return webEngine.getTitle();
    }
}
