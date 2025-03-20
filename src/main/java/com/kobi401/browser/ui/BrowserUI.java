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
    private BrowserEngine browserEngine;

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
       // goToURL.setOnAction(e -> loadURL(urlField.getText()));

        navigationMenu.getItems().addAll(back, forward, refresh, goToURL);

        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAboutWindow());
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

        goButton.setOnAction(e -> browserEngine.loadPage(String.valueOf(urlField)));
        backButton.setOnAction(e -> getCurrentWebView().getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(e -> getCurrentWebView().getEngine().executeScript("history.forward()"));
        refreshButton.setOnAction(e -> getCurrentWebView().getEngine().reload());
        newTabButton.setOnAction(e -> createNewTab("https://www.google.com"));

        settingsButton.setOnAction(e -> openSettingsPage(BrowserEngine.getInstance()));

        downloadsButton.setOnAction(e -> showNotificationInBrowser("Error", "Downloads isn't supported yet!"));

        bookmarksBar = new BookmarksBar();
        bookmarksBar.addBookmark("Google", "https://www.google.com", getCurrentWebView().getEngine(), BrowserEngine.getWebView());
        bookmarksBar.addBookmark("YouTube", "https://www.youtube.com", getCurrentWebView().getEngine(), BrowserEngine.getWebView());
        bookmarksBar.addBookmark("GitHub", "https://github.com", getCurrentWebView().getEngine(), BrowserEngine.getWebView());

        HBox navigationContainer = new HBox(backButton, forwardButton, refreshButton, urlField, goButton, newTabButton, bookmarksBar.getBookmarksContainer(), settingsButton, downloadsButton);
        navigationContainer.setSpacing(5);
        return navigationContainer;
    }

    private void openSettingsPage(BrowserEngine browserEngine) {
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

            // Debug Mode Toggle
            CheckBox debugModeCheckbox = new CheckBox("Enable Debug Mode");
            debugModeCheckbox.setSelected(Boolean.parseBoolean(System.getProperty("debug.mode", "false")));

            // Theme Selection
            ChoiceBox<String> themeChoiceBox = new ChoiceBox<>();
            themeChoiceBox.getItems().addAll("Light", "Dark", "System Default");
            themeChoiceBox.setValue(System.getProperty("browser.theme", "Light")); // Load saved theme

            // Font Size Selection
            ChoiceBox<String> fontSizeChoiceBox = new ChoiceBox<>();
            fontSizeChoiceBox.getItems().addAll("Small", "Medium", "Large");
            fontSizeChoiceBox.setValue(System.getProperty("browser.fontSize", "Medium")); // Load saved font size

            // Cookies Toggle
            CheckBox cookiesCheckbox = new CheckBox("Enable Cookies");
            cookiesCheckbox.setSelected(Boolean.parseBoolean(System.getProperty("browser.cookies", "true")));

            // Apply Button
            Button applyButton = new Button("Apply");
            applyButton.setOnAction(e -> {
                // Apply Debug Mode
                System.setProperty("debug.mode", String.valueOf(debugModeCheckbox.isSelected()));

                // Apply Theme
                String selectedTheme = themeChoiceBox.getValue().toLowerCase();
                browserEngine.setTheme(selectedTheme);
                System.setProperty("browser.theme", selectedTheme);

                // Apply Font Size
                String selectedFontSize = fontSizeChoiceBox.getValue().toLowerCase();
                browserEngine.setFontSize(selectedFontSize);
                System.setProperty("browser.fontSize", selectedFontSize);

                // Apply Cookies
                boolean cookiesEnabled = cookiesCheckbox.isSelected();
                browserEngine.setCookiesEnabled(cookiesEnabled);
                System.setProperty("browser.cookies", String.valueOf(cookiesEnabled));

                System.out.println("Settings applied: Debug Mode = " + debugModeCheckbox.isSelected() +
                        ", Theme = " + selectedTheme +
                        ", Font Size = " + selectedFontSize +
                        ", Cookies = " + cookiesEnabled);

                settingsStage.close();
            });

            settingsLayout.getChildren().addAll(
                    titleLabel,
                    debugModeCheckbox,
                    new Label("Theme:"), themeChoiceBox,
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

    private void createNewTab(String url) {
        Tab tab = new Tab("New Tab");

        // Create a new instance of BrowserEngine for this tab
        browserEngine = new BrowserEngine();
        WebView webView = browserEngine.getWebView();
        browserEngine.loadPage(url);

        tab.setContent(webView);
        tab.setOnClosed(e -> tabPane.getTabs().remove(tab));

        WebEngine webEngine = browserEngine.getWebEngine();

        // Update progress bar for this tab
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            progressBar.setProgress(newVal.doubleValue());
        });

        // Memory monitoring
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
                statusLabel.setText("Done: " + shortenUrl(webEngine.getLocation())); // Show shortened URL in status bar
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
            return url.substring(0, 37) + "..."; // Show only first 37 characters, add "..."
        }
        return url;
    }


    private void updateMemoryUsage(TabMemoryManager tabMemoryManager, Tooltip tooltip) {
        tabMemoryManager.updateMemoryUsage();
        tooltip.setText("Memory Usage: " + tabMemoryManager.getFormattedMemoryUsage());
    }

    private WebView getCurrentWebView() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getContent() instanceof WebView) {
            return (WebView) selectedTab.getContent();
        }
        return new WebView();
    }

    //TODO redo this
    public void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.setTitle("About Link Browser");

        // Browser Info Tile (Centered with bigger text)
        Tile browserInfoTile = TileBuilder.create()
                .skinType(Tile.SkinType.TEXT)
                .title("Link Browser")
                .text("Version: 2.1.0\nBuild: 2025.03.20\nLicense: MIT")
                .textSize(Tile.TextSize.BIGGER)
                .description("Modern web browsing experience.")
                .build();

        // Dependencies Tile (Well-formatted text)
        Tile dependenciesTile = TileBuilder.create()
                .skinType(Tile.SkinType.TEXT)
                .title("Dependencies Used")
                .text("• JavaFX\n• ControlsFX\n• FXGL")
                .textSize(Tile.TextSize.BIGGER)
                .description("Frameworks and libraries used in development.")
                .build();

        // Acknowledgements Tile (Bigger text for better readability)
        Tile acknowledgementsTile = TileBuilder.create()
                .skinType(Tile.SkinType.TEXT)
                .title("Acknowledgements")
                .text("Thanks to the JavaFX, ControlsFX, and FXGL communities!")
                .textSize(Tile.TextSize.BIGGER)
                .description("Community contributions that made this possible.")
                .build();

        // GitHub Link Tile (Clickable-looking text)
        Tile githubTile = TileBuilder.create()
                .skinType(Tile.SkinType.TEXT)
                .title("GitHub Repository")
                .text("Visit:\nhttps://github.com/YourRepository")
                .textSize(Tile.TextSize.BIGGER)
                .description("Open-source contributions & project updates.")
                .build();

        // CPU Usage Gauge
        Tile cpuTile = TileBuilder.create()
                .skinType(Tile.SkinType.GAUGE)
                .title("CPU Usage")
                .unit("%")
                .maxValue(100)
                .threshold(80)
                .textSize(Tile.TextSize.BIGGER)
                .build();

        // Memory Usage Gauge
        Tile memoryTile = TileBuilder.create()
                .skinType(Tile.SkinType.GAUGE)
                .title("Memory Usage")
                .unit("%")
                .maxValue(100)
                .threshold(80)
                .textSize(Tile.TextSize.BIGGER)
                .build();

        // Timeline to update CPU and Memory gauges every 500ms
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad() * 100; // Get CPU usage as a percentage
            double memoryUsage = ((double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().maxMemory()) * 100;

            Platform.runLater(() -> {
                cpuTile.setValue(cpuLoad);
                memoryTile.setValue(memoryUsage);
            });
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Close Button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 20px;");
        closeButton.setOnAction(e -> {
            timeline.stop(); // Stop updates when closing the window
            aboutStage.close();
        });

        VBox layout = new VBox(15, browserInfoTile, dependenciesTile, acknowledgementsTile, githubTile, cpuTile, memoryTile, closeButton);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 20;");

        Scene scene = new Scene(layout, 500, 750);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
