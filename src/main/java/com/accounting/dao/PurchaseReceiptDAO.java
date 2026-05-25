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

    public void save(PurchaseReceipt receipt) throws Exception {
        String sql = """
                INSERT INTO purchase_receipts (receipt_no, receipt_date, party_id, party_name, amount,
                payment_mode, cheque_no, cheque_date, bank_name, remarks, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
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
            pstmt.setString(11, receipt.getStatus());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) receipt.setId(rs.getInt(1));
            }
            log.info("Purchase receipt saved: {}", receipt.getReceiptNo());
        }
    }

    public void update(PurchaseReceipt receipt) throws Exception {
        String sql = """
                UPDATE purchase_receipts SET receipt_no=?, receipt_date=?, party_id=?, party_name=?,
                amount=?, payment_mode=?, cheque_no=?, cheque_date=?, bank_name=?, remarks=?,
                status=?, updated_at=NOW() WHERE id=?
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
            pstmt.setString(11, receipt.getStatus());
            pstmt.setInt(12, receipt.getId());

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
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getDate("created_at").toLocalDate());
        r.setUpdatedAt(rs.getDate("updated_at").toLocalDate());
        return r;
    }
}
