package com.kobi401.browser.utils.tracking;

import com.kobi401.browser.encryption.EncryptionUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.web.WebEngine;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class BrowserTracker {

    private static final String TRACKER_FILE_NAME = "LinkBrowser/User/browserTracker.dat";

    private final Set<String> visitedWebsites;
    private int warningsIssued;
    private Instant sessionStart;
    private Duration totalUsageTime;
    private final Timer timer;
    private final EncryptionUtils encryptionUtils;

    public BrowserTracker(WebEngine webEngine) {
        this.visitedWebsites = new HashSet<>();
        this.warningsIssued = 0;
        this.sessionStart = Instant.now();
        this.totalUsageTime = Duration.ZERO;
        this.timer = new Timer(true);
        this.encryptionUtils = new EncryptionUtils();
        loadTrackingData();
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue != null && !newValue.isEmpty()) {
                    visitedWebsites.add(newValue);
                    saveTrackingData();
                }
            }
        });

        startTrackingTime();
    }

    private void startTrackingTime() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                totalUsageTime = Duration.between(sessionStart, Instant.now());
                saveTrackingData();
            }
        }, 1000, 1000);
    }

    public void issueWarning() {
        warningsIssued++;
        saveTrackingData();
    }

    public int getWarningsIssued() {
        return warningsIssued;
    }

    public Set<String> getVisitedWebsites() {
        return visitedWebsites;
    }

    public Duration getTotalUsageTime() {
        return totalUsageTime;
    }

    public String getFormattedUsageTime() {
        long hours = totalUsageTime.toHours();
        long minutes = totalUsageTime.toMinutes() % 60;
        long seconds = totalUsageTime.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getBrowserStats() {
        return "Total Usage Time: " + getFormattedUsageTime() +
                "\nWebsites Visited: " + visitedWebsites.size() +
                "\nWarnings Issued: " + warningsIssued;
    }

    public void stopTracking() {
        timer.cancel();
        saveTrackingData();
    }

    private void saveTrackingData() {
        try {
            String data = warningsIssued + "\n" + totalUsageTime.toSeconds();
            for (String site : visitedWebsites) {
                data += "\n" + site;
            }
            encryptionUtils.saveToFile(data, TRACKER_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Error saving tracking data: " + e.getMessage());
        }
    }

    private void loadTrackingData() {
        try {
            File trackerFile = new File(System.getProperty("user.home"), TRACKER_FILE_NAME);
            if (!trackerFile.exists()) {
                return;
            }

            String decryptedData = encryptionUtils.loadFromFile(TRACKER_FILE_NAME);
            if (decryptedData == null || decryptedData.isEmpty()) {
                return;
            }

            String[] lines = decryptedData.split("\n");
            if (lines.length > 1) {
                warningsIssued = Integer.parseInt(lines[0]);
                totalUsageTime = Duration.ofSeconds(Long.parseLong(lines[1]));
            }

            for (int i = 2; i < lines.length; i++) {
                visitedWebsites.add(lines[i]);
            }
        } catch (Exception e) {
            System.err.println("Error loading tracking data: " + e.getMessage());
        }
    }

    public void clearTrackingData() {
        encryptionUtils.clearFile(TRACKER_FILE_NAME);
        visitedWebsites.clear();
        warningsIssued = 0;
        totalUsageTime = Duration.ZERO;
    }
}