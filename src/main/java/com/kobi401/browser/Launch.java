package com.kobi401.browser;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.kobi401.browser.ui.BrowserUI;
import com.kobi401.browser.ui.SplashScreen;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.util.Duration;

public class Launch extends GameApplication {

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
        settings.setDeveloperMenuEnabled(isDebugMode);
        System.out.println("Debug mode: " + (isDebugMode ? "Enabled" : "Disabled"));

        String osName = System.getProperty("os.name").toLowerCase();
        settings.setWidth(getWidthForOS(osName));
        settings.setHeight(getHeightForOS(osName));
        settings.setFullScreenAllowed(!osName.contains("android"));
    }

    private int getWidthForOS(String osName) {
        if (osName.contains("android")) return 480;
        if (osName.contains("ios")) return 640;
        if (osName.contains("linux")) return 1280;
        return 1024;
    }

    private int getHeightForOS(String osName) {
        if (osName.contains("android")) return 800;
        if (osName.contains("ios")) return 1136;
        if (osName.contains("linux")) return 720;
        return 768;
    }

    @Override
    protected void initGame() {
        Platform.runLater(() -> FXGL.getPrimaryStage().hide());
        FXGL.runOnce(this::showMainStage, Duration.seconds(3));
    }

    @Override
    protected void onPreInit() {
        System.setProperty("prism.maxvram", "8G");
        new SplashScreen(this::showMainStage).show();
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