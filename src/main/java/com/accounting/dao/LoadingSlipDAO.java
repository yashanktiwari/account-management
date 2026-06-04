package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.LoadingSlip;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class LoadingSlipDAO {

    private static final Logger log = get(LoadingSlipDAO.class);

    public void ensureTable() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS loading_slips (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    slip_no VARCHAR(50) NOT NULL UNIQUE,
                    slip_date DATE NOT NULL,
                    party_name VARCHAR(255),
                    vehicle_no VARCHAR(50),
                    gr_no VARCHAR(50),
                    station VARCHAR(255),
                    to_location VARCHAR(255),
                    weight VARCHAR(50),
                    rate VARCHAR(50),
                    freight_amount DECIMAL(15,2) DEFAULT 0,
                    advance_amount DECIMAL(15,2) DEFAULT 0,
                    balance_amount DECIMAL(15,2) DEFAULT 0,
                    bank_name VARCHAR(255),
                    account_no VARCHAR(100),
                    ifsc_code VARCHAR(20),
                    status VARCHAR(20) DEFAULT 'SAVED',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);
        }
    }

    public void save(LoadingSlip slip) throws Exception {
        ensureTable();
        String sql = """
                INSERT INTO loading_slips (slip_no, slip_date, party_name, vehicle_no, gr_no,
                station, to_location, weight, rate, freight_amount, advance_amount, balance_amount,
                bank_name, account_no, ifsc_code, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, slip.getSlipNo());
            pstmt.setDate(2, java.sql.Date.valueOf(slip.getSlipDate()));
            pstmt.setString(3, slip.getPartyName());
            pstmt.setString(4, slip.getVehicleNo());
            pstmt.setString(5, slip.getGrNo());
            pstmt.setString(6, slip.getStation());
            pstmt.setString(7, slip.getToLocation());
            pstmt.setString(8, slip.getWeight());
            pstmt.setString(9, slip.getRate());
            pstmt.setDouble(10, slip.getFreightAmount());
            pstmt.setDouble(11, slip.getAdvanceAmount());
            pstmt.setDouble(12, slip.getBalanceAmount());
            pstmt.setString(13, slip.getBankName());
            pstmt.setString(14, slip.getAccountNo());
            pstmt.setString(15, slip.getIfscCode());
            pstmt.setString(16, slip.getStatus());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) slip.setId(rs.getInt(1));
            }
            log.info("Loading slip saved: {}", slip.getSlipNo());
        }
    }

    public void update(LoadingSlip slip) throws Exception {
        ensureTable();
        String sql = """
                UPDATE loading_slips SET slip_no=?, slip_date=?, party_name=?, vehicle_no=?, gr_no=?,
                station=?, to_location=?, weight=?, rate=?, freight_amount=?, advance_amount=?,
                balance_amount=?, bank_name=?, account_no=?, ifsc_code=?, status=?, updated_at=NOW()
                WHERE id=?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, slip.getSlipNo());
            pstmt.setDate(2, java.sql.Date.valueOf(slip.getSlipDate()));
            pstmt.setString(3, slip.getPartyName());
            pstmt.setString(4, slip.getVehicleNo());
            pstmt.setString(5, slip.getGrNo());
            pstmt.setString(6, slip.getStation());
            pstmt.setString(7, slip.getToLocation());
            pstmt.setString(8, slip.getWeight());
            pstmt.setString(9, slip.getRate());
            pstmt.setDouble(10, slip.getFreightAmount());
            pstmt.setDouble(11, slip.getAdvanceAmount());
            pstmt.setDouble(12, slip.getBalanceAmount());
            pstmt.setString(13, slip.getBankName());
            pstmt.setString(14, slip.getAccountNo());
            pstmt.setString(15, slip.getIfscCode());
            pstmt.setString(16, slip.getStatus());
            pstmt.setInt(17, slip.getId());

            pstmt.executeUpdate();
            log.info("Loading slip updated: {}", slip.getSlipNo());
        }
    }

    public void delete(int id) throws Exception {
        ensureTable();
        String sql = "DELETE FROM loading_slips WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            log.info("Loading slip deleted: {}", id);
        }
    }

    public LoadingSlip findById(int id) throws Exception {
        ensureTable();
        String sql = "SELECT * FROM loading_slips WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapSlip(rs);
                }
            }
        }
        return null;
    }

    public List<LoadingSlip> getAll() throws Exception {
        ensureTable();
        String sql = "SELECT * FROM loading_slips ORDER BY id DESC";
        List<LoadingSlip> slips = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                slips.add(mapSlip(rs));
            }
        }
        return slips;
    }

    public List<LoadingSlip> searchAllColumns(String keyword) throws Exception {
        ensureTable();
        String sql = """
                SELECT *
                FROM loading_slips
                WHERE CONCAT_WS(' ',
                    IFNULL(CAST(id AS CHAR), ''),
                    IFNULL(slip_no, ''),
                    IFNULL(CAST(slip_date AS CHAR), ''),
                    IFNULL(party_name, ''),
                    IFNULL(vehicle_no, ''),
                    IFNULL(gr_no, ''),
                    IFNULL(station, ''),
                    IFNULL(to_location, ''),
                    IFNULL(weight, ''),
                    IFNULL(rate, ''),
                    IFNULL(CAST(freight_amount AS CHAR), ''),
                    IFNULL(CAST(advance_amount AS CHAR), ''),
                    IFNULL(CAST(balance_amount AS CHAR), ''),
                    IFNULL(bank_name, ''),
                    IFNULL(account_no, ''),
                    IFNULL(ifsc_code, ''),
                    IFNULL(status, '')
                ) LIKE ?
                ORDER BY id DESC
                """;
        List<LoadingSlip> slips = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    slips.add(mapSlip(rs));
                }
            }
        }
        return slips;
    }

    public String getLastSlipNumber() throws Exception {
        ensureTable();
        String sql = "SELECT slip_no FROM loading_slips ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("slip_no");
            }
        }
        return null;
    }

    private LoadingSlip mapSlip(ResultSet rs) throws SQLException {
        LoadingSlip slip = new LoadingSlip();
        slip.setId(rs.getInt("id"));
        slip.setSlipNo(rs.getString("slip_no"));
        slip.setSlipDate(rs.getDate("slip_date").toLocalDate());
        slip.setPartyName(rs.getString("party_name"));
        slip.setVehicleNo(rs.getString("vehicle_no"));
        slip.setGrNo(rs.getString("gr_no"));
        slip.setStation(rs.getString("station"));
        slip.setToLocation(rs.getString("to_location"));
        slip.setWeight(rs.getString("weight"));
        slip.setRate(rs.getString("rate"));
        slip.setFreightAmount(rs.getDouble("freight_amount"));
        slip.setAdvanceAmount(rs.getDouble("advance_amount"));
        slip.setBalanceAmount(rs.getDouble("balance_amount"));
        slip.setBankName(rs.getString("bank_name"));
        slip.setAccountNo(rs.getString("account_no"));
        slip.setIfscCode(rs.getString("ifsc_code"));
        slip.setStatus(rs.getString("status"));
        slip.setCreatedAt(rs.getDate("created_at").toLocalDate());
        slip.setUpdatedAt(rs.getDate("updated_at").toLocalDate());
        return slip;
    }
}
