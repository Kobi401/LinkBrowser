package com.kobi401.browser.security;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

import com.kobi401.browser.jsbridge.JavaScriptBridge;
import netscape.javascript.JSObject;  // Import JSObject to allow interaction with JavaScript

public class AdBlocker {

    private static final Logger logger = Logger.getLogger(AdBlocker.class.getName());
    private static final Set<String> blockedUrls = new HashSet<>();
    private static final String EASYLIST_URL = "https://easylist.to/easylist/easylist.txt";
   // private JSObject jsBridge;  // JavaScript bridge to inject JS into the browser engine

    public AdBlocker() {
        try {
            fetchEasyList();
        } catch (IOException e) {
            logger.severe("Failed to fetch EasyList: " + e.getMessage());
        }
    }


    // Fetch and parse the EasyList
    public void fetchEasyList() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(EASYLIST_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            // Skip comments in the list (lines starting with "!")
            if (inputLine.startsWith("!") || inputLine.trim().isEmpty()) {
                continue;
            }

            // Regex to match domains or URLs
            String domainPattern = "(?<=//)([a-zA-Z0-9.-]+)";
            Pattern pattern = Pattern.compile(domainPattern);
            Matcher matcher = pattern.matcher(inputLine);
            while (matcher.find()) {
                String domain = matcher.group(1);
                blockedUrls.add(domain); // Add the domain to blocked URLs set
            }
        }
        in.close();
        logger.info("Successfully fetched and parsed EasyList.");
    }

    // Check if the URL should be blocked based on EasyList
    public boolean isAdUrl(String url) {
        for (String blockedUrl : blockedUrls) {
            if (url.contains(blockedUrl)) {
                return true; // Block the URL if it contains a blocked domain
            }
        }
        return false; // Not blocked
    }

    public void injectJavaScript(String script) {
        JSObject jsBridge = JavaScriptBridge.getInstance().getJsBridge();
        if (jsBridge == null) {
            logger.warning("JavaScript bridge is not available.");
            return;
        }

        try {
            jsBridge.eval(script);
            logger.info("Injected JS: " + script);
        } catch (Exception e) {
            logger.severe("Failed to inject JavaScript: " + e.getMessage());
        }
    }

    // Example usage: Inject JS to block ad scripts or modify the DOM
    public void blockAdsWithJS() {
        String adBlockerScript =
                "(function() { " +
                        "   var adSelectors = [" +
                        "       'iframe[src*=\"ads\"]', 'iframe[src*=\"doubleclick\"]', 'iframe[src*=\"adservice\"]', 'iframe[src*=\"googlesyndication\"]', " +
                        "       'script[src*=\"ads\"]', 'script[src*=\"doubleclick\"]', 'script[src*=\"adservice\"]', 'script[src*=\"googlesyndication\"]', " +
                        "       'div[class*=\"ad\"]', 'div[id*=\"ad\"]', 'div[class*=\"sponsored\"]', 'div[class*=\"promoted\"]', 'div[id*=\"sponsored\"]' " +
                        "   ]; " +
                        "   function removeAds() { " +
                        "       adSelectors.forEach(selector => { " +
                        "           document.querySelectorAll(selector).forEach(ad => ad.remove()); " +
                        "       }); " +
                        "   } " +
                        "   removeAds(); " +
                        "   var observer = new MutationObserver(mutations => { " +
                        "       mutations.forEach(mutation => { " +
                        "           if (mutation.addedNodes.length) removeAds(); " +
                        "       }); " +
                        "   }); " +
                        "   observer.observe(document.body, { childList: true, subtree: true }); " +
                        "})();";

        injectJavaScript(adBlockerScript);  // Inject the enhanced ad-blocking script
    }

    // Method to block ads by URL (checks if a URL should be blocked and then injects JS to remove ad elements)
    public void blockAdsByUrl(String url) {
        if (isAdUrl(url)) {
            blockAdsWithJS();  // Block the ads if the URL is recognized as an ad source
        }
    }

    // Getter for the blocked URLs for external use
    public Set<String> getBlockedUrls() {
        return blockedUrls;
    }

    // Method to clear the blocked URLs (for example, when refreshing the list)
    public void clearBlockedUrls() {
        blockedUrls.clear();
    }

    // Method to refresh the EasyList (useful for periodic updates)
    public void refreshEasyList() {
        try {
            blockedUrls.clear();
            fetchEasyList();
            logger.info("EasyList refreshed successfully.");
        } catch (IOException e) {
            logger.severe("Failed to refresh EasyList: " + e.getMessage());
        }
    }
}
