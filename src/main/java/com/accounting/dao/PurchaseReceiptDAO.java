package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.PurchaseReceipt;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class PurchaseReceiptDAO {

    private static final Logger log = get(PurchaseReceiptDAO.class);

    private void ensureReceiptColumns() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> existing = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "purchase_receipts", null)) {
                while (rs.next()) {
                    existing.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            try (Statement stmt = conn.createStatement()) {
                if (!existing.contains("tds_percentage")) {
                    stmt.executeUpdate("ALTER TABLE purchase_receipts ADD COLUMN tds_percentage DOUBLE DEFAULT 0");
                }
                if (!existing.contains("tds_amount")) {
                    stmt.executeUpdate("ALTER TABLE purchase_receipts ADD COLUMN tds_amount DOUBLE DEFAULT 0");
                }
                if (!existing.contains("kasar_amount")) {
                    stmt.executeUpdate("ALTER TABLE purchase_receipts ADD COLUMN kasar_amount DOUBLE DEFAULT 0");
                }
            }
        }
    }

    public void save(PurchaseReceipt receipt) throws Exception {
        ensureReceiptColumns();
        String sql = """
                INSERT INTO purchase_receipts (receipt_no, receipt_date, party_id, party_name, amount,
                payment_mode, cheque_no, cheque_date, bank_name, remarks, tds_percentage, tds_amount, kasar_amount, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, receipt.getReceiptNo());
            pstmt.setDate(2, java.sql.Date.valueOf(receipt.getReceiptDate()));
            pstmt.setInt(3, receipt.getPartyId());
            pstmt.setString(4, receipt.getPartyName());
            pstmt.setDouble(5, receipt.getAmount());
            pstmt.setString(6, receipt.getPaymentMode());
            pstmt.setString(7, receipt.getChequeNo());
            pstmt.setDate(8, receipt.getChequeDate() != null ? java.sql.Date.valueOf(receipt.getChequeDate()) : null);
            pstmt.setString(9, receipt.getBankName());
            pstmt.setString(10, receipt.getRemarks());
            pstmt.setDouble(11, receipt.getTdsPercentage());
            pstmt.setDouble(12, receipt.getTdsAmount());
            pstmt.setDouble(13, receipt.getKasarAmount());
            pstmt.setString(14, receipt.getStatus());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) receipt.setId(rs.getInt(1));
            }
            log.info("Purchase receipt saved: {}", receipt.getReceiptNo());
        }
    }

    public void update(PurchaseReceipt receipt) throws Exception {
        ensureReceiptColumns();
        String sql = """
                UPDATE purchase_receipts SET receipt_no=?, receipt_date=?, party_id=?, party_name=?,
                amount=?, payment_mode=?, cheque_no=?, cheque_date=?, bank_name=?, remarks=?,
                tds_percentage=?, tds_amount=?, kasar_amount=?, status=?, updated_at=NOW() WHERE id=?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receipt.getReceiptNo());
            pstmt.setDate(2, java.sql.Date.valueOf(receipt.getReceiptDate()));
            pstmt.setInt(3, receipt.getPartyId());
            pstmt.setString(4, receipt.getPartyName());
            pstmt.setDouble(5, receipt.getAmount());
            pstmt.setString(6, receipt.getPaymentMode());
            pstmt.setString(7, receipt.getChequeNo());
            pstmt.setDate(8, receipt.getChequeDate() != null ? java.sql.Date.valueOf(receipt.getChequeDate()) : null);
            pstmt.setString(9, receipt.getBankName());
            pstmt.setString(10, receipt.getRemarks());
            pstmt.setDouble(11, receipt.getTdsPercentage());
            pstmt.setDouble(12, receipt.getTdsAmount());
            pstmt.setDouble(13, receipt.getKasarAmount());
            pstmt.setString(14, receipt.getStatus());
            pstmt.setInt(15, receipt.getId());

            pstmt.executeUpdate();
            log.info("Purchase receipt updated: {}", receipt.getReceiptNo());
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM purchase_receipts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            log.info("Purchase receipt deleted: {}", id);
        }
    }

    public PurchaseReceipt findById(int id) throws Exception {
        String sql = "SELECT * FROM purchase_receipts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapReceipt(rs);
            }
        }
        return null;
    }

    public List<PurchaseReceipt> getAll() throws Exception {
        String sql = "SELECT * FROM purchase_receipts ORDER BY receipt_date DESC";
        List<PurchaseReceipt> receipts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) receipts.add(mapReceipt(rs));
        }
        return receipts;
    }

    public List<PurchaseReceipt> findByPartyId(int partyId) throws Exception {
        String sql = "SELECT * FROM purchase_receipts WHERE party_id=? ORDER BY receipt_date DESC";
        List<PurchaseReceipt> receipts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, partyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) receipts.add(mapReceipt(rs));
            }
        }
        return receipts;
    }

    public List<PurchaseReceipt> searchAllColumns(String keyword) throws Exception {
        String sql = """
                SELECT * FROM purchase_receipts
                WHERE CONCAT_WS(' ',
                    IFNULL(CAST(id AS CHAR),''), IFNULL(receipt_no,''), IFNULL(CAST(receipt_date AS CHAR),''),
                    IFNULL(CAST(party_id AS CHAR),''), IFNULL(party_name,''), IFNULL(CAST(amount AS CHAR),''),
                    IFNULL(payment_mode,''), IFNULL(cheque_no,''), IFNULL(bank_name,''), IFNULL(remarks,'')
                ) LIKE ?
                ORDER BY receipt_date DESC
                """;
        List<PurchaseReceipt> receipts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) receipts.add(mapReceipt(rs));
            }
        }
        return receipts;
    }

    public String getLastReceiptNumber() throws Exception {
        String sql = "SELECT receipt_no FROM purchase_receipts ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("receipt_no");
        }
        return null;
    }

    private PurchaseReceipt mapReceipt(ResultSet rs) throws SQLException {
        PurchaseReceipt r = new PurchaseReceipt();
        r.setId(rs.getInt("id"));
        r.setReceiptNo(rs.getString("receipt_no"));
        r.setReceiptDate(rs.getDate("receipt_date").toLocalDate());
        r.setPartyId(rs.getInt("party_id"));
        r.setPartyName(rs.getString("party_name"));
        r.setAmount(rs.getDouble("amount"));
        r.setPaymentMode(rs.getString("payment_mode"));
        r.setChequeNo(rs.getString("cheque_no"));
        r.setChequeDate(rs.getDate("cheque_date") != null ? rs.getDate("cheque_date").toLocalDate() : null);
        r.setBankName(rs.getString("bank_name"));
        r.setRemarks(rs.getString("remarks"));
        try { r.setTdsPercentage(rs.getDouble("tds_percentage")); } catch (Exception ignored) {}
        try { r.setTdsAmount(rs.getDouble("tds_amount")); } catch (Exception ignored) {}
        try { r.setKasarAmount(rs.getDouble("kasar_amount")); } catch (Exception ignored) {}
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getDate("created_at").toLocalDate());
        r.setUpdatedAt(rs.getDate("updated_at").toLocalDate());
        return r;
    }
}
