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
                CREATE TABLE IF NOT EXISTS accounts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    account_name VARCHAR(255) NOT NULL,
                    account_type ENUM('CUSTOMER','SUPPLIER') NOT NULL,
                    ac_as VARCHAR(50),
                    ac_type VARCHAR(100),
                    mailing_name VARCHAR(255),
                    address TEXT,
                    state_name VARCHAR(100),
                    state_code VARCHAR(10),
                    city_name VARCHAR(100),
                    fax VARCHAR(50),
                    pin_code VARCHAR(10),
                    email VARCHAR(255),
                    mobile VARCHAR(20),
                    root_area_name VARCHAR(255),
                    gstin VARCHAR(20),
                    cst_no VARCHAR(50),
                    tan_no VARCHAR(50),
                    pan_no VARCHAR(20),
                    tds_percent DECIMAL(5,2) DEFAULT 0,
                    tds_applicable ENUM('YES','NO') DEFAULT 'NO',
                    aadhar_no VARCHAR(20),
                    drugs_lic_no VARCHAR(50),
                    credit_period INT DEFAULT 0,
                    credit_amt_limit DECIMAL(15,2) DEFAULT 0,
                    opening_balance DECIMAL(15,2) DEFAULT 0,
                    balance_type ENUM('Debit','Credit') DEFAULT 'Debit',
                    nature_of_payment VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """);

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
                CREATE TABLE IF NOT EXISTS invoices (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_date DATE NOT NULL,
                    invoice_no VARCHAR(50) NOT NULL,
                    account_id INT,
                    account_name VARCHAR(255),
                    total_qty DECIMAL(15,3) DEFAULT 0,
                    total_amt DECIMAL(15,2) DEFAULT 0,
                    total_tax DECIMAL(15,2) DEFAULT 0,
                    grand_total DECIMAL(15,2) DEFAULT 0,
                    trans_type ENUM('SALE','PURCHASE') NOT NULL,
                    tax_type VARCHAR(20),
                    cash_credit ENUM('CASH','CREDIT') DEFAULT 'CREDIT',
                    voucher_type VARCHAR(50),
                    vehicle_no VARCHAR(20),
                    remarks TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    payment_date DATE NOT NULL,
                    voucher_no VARCHAR(50),
                    voucher_type ENUM('RECEIPT','PAYMENT') NOT NULL,
                    account_id INT,
                    account_name VARCHAR(255),
                    particulars VARCHAR(255),
                    amount DECIMAL(15,2) DEFAULT 0,
                    against_invoice_id INT,
                    against_invoice_no VARCHAR(50),
                    remarks TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL,
                    FOREIGN KEY (against_invoice_id) REFERENCES invoices(id) ON DELETE SET NULL
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
