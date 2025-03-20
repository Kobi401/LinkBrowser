package com.kobi401.browser.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//TODO:
//fix easylist blocking normal urls like youtube, google, local hmtls, etc
public class TrackingProtection {

    private Set<String> blockedTrackingUrls;

    public TrackingProtection() {
        blockedTrackingUrls = new HashSet<>();
        fetchTrackingUrlsFromEasyList();
    }

    public boolean isBlocked(String url) {
        for (String blockedUrl : blockedTrackingUrls) {
            if (url.contains(blockedUrl)) {
                return true;
            }
        }
        return false;
    }

    public void addBlockedUrl(String url) {
        blockedTrackingUrls.add(url);
    }

    public void clearBlockedUrls() {
        blockedTrackingUrls.clear();
    }

    private void fetchTrackingUrlsFromEasyList() {
        String easyListUrl = "https://easylist.to/easylist/easyprivacy.txt";

        try {
            URL url = new URL(easyListUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (!inputLine.startsWith("!") && !inputLine.isEmpty()) {
                    String domainPattern = "(?<=//)([a-zA-Z0-9.-]+)";
                    Pattern pattern = Pattern.compile(domainPattern);
                    Matcher matcher = pattern.matcher(inputLine);
                    while (matcher.find()) {
                        String domain = matcher.group(1);
                        blockedTrackingUrls.add(domain);
                    }
                }
            }
            in.close();
            System.out.println("Successfully fetched and parsed tracking URLs from EasyList.");
        } catch (IOException e) {
            System.out.println("Error fetching EasyList: " + e.getMessage());
        }
    }
}