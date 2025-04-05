package com.kobi401.browser.jsbridge.Flash;

import com.kobi401.browser.jsbridge.linkWebBridge.LinkWebBridge;
import com.kobi401.browser.utils.debug.Debugger;
import javafx.scene.web.WebEngine;

public class RuffleFlashPlayer {

    private final LinkWebBridge linkWebBridge;
    private final WebEngine webEngine;

    public RuffleFlashPlayer(WebEngine engine, LinkWebBridge linkWebBridge) {
        this.webEngine = engine;
        this.linkWebBridge = linkWebBridge;
        injectRuffleScript();
    }

    private void injectRuffleScript() {
        try {
            String ruffleJsPath = getClass().getResource("/Ruffle/ruffle.js").toExternalForm();
            String ruffleWasmPath = getClass().getResource("/Ruffle/ruffle.wasm").toExternalForm();
            String scriptInjection = "var ruffle = document.createElement('script');" +
                    "ruffle.src = '" + ruffleJsPath + "';" +
                    "ruffle.onload = function() {" +
                    "    var rufflePlayer = window.RufflePlayer.new();" +
                    "    rufflePlayer.setWasmPath('" + ruffleWasmPath + "');" +
                    "    rufflePlayer.load('flash-player-container');" +
                    "    console.log('Ruffle Flash Player initialized.');" +
                    "};" +
                    "document.head.appendChild(ruffle);";
            linkWebBridge.addScript(scriptInjection);

            Debugger.println("Ruffle Flash Player script injected from local resources.");

        } catch (Exception e) {
            System.err.println("Error injecting Ruffle script: " + e.getMessage());
        }
    }

    /**
     * expose a Java bridge to communicate with Ruffle if needed.
     */
    public void addJavaBridgeForRuffle(String bridgeName, Object bridgeObject) {
        linkWebBridge.addJavaBridge(bridgeName, bridgeObject);
    }

    /**
     * Load Flash content from a web page URL (e.g., Newgrounds, ArmorGames, etc.).
     * This method dynamically detects embedded Flash content and uses Ruffle to play it.
     */
    public void loadFlashContentFromUrl(String flashUrl) {
        linkWebBridge.addScript("var flashContainer = document.getElementById('flash-player-container');" +
                "flashContainer.innerHTML = '<object type=\"application/x-shockwave-flash\" data=\"" + flashUrl + "\" width=\"100%\" height=\"100%\"></object>';");
    }

    public void loadFlashContentFromExternalPage(String pageUrl) {
        try {
            String ruffleWasmPath = getClass().getResource("/Ruffle/ruffle.wasm").toExternalForm();
            Debugger.println("WASM Path: " + ruffleWasmPath);
            linkWebBridge.addScript("fetch('" + pageUrl + "').then(response => response.text()).then(html => {" +
                    "    var flashContent = extractFlashContent(html);" +
                    "    var flashContainer = document.getElementById('flash-player-container');" +
                    "    var rufflePlayer = window.RufflePlayer.new();" +
                    "    rufflePlayer.setWasmPath('" + ruffleWasmPath + "');" +
                    "    rufflePlayer.load(flashContainer, flashContent);" +
                    "    console.log('Flash content loaded from external page using Ruffle.');" +
                    "}).catch(error => console.error('Error fetching page HTML:', error));");

            linkWebBridge.addScript("function extractFlashContent(html) {" +
                    "    var flashObjectMatch = html.match(/<object [^>]*data=['\"]([^'\"]+\\.swf)['\"][^>]*>/);" +
                    "    if (flashObjectMatch) {" +
                    "        return flashObjectMatch[1];" +
                    "    }" +
                    "    var flashEmbedMatch = html.match(/<embed [^>]*src=['\"]([^'\"]+\\.swf)['\"][^>]*>/);" +
                    "    if (flashEmbedMatch) {" +
                    "        return flashEmbedMatch[1];" +
                    "    }" +
                    "    return '';" +
                    "}");
        } catch (Exception e) {
            System.err.println("Error loading Flash content from external page: " + e.getMessage());
        }
    }
}
