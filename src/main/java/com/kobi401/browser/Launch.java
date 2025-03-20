package com.kobi401.browser;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.kobi401.browser.ui.BrowserUI;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;

import java.util.Locale;
import java.util.Objects;

public class Launch extends GameApplication {

    private Stage splashStage;
    private Label pluginStatusLabel;
    private String buildType;
    private String detectedOS;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Link Browser");
        settings.setVersion("2.1");
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
        settings.setManualResizeEnabled(true);

        boolean isDebugMode = Boolean.parseBoolean(System.getProperty("debug.mode", "false"));

        if (isDebugMode) {
            settings.setDeveloperMenuEnabled(true);
            System.out.println("Debug mode enabled.");
        } else {
            System.out.println("Debug mode not enabled.");
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("android")) {
            System.out.println("Running on Android");
            settings.setWidth(480);
            settings.setHeight(800);
            settings.setFullScreenAllowed(false);
        } else if (osName.contains("ios")) {
            System.out.println("Running on iOS");
            settings.setWidth(640);
            settings.setHeight(1136);
            settings.setFullScreenAllowed(true);
        } else if (osName.contains("linux")) {
            System.out.println("Running on Linux");
            settings.setWidth(1280);
            settings.setHeight(720);
            settings.setFullScreenAllowed(true);
        } else {
            System.out.println("Running on Desktop (Windows/Mac or others)");
            settings.setWidth(1024);
            settings.setHeight(768);
            settings.setFullScreenAllowed(true);
        }
    }


    @Override
    protected void initGame() {
        Platform.runLater(() -> FXGL.getPrimaryStage().hide());
        FXGL.runOnce(this::fadeOutSplashScreen, Duration.seconds(3));
    }

    @Override
    protected void onPreInit() {
        System.setProperty("prism.maxvram", "8G");
        buildType = System.getProperty("build.type", "STABLE").toUpperCase(Locale.ROOT);
        showSplashScreen();
    }

    private void showSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);

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

        String osName = System.getProperty("os.name");
        this.detectedOS = osName;

        Label versionLabel = new Label("Version 2.1-" + osName + " (" + buildType + ")");
        versionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setPrefSize(50, 50);

        pluginStatusLabel = new Label("Loading FXGL Engine...");
        pluginStatusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        Label devLabel = new Label("Developed by Kobi401");
        devLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        splashContent.getChildren().addAll(logo, welcomeLabel, versionLabel, progressIndicator, pluginStatusLabel, devLabel);
        root.getChildren().add(splashContent);

        Scene splashScene = new Scene(root, 400, 500);
        splashScene.setFill(null);

        splashStage.setScene(splashScene);
        splashStage.setTitle("Loading...");
        splashStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void fadeOutSplashScreen() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashStage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            splashStage.close();
            showMainStage();
        });
        fadeOut.play();
    }

    private void showMainStage() {
        BrowserUI browserUI = new BrowserUI();
        Scene scene = new Scene(browserUI.getRoot(), 800, 600);
        Platform.runLater(() -> {
            FXGL.getPrimaryStage().setScene(scene);
            FXGL.getPrimaryStage().show();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
