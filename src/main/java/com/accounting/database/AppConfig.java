package com.accounting.database;

import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

public class AppConfig {

    private static final Logger log = AppLogger.get(AppConfig.class);

    private static final String CONFIG_FOLDER =
            System.getenv("LOCALAPPDATA") + File.separator + "AccountManagement";
    private static final String CONFIG_FILE =
            CONFIG_FOLDER + File.separator + "config.properties";

    private static final String KDF_SALT = "AccMgmt-KDF-Salt-2026";
    private static final int PBKDF2_ITERS = 100_000;
    private static final int KEY_BITS = 256;

    public static boolean loadDatabaseConfig() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) return false;

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            }

            String host = getSecureProperty(props, "db.host");
            String port = getSecureProperty(props, "db.port");
            String dbName = getSecureProperty(props, "db.name");
            String user = getSecureProperty(props, "db.user");
            String pass = getSecureProperty(props, "db.pass");

            DBConnection.setDatabaseConfig(host, port, dbName, user, pass);
            return true;

        } catch (IOException e) {
            log.error("Failed to load database config", e);
            return false;
        }
    }

    public static void saveDatabaseConfig(
            String host, String port, String dbName, String user, String pass
    ) {
        try {
            File folder = new File(CONFIG_FOLDER);
            if (!folder.exists()) folder.mkdirs();

            Properties props = loadProperties();

            setSecureProperty(props, "db.host", host);
            setSecureProperty(props, "db.port", port);
            setSecureProperty(props, "db.name", dbName);
            setSecureProperty(props, "db.user", user);
            setSecureProperty(props, "db.pass", pass);

            saveProperties(props);
            log.info("Database config saved successfully.");

        } catch (Exception e) {
            log.error("Failed to save database config", e);
        }
    }

    public static String getCompanyName() {
        try {
            Properties props = loadProperties();
            String val = props.getProperty("company.name");
            return val != null ? val : "Account Management";
        } catch (Exception e) {
            return "Account Management";
        }
    }

    public static void saveCompanyName(String name) {
        try {
            Properties props = loadProperties();
            props.setProperty("company.name", name);
            saveProperties(props);
        } catch (Exception e) {
            log.error("Failed to save company name", e);
        }
    }

    public static String getFinancialYear() {
        try {
            Properties props = loadProperties();
            String val = props.getProperty("financial.year");
            return val != null ? val : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static void saveFinancialYear(String year) {
        try {
            Properties props = loadProperties();
            props.setProperty("financial.year", year);
            saveProperties(props);
        } catch (Exception e) {
            log.error("Failed to save financial year", e);
        }
    }

    // ── Encryption helpers ──

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            }
        }
        return props;
    }

    private static void saveProperties(Properties props) throws IOException {
        File folder = new File(CONFIG_FOLDER);
        if (!folder.exists()) folder.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Account Management Configuration");
        }
    }

    private static String getSecureProperty(Properties props, String key) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) return raw;
        try {
            return decrypt(raw);
        } catch (Exception e) {
            log.warn("Could not decrypt property '{}', returning raw value", key);
            return raw;
        }
    }

    private static void setSecureProperty(Properties props, String key, String value) {
        try {
            props.setProperty(key, encrypt(value));
        } catch (Exception e) {
            log.warn("Could not encrypt property '{}', storing raw", key);
            props.setProperty(key, value);
        }
    }

    private static String encrypt(String plaintext) throws GeneralSecurityException {
        SecretKey key = getAppKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);
        return Base64.getEncoder().encodeToString(out);
    }

    private static String decrypt(String ciphertext) throws GeneralSecurityException {
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] iv = new byte[12];
        System.arraycopy(decoded, 0, iv, 0, 12);
        byte[] ct = new byte[decoded.length - 12];
        System.arraycopy(decoded, 12, ct, 0, ct.length);

        SecretKey key = getAppKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] pt = cipher.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }

    private static SecretKey getAppKey() throws GeneralSecurityException {
        String material = System.getProperty("user.name", "")
                + "|" + System.getProperty("user.home", "")
                + "|" + System.getProperty("os.name", "")
                + "|" + System.getProperty("os.arch", "")
                + "|" + CONFIG_FILE;

        PBEKeySpec spec = new PBEKeySpec(
                material.toCharArray(),
                KDF_SALT.getBytes(StandardCharsets.UTF_8),
                PBKDF2_ITERS,
                KEY_BITS
        );

        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] encoded = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }
}
