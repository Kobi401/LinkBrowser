package com.kobi401.browser.download;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

public class Download {
    private StringProperty fileName;
    private DoubleProperty progress;
    private StringProperty estimatedTime;
    private boolean isPaused;
    private boolean isCancelled;

    public Download(String fileName) {
        this.fileName = new SimpleStringProperty(fileName);
        this.progress = new SimpleDoubleProperty(0);
        this.estimatedTime = new SimpleStringProperty("Calculating...");
        this.isPaused = false;
        this.isCancelled = false;
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double progress) {
        if (!isPaused && !isCancelled) {
            this.progress.set(progress);
        }
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public String getEstimatedTime() {
        return estimatedTime.get();
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime.set(estimatedTime);
    }

    public StringProperty estimatedTimeProperty() {
        return estimatedTime;
    }

    public void pauseDownload() {
        isPaused = true;
        estimatedTime.set("Paused");
    }

    public void resumeDownload() {
        if (isPaused) {
            isPaused = false;
            estimatedTime.set("Resuming...");
        }
    }

    public void cancelDownload() {
        isCancelled = true;
        progress.set(0);
        estimatedTime.set("Cancelled");
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}