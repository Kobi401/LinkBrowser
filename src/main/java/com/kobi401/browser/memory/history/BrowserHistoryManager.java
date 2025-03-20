package com.kobi401.browser.memory.history;

import com.kobi401.browser.encryption.EncryptionUtils;

import java.util.List;

public class BrowserHistoryManager {
    private EncryptionUtils encryptionUtils;
    private List<String> webHistory;

    public BrowserHistoryManager() {
        encryptionUtils = new EncryptionUtils();
        webHistory = encryptionUtils.loadHistory();
    }

    public void addToHistory(String url) {
        webHistory.add(url);
        encryptionUtils.saveHistory(webHistory);
    }

    public List<String> getHistory() {
        return webHistory;
    }
}


