package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.SaleReceipt;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class SaleReceiptDAO {

    private static final Logger log = get(SaleReceiptDAO.class);

    public void save(SaleReceipt receipt) throws Exception {
        String sql = """
                INSERT INTO sale_receipts (receipt_no, receipt_date, party_id, party_name, amount,
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
            log.info("Sale receipt saved: {}", receipt.getReceiptNo());
        }
    }

    public void update(SaleReceipt receipt) throws Exception {
        String sql = """
                UPDATE sale_receipts SET receipt_no=?, receipt_date=?, party_id=?, party_name=?,
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
            log.info("Sale receipt updated: {}", receipt.getReceiptNo());
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM sale_receipts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            log.info("Sale receipt deleted: {}", id);
        }
    }

    public SaleReceipt findById(int id) throws Exception {
        String sql = "SELECT * FROM sale_receipts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapReceipt(rs);
            }
        }
        return null;
    }

    public List<SaleReceipt> getAll() throws Exception {
        String sql = "SELECT * FROM sale_receipts ORDER BY receipt_date DESC";
        List<SaleReceipt> receipts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) receipts.add(mapReceipt(rs));
        }
        return receipts;
    }

    public List<SaleReceipt> findByPartyId(int partyId) throws Exception {
        String sql = "SELECT * FROM sale_receipts WHERE party_id=? ORDER BY receipt_date DESC";
        List<SaleReceipt> receipts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, partyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) receipts.add(mapReceipt(rs));
            }
        }
        return receipts;
    }

    public List<SaleReceipt> searchAllColumns(String keyword) throws Exception {
        String sql = """
                SELECT * FROM sale_receipts
                WHERE CONCAT_WS(' ',
                    IFNULL(CAST(id AS CHAR),''), IFNULL(receipt_no,''), IFNULL(CAST(receipt_date AS CHAR),''),
                    IFNULL(CAST(party_id AS CHAR),''), IFNULL(party_name,''), IFNULL(CAST(amount AS CHAR),''),
                    IFNULL(payment_mode,''), IFNULL(cheque_no,''), IFNULL(bank_name,''), IFNULL(remarks,'')
                ) LIKE ?
                ORDER BY receipt_date DESC
                """;
        List<SaleReceipt> receipts = new ArrayList<>();
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
        String sql = "SELECT receipt_no FROM sale_receipts ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("receipt_no");
        }
        return null;
    }

    private SaleReceipt mapReceipt(ResultSet rs) throws SQLException {
        SaleReceipt r = new SaleReceipt();
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
