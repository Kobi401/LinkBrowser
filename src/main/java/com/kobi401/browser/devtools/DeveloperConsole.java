package com.kobi401.browser.devtools;

import com.kobi401.browser.ui.BrowserUI;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.web.WebView;

public class DeveloperConsole {
    private final Stage consoleStage;
    private final TextArea outputArea;
    private final TextField inputField;
    private final WebView webView;

    public DeveloperConsole(BrowserUI browserUI) {
        this.webView = browserUI.getCurrentWebView();

        consoleStage = new Stage();
        consoleStage.setTitle("Developer Console");
        consoleStage.setWidth(600);
        consoleStage.setHeight(400);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(300);

        inputField = new TextField();
        inputField.setPromptText("Enter JavaScript code...");
        inputField.setOnAction(e -> executeJavaScript());

        Button runButton = new Button("Run");
        runButton.setOnAction(e -> executeJavaScript());

        VBox layout = new VBox(10, outputArea, inputField, runButton);
        Scene scene = new Scene(layout);
        consoleStage.setScene(scene);
    }

    private void executeJavaScript() {
        String script = inputField.getText();
        try {
            Object result = webView.getEngine().executeScript(script);
            outputArea.appendText("> " + script + "\n" + result + "\n");
        } catch (Exception e) {
            outputArea.appendText("> " + script + "\nError: " + e.getMessage() + "\n");
        }
        inputField.clear();
    }

    public void show() {
        consoleStage.show();
    }
}