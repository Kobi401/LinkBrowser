package com.kobi401.browser.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

public class EncryptionUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String ENCODING = "UTF-8";
    private static final int IV_LENGTH = 12; //length of IV for GCM mode
    private static final String HISTORY_FILE_NAME = "LinkBrowser/User/webHistory.dat";
    private SecretKey secretKey;

    public EncryptionUtils() {
        this.secretKey = generateSecretKey();
    }

    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH]; //12-byte IV for GCM
        secureRandom.nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] encryptedData = cipher.doFinal(data.getBytes(ENCODING));

        byte[] ivAndEncryptedData = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, ivAndEncryptedData, 0, iv.length);
        System.arraycopy(encryptedData, 0, ivAndEncryptedData, iv.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(ivAndEncryptedData);
    }

    public String decrypt(String encryptedData) throws Exception {
        byte[] ivAndEncryptedData = Base64.getDecoder().decode(encryptedData);
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[ivAndEncryptedData.length - iv.length];

        System.arraycopy(ivAndEncryptedData, 0, iv, 0, iv.length);
        System.arraycopy(ivAndEncryptedData, iv.length, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decryptedData = cipher.doFinal(encryptedBytes);
        return new String(decryptedData, ENCODING);
    }

    private SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Error generating secret key", e);
        }
    }

    public void saveHistory(List<String> history) {
        try {
            File historyFile = getHistoryFile();
            historyFile.getParentFile().mkdirs();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(historyFile))) {
                for (String url : history) {
                    oos.writeObject(encrypt(url));
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
    }

    public List<String> loadHistory() {
        List<String> history = new ArrayList<>();
        try {
            File historyFile = getHistoryFile();
            if (!historyFile.exists()) {
                return history;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(historyFile))) {
                while (true) {
                    try {
                        String encryptedUrl = (String) ois.readObject();
                        history.add(decrypt(encryptedUrl));
                    } catch (EOFException e) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
        return history;
    }

    private File getHistoryFile() {
        String userHome = System.getProperty("user.home");
        File historyFile = new File(userHome, HISTORY_FILE_NAME);
        return historyFile;
    }

    public void clearHistory() {
        File historyFile = getHistoryFile();
        if (historyFile.exists()) {
            if (historyFile.delete()) {
                System.out.println("History file deleted successfully.");
            } else {
                System.err.println("Failed to delete history file.");
            }
        } else {
            System.err.println("History file does not exist.");
        }
    }
}
