package com.kobi401.browser.security;

import com.kobi401.browser.encryption.EncryptionUtils;
import com.kobi401.browser.engine.BrowserEngine;

import javax.crypto.SecretKey;

public class SecurityUtils {

    private EncryptionUtils encryptionUtils;
    private TrackingProtection trackingProtection;
    private SecureCommunication secureCommunication;

    public SecurityUtils() {
        this.encryptionUtils = new EncryptionUtils();
        this.trackingProtection = new TrackingProtection();
        this.secureCommunication = new SecureCommunication();
    }

    // Check if the URL is a blocked tracking URL
    public boolean isTrackingUrlBlocked(String url) {
        return trackingProtection.isBlocked(url);
    }

    // Ensure HTTPS and validate certificate
    public boolean isSecureUrl(String url) {
        return secureCommunication.isHttpsAndValid(url);
    }
}


