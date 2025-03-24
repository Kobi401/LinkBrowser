package com.kobi401.browser.security;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.*;

import com.kobi401.browser.jsbridge.JSInjectionSystem;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

public class AdBlocker {

    private static final Logger logger = Logger.getLogger(AdBlocker.class.getName());
    private static final Set<String> blockedUrls = new HashSet<>();
    private static final String EASYLIST_URL = "https://easylist.to/easylist/easylist.txt";
    private JSInjectionSystem jsInjectionSystem;

    public AdBlocker(JSInjectionSystem jsInjectionSystem) {
        if (jsInjectionSystem == null) {
            logger.severe("JSInjectionSystem is null. AdBlocker will not function.");
            return;
        }
        this.jsInjectionSystem = jsInjectionSystem;
        try {
            jsInjectionSystem.addScript("console.log('AdBlocker Test Script');");
        } catch (Exception e) {
            logger.severe("JSInjectionSystem failed to inject scripts. AdBlocker will not function.");
            return;
        }

        try {
            fetchEasyList();
        } catch (IOException e) {
            logger.severe("Failed to fetch EasyList: " + e.getMessage());
        }
    }

    /** Fetch and parse the EasyList */
    private void fetchEasyList() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(EASYLIST_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("!") || line.trim().isEmpty() || line.startsWith("[")) {
                    continue; // Ignore comments and section headers
                }

                String domain = extractDomain(line);
                if (domain != null) {
                    blockedUrls.add(domain);
                }
            }
        }

        logger.info("Successfully fetched and parsed EasyList.");
    }

    /** Extracts domains from filter rules */
    private String extractDomain(String rule) {
        if (rule.contains("||")) {
            return rule.substring(2).split("/")[0];
        } else if (rule.contains("/")) {
            return rule.split("/")[0];
        }
        return null;
    }

    /** Check if a URL is blocked */
    public boolean isAdUrl(String url) {
        for (String blockedUrl : blockedUrls) {
            if (url.contains(blockedUrl)) {
                return true;
            }
        }
        return false;
    }

    /** Inject JavaScript to remove ads dynamically */
    public void blockAdsWithJS() {
        if (blockedUrls.isEmpty()) {
            logger.warning("EasyList is empty or failed to load. AdBlocker may not work.");
            return;
        }

        StringBuilder blockedUrlsJS = new StringBuilder("var blockedDomains = [");
        List<String> blockedUrlsList = new ArrayList<>(blockedUrls);
        for (int i = 0; i < blockedUrlsList.size(); i++) {
            String domain = blockedUrlsList.get(i);
            blockedUrlsJS.append("\"").append(domain.replace("\"", "\\\"")).append("\"");
            if (i < blockedUrlsList.size() - 1) {
                blockedUrlsJS.append(",");
            }
        }

        blockedUrlsJS.append("];");

        String adBlockerScript =
                "(function() { " +
                        blockedUrlsJS +  // Insert blocked domains list
                        "   var adSelectors = [" +
                        "       'iframe', 'script', 'img', 'div', 'a'" +
                        "   ]; " +
                        "   function removeAds() { " +
                        "       document.querySelectorAll(adSelectors.join(', ')).forEach(element => { " +
                        "           var src = element.src || element.href || ''; " +
                        "           if (blockedDomains.some(domain => src.includes(domain))) { " +
                        "               element.remove(); " +
                        "           } " +
                        "       }); " +
                        "   } " +
                        "   removeAds(); " +

                        // YouTube-specific ad blocking
                        "   function blockYouTubeAds() { " +
                        "       var ytSelectors = [" +
                        "           'ytd-ad-slot-renderer', 'ytd-companion-slot-renderer', " + // Video ads
                        "           '.ytp-ad-player-overlay', '.ytp-ad-image-overlay', " + // Overlays
                        "           '.ytp-ad-text', '.ytp-ad-preview-container', " + // Ad banners
                        "           '.ytp-ad-skip-button', '.video-ads', " + // Skip button
                        "           'ytd-promoted-sparkles-text-search-renderer' " + // Promoted results
                        "       ]; " +
                        "       ytSelectors.forEach(selector => { " +
                        "           document.querySelectorAll(selector).forEach(ad => ad.remove()); " +
                        "       }); " +
                        "       var skipButton = document.querySelector('.ytp-ad-skip-button'); " +
                        "       if (skipButton) skipButton.click(); " + // Auto-skip video ads
                        "   } " +
                        "   blockYouTubeAds(); " +

                        "   var observer = new MutationObserver(mutations => { " +
                        "       mutations.forEach(mutation => { " +
                        "           if (mutation.addedNodes.length) { " +
                        "               removeAds(); " +
                        "               blockYouTubeAds(); " + // Run YouTube ad removal again
                        "           } " +
                        "       }); " +
                        "   }); " +
                        "   observer.observe(document.body, { childList: true, subtree: true }); " +
                        "})();";

        // Inject the script into the page
        jsInjectionSystem.addScript(adBlockerScript);
    }

    /** Block ads based on URL */
    public void blockAdsByUrl(String url) {
        if (isAdUrl(url)) {
            blockAdsWithJS();
        }
    }

    /** Get blocked URLs */
    public Set<String> getBlockedUrls() {
        return Collections.unmodifiableSet(blockedUrls);
    }

    /** Clear blocked URLs */
    public void clearBlockedUrls() {
        blockedUrls.clear();
    }

    /** Refresh EasyList */
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