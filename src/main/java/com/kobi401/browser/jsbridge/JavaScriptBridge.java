package com.kobi401.browser.jsbridge;

import netscape.javascript.JSObject;
import java.util.logging.Logger;

public class JavaScriptBridge {
    private static final Logger logger = Logger.getLogger(JavaScriptBridge.class.getName());
    private static JavaScriptBridge instance;
    private JSObject jsBridge;

    private JavaScriptBridge() {
        // Private constructor to enforce singleton pattern
    }

    public static JavaScriptBridge getInstance() {
        if (instance == null) {
            instance = new JavaScriptBridge();
        }
        return instance;
    }

    public void setJsBridge(JSObject jsBridge) {
        if (this.jsBridge == null && jsBridge != null) {
            this.jsBridge = jsBridge;
            logger.info("JavaScript bridge initialized successfully.");
        } else {
            logger.warning("JavaScript bridge is already set or attempted to set a null value.");
        }
    }
    public JSObject getJsBridge() {
        if (jsBridge == null) {
            logger.warning("JavaScript bridge is not yet initialized.");
        }
        return jsBridge;
    }

    public boolean isInitialized() {
        return jsBridge != null;
    }
}