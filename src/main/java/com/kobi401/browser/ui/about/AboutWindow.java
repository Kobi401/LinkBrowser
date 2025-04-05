package com.kobi401.browser.ui.about;

import com.kobi401.browser.utils.update.UpdateChecker;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import com.kobi401.browser.utils.application.AppInfo;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import java.io.File;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class AboutWindow {

    public static void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.setTitle("About Link Browser");

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setStyle("-fx-background-color: #C0C0C0; -fx-border-color: #808080; -fx-border-width: 2px; -fx-effect: innershadow(gaussian, #A0A0A0, 4, 0, 2, 2);");

        AppInfo appInfo = AppInfo.createDefaultAppInfo();

        ImageView logoView = new ImageView(new Image(
                AboutWindow.class.getResourceAsStream("/Images/LinkLogo_Big.png")
        ));
        logoView.setFitWidth(80);
        logoView.setFitHeight(80);

        Label browserName = new Label("Link Browser");
        browserName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: black; -fx-font-family: 'Verdana';");

        Label versionLabel = new Label("Version " + appInfo.getVersion() + " (Build " + appInfo.getBuild() + ")");
        versionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black; -fx-font-family: 'Tahoma';");

        Label updateStatus = new Label("Update status: Idle");
        updateStatus.setStyle("-fx-font-size: 13px; -fx-text-fill: black; -fx-font-family: 'Tahoma';");

        Button checkUpdateButton = createButton("Check for Updates");
        Button downloadUpdateButton = createButton("Download Update");
        downloadUpdateButton.setDisable(true);

        ProgressBar updateProgress = new ProgressBar(0);
        updateProgress.setPrefWidth(180);
        updateProgress.setVisible(false);

        VBox updateBox = new VBox(5, versionLabel, updateStatus, checkUpdateButton, downloadUpdateButton, updateProgress);
        updateBox.setAlignment(Pos.CENTER);
        updateBox.setStyle("-fx-border-color: #808080; -fx-border-width: 1px; -fx-padding: 10px; -fx-background-color: #D0D0D0;");

        checkUpdateButton.setOnAction(e -> {
            updateStatus.setText("Checking for updates...");
            new Thread(() -> {
                boolean updateAvailable = UpdateChecker.isUpdateAvailable(appInfo.getVersion());

                Platform.runLater(() -> {
                    if (updateAvailable) {
                        updateStatus.setText("Update available!");
                        downloadUpdateButton.setDisable(false);
                    } else {
                        updateStatus.setText("No updates available.");
                    }
                });
            }).start();
        });

        downloadUpdateButton.setOnAction(e -> {
            updateStatus.setText("Downloading update...");
            updateProgress.setVisible(true);
            updateProgress.setProgress(-1);

            new Thread(() -> {
                File currentJar = new File("Browser.jar");
                UpdateChecker.downloadAndReplaceJar(currentJar);

                Platform.runLater(() -> {
                    updateStatus.setText("Update complete. Please relaunch.");
                    updateProgress.setProgress(1);
                });
            }).start();
        });

        Tile cpuTile = TileBuilder.create()
                .skinType(Tile.SkinType.GAUGE)
                .title("CPU Usage")
                .unit("%")
                .maxValue(100)
                .threshold(80)
                .textSize(Tile.TextSize.BIGGER)
                .backgroundColor(Color.web("#D0D0D0"))
                .foregroundBaseColor(Color.BLACK)
                .build();

        Tile memoryTile = TileBuilder.create()
                .skinType(Tile.SkinType.GAUGE)
                .title("Memory Usage")
                .unit("%")
                .maxValue(100)
                .threshold(80)
                .textSize(Tile.TextSize.BIGGER)
                .backgroundColor(Color.web("#D0D0D0"))
                .foregroundBaseColor(Color.BLACK)
                .build();

        HBox systemStatsBox = new HBox(15, cpuTile, memoryTile);
        systemStatsBox.setAlignment(Pos.CENTER);
        systemStatsBox.setStyle("-fx-border-color: #808080; -fx-border-width: 1px; -fx-padding: 10px; -fx-background-color: #D0D0D0;");

        Timeline statsUpdate = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad() * 100;
            double memoryUsage = ((double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().maxMemory()) * 100;

            Platform.runLater(() -> {
                cpuTile.setValue(cpuLoad);
                memoryTile.setValue(memoryUsage);
            });
        }));
        statsUpdate.setCycleCount(Timeline.INDEFINITE);
        statsUpdate.play();

        Label librariesLabel = new Label("Libraries Used:");
        librariesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: black; -fx-font-family: 'Verdana';");

        VBox librariesList = new VBox(5);
        librariesList.setPadding(new Insets(5));
        librariesList.setStyle("-fx-background-color: #D0D0D0;");

        String[][] libraries = {
                {"JavaFX", "GUI framework for Java applications", "Oracle"},
                {"FXGL", "JavaFX-based game engine", "Almas Baimagambetov"},
                {"TilesFX", "Dashboard and tile UI components", "Gerrit Grunwald"},
                {"ControlsFX", "Advanced UI controls for JavaFX", "FX Experience Team"}
        };

        for (String[] lib : libraries) {
            Label nameLabel = new Label(lib[0]);
            nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: black; -fx-font-family: 'Tahoma';");

            Label descLabel = new Label(lib[1]);
            descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: black; -fx-font-family: 'Tahoma';");

            Label creatorLabel = new Label("By " + lib[2]);
            creatorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray; -fx-font-family: 'Tahoma';");

            VBox libraryEntry = new VBox(2, nameLabel, descLabel, creatorLabel);
            libraryEntry.setStyle("-fx-padding: 5px; -fx-border-color: #808080; -fx-border-width: 1px; -fx-background-color: #E0E0E0;");

            librariesList.getChildren().add(libraryEntry);
        }

        ScrollPane librariesScroll = new ScrollPane(librariesList);
        librariesScroll.setFitToWidth(true);
        librariesScroll.setPrefHeight(150);

        VBox librariesBox = new VBox(5, librariesLabel, librariesScroll);
        librariesBox.setAlignment(Pos.CENTER);
        librariesBox.setStyle("-fx-border-color: #808080; -fx-border-width: 1px; -fx-padding: 10px; -fx-background-color: #D0D0D0;");

        Button closeButton = createButton("Close");
        closeButton.setOnAction(e -> {
            statsUpdate.stop();
            aboutStage.close();
        });

        VBox headerBox = new VBox(logoView, browserName, updateBox);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setSpacing(10);

        VBox footerBox = new VBox(systemStatsBox, librariesBox, closeButton);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setSpacing(15);

        mainLayout.getChildren().addAll(headerBox, footerBox);

        Scene scene = new Scene(mainLayout, 500, 650);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
    }

    private static Button createButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 13px; -fx-background-color: #D0D0D0; -fx-border-color: #808080; -fx-border-width: 1px; -fx-padding: 5px 15px;");
        return button;
    }
}