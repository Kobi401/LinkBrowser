package com.kobi401.browser.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.kobi401.browser.engine.BrowserEngine;
import com.kobi401.browser.download.DownloadsManager;
import com.kobi401.browser.memory.TabMemoryManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;

public class BrowserUI {
    private BorderPane root;
    private TabPane tabPane;
    private MenuBar menuBar;
    private StatusBar statusBar;
    private ProgressBar progressBar;
    private Label statusLabel;
    private CustomTextField urlField;
    private Button goButton, backButton, forwardButton, refreshButton, newTabButton, settingsButton, downloadsButton;
    private DownloadsManager downloadsManager;
    private VBox downloadsPanel;
    private BookmarksBar bookmarksBar;
    private ProgressIndicator progressIndicator;

    public BrowserUI() {
        root = new BorderPane();
        tabPane = new TabPane();
        menuBar = createMenuBar();
        HBox navigationBar = createNavigationBar();
        statusBar = createStatusBar();

        root.setTop(new VBox(menuBar, navigationBar));
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        createNewTab("welcome.html");
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem newTab = new MenuItem("New Tab");
        MenuItem exit = new MenuItem("Exit");
        newTab.setOnAction(e -> createNewTab("https://www.google.com"));
        exit.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(newTab, exit);

        Menu navigationMenu = new Menu("Navigation");
        MenuItem back = new MenuItem("← Back");
        MenuItem forward = new MenuItem("→ Forward");
        MenuItem refresh = new MenuItem("⟳ Refresh");
        MenuItem goToURL = new MenuItem("Go to URL");

        back.setOnAction(e -> getCurrentWebView().getEngine().executeScript("history.back()"));
        forward.setOnAction(e -> getCurrentWebView().getEngine().executeScript("history.forward()"));
        refresh.setOnAction(e -> getCurrentWebView().getEngine().reload());
        goToURL.setOnAction(e -> loadURL(urlField.getText()));

        navigationMenu.getItems().addAll(back, forward, refresh, goToURL);

        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAlert("About", getAboutText()));
        helpMenu.getItems().add(about);

