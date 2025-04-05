package com.kobi401.browser.jsbridge.webGL;

import com.kobi401.browser.utils.debug.Debugger;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//TODO Finish this and fix it not working
public class WebGL {
    private final WebEngine webEngine;
    private static final String WEBGL_DIRECTORY = System.getProperty("user.home") + "/LinkBrowser/WebGL";
    private static final String WEBGL_URL = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js";
    private boolean webGLInjected = false;

    public WebGL(WebEngine engine) {
        this.webEngine = engine;
        File directory = new File(WEBGL_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Downloads the WebGL library to the user's local directory.
     */
    public void downloadWebGLLibrary() {
        try {
            Path filePath = Paths.get(WEBGL_DIRECTORY, "three.min.js");
            if (Files.exists(filePath)) {
                Debugger.println("WebGL library already exists. Skipping download.");
                return;
            }

            URL url = new URL(WEBGL_URL);
            try (InputStream inputStream = url.openStream();
                 OutputStream outputStream = new FileOutputStream(filePath.toFile())) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                Debugger.println("WebGL library downloaded successfully.");
            }
        } catch (IOException e) {
            Debugger.println("Error downloading WebGL library: " + e.getMessage());
        }
    }

    /**
     * Loads the WebGL library into the WebEngine.
     */
    public void loadWebGLLibrary() {
        try {
            Path filePath = Paths.get(WEBGL_DIRECTORY, "three.min.js");
            if (Files.exists(filePath)) {
                String script = new String(Files.readAllBytes(filePath));
                webEngine.executeScript(script);
                Debugger.println("WebGL library loaded successfully.");
            } else {
                Debugger.println("WebGL library not found. Please download it first.");
            }
        } catch (IOException e) {
            Debugger.println("Error loading WebGL library: " + e.getMessage());
        }
    }

    /**
     * Injects WebGL content if WebGL is detected in the current page.
     */
    public void injectWebGLIfNeeded() {
        if (!webGLInjected) {
            webGLInjected = true;
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");
                    if (htmlContent != null && htmlContent.contains("webgl")) {
                        loadWebGLLibrary();
                        Debugger.println("WebGL content detected. Injecting WebGL library...");
                    }
                }
            });
        }
    }
}
