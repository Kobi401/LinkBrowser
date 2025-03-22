package com.kobi401.browser.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final String REPO_URL = "https://github.com/Kobi401/LinkBrowser/releases/tag/Stable";
    private static final String DOWNLOAD_URL = "https://github.com/Kobi401/LinkBrowser/releases/download/Stable/Browser.jar";
    private static final String VERSION_REGEX = "Stable\\s(\\d+\\.\\d+\\.\\d+)";

    public static String getLatestVersion() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(REPO_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder response = new StringBuilder();

                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                Pattern pattern = Pattern.compile(VERSION_REGEX);
                Matcher matcher = pattern.matcher(response.toString());

                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        } catch (IOException e) {
            System.err.println("Error checking for updates: " + e.getMessage());
        }
        return null;
    }

    public static boolean isUpdateAvailable(String currentVersion) {
        String latestVersion = getLatestVersion();
        if (latestVersion == null) return false;
        return compareVersions(currentVersion, latestVersion);
    }

    public static boolean compareVersions(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
            int currentVal = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;
            int latestVal = (i < latestParts.length) ? Integer.parseInt(latestParts[i]) : 0;

            if (currentVal < latestVal) return true;
            if (currentVal > latestVal) return false;
        }
        return false;
    }

    public static void downloadAndReplaceJar(File jarFile) {
        try {
            Platform.runLater(() -> showAlert("Updating...", "Downloading the latest version..."));
            File tempFile = new File(jarFile.getParent(), "Browser.jar");
            HttpURLConnection connection = (HttpURLConnection) new URL(DOWNLOAD_URL).openConnection();
            connection.setRequestMethod("GET");

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            Platform.runLater(() -> showAlert("Update Complete", "Replacing old version..."));

            Files.move(tempFile.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Platform.runLater(() -> {
                try {
                    relaunchLink(jarFile);
                } catch (IOException e) {
                    showAlert("Relaunch Failed", "Failed to relaunch Link: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Platform.runLater(() -> showAlert("Update Failed", "An error occurred: " + e.getMessage()));
        }
    }

    private static void relaunchLink(File jarFile) throws IOException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        ProcessBuilder processBuilder;
        // For Windows
        if (os.contains("win")) {
            processBuilder = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());
        }
        // For Linux or Mac
        else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            String javaPath = getJavaExecutablePath();
            if (javaPath == null) {
                javaPath = "java";
            }

            processBuilder = new ProcessBuilder(javaPath, "-jar", jarFile.getAbsolutePath());
        }
        else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        processBuilder.start();
        Platform.exit();
    }

    private static String getJavaExecutablePath() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && os.contains("nix")) {
            File javaExecutable = new File(javaHome, "bin/java");
            if (javaExecutable.exists()) {
                return javaExecutable.getAbsolutePath();
            }
        }
        try {
            Process process = new ProcessBuilder("which", "java").start();
            process.waitFor();
            File outputFile = new File("/tmp/java-path.txt");
            process = new ProcessBuilder("sh", "-c", "which java > /tmp/java-path.txt").start();
            process.waitFor();
            String javaPath = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
            if (javaPath != null && !javaPath.isEmpty()) {
                return javaPath.trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
