package com.kobi401.browser;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.kobi401.browser.ui.GUI.BrowserUI;
import com.kobi401.browser.ui.splash.SplashScreen;
import com.kobi401.browser.utils.application.AppInfo;
import com.kobi401.browser.utils.debug.Debugger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.util.Duration;

// Since Link uses FXGL now for the engine it should allow running on anything with a screen!

public class Launch extends GameApplication {

    private BrowserUI browserUI;

    @Override
    protected void initSettings(GameSettings settings) {
        AppInfo appInfo = AppInfo.createDefaultAppInfo();
        settings.setTitle("Link Browser");
        settings.setVersion(appInfo.getVersion());
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
        settings.setManualResizeEnabled(true);

        boolean isDebugMode = Boolean.parseBoolean(System.getProperty("debug.mode", "false"));

        settings.setDeveloperMenuEnabled(isDebugMode);
        Debugger.enableDebugging(isDebugMode);
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
        FXGL.runOnce(this::showMainStage, Duration.seconds(5));
    }

    @Override
    protected void onPreInit() {
        System.setProperty("prism.maxvram", "8G");
        System.setProperty("prism.verbose", "false");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.dirtyopts", "true");
        System.setProperty("javafx.gpu", "true");
        System.setProperty("prism.graphics", "OpenGL");
        System.setProperty("prism.forceGPU", "true");

        new SplashScreen(this::showMainStage).show();
    }


    private void showMainStage() {
        browserUI = new BrowserUI();
        Scene scene = new Scene(browserUI.getRoot(), 800, 600);
        //scene.getStylesheets().add(getClass().getResource("/css/cupertino-dark.css").toExternalForm()); //Works but tabs look funny
        Platform.runLater(() -> {
            FXGL.getPrimaryStage().setScene(scene);
            FXGL.getPrimaryStage().show();
        });
    }


    public BrowserUI getbrowserUI() {
        return browserUI;
    }

    public static void main(String[] args) {
        launch(args);
    }
}