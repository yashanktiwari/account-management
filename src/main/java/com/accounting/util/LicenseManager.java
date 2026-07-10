package com.accounting.util;

import com.accounting.dao.SettingsDAO;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Manages application licensing and demo mode.
 * Demo mode allows limited usage. Full license unlocks all features.
 */
public class LicenseManager {

    private static final Logger log = AppLogger.get(LicenseManager.class);
    private static final String LICENSE_KEY_SETTING = "license_key";
    private static final String DEMO_START_DATE_SETTING = "demo_start_date";
    
    // Secret key for encryption (keep this private - only you should know this)
    private static final String SECRET = "AccountMgmt2026Key!@#";
    
    private static LicenseStatus currentStatus = null;

    public enum LicenseType {
        DEMO,
        FULL
    }

    public static class LicenseStatus {
        public LicenseType type;
        public String message;
        public int daysRemaining;
        public boolean isValid;

        public LicenseStatus(LicenseType type, String message, int daysRemaining, boolean isValid) {
            this.type = type;
            this.message = message;
            this.daysRemaining = daysRemaining;
            this.isValid = isValid;
        }
    }

    /**
     * Check the current license status
     */
    public static LicenseStatus checkLicense() {
        if (currentStatus != null) {
            return currentStatus;
        }

        try {
            SettingsDAO settingsDAO = new SettingsDAO();
            String licenseKey = settingsDAO.getSetting(LICENSE_KEY_SETTING);

            if (licenseKey != null && !licenseKey.isBlank()) {
                // Validate license key
                if (validateLicenseKey(licenseKey)) {
                    currentStatus = new LicenseStatus(
                        LicenseType.FULL,
                        "Full License Active",
                        -1,
                        true
                    );
                    log.info("Valid license key found");
                    return currentStatus;
                }
            }

            // Check demo mode
            String demoStartStr = settingsDAO.getSetting(DEMO_START_DATE_SETTING);
            LocalDate demoStart;
            
            if (demoStartStr == null || demoStartStr.isBlank()) {
                // First time - start demo
                demoStart = LocalDate.now();
                settingsDAO.saveSetting(DEMO_START_DATE_SETTING, demoStart.toString());
                log.info("Demo mode started");
            } else {
                demoStart = LocalDate.parse(demoStartStr);
            }

            int daysUsed = (int) java.time.temporal.ChronoUnit.DAYS.between(demoStart, LocalDate.now());
            int daysRemaining = 30 - daysUsed;

            if (daysRemaining > 0) {
                currentStatus = new LicenseStatus(
                    LicenseType.DEMO,
                    "Demo Mode - " + daysRemaining + " days remaining",
                    daysRemaining,
                    true
                );
            } else {
                currentStatus = new LicenseStatus(
                    LicenseType.DEMO,
                    "Demo Expired - Please purchase a license",
                    0,
                    false
                );
            }

            return currentStatus;

        } catch (Exception e) {
            log.error("Error checking license (DB may not be connected)", e);
            // Default to valid demo when DB is unavailable (fresh install / no DB configured)
            currentStatus = new LicenseStatus(
                LicenseType.DEMO,
                "Demo Mode - Database not connected",
                30,
                true
            );
            return currentStatus;
        }
    }

    /**
     * Activate a license key
     */
    public static boolean activateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return false;
        }

        if (validateLicenseKey(licenseKey)) {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                settingsDAO.saveSetting(LICENSE_KEY_SETTING, licenseKey);
                currentStatus = null; // Reset cache
                log.info("License activated successfully");
                return true;
            } catch (Exception e) {
                log.error("Failed to save license key", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Generate a license key (for admin use only)
     * Format: COMPANY_NAME|EXPIRY_DATE|HASH
     */
    public static String generateLicenseKey(String companyName) {
        try {
            // License never expires for full version
            String data = companyName.toUpperCase() + "|PERMANENT";
            String hash = generateHash(data);
            String licenseKey = data + "|" + hash;
            
            // Encrypt the license key
            return encrypt(licenseKey);
        } catch (Exception e) {
            log.error("Failed to generate license key", e);
            return null;
        }
    }

    /**
     * Validate a license key
     */
    private static boolean validateLicenseKey(String encryptedKey) {
        try {
            String decrypted = decrypt(encryptedKey);
            String[] parts = decrypted.split("\\|");
            
            if (parts.length != 3) {
                return false;
            }

            String companyName = parts[0];
            String expiry = parts[1];
            String providedHash = parts[2];

            // Verify hash
            String data = companyName + "|" + expiry;
            String calculatedHash = generateHash(data);

            if (!calculatedHash.equals(providedHash)) {
                return false;
            }

            // Check expiry (PERMANENT means never expires)
            if (!"PERMANENT".equals(expiry)) {
                LocalDate expiryDate = LocalDate.parse(expiry, DateTimeFormatter.ISO_LOCAL_DATE);
                if (LocalDate.now().isAfter(expiryDate)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("License validation failed", e);
            return false;
        }
    }

    private static String generateHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((data + SECRET).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash).substring(0, 16);
    }

    private static String encrypt(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Reset license status cache (call after activation)
     */
    public static void resetCache() {
        currentStatus = null;
    }
}
