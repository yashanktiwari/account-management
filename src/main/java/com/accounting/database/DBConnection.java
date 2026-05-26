package com.accounting.database;

import com.accounting.util.AppLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final Logger log = AppLogger.get(DBConnection.class);

    private static String host;
    private static String port;
    private static String databaseName;
    private static String username;
    private static String password;

    private static HikariDataSource dataSource;

    public static void setDatabaseConfig(
            String host, String port, String databaseName,
            String username, String password
    ) {
        DBConnection.host = host;
        DBConnection.port = port;
        DBConnection.databaseName = databaseName;
        DBConnection.username = username;
        DBConnection.password = password;
        initializePool();
    }

    public static void initializePool() {
        if (dataSource != null) {
            log.info("[HikariPool] Closing existing pool before re-initializing.");
            dataSource.close();
        }

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Kolkata";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(2000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        log.info("[HikariPool] Initializing pool -> {}:{}/{}", host, port, databaseName);
        long t0 = System.nanoTime();
        dataSource = new HikariDataSource(config);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("[HikariPool] Pool ready in {} ms (maxPoolSize={}, minIdle={})",
                elapsedMs, config.getMaximumPoolSize(), config.getMinimumIdle());
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized.");
        }
        long t0 = System.nanoTime();
        Connection conn = dataSource.getConnection();
        long durationMs = (System.nanoTime() - t0) / 1_000_000;

        if (durationMs > 50) {
            Logger perfLog = LoggerFactory.getLogger("PERF");
            perfLog.warn("[POOL WAIT] Connection took {} ms", durationMs);
        }

        return conn;
    }

    public static String getHost() { return host; }
    public static String getPort() { return port; }
    public static String getDatabaseName() { return databaseName; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }

    public static void createDatabaseIfNotExists() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + databaseName + "`");
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS parties (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    type ENUM('CUSTOMER','SUPPLIER') NOT NULL,
                    mailing_name VARCHAR(255),
                    address TEXT,
                    city VARCHAR(100),
                    state VARCHAR(100),
                    pincode VARCHAR(10),
                    mobile VARCHAR(20),
                    email VARCHAR(255),
                    pan VARCHAR(20),
                    gstin VARCHAR(20),
                    credit_limit VARCHAR(50),
                    opening_balance VARCHAR(50),
                    balance_type ENUM('DEBIT','CREDIT'),
                    nature_of_payment VARCHAR(100),
                    bank_name VARCHAR(100),
                    bank_account VARCHAR(50),
                    ifsc_code VARCHAR(20),
                    remarks TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """);

            // Migrate parties table — add new columns if they don't exist yet
            for (String alter : new String[]{
                    "ALTER TABLE parties ADD COLUMN IF NOT EXISTS owner_name VARCHAR(255)",
                    "ALTER TABLE parties ADD COLUMN IF NOT EXISTS cst_no VARCHAR(30)",
                    "ALTER TABLE parties ADD COLUMN IF NOT EXISTS tan_no VARCHAR(30)",
                    "ALTER TABLE parties ADD COLUMN IF NOT EXISTS tds VARCHAR(20)",
                    "ALTER TABLE parties ADD COLUMN IF NOT EXISTS aadhar_no VARCHAR(20)",
                    "ALTER TABLE parties ADD COLUMN IF NOT EXISTS routes TEXT"
            }) {
                try { stmt.executeUpdate(alter); } catch (Exception ignored) {}
            }

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS vehicles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    vehicle_no VARCHAR(20) NOT NULL UNIQUE,
                    vehicle_model VARCHAR(100),
                    account_name VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS purchase_invoices (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_no VARCHAR(50) NOT NULL UNIQUE,
                    invoice_date DATE NOT NULL,
                    party_id INT NOT NULL,
                    party_name VARCHAR(255),
                    voucher_type VARCHAR(50),
                    gst VARCHAR(10),
                    taxable_amount DECIMAL(15,2) DEFAULT 0,
                    sgst_amount DECIMAL(15,2) DEFAULT 0,
                    cgst_amount DECIMAL(15,2) DEFAULT 0,
                    igst_amount DECIMAL(15,2) DEFAULT 0,
                    total_gst DECIMAL(15,2) DEFAULT 0,
                    net_amount DECIMAL(15,2) DEFAULT 0,
                    remarks TEXT,
                    bank_name VARCHAR(100),
                    bank_account VARCHAR(50),
                    ifsc_code VARCHAR(20),
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sale_invoices (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_no VARCHAR(50) NOT NULL UNIQUE,
                    invoice_date DATE NOT NULL,
                    delivery_date DATE,
                    party_id INT NOT NULL,
                    party_name VARCHAR(255),
                    voucher_type VARCHAR(50),
                    gst VARCHAR(10),
                    taxable_amount DECIMAL(15,2) DEFAULT 0,
                    sgst_amount DECIMAL(15,2) DEFAULT 0,
                    cgst_amount DECIMAL(15,2) DEFAULT 0,
                    igst_amount DECIMAL(15,2) DEFAULT 0,
                    total_gst DECIMAL(15,2) DEFAULT 0,
                    net_amount DECIMAL(15,2) DEFAULT 0,
                    remarks TEXT,
                    rcvr_name VARCHAR(255),
                    rcvr_address TEXT,
                    rcvr_contact_no VARCHAR(20),
                    rcvr_gstin VARCHAR(20),
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS invoice_line_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_id INT NOT NULL,
                    lr_no VARCHAR(50),
                    container_no VARCHAR(50),
                    vehicle_no VARCHAR(20),
                    from_location VARCHAR(100),
                    to_location VARCHAR(100),
                    type VARCHAR(50),
                    basic_freight DECIMAL(15,2) DEFAULT 0,
                    detention_charge DECIMAL(15,2) DEFAULT 0,
                    total DECIMAL(15,2) DEFAULT 0,
                    FOREIGN KEY (invoice_id) REFERENCES purchase_invoices(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS purchase_receipts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    receipt_no VARCHAR(50) NOT NULL UNIQUE,
                    receipt_date DATE NOT NULL,
                    party_id INT NOT NULL,
                    party_name VARCHAR(255),
                    amount DECIMAL(15,2) DEFAULT 0,
                    payment_mode VARCHAR(50),
                    cheque_no VARCHAR(50),
                    cheque_date DATE,
                    bank_name VARCHAR(100),
                    remarks TEXT,
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sale_receipts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    receipt_no VARCHAR(50) NOT NULL UNIQUE,
                    receipt_date DATE NOT NULL,
                    party_id INT NOT NULL,
                    party_name VARCHAR(255),
                    amount DECIMAL(15,2) DEFAULT 0,
                    payment_mode VARCHAR(50),
                    cheque_no VARCHAR(50),
                    cheque_date DATE,
                    bank_name VARCHAR(100),
                    remarks TEXT,
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS root_areas (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    area_name VARCHAR(255) NOT NULL UNIQUE
                )
            """);

            log.info("[DB] All tables initialized successfully.");

        } catch (SQLException e) {
            log.error("Failed to initialize database tables", e);
        }
    }

    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
            log.info("[HikariPool] Pool closed.");
        }
    }
}
