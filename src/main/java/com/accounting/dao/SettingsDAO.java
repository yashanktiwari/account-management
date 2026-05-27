package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Settings;
import org.slf4j.Logger;

import java.sql.*;

import static com.accounting.util.AppLogger.get;

public class SettingsDAO {

    private static final Logger log = get(SettingsDAO.class);

    public void ensureSettingsTable() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "settings", null)) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("""
                            CREATE TABLE settings (
                                id INT AUTO_INCREMENT PRIMARY KEY,
                                setting_key VARCHAR(100) UNIQUE NOT NULL,
                                setting_value VARCHAR(255) NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                            )
                        """);
                        log.info("Settings table created");
                    }
                }
            }
        }
    }

    public String getSetting(String key) throws Exception {
        ensureSettingsTable();
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        }
        return null;
    }

    public void saveSetting(String key, String value) throws Exception {
        ensureSettingsTable();
        String sql = """
            INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE setting_value = ?, updated_at = CURRENT_TIMESTAMP
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.setString(3, value);
            pstmt.executeUpdate();
            log.info("Setting saved: {} = {}", key, value);
        }
    }
}
