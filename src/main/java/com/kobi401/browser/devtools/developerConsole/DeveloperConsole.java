package com.kobi401.browser.devtools.developerConsole;

import com.kobi401.browser.ui.GUI.BrowserUI;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

public class DeveloperConsole {
    private final Stage consoleStage;
    private final TextArea outputArea;
    private final TextArea inputArea;
    private final WebView webView;
    private final WebEngine webEngine;

    public DeveloperConsole(BrowserUI browserUI) {
        this.webView = browserUI.getCurrentWebView();
        this.webEngine = webView.getEngine();
        consoleStage = new Stage();
        consoleStage.setTitle("Developer Console");
        consoleStage.setWidth(800);
        consoleStage.setHeight(500);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");
        outputArea.setPrefHeight(300);

        inputArea = new TextArea();
        inputArea.setPromptText("Enter JavaScript here...");
        inputArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");
        inputArea.setPrefHeight(50);
        inputArea.setWrapText(true);

        Button runButton = new Button("Run JS");
        runButton.setOnAction(e -> executeJavaScript());

        Button injectScriptButton = new Button("Inject Script");
        injectScriptButton.setOnAction(e -> injectScript());

        Button clearButton = new Button("Clear Console");
        clearButton.setOnAction(e -> outputArea.clear());

        HBox buttonBox = new HBox(10, runButton, injectScriptButton, clearButton);
        VBox layout = new VBox(10, outputArea, inputArea, buttonBox);
        layout.setPadding(new Insets(10));

        VBox.setVgrow(outputArea, Priority.ALWAYS);
        VBox.setVgrow(inputArea, Priority.NEVER);

        Scene scene = new Scene(layout);
        consoleStage.setScene(scene);
    }

    /** Executes JavaScript directly */
    private void executeJavaScript() {
        String script = inputArea.getText();
        if (script.isEmpty()) return;
        try {
            Object result = webEngine.executeScript(script);
            outputArea.appendText("> " + script + "\n" + result + "\n\n");
        } catch (Exception e) {
            outputArea.appendText("> " + script + "\nError: " + e.getMessage() + "\n\n");
        }
        inputArea.clear();
    }

    /** Injects JavaScript into the page (runs persistently) */
    private void injectScript() {
        String script = inputArea.getText();
        if (script.isEmpty()) return;

        String wrappedScript = "(function() {" + script + "})();";

        try {
            webEngine.executeScript(wrappedScript);
            outputArea.appendText("Injected Script:\n" + script + "\n\n");
        } catch (Exception e) {
            outputArea.appendText("Injection Error: " + e.getMessage() + "\n\n");
        }
        inputArea.clear();
    }

    /**Show Console */
    public void show() {
        consoleStage.show();
    }
}
