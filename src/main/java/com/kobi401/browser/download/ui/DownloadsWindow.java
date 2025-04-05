package com.kobi401.browser.download.ui;

import com.kobi401.browser.download.downloadManager.Download;
import com.kobi401.browser.download.downloadTask.DownloadTask;
import com.kobi401.browser.download.downloadManager.DownloadsManager;
import com.kobi401.browser.utils.debug.Debugger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

public class DownloadsWindow extends Stage {
    private ListView<HBox> currentDownloadsList;
    private ListView<String> completedDownloadsList;
    private DownloadsManager downloadsManager;

    public DownloadsWindow(DownloadsManager downloadsManager) {
        this.downloadsManager = downloadsManager;
        setTitle("Downloads Manager");

        currentDownloadsList = new ListView<>();
        completedDownloadsList = new ListView<>();

        refreshDownloads();

        Button debugDownloadButton = new Button("Debug: Download Test File");
        debugDownloadButton.setOnAction(e -> debugDownloadTestFile());

        VBox layout = new VBox(10,
                new Label("Current Downloads:"), currentDownloadsList,
                new Label("Completed Downloads:"), completedDownloadsList,
                debugDownloadButton);
        layout.setPadding(new Insets(10));

        setScene(new Scene(layout, 500, 400));
    }

    private void refreshDownloads() {
        Platform.runLater(() -> {
            currentDownloadsList.getItems().clear();
            completedDownloadsList.getItems().clear();

            downloadsManager.getCurrentDownloads().forEach(d -> {
                ProgressBar progressBar = new ProgressBar();
                progressBar.progressProperty().bind(d.progressProperty());

                Label fileNameLabel = new Label(d.getFileName());
                Label timeLabel = new Label();
                timeLabel.textProperty().bind(d.estimatedTimeProperty());

                Button pauseButton = new Button("Pause");
                Button resumeButton = new Button("Resume");
                Button cancelButton = new Button("Cancel");

                pauseButton.setOnAction(e -> d.pauseDownload());
                resumeButton.setOnAction(e -> d.resumeDownload());
                cancelButton.setOnAction(e -> {
                    d.cancelDownload();
                    refreshDownloads();
                });

                HBox downloadRow = new HBox(10, fileNameLabel, progressBar, timeLabel, pauseButton, resumeButton, cancelButton);
                currentDownloadsList.getItems().add(downloadRow);
            });

            downloadsManager.getCompletedDownloads().forEach(d ->
                    completedDownloadsList.getItems().add(d.getFileName() + " - Completed"));
        });
    }

    private void debugDownloadTestFile() {
        String testFileURL = "https://nbg1-speed.hetzner.com/100MB.bin";
        File saveFile = new File("test_download.bin");

        Download testDownload = new Download("Test File");
        downloadsManager.addDownload(testDownload);

        DownloadTask downloadTask = new DownloadTask(testDownload, testFileURL, saveFile);

        downloadTask.setOnSucceeded(event -> {
            downloadsManager.completeDownload(testDownload);
            refreshDownloads();
        });

        downloadTask.setOnFailed(event -> {
            Debugger.println("Download failed: " + downloadTask.getMessage());
            refreshDownloads();
        });

        new Thread(downloadTask).start();
        refreshDownloads();
    }
}
