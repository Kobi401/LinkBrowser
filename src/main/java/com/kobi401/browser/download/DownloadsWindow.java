package com.kobi401.browser.download;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DownloadsWindow extends Stage {
    public DownloadsWindow(DownloadsManager downloadsManager) {
        setTitle("Downloads");

        ListView<String> currentDownloadsList = new ListView<>();
        ListView<String> completedDownloadsList = new ListView<>();

        downloadsManager.getCurrentDownloads().forEach(d ->
                currentDownloadsList.getItems().add(d.getFileName() + " - " + (int) (d.getProgress() * 100) + "%"));

        downloadsManager.getCompletedDownloads().forEach(d ->
                completedDownloadsList.getItems().add(d.getFileName() + " - Completed"));

        VBox layout = new VBox(10,
                new Label("Current Downloads:"), currentDownloadsList,
                new Label("Completed Downloads:"), completedDownloadsList);

        setScene(new Scene(layout, 400, 300));
    }
}
