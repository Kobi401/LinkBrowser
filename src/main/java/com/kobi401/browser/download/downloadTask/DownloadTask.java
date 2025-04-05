package com.kobi401.browser.download.downloadTask;

import com.kobi401.browser.download.downloadManager.Download;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadTask extends Task<Void> {
    private final Download download;
    private final String fileUrl;
    private final File destinationFile;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public DownloadTask(Download download, String fileUrl, File destinationFile) {
        this.download = download;
        this.fileUrl = fileUrl;
        this.destinationFile = destinationFile;
    }

    @Override
    protected Void call() {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");

            String cookies = "cookie1=value1; cookie2=value2";
            connection.setRequestProperty("Cookie", cookies);

            connection.connect();

            //handle redirect if necessary
            if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                connection.connect();
            }

            long totalSize = connection.getContentLengthLong();
            if (totalSize > 10L * 1024 * 1024 * 1024) {
                updateMessage("File too large!");
                return null;
            }

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int bytesRead;
                long startTime = System.nanoTime();

                while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
                    if (cancelled.get()) {
                        updateMessage("Download Cancelled");
                        return null;
                    }

                    while (paused.get()) {
                        updateMessage("Paused...");
                        Thread.sleep(500);
                    }

                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    double progress = (double) downloaded / totalSize;

                    Platform.runLater(() -> {
                        download.setProgress(progress);
                    });

                    long elapsedTime = System.nanoTime() - startTime;
                    double speed = downloaded / (elapsedTime / 1e9); // bytes per second
                    long remainingTime = (long) ((totalSize - downloaded) / speed);
                    String formattedTime = formatTime(remainingTime);

                    Platform.runLater(() -> {
                        download.setEstimatedTime(formattedTime);
                    });

                    updateProgress(progress, 1);
                }

                Platform.runLater(() -> {
                    download.setProgress(1.0);
                    download.setEstimatedTime("Completed");
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> updateMessage("Error: " + e.getMessage()));
        }
        return null;
    }

    public void pauseDownload() {
        paused.set(true);
    }

    public void resumeDownload() {
        paused.set(false);
    }

    public void cancelDownload() {
        cancelled.set(true);
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "Calculating...";
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d remaining", mins, secs);
    }
}
