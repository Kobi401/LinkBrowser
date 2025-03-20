package com.kobi401.browser.download;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DownloadsManager {
    private ObservableList<Download> currentDownloads;
    private ObservableList<Download> completedDownloads;

    public DownloadsManager() {
        currentDownloads = FXCollections.observableArrayList();
        completedDownloads = FXCollections.observableArrayList();
    }

    public void addDownload(Download download) {
        currentDownloads.add(download);
    }

    public void completeDownload(Download download) {
        currentDownloads.remove(download);
        completedDownloads.add(download);
    }

    public ObservableList<Download> getCurrentDownloads() {
        return currentDownloads;
    }

    public ObservableList<Download> getCompletedDownloads() {
        return completedDownloads;
    }

    public void updateDownloadProgress(Download download, double progress, String estimatedTime) {
        download.setProgress(progress);
        download.setEstimatedTime(estimatedTime);
    }
}

