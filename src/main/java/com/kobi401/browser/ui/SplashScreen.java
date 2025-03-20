package com.kobi401.browser.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Locale;
import java.util.Objects;

public class SplashScreen {

    private final Stage splashStage;
    private final Label pluginStatusLabel;
    private final String buildType;
    private final String detectedOS;
    private final Runnable onComplete;

    public SplashScreen(Runnable onComplete) {
        this.onComplete = onComplete;
        this.splashStage = new Stage();
        this.splashStage.initStyle(StageStyle.TRANSPARENT);

        String osName = System.getProperty("os.name");
        this.detectedOS = osName;
        this.buildType = System.getProperty("build.type", "STABLE").toUpperCase(Locale.ROOT);

        this.pluginStatusLabel = new Label("Loading FXGL Engine...");
        setupUI();
    }

    private void setupUI() {
        StackPane root = new StackPane();
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #e0e0e0);" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 2);"
        );

        VBox splashContent = new VBox(20);
        splashContent.setAlignment(Pos.CENTER);
        splashContent.setPadding(new Insets(30));

        ImageView logo = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Images/LinkLogo_Big.png"))));
        logo.setFitWidth(120);
        logo.setPreserveRatio(true);

        Label welcomeLabel = new Label("Welcome to Link");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        Label versionLabel = new Label("Version 2.1-" + detectedOS + " (" + buildType + ")");
        versionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setPrefSize(50, 50);

        pluginStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        Label devLabel = new Label("Developed by Kobi401");
        devLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        splashContent.getChildren().addAll(logo, welcomeLabel, versionLabel, progressIndicator, pluginStatusLabel, devLabel);
        root.getChildren().add(splashContent);

        Scene splashScene = new Scene(root, 400, 500);
        splashScene.setFill(null);
        splashStage.setScene(splashScene);
        splashStage.setTitle("Loading...");

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> startFadeOutTimer());
        fadeIn.play();
    }

    public void show() {
        splashStage.show();
    }

    private void startFadeOutTimer() {
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
    }
}
