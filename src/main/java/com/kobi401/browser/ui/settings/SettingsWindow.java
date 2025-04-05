package com.kobi401.browser.ui.settings;

import com.kobi401.browser.engine.main.BrowserEngine;
import com.kobi401.browser.utils.debug.Debugger;
import com.kobi401.browser.utils.tracking.BrowserTracker;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class SettingsWindow {

    private final BrowserEngine browserEngine;
    private final WebView webView;
    private final BrowserTracker browserTracker;

    public SettingsWindow(BrowserEngine browserEngine, WebView webView, BrowserTracker browserTracker) {
        this.browserEngine = browserEngine;
        this.webView = webView;
        this.browserTracker = browserTracker;
    }

    public void open() {
        Platform.runLater(() -> {
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.setWidth(400);
            settingsStage.setHeight(550);  // Increased height to accommodate tracking stats
            settingsStage.setResizable(false);

            VBox settingsLayout = new VBox(15);
            settingsLayout.setPadding(new Insets(20));
            settingsLayout.setAlignment(Pos.TOP_LEFT);

            Label titleLabel = new Label("Link Settings");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            CheckBox debugModeCheckbox = new CheckBox("Enable Debug Mode");
            debugModeCheckbox.setSelected(Boolean.parseBoolean(System.getProperty("debug.mode", "false")));

            ChoiceBox<String> fontSizeChoiceBox = new ChoiceBox<>();
            fontSizeChoiceBox.getItems().addAll("Small", "Medium", "Large");
            fontSizeChoiceBox.setValue(System.getProperty("browser.fontSize", "Medium"));

            CheckBox cookiesCheckbox = new CheckBox("Enable Cookies");
            cookiesCheckbox.setSelected(Boolean.parseBoolean(System.getProperty("browser.cookies", "true")));

            Button clearCacheButton = new Button("Clear Cache");
            clearCacheButton.setOnAction(e -> clearCache());

            Button clearCookiesButton = new Button("Clear Cookies");
            clearCookiesButton.setOnAction(e -> clearCookies());

            Button deleteLinkDirButton = new Button("Delete Link Directory (requires restart)");
            deleteLinkDirButton.setOnAction(e -> deleteLinkDirectory());

            Label statsLabel = new Label("Tracking Stats:");
            statsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            TextArea statsTextArea = new TextArea();
            statsTextArea.setEditable(false);
            statsTextArea.setPrefHeight(100);
            statsTextArea.setText(getTrackingStats());

            Button clearStatsButton = new Button("Clear Tracking Stats");
            clearStatsButton.setOnAction(e -> clearTrackingStats(statsTextArea));

            Button applyButton = new Button("Apply");
            applyButton.setOnAction(e -> applySettings(debugModeCheckbox, fontSizeChoiceBox, cookiesCheckbox, settingsStage));

            settingsLayout.getChildren().addAll(
                    titleLabel,
                    debugModeCheckbox,
                    new Label("Font Size:"), fontSizeChoiceBox,
                    cookiesCheckbox,
                    clearCacheButton,
                    clearCookiesButton,
                    deleteLinkDirButton,
                    statsLabel,
                    statsTextArea,
                    clearStatsButton,
                    applyButton
            );

            Scene scene = new Scene(settingsLayout);
            settingsStage.setScene(scene);
            settingsStage.show();
        });
    }

    private void applySettings(CheckBox debugModeCheckbox, ChoiceBox<String> fontSizeChoiceBox, CheckBox cookiesCheckbox, Stage settingsStage) {
        System.setProperty("debug.mode", String.valueOf(debugModeCheckbox.isSelected()));
        String selectedFontSize = fontSizeChoiceBox.getValue().toLowerCase();
        browserEngine.setFontSize(selectedFontSize);
        System.setProperty("browser.fontSize", selectedFontSize);

        boolean cookiesEnabled = cookiesCheckbox.isSelected();
        browserEngine.setCookiesEnabled(cookiesEnabled);
        System.setProperty("browser.cookies", String.valueOf(cookiesEnabled));

        Debugger.println("Settings applied: Debug Mode = " + debugModeCheckbox.isSelected() +
                ", Font Size = " + selectedFontSize +
                ", Cookies = " + cookiesEnabled);

        showAlert(Alert.AlertType.INFORMATION, "Settings Applied", "Your changes have been saved.");
        settingsStage.close();
    }

    private void clearCache() {
        try {
            File cacheDir = new File(System.getProperty("user.home") + File.separator + ".LinkBrowser" + File.separator + "webview" + File.separator + "localstorage");
            if (cacheDir.exists()) {
                deleteDirectory(cacheDir);
                showAlert(Alert.AlertType.INFORMATION, "Cache Cleared", "Browser cache has been successfully cleared.");
            } else {
                showAlert(Alert.AlertType.WARNING, "No Cache Found", "There is no cache to clear.");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Cache Clearing Error", "Error clearing cache: " + e.getMessage());
        }
    }

    private void clearCookies() {
        try {
            WebEngine webEngine = browserEngine.getWebEngine();
            if (webEngine != null) {
                webEngine.getLoadWorker().cancel(); // Cancel any running loads
                showAlert(Alert.AlertType.INFORMATION, "Cookies Cleared", "All stored cookies have been cleared.");
            } else {
                showAlert(Alert.AlertType.WARNING, "No WebEngine", "Unable to find the WebEngine to clear cookies.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Cookie Clearing Error", "Error clearing cookies: " + e.getMessage());
        }
    }

    private void deleteLinkDirectory() {
        try {
            File linkDir = new File(System.getProperty("user.home") + File.separator + "LinkBrowser");
            if (linkDir.exists()) {
                deleteDirectory(linkDir);
                showAlert(Alert.AlertType.INFORMATION, "Directory Deleted", "The LinkBrowser directory has been deleted. Restart required.");
            } else {
                showAlert(Alert.AlertType.WARNING, "No Directory Found", "No LinkBrowser directory was found to delete.");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Deletion Error", "Error deleting LinkBrowser directory: " + e.getMessage());
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete: " + directory);
        }
    }

    private String getTrackingStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Total Usage Time: ").append(browserTracker.getFormattedUsageTime()).append("\n")
                .append("Websites Visited: ").append(browserTracker.getVisitedWebsites().size()).append("\n")
                .append("Warnings Issued: ").append(browserTracker.getWarningsIssued()).append("\n");
        return stats.toString();
    }

    private void clearTrackingStats(TextArea statsTextArea) {
        browserTracker.clearTrackingData();
        statsTextArea.setText(getTrackingStats());  // Refresh the stats display
        showAlert(Alert.AlertType.INFORMATION, "Tracking Stats Cleared", "Tracking stats have been cleared.");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}