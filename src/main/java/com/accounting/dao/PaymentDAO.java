package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Payment;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final Logger log = AppLogger.get(PaymentDAO.class);

    public void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS payments (
                id INT AUTO_INCREMENT PRIMARY KEY,
                payment_date DATE NOT NULL,
                voucher_no VARCHAR(50),
                voucher_type VARCHAR(20),
                account_id INT,
                account_name VARCHAR(255),
                particulars VARCHAR(500),
                amount DECIMAL(15, 2) DEFAULT 0,
                against_invoice_id INT,
                against_invoice_no VARCHAR(50),
                remarks VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            log.error("Failed to ensure payments table", e);
        }
    }

    public int save(Payment p) {
        ensureTable();
        String sql = """
            INSERT INTO payments (payment_date, voucher_no, voucher_type, account_id, account_name,
                particulars, amount, against_invoice_id, against_invoice_no, remarks)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, Date.valueOf(p.getPaymentDate()));
            ps.setString(2, p.getVoucherNo());
            ps.setString(3, p.getVoucherType());
            ps.setInt(4, p.getAccountId());
            ps.setString(5, p.getAccountName());
            ps.setString(6, p.getParticulars());
            ps.setDouble(7, p.getAmount());

            if (p.getAgainstInvoiceId() != null) {
                ps.setInt(8, p.getAgainstInvoiceId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.setString(9, p.getAgainstInvoiceNo());
            ps.setString(10, p.getRemarks());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Failed to save payment: {}", p.getVoucherNo(), e);
        }
        return -1;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM payments WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to delete payment id={}", id, e);
        }
        return false;
    }

    public List<Payment> search(String voucherType, LocalDate fromDate, LocalDate toDate) {
        List<Payment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM payments WHERE payment_date BETWEEN ? AND ?");
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(fromDate));
        params.add(Date.valueOf(toDate));

        if (voucherType != null && !voucherType.isBlank() && !"All".equals(voucherType)) {
            sql.append(" AND voucher_type=?");
            params.add(voucherType);
        }

        sql.append(" ORDER BY payment_date DESC, id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Date d) ps.setDate(i + 1, d);
                else ps.setString(i + 1, (String) p);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPayment(rs));
            }
        } catch (Exception e) {
            log.error("Failed to search payments", e);
        }
        return list;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setPaymentDate(rs.getDate("payment_date").toLocalDate());
        p.setVoucherNo(rs.getString("voucher_no"));
        p.setVoucherType(rs.getString("voucher_type"));
        p.setAccountId(rs.getInt("account_id"));
        p.setAccountName(rs.getString("account_name"));
        p.setParticulars(rs.getString("particulars"));
        p.setAmount(rs.getDouble("amount"));

        int againstId = rs.getInt("against_invoice_id");
        p.setAgainstInvoiceId(rs.wasNull() ? null : againstId);

        p.setAgainstInvoiceNo(rs.getString("against_invoice_no"));
        p.setRemarks(rs.getString("remarks"));
        p.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        p.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return p;
    }
}
