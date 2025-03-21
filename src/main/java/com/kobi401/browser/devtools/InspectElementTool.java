package com.kobi401.browser.devtools;

import com.kobi401.browser.ui.BrowserUI;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class InspectElementTool {
    private final Stage inspectStage;
    private final TextArea elementDetails;
    private final WebView webView;
    private final WebEngine webEngine;
    private final Button closeButton;
    private final Button copyButton;
    private final Label infoLabel;

    public InspectElementTool(BrowserUI browserUI) {
        this.webView = browserUI.getCurrentWebView();
        this.webEngine = webView.getEngine();

        inspectStage = new Stage();
        inspectStage.setTitle("Inspect Element");
        inspectStage.setWidth(600);
        inspectStage.setHeight(400);

        infoLabel = new Label("Hover over an element or select text to inspect:");
        elementDetails = new TextArea();
        elementDetails.setEditable(false);
        elementDetails.setPrefHeight(250);
        elementDetails.setWrapText(true);

        copyButton = new Button("Copy HTML");
        copyButton.setOnAction(e -> copyToClipboard(elementDetails.getText()));

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> inspectStage.close());

        VBox layout = new VBox(10, infoLabel, elementDetails, copyButton, closeButton);
        layout.setStyle("-fx-padding: 10;");
        Scene scene = new Scene(layout);
        inspectStage.setScene(scene);

        injectInspectorScript();
    }

    private void injectInspectorScript() {
        String script = """
            (function() {
                // Add hover listener
                document.addEventListener("mouseover", function(event) {
                    let element = event.target;
                    let details = "Tag: " + element.tagName +
                                  "\\nID: " + (element.id || "None") +
                                  "\\nClass: " + (element.className || "None") +
                                  "\\nHTML: " + element.outerHTML;
                    window.javaInspector.updateElementDetails(details);
                });

                // Add text selection listener
                document.addEventListener("mouseup", function() {
                    let selection = window.getSelection().toString().trim();
                    if (selection.length > 0) {
                        let range = window.getSelection().getRangeAt(0);
                        let selectedHTML = range.cloneContents().textContent;
                        window.javaInspector.updateElementDetails("Selected HTML:\\n" + selectedHTML);
                    }
                });
            })();
            """;

        webEngine.executeScript("""
            window.javaInspector = {
                updateElementDetails: function(details) {
                    javafx.run(() => javaInspectorBridge.updateElementDetails(details));
                }
            };
        """);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                webEngine.executeScript(script);
            }
        });
    }

    /**
     * Updates the UI with element details.
     */
    public void updateElementDetails(String details) {
        Platform.runLater(() -> elementDetails.setText(details));
    }

    /**
     * Displays the Inspect Element tool.
     */
    public void show() {
        inspectStage.show();
    }

    /**
     * Copies text to clipboard.
     */
    private void copyToClipboard(String text) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}
