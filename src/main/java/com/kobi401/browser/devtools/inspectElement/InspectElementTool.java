package com.kobi401.browser.devtools.inspectElement;

import com.kobi401.browser.jsbridge.linkWebBridge.LinkWebBridge;
import com.kobi401.browser.ui.GUI.BrowserUI;
import com.kobi401.browser.utils.debug.Debugger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class InspectElementTool {
    private final Stage inspectStage;
    private final TextArea elementDetails;
    private final WebView webView;
    private final WebEngine webEngine;
    private final Button copyButton;
    private final Button modifyButton;
    private final Button closeButton;
    private final LinkWebBridge linkWebBridge;
    private String lastInspectedElement;

    public InspectElementTool(BrowserUI browserUI) {
        this.webView = browserUI.getCurrentWebView();
        this.webEngine = webView.getEngine();
        this.linkWebBridge = new LinkWebBridge(webEngine);

        inspectStage = new Stage();
        inspectStage.setTitle("Inspect Element");
        inspectStage.setWidth(700);
        inspectStage.setHeight(500);
        inspectStage.setResizable(true);

        Label infoLabel = new Label("Hover over an element or click to inspect and edit:");
        elementDetails = new TextArea();
        elementDetails.setEditable(true);
        elementDetails.setWrapText(true);
        elementDetails.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");
        VBox.setVgrow(elementDetails, Priority.ALWAYS);

        copyButton = new Button("Copy HTML");
        copyButton.setOnAction(e -> copyToClipboard(elementDetails.getText()));

        modifyButton = new Button("Modify Element");
        modifyButton.setOnAction(e -> modifyElement());

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> inspectStage.close());

        HBox buttonBox = new HBox(10, copyButton, modifyButton, closeButton);
        VBox layout = new VBox(10, infoLabel, elementDetails, buttonBox);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout);
        inspectStage.setScene(scene);

        linkWebBridge.addJavaBridge("javaInspectorBridge", this);
        linkWebBridge.addScript(getInspectorScript());
    }

    private String getInspectorScript() {
        return """
    (function() {
        if (window.javaInspector) return; // Prevent multiple injections
        
        window.javaInspector = {
            updateElementDetails: function(details, elementPath) {
                try {
                    javafx.run(() => javaInspectorBridge.updateElementDetails(details, elementPath));
                } catch (e) {
                    console.error("JavaFX Bridge Error:", e);
                }
            },
            selectElement: function(elementPath, html, styles, events) {
                try {
                    javafx.run(() => javaInspectorBridge.selectElement(elementPath, html, styles, events));
                } catch (e) {
                    console.error("JavaFX Bridge Error:", e);
                }
            },
            executeScript: function(script) {
                try {
                    let result = eval(script);
                    javafx.run(() => javaInspectorBridge.logResult(result));
                } catch (e) {
                    javafx.run(() => javaInspectorBridge.logError(e.toString()));
                }
            },
            captureLogs: function(message) {
                javafx.run(() => javaInspectorBridge.logConsole(message));
            }
        };

        console.log = function(message) {
            window.javaInspector.captureLogs("LOG: " + message);
        };

        console.error = function(message) {
            window.javaInspector.captureLogs("ERROR: " + message);
        };

        document.addEventListener("mouseover", function(event) {
            let element = event.target;
            if (!element) return;
            element.style.outline = "2px solid red"; // Highlight element
            
            let details = "Tag: " + element.tagName +
                          "\\nID: " + (element.id || "None") +
                          "\\nClass: " + (element.className || "None") +
                          "\\nAttributes: " + getAttributes(element) +
                          "\\nHTML: " + element.outerHTML;

            let path = getElementSelector(element);
            let styles = getComputedStyles(element);
            let events = getEventListeners(element);
            
            window.javaInspector.updateElementDetails(details, path);
        }, true);

        document.addEventListener("mouseout", function(event) {
            let element = event.target;
            if (!element) return;
            element.style.outline = ""; // Remove highlight
        }, true);

        document.addEventListener("click", function(event) {
            event.preventDefault();
            event.stopPropagation();
            
            let element = event.target;
            let elementPath = getElementSelector(element);
            let styles = getComputedStyles(element);
            let events = getEventListeners(element);

            window.javaInspector.selectElement(elementPath, element.outerHTML, styles, events);
        }, true);

        function getElementSelector(element) {
            let path = [];
            while (element && element.nodeType === Node.ELEMENT_NODE) {
                let tag = element.tagName.toLowerCase();
                if (element.id) {
                    tag += "#" + element.id;
                } else if (element.className) {
                    let classes = element.className.trim().split(/\\s+/).join(".");
                    tag += "." + classes;
                }
                path.unshift(tag);
                element = element.parentElement;
            }
            return path.join(" > ");
        }

        function getAttributes(element) {
            return Array.from(element.attributes)
                        .map(a => a.name + '="' + a.value + '"')
                        .join(' ');
        }

        function getComputedStyles(element) {
            let computed = window.getComputedStyle(element);
            return Array.from(computed)
                        .map(prop => prop + ": " + computed.getPropertyValue(prop))
                        .join("\\n");
        }

        function getEventListeners(element) {
            let listeners = [];
            for (let key in element) {
                if (key.startsWith("on") && element[key] !== null) {
                    listeners.push(key);
                }
            }
            return listeners.join(", ");
        }

    })();
    """;
    }

    //for the bridge
    public void selectElement(String elementPath, String html, String styles) {
        Platform.runLater(() -> {
            Debugger.println("JavaFX received click event!");
            Debugger.println("Path: " + elementPath);
            Debugger.println("HTML: " + html);
            Debugger.println("Styles: " + styles);
            lastInspectedElement = elementPath;
            elementDetails.setText("Path: " + elementPath + "\n\nHTML:\n" + html + "\n\nComputed Styles:\n" + styles);
        });
    }

    /**
     * Updates the UI with element details and tracks the last inspected element.
     */
    //for the bridge
    public void updateElementDetails(String details, String elementPath) {
        Platform.runLater(() -> {
            elementDetails.setText(details);
            lastInspectedElement = elementPath;
        });
    }

    /**
     * Modifies the currently selected element on the page.
     */
    private void modifyElement() {
        if (lastInspectedElement == null) {
            Debugger.println("No element selected.");
            return;
        }

        String newHtml = elementDetails.getText().replace("\"", "\\\"").replace("\n", " ");
        String modifyScript = "document.querySelector('" + lastInspectedElement + "').outerHTML = \"" + newHtml + "\";";
        webEngine.executeScript(modifyScript);
        Debugger.println("Element modified.");
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
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}