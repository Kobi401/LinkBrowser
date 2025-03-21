package com.kobi401.browser.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.kobi401.browser.engine.BrowserEngine;
import com.kobi401.browser.download.DownloadsManager;
import com.kobi401.browser.memory.TabMemoryManager;
import com.sun.management.OperatingSystemMXBean;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.skins.*;

import java.lang.management.ManagementFactory;

public class BrowserUI {
    private BorderPane root;
    private TabPane tabPane;
    private BrowserMenuBar menuBar;
    private BrowserNavigationBar navigationBar;
    private StatusBar statusBar;
    private ProgressBar progressBar;
    private Label statusLabel;
    private CustomTextField urlField;
    private Button goButton, backButton, forwardButton, refreshButton, newTabButton, settingsButton, downloadsButton;
    private DownloadsManager downloadsManager;
    private VBox downloadsPanel;
    private BookmarksBar bookmarksBar;
    private ProgressIndicator progressIndicator;
    private BrowserEngine browserEngine;

    public BrowserUI() {
        root = new BorderPane();
        tabPane = new TabPane();
        menuBar = new BrowserMenuBar(this);
        navigationBar = new BrowserNavigationBar(this);
        statusBar = createStatusBar();

        root.setTop(new VBox(menuBar.getMenuBar(), navigationBar.getNavigationBar()));
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        createNewTab("welcome.html");
    }

    public void openSettingsPage(BrowserEngine browserEngine) {
        Platform.runLater(() -> {
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.setWidth(400);
            settingsStage.setHeight(500);
            settingsStage.setResizable(false);

            VBox settingsLayout = new VBox(15);
            settingsLayout.setPadding(new Insets(20));
            settingsLayout.setAlignment(Pos.TOP_LEFT);

            Label titleLabel = new Label("Link Settings");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            CheckBox debugModeCheckbox = new CheckBox("Enable Debug Mode");
            debugModeCheckbox.setSelected(Boolean.parseBoolean(System.getProperty("debug.mode", "false")));

            ChoiceBox<String> themeChoiceBox = new ChoiceBox<>();
            themeChoiceBox.getItems().addAll("Light", "Dark", "System Default");
            themeChoiceBox.setValue(System.getProperty("browser.theme", "Light")); // Load saved theme

            ChoiceBox<String> fontSizeChoiceBox = new ChoiceBox<>();
            fontSizeChoiceBox.getItems().addAll("Small", "Medium", "Large");
            fontSizeChoiceBox.setValue(System.getProperty("browser.fontSize", "Medium")); // Load saved font size

            CheckBox cookiesCheckbox = new CheckBox("Enable Cookies");
            cookiesCheckbox.setSelected(Boolean.parseBoolean(System.getProperty("browser.cookies", "true")));

            Button applyButton = new Button("Apply");
            applyButton.setOnAction(e -> {
                System.setProperty("debug.mode", String.valueOf(debugModeCheckbox.isSelected()));
                String selectedFontSize = fontSizeChoiceBox.getValue().toLowerCase();
                browserEngine.setFontSize(selectedFontSize);
                System.setProperty("browser.fontSize", selectedFontSize);
                boolean cookiesEnabled = cookiesCheckbox.isSelected();
                browserEngine.setCookiesEnabled(cookiesEnabled);
                System.setProperty("browser.cookies", String.valueOf(cookiesEnabled));
                System.out.println("Settings applied: Debug Mode = " + debugModeCheckbox.isSelected() +
                        ", Font Size = " + selectedFontSize +
                        ", Cookies = " + cookiesEnabled);

                settingsStage.close();
            });

            settingsLayout.getChildren().addAll(
                    titleLabel,
                    debugModeCheckbox,
                    new Label("Font Size:"), fontSizeChoiceBox,
                    cookiesCheckbox,
                    applyButton
            );

            Scene scene = new Scene(settingsLayout);
            settingsStage.setScene(scene);
            settingsStage.show();
        });
    }

