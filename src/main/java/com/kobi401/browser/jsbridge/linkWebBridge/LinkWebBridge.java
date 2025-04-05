package com.kobi401.browser.jsbridge.linkWebBridge;

import com.kobi401.browser.utils.debug.Debugger;
import com.kobi401.browser.utils.json.JSON;
import netscape.javascript.JSObject;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import java.util.ArrayList;
import java.util.List;

public class LinkWebBridge {
    private final WebEngine webEngine;
    private final List<String> scripts = new ArrayList<>();
    private final List<JavaBridge> bridges = new ArrayList<>();

    private static final String BUILD_VERSION = "2.0.3";

    public static class JavaBridge {
        public final String name;
        public final Object object;

        public JavaBridge(String name, Object object) {
            this.name = name;
            this.object = object;
        }
    }

    public LinkWebBridge(WebEngine engine) {
        this.webEngine = engine;
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                injectBridges();
                injectAllScripts();
                exposeBuildVersion();
                Debugger.println("Scripts Injected! LinkWebBridge Version: " + BUILD_VERSION);
            }
        });
    }

    /** Securely adds a JavaScript snippet to be injected. */
    public void addScript(String script) {
        scripts.add(script);
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            webEngine.executeScript(script);
        }
    }

    /** Restrict Java bridge exposure */
    public void addJavaBridge(String name, Object bridge) {
        if (!isBridgeSafe(name, bridge)) {
            System.err.println("Blocked unsafe Java bridge: " + name);
            return;
        }

        bridges.add(new JavaBridge(name, bridge));
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember(name, bridge);
        }
    }

    private void injectBridges() {
        JSObject window = (JSObject) webEngine.executeScript("window");
        for (JavaBridge bridge : bridges) {
            Debugger.println("Bridge injected: " + bridge.name);
            window.setMember(bridge.name, bridge.object);
        }
    }

    /** Injects all registered scripts securely */
    private void injectAllScripts() {
        for (String script : scripts) {
            //if (isScriptSafe(script)) {
                webEngine.executeScript(script);
            //}
        }
    }

    /** Exposes the build version to JavaScript as window.jsBridgeVersion */
    private void exposeBuildVersion() {
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("jsBridgeVersion", BUILD_VERSION);
    }

    /** Checks if a script is safe */
    private boolean isScriptSafe(String script) {
        //Dangerous JavaScript patterns to block
        String[] dangerousPatterns = {
                "eval(", "document.write(", "innerHTML += ", "outerHTML =", "setTimeout(", "setInterval(",
                "Function(", "XMLHttpRequest(", "fetch(", "import(", "atob(", "btoa(", "crypto.subtle.encrypt",
                "crypto.subtle.decrypt", "localStorage.setItem(", "sessionStorage.setItem(",
                "window.open(", "onerror=", "onload=", "javascript:", "data:text/html", "data:text/javascript"
        };

        //Safe keywords that indicate normal script behavior
        String[] safeKeywords = {
                "console.log(", "document.getElementById(", "addEventListener(", "querySelector(",
                "window.alert(", "window.confirm(", "JSON.parse(", "JSON.stringify(", "Math.random(", "Date()"
        };

        //If script contains any of the dangerous patterns, block it
        for (String pattern : dangerousPatterns) {
            if (script.contains(pattern)) {
                System.err.println("Blocked script: contains unsafe pattern -> " + pattern);
                return false;
            }
        }

        //If script contains only safe keywords, allow it
        for (String safe : safeKeywords) {
            if (script.contains(safe)) {
                return true;
            }
        }

        //If script contains suspicious characters in odd patterns, block it
        if (script.contains("<!--") || script.contains("-->") || script.contains("<script") || script.contains("</script>")) {
            System.err.println("Blocked script: contains suspicious HTML tags.");
            return false;
        }

        //Prevent attempts to bypass restrictions using obfuscation or encoding
        if (script.matches(".*(\\b(?:escape|unescape|decodeURI|decodeURIComponent)\\b).*")) {
            System.err.println("Blocked script: contains potential obfuscation or encoding methods.");
            return false;
        }

        //Block scripts with excessive special characters (potential obfuscation)
        if (script.replaceAll("[a-zA-Z0-9]", "").length() > script.length() / 2) {
            System.err.println("Blocked script: contains excessive special characters (possible obfuscation).");
            return false;
        }

        return true;
    }

    /** Restricts Java bridge exposure */
    private boolean isBridgeSafe(String name, Object bridge) {
        String[] allowedBridges = {"javaInspectorBridge", "java"};

        for (String allowed : allowedBridges) {
            if (name.equals(allowed)) {
                return true;
            }
        }

        return false;
    }

    public void injectWasm(JSON wasmData) {
        String wasmBase64 = wasmData.getString("wasm");
        String script = String.format("window.wasmModule = new Uint8Array(atob('%s').split('').map(function(c) { return c.charCodeAt(0); }));", wasmBase64);
        webEngine.executeScript(script);
        Debugger.println("WASM module injected.");
    }

    public void invokeWasmFunction(JSON wasmCallData) {
        String functionName = wasmCallData.getString("function");
        String paramsJson = wasmCallData.getString("params");
        String script = String.format("window.wasmModule.%s(%s);", functionName, paramsJson);
        webEngine.executeScript(script);
        Debugger.println("WASM function invoked: " + functionName);
    }

    public void initializeWasmModule() {
        String script = "if (!window.wasmModule) { console.error('WASM module not loaded!'); } else { console.log('WASM module initialized'); }";
        webEngine.executeScript(script);
        Debugger.println("WASM module initialized.");
    }

    /** Returns the current build version. */
    public static String getBuildVersion() {
        return BUILD_VERSION;
    }
}