        return new MenuBar(fileMenu, navigationMenu, helpMenu);
    }

    private HBox createNavigationBar() {
        urlField = new CustomTextField();
        urlField.setPromptText("Enter URL...");
        goButton = new Button("Go");
        backButton = new Button("←");
        forwardButton = new Button("→");
        refreshButton = new Button("⟳");
        newTabButton = new Button("+");
        settingsButton = new Button("⚙");
        downloadsButton = new Button("↓");

        downloadsManager = new DownloadsManager();

        goButton.setOnAction(e -> loadURL(urlField.getText()));
        backButton.setOnAction(e -> getCurrentWebView().getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(e -> getCurrentWebView().getEngine().executeScript("history.forward()"));
        refreshButton.setOnAction(e -> getCurrentWebView().getEngine().reload());
        newTabButton.setOnAction(e -> createNewTab("https://www.google.com"));

        settingsButton.setOnAction(e -> openSettingsPage());

        downloadsButton.setOnAction(e -> showNotificationInBrowser("Error", "Downloads isn't supported yet!"));

        bookmarksBar = new BookmarksBar();
        bookmarksBar.addBookmark("Google", "https://www.google.com", getCurrentWebView().getEngine(), BrowserEngine.getWebView());
        bookmarksBar.addBookmark("YouTube", "https://www.youtube.com", getCurrentWebView().getEngine(), BrowserEngine.getWebView());
        bookmarksBar.addBookmark("GitHub", "https://github.com", getCurrentWebView().getEngine(), BrowserEngine.getWebView());

        HBox navigationContainer = new HBox(backButton, forwardButton, refreshButton, urlField, goButton, newTabButton, bookmarksBar.getBookmarksContainer(), settingsButton, downloadsButton);
        navigationContainer.setSpacing(5);
        return navigationContainer;
    }

    private void openSettingsPage() {
        createNewTab("settings.html");
    }

    private StatusBar createStatusBar() {
        StatusBar bar = new StatusBar();
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        statusLabel = new Label("Ready");

        HBox statusContainer = new HBox(statusLabel, progressBar, progressIndicator);
        statusContainer.setSpacing(10);
        statusContainer.setAlignment(Pos.CENTER_RIGHT);

        bar.getRightItems().add(statusContainer);

        bar.setOnMouseClicked(e -> showNotificationInBrowser("Status Clicked", "You clicked the status bar!"));

        return bar;
    }

    public void updateProgress(double progress) {
        progressBar.setProgress(progress);
        progressIndicator.setProgress(progress);
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    public void showNotificationInBrowser(String title, String message) {
        String notificationHtml = generateNotificationHTML(title, message);
        getCurrentWebView().getEngine().executeScript("document.body.innerHTML += `" + notificationHtml + "`;");
        getCurrentWebView().getEngine().executeScript(
                "var notification = document.getElementById('custom-notification');" +
                        "if (notification) { notification.style.opacity = 0; notification.style.transition = 'opacity 1s'; notification.style.opacity = 1; }");

        Platform.runLater(() -> {
            Timeline fadeOutTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(3), event -> {
                        getCurrentWebView().getEngine().executeScript(
                                "var notification = document.getElementById('custom-notification');" +
                                        "if (notification) { notification.style.transition = 'opacity 1s'; notification.style.opacity = 0; }");
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
                "background-color: rgba(0, 0, 0, 0.7); color: white; " +
                "padding: 10px; border-radius: 5px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3); " +
                "font-family: Arial, sans-serif; font-size: 14px; z-index: 9999;'>" +
                "<strong>" + title + "</strong><br/>" + message + "</div>";
    }

    public void startBackgroundProcess() {
        new Thread(() -> {
            for (double i = 0; i <= 1.0; i += 0.1) {
                final double progress = i;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                javafx.application.Platform.runLater(() -> {
                    updateProgress(progress);
                    if (progress == 1.0) {
                        setStatusMessage("Task Completed");
                        showNotificationInBrowser("Process Complete", "The background process has finished!");
                    }
                });
            }
        }).start();
    }

    private void createNewTab(String url) {
        Tab tab = new Tab("New Tab");
        BrowserEngine browserEngine = new BrowserEngine();
        WebView webView = browserEngine.getWebView();
        browserEngine.loadPage(url);

        tab.setContent(webView);
        tab.setOnClosed(e -> tabPane.getTabs().remove(tab));

        browserEngine.getWebEngine().getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
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

        browserEngine.getWebEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading...");
            } else if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Done");
                tab.setText(browserEngine.getPageTitle());
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load");
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void updateMemoryUsage(TabMemoryManager tabMemoryManager, Tooltip tooltip) {
        tabMemoryManager.updateMemoryUsage();
        tooltip.setText("Memory Usage: " + tabMemoryManager.getFormattedMemoryUsage());
    }


    private void loadURL(String url) {
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        getCurrentWebView().getEngine().load(url);
    }

    private WebView getCurrentWebView() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getContent() instanceof WebView) {
            return (WebView) selectedTab.getContent();
        }
        return new WebView();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("About Link Browser");
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String getAboutText() {
        String browserName = "Link Browser";
        String version = "2.1.0";
        String build = "Build 2025.03.19";
        String licenseText = "This project is open-source under the MIT License.\n\n" +
                "Dependencies used:\n" +
                "- JavaFX: A framework for building rich desktop applications.\n" +
                "- ControlsFX: A library that extends JavaFX with additional controls.\n" +
                "- FXGL: A game development framework built on top of JavaFX.\n\n" +
                "For more details, visit the GitHub repository.";

        return String.format("Browser: %s\nVersion: %s\nBuild: %s\n\nLicense:\n%s",
                browserName, version, build, licenseText);
    }

    public BorderPane getRoot() {
        return root;
    }
}
