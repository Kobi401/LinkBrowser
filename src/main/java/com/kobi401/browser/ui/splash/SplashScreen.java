package com.kobi401.browser.ui.splash;

import com.kobi401.browser.utils.application.AppInfo;
import com.kobi401.browser.utils.update.UpdateChecker;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class SplashScreen {

    private final Stage splashStage;
    private final Label pluginStatusLabel;
    private final AppInfo appInfo;
    private final Runnable onComplete;

    public SplashScreen(Runnable onComplete) {
        this.onComplete = onComplete;
        this.splashStage = new Stage();
        this.splashStage.initStyle(StageStyle.TRANSPARENT);

        this.appInfo = AppInfo.createDefaultAppInfo();

        this.pluginStatusLabel = new Label("Loading FXGL Engine...");
        setupUI();

        checkForUpdates();
    }

    private void checkForUpdates() {
        new Thread(() -> {
            Platform.runLater(() -> pluginStatusLabel.setText("Checking for updates..."));
            String currentVersion = appInfo.getVersion();
            String latestVersion = UpdateChecker.getLatestVersion();
            if (latestVersion != null) {
                int versionComparison = UpdateChecker.compareVersionsInteger(currentVersion, latestVersion);
                if (versionComparison > 0) {
                    Platform.runLater(() -> {
                        pluginStatusLabel.setText("Unstable build detected! (v" + currentVersion + ")");
                        pluginStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff8000;");
                        startFadeOutTimer();
                    });
                } else if (versionComparison < 0) {
                    Platform.runLater(() -> {
                        pluginStatusLabel.setText("An update is available! (v" + latestVersion + ")");
                        pluginStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff0000;");
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Update Available");
                        alert.setHeaderText("A new version (v" + latestVersion + ") is available!");
                        alert.setContentText("Would you like to update now?");
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            pluginStatusLabel.setText("Downloading update...");
                            new Thread(() -> {
                                UpdateChecker.downloadAndReplaceJar(new File("Browser.jar"));
                                Platform.runLater(() -> {
                                    pluginStatusLabel.setText("Update complete! Please restart Link.");
                                    showRestartAlert();
                                });
                            }).start();
                        } else {
                            startFadeOutTimer();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        pluginStatusLabel.setText("No updates found. Launching...");
                        startFadeOutTimer();
                    });
                }
            } else {
                Platform.runLater(() -> {
                    pluginStatusLabel.setText("Failed to check for updates.");
                    startFadeOutTimer();
                });
            }
        }).start();
    }

    private void showRestartAlert() {
        Alert restartAlert = new Alert(Alert.AlertType.INFORMATION);
        restartAlert.setTitle("Update Complete");
        restartAlert.setHeaderText(null);
        restartAlert.setContentText("Update completed! Please restart Link.");
        restartAlert.showAndWait();
    }

    private void setupUI() {
        StackPane root = new StackPane();
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #f5f5f5, #d1d1d1);" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 10);"
        );

        VBox splashContent = new VBox(25);
        splashContent.setAlignment(Pos.CENTER);
        splashContent.setPadding(new Insets(40));

        ImageView logo = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Images/LinkLogo_Big.png"))));
        logo.setFitWidth(150);
        logo.setPreserveRatio(true);

        Label welcomeLabel = new Label("Welcome to Link");
        welcomeLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif; " +
                        "-fx-font-size: 28px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: #333333; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0, 0, 2);"
        );

        Label versionLabel = new Label("Version " + appInfo.getFormattedVersion());
        versionLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif; " +
                        "-fx-font-size: 16px; " +
                        "-fx-text-fill: #777777;"
        );

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.setStyle("-fx-accent: #0078d4;");

        pluginStatusLabel.setText("Initializing...");
        pluginStatusLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #555555;"
        );

        Label devLabel = new Label("Developed by Kobi401");
        devLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #888888;"
        );

        splashContent.getChildren().addAll(logo, welcomeLabel, versionLabel, progressIndicator, pluginStatusLabel, devLabel);
        root.getChildren().add(splashContent);

        Scene splashScene = new Scene(root, 420, 520);
        splashScene.setFill(null);
        splashStage.setScene(splashScene);
        splashStage.setTitle("Loading...");

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    public void show() {
        splashStage.show();
    }

    private void startFadeOutTimer() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashStage.getScene().getRoot());
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    splashStage.close();
                    onComplete.run();
                });
                fadeOut.play();
            });
        }).start();
    }
}
