package com.kobi401.browser.security;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecureCommunication {

    private static final Logger logger = Logger.getLogger(SecureCommunication.class.getName());
    private static final String CERTS_DIRECTORY = System.getProperty("user.home") + File.separator + "LinkBrowser" + File.separator + "certs";
    private static final String RESOURCE_DIR = "src/resources";

    static {
        File certsDir = new File(CERTS_DIRECTORY);
        if (!certsDir.exists()) {
            certsDir.mkdirs();
        }
        try {
            FileHandler fileHandler = new FileHandler(System.getProperty("user.home") + File.separator + "LinkBrowser" + File.separator + "secure_communication.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }

    public boolean isHttpsAndValid(String url) {
        if (!isValidUrl(url)) {
            System.err.println("Invalid URL: " + url);
            return false; // Reject invalid URL
        }
        if (isLocalHtml(url)) {
            System.out.println("Ignoring local .html page: " + url);
            return true; // Skip validation for local .html files
        }

        try {
            URL websiteUrl = new URL(url);
            if ("https".equals(websiteUrl.getProtocol())) {
                HttpsURLConnection connection = (HttpsURLConnection) websiteUrl.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                connection.connect();

                X509Certificate cert = (X509Certificate) connection.getServerCertificates()[0];
                saveCertificate(cert);
                connection.getResponseCode();
                System.out.println("HTTPS connection established with valid certificate: " + url);
                return true;
            } else {
                System.out.println("Insecure URL: " + url);
            }
        } catch (Exception e) {
            System.err.println("Error with certificate or connection: " + e.getMessage());
        }
        return false;
    }

    private boolean isValidUrl(String url) {
        String urlRegex = "^(https?|ftp)://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?::\\d+)?(/[^\\s]*)?$";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    private boolean isLocalHtml(String url) {
        if (url.endsWith(".html")) {
            File file = new File(url);
            if (file.exists()) {
                return true;
            } else {
                String userDir = System.getProperty(RESOURCE_DIR);
                File resourceFile = new File(userDir, url);
                return resourceFile.exists();
            }
        }
        return false; // Not a local .html file
    }

    private void saveCertificate(X509Certificate cert) {
        try {
            String certFileName = CERTS_DIRECTORY + File.separator + cert.getSerialNumber() + ".cer";
            try (FileOutputStream fos = new FileOutputStream(certFileName)) {
                fos.write(cert.getEncoded());
            }
            logger.info("Saved certificate: " + cert.getSerialNumber() + " to " + certFileName);
        } catch (Exception e) {
            logger.severe("Error saving certificate: " + e.getMessage());
        }
    }

    public void logConnectionAttempt(String url) {
        logger.info("Attempting connection: " + url);
    }
}
