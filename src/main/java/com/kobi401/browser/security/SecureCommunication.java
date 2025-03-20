package com.kobi401.browser.security;

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
    private static final String RESOURCE_DIR = "src/resources"; // Adjust the path as necessary for your project

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

    // Check if a URL is valid, HTTPS, and the certificate is valid
    public boolean isHttpsAndValid(String url) {
        if (!isValidUrl(url)) {
            System.err.println("Invalid URL: " + url);
            return false; // Reject invalid URL
        }

        // Ignore local .html files in the resource directory
        if (isLocalHtml(url)) {
            System.out.println("Ignoring local .html page: " + url);
            return true; // Skip validation for local .html files
        }

        try {
            URL websiteUrl = new URL(url);
            if ("https".equals(websiteUrl.getProtocol())) {
                HttpURLConnection connection = (HttpURLConnection) websiteUrl.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                // Attempt to connect and get the certificate
                connection.connect();

                // Extract the server's SSL certificate and log the status
                connection.getResponseCode(); // Will throw an exception for invalid certificates
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

    // Simple URL validation using regex
    private boolean isValidUrl(String url) {
        String urlRegex = "^(https?|ftp)://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?::\\d+)?(/[^\\s]*)?$";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    // Check if the URL is a local .html file (either in the resource directory or in the current directory)
    private boolean isLocalHtml(String url) {
        if (url.endsWith(".html")) {
            File file = new File(url);
            if (file.exists()) {
                // Check if it's an existing local file (absolute or relative path)
                return true;
            } else {
                // Check if it's a local file within the resource directory
                String userDir = System.getProperty(RESOURCE_DIR);
                File resourceFile = new File(userDir, url);
                return resourceFile.exists();
            }
        }
        return false; // Not a local .html file
    }

    // Save the certificate to a file
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

    // Log all connection attempts
    public void logConnectionAttempt(String url) {
        logger.info("Attempting connection: " + url);
    }
}
