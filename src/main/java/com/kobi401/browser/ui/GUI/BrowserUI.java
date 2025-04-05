package com.kobi401.browser.ui.GUI;

import com.kobi401.browser.download.downloadManager.Download;
import com.kobi401.browser.download.downloadTask.DownloadTask;
import com.kobi401.browser.engine.main.BrowserEngine;
import com.kobi401.browser.memory.tabs.TabMemoryManager;
import com.kobi401.browser.ui.menubar.BrowserMenuBar;
import com.kobi401.browser.ui.navigationbar.BrowserNavigationBar;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;


//TODO: Clean up this class

public class BrowserUI {
    private BorderPane root;
    private TabPane tabPane;
    private BrowserMenuBar menuBar;
    private BrowserNavigationBar navigationBar;
    private StatusBar statusBar;
    private ProgressBar progressBar;
    private Label statusLabel;
    private BrowserEngine browserEngine;

    private String lastClickedElementUrl = null;

    public BrowserUI() {
        root = new BorderPane();
        tabPane = new TabPane();
        menuBar = new BrowserMenuBar(this);
        navigationBar = new BrowserNavigationBar(this);
        statusBar = createStatusBar();

        root.setTop(new VBox(menuBar.getMenuBar(), navigationBar.getNavigationBar()));
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        //This wont work unless theres a createNewTab and loadLocalHTML page methods are called?
        createNewTab("welcome.html");;
    }

    private StatusBar createStatusBar() {
        StatusBar bar = new StatusBar();

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        statusLabel = new Label("Ready");

        Label networkStatusLabel = new Label("Online");
        networkStatusLabel.setStyle("-fx-text-fill: green;");

        Tooltip statusTooltip = new Tooltip("Click to view more details.");
        statusLabel.setTooltip(statusTooltip);

        Label errorStatusLabel = new Label("");
        errorStatusLabel.setStyle("-fx-text-fill: red;");

        HBox statusContainer = new HBox(statusLabel, progressBar, networkStatusLabel, errorStatusLabel);
        statusContainer.setSpacing(10);
        statusContainer.setAlignment(Pos.CENTER_RIGHT);
        bar.getRightItems().add(statusContainer);
        bar.setOnMouseClicked(e -> showNotificationInBrowser("Statusbar", "This will be used at some point lol."));
        updateNetworkStatus(networkStatusLabel);
        updateErrorStatus(errorStatusLabel);

        return bar;
    }