    private StatusBar createStatusBar() {
        StatusBar bar = new StatusBar();
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        statusLabel = new Label("Ready");

        HBox statusContainer = new HBox(statusLabel, progressBar);
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

    public void createNewTab(String url) {
        Tab tab = new Tab("New Tab");
        browserEngine = new BrowserEngine();
        WebView webView = browserEngine.getWebView();
        browserEngine.loadPage(url);

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
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load: " + shortenUrl(webEngine.getLocation()));
            }
        });

        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            statusLabel.setText("URL: " + shortenUrl(newUrl));
        });

        statusLabel.setStyle("-fx-padding: 0 10px 0 10px; -fx-alignment: center-left;");

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
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

    public void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.setTitle("About Link Browser");
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setStyle("-fx-background-color: #252525;");
        ImageView logoView = new ImageView(new Image("file:Images/LinkLogo_Big.png"));
        logoView.setFitWidth(80);
        logoView.setFitHeight(80);
        Label browserName = new Label("Link Browser");
        browserName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label versionLabel = new Label("Version: 2.1.0  |  Build: 2025.03.20  |  License: MIT");
        versionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #BBBBBB;");
        VBox headerBox = new VBox(logoView, browserName, versionLabel);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setSpacing(5);

        String sectionStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;";
        String contentStyle = "-fx-font-size: 14px; -fx-text-fill: #BBBBBB;";

        Label developerTitle = new Label("Developer:");
        developerTitle.setStyle(sectionStyle);

        Label developerContent = new Label("Kobi401");
        developerContent.setStyle(contentStyle);

        VBox developerBox = new VBox(developerTitle, developerContent);
        developerBox.setSpacing(5);
        developerBox.setAlignment(Pos.CENTER_LEFT);

        Label dependenciesTitle = new Label("Dependencies Used:");
        dependenciesTitle.setStyle(sectionStyle);

        Label dependenciesContent = new Label("• JavaFX\n• ControlsFX\n• TilesFX\n• FXGL");
        dependenciesContent.setStyle(contentStyle);

        VBox dependenciesBox = new VBox(dependenciesTitle, dependenciesContent);
        dependenciesBox.setSpacing(5);
        dependenciesBox.setAlignment(Pos.CENTER_LEFT);

        Label fxglTitle = new Label("FXGL Framework:");
        fxglTitle.setStyle(sectionStyle);

        Label fxglContent = new Label("Powered by FXGL for advanced graphics & game development.");
        fxglContent.setStyle(contentStyle);

        VBox fxglBox = new VBox(fxglTitle, fxglContent);
        fxglBox.setSpacing(5);
        fxglBox.setAlignment(Pos.CENTER_LEFT);

        Label acknowledgementsTitle = new Label("Acknowledgements:");
        acknowledgementsTitle.setStyle(sectionStyle);

        Label acknowledgementsContent = new Label("Special thanks to the JavaFX, ControlsFX, TilesFX, and FXGL communities!");
        acknowledgementsContent.setStyle(contentStyle);

        VBox acknowledgementsBox = new VBox(acknowledgementsTitle, acknowledgementsContent);
        acknowledgementsBox.setSpacing(5);
        acknowledgementsBox.setAlignment(Pos.CENTER_LEFT);

        Label githubTitle = new Label("GitHub Repository:");
        githubTitle.setStyle(sectionStyle);

        Hyperlink githubLink = new Hyperlink("https://github.com/Kobi401/LinkBrowser");
        githubLink.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E90FF;");

        VBox githubBox = new VBox(githubTitle, githubLink);
        githubBox.setSpacing(5);
        githubBox.setAlignment(Pos.CENTER_LEFT);

        Tile cpuTile = TileBuilder.create()
                .skinType(Tile.SkinType.GAUGE)
                .title("CPU Usage")
                .unit("%")
                .maxValue(100)
                .threshold(80)
                .textSize(Tile.TextSize.BIGGER)
                .build();

        Tile memoryTile = TileBuilder.create()
                .skinType(Tile.SkinType.GAUGE)
                .title("Memory Usage")
                .unit("%")
                .maxValue(100)
                .threshold(80)
                .textSize(Tile.TextSize.BIGGER)
                .build();

        HBox systemStatsBox = new HBox(20, cpuTile, memoryTile);
        systemStatsBox.setAlignment(Pos.CENTER);

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad() * 100;
            double memoryUsage = ((double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().maxMemory()) * 100;

            Platform.runLater(() -> {
                cpuTile.setValue(cpuLoad);
                memoryTile.setValue(memoryUsage);
            });
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 20px; -fx-background-color: #FF5555; -fx-text-fill: white;");
        closeButton.setOnAction(e -> {
            timeline.stop();
            aboutStage.close();
        });

        mainLayout.getChildren().addAll(headerBox, developerBox, dependenciesBox, fxglBox, acknowledgementsBox, githubBox, systemStatsBox, closeButton);

        Scene scene = new Scene(mainLayout, 500, 750);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