    private void updateNetworkStatus(Label networkStatusLabel) {
        boolean isOnline = isNetworkAvailable();
        if (isOnline) {
            networkStatusLabel.setText("Online");
            networkStatusLabel.setStyle("-fx-text-fill: green;");
        } else {
            networkStatusLabel.setText("Offline");
            networkStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private boolean isNetworkAvailable() {
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(1500);
            urlConnection.connect();
            return (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateErrorStatus(Label errorStatusLabel) {
        boolean hasError = false;
        if (hasError) {
            errorStatusLabel.setText("Error: Unable to load page.");
        } else {
            errorStatusLabel.setText("");
        }
    }

    public void showNotificationInBrowser(String title, String message) {
        String notificationHtml = generateNotificationHTML(title, message);
        getCurrentWebView().getEngine().executeScript("document.body.innerHTML += `" + notificationHtml + "`;");
        getCurrentWebView().getEngine().executeScript(
                "var notification = document.getElementById('custom-notification');" +
                        "if (notification) { notification.style.opacity = 1; notification.style.transform = 'translateY(0)'; }");
        Platform.runLater(() -> {
            Timeline fadeOutTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(3), event -> {
                        getCurrentWebView().getEngine().executeScript(
                                "var notification = document.getElementById('custom-notification');" +
                                        "if (notification) { notification.style.opacity = 0; notification.style.transform = 'translateY(20px)'; }");
                    }),
                    new KeyFrame(Duration.seconds(4), event -> {
                        getCurrentWebView().getEngine().executeScript(
                                "var notification = document.getElementById('custom-notification');" +
                                        "if (notification) { notification.remove(); }");
                    })
            );
            fadeOutTimeline.play();
        });
    }

    private String generateNotificationHTML(String title, String message) {
        return "<div id='custom-notification' style='" +
                "position: fixed; bottom: 20px; right: 20px; " +
                "background-color: #333; color: white; " +
                "padding: 12px 20px; border-radius: 8px; box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2); " +
                "font-family: 'Roboto', sans-serif; font-size: 16px; line-height: 1.4; z-index: 9999; " +
                "transition: transform 0.3s ease-in-out, opacity 0.3s ease-in-out; transform: translateY(20px); opacity: 0;'>" +
                "<strong style='font-weight: bold; font-size: 18px;'>" + title + "</strong><br/>" +
                "<span style='font-size: 14px;'>" + message + "</span>" +
                "</div>";
    }

    public void createNewTab(String url) {
        Tab tab = new Tab("New Tab");
        browserEngine = new BrowserEngine();
        WebView webView = browserEngine.getWebView();
        browserEngine.loadPage(url);

        setFavicon(tab, browserEngine.getWebEngine());

        tab.setContent(webView);
        tab.setOnClosed(e -> tabPane.getTabs().remove(tab));

        WebEngine webEngine = browserEngine.getWebEngine();

        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            progressBar.setProgress(newVal.doubleValue());
        });

        TabMemoryManager tabMemoryManager = new TabMemoryManager(tab);
        Tooltip memoryTooltip = new Tooltip("Loading memory usage...");
        tab.setTooltip(memoryTooltip);

        Timeline memoryUpdateTimeline = new Timeline(
                new KeyFrame(Duration.millis(500), e -> updateMemoryUsage(tabMemoryManager, memoryTooltip))
        );
        memoryUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        memoryUpdateTimeline.play();

        tab.setOnClosed(e -> memoryUpdateTimeline.stop());

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading...");
            } else if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Done: " + shortenUrl(webEngine.getLocation()));
                tab.setText(browserEngine.getPageTitle());
                setFavicon(tab, webEngine);
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load: " + shortenUrl(webEngine.getLocation()));
            }
        });

        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            statusLabel.setText("URL: " + shortenUrl(newUrl));
        });

        statusLabel.setStyle("-fx-padding: 0 10px 0 10px; -fx-alignment: center-left;");

        tab.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Close Tab?");
            alert.setHeaderText("Are you sure you want to close this tab?");
            alert.setContentText("You might lose unsaved data.");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                event.consume();
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void setFavicon(Tab tab, WebEngine webEngine) {
        String url = webEngine.getLocation();
        String faviconUrl = "https://www.google.com/s2/favicons?domain=" + url; // Favicon API
        Image favicon = new Image(faviconUrl);
        tab.setGraphic(new ImageView(favicon));
    }

    private ContextMenu createContextMenu(WebView webView, WebEngine webEngine) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem backItem = new MenuItem("Back");
        backItem.setOnAction(e -> {
            if (webEngine.getHistory().getCurrentIndex() > 0) {
                webEngine.getHistory().go(-1);
            }
        });

        MenuItem forwardItem = new MenuItem("Forward");
        forwardItem.setOnAction(e -> {
            if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
                webEngine.getHistory().go(1);
            }
        });

        MenuItem reloadItem = new MenuItem("Reload");
        reloadItem.setOnAction(e -> webEngine.reload());

        MenuItem copyUrlItem = new MenuItem("Copy URL");
        copyUrlItem.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(webEngine.getLocation());
            clipboard.setContent(content);
        });

        MenuItem openNewTabItem = new MenuItem("Open in New Tab");
        openNewTabItem.setOnAction(e -> createNewTab(webEngine.getLocation()));

        MenuItem downloadFileItem = new MenuItem("Download File");
        downloadFileItem.setOnAction(e -> {
            if (lastClickedElementUrl != null && !lastClickedElementUrl.isEmpty()) {
                startDownload(lastClickedElementUrl);
            }
        });

        contextMenu.getItems().addAll(backItem, forwardItem, reloadItem, copyUrlItem, openNewTabItem);

        webView.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                if (lastClickedElementUrl != null && !lastClickedElementUrl.isEmpty() && isDownloadable(lastClickedElementUrl)) {
                    if (!contextMenu.getItems().contains(downloadFileItem)) {
                        contextMenu.getItems().add(downloadFileItem);
                    }
                } else {
                    contextMenu.getItems().remove(downloadFileItem);
                }
            }
        });

        return contextMenu;
    }

    private boolean isDownloadable(String url) {
        String[] downloadExtensions = {
                ".zip", ".exe", ".tar", ".mp4", ".jpg", ".png", ".pdf", ".iso",
                ".mid", ".midi", ".rar", ".7z", ".mp3", ".wav", ".doc", ".docx",
                ".xls", ".xlsx", ".ppt", ".pptx", ".apk", ".dmg", ".bin", ".tar.gz"
        };

        for (String ext : downloadExtensions) {
            if (url.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void startDownload(String fileUrl) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(fileUrl.substring(fileUrl.lastIndexOf("/") + 1));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            DownloadTask downloadTask = new DownloadTask(new Download(fileUrl), fileUrl, file);
            new Thread(downloadTask).start();
        }
    }


    /**
     * Shortens a URL for display purposes while keeping its full value accessible in a tooltip.
     */
    private String shortenUrl(String url) {
        if (url.length() > 40) {
            return url.substring(0, 37) + "...";
        }
        return url;
    }


    private void updateMemoryUsage(TabMemoryManager tabMemoryManager, Tooltip tooltip) {
        tabMemoryManager.updateMemoryUsage();
        tooltip.setText("Memory Usage: " + tabMemoryManager.getFormattedMemoryUsage());
    }

    public WebView getCurrentWebView() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getContent() instanceof WebView) {
            return (WebView) selectedTab.getContent();
        }
        return new WebView();
    }

    public BorderPane getRoot() {
        return root;
    }
}
