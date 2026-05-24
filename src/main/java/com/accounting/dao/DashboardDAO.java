package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Payment;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    private static final Logger log = AppLogger.get(DashboardDAO.class);

    public double getTotalOutstanding() {
        String sql = """
            SELECT COALESCE(SUM(i.grand_total), 0) - COALESCE(
                (SELECT SUM(p.amount) FROM payments p WHERE p.voucher_type='RECEIPT'), 0
            ) AS outstanding
            FROM invoices i WHERE i.trans_type='SALE' AND i.cash_credit='CREDIT'
        """;
        return fetchDouble(sql);
    }

    public double getOverdueAmount() {
        String sql = """
            SELECT COALESCE(SUM(i.grand_total), 0) - COALESCE(
                (SELECT SUM(p.amount) FROM payments p
                 WHERE p.voucher_type='RECEIPT'
                   AND p.against_invoice_id IN (
                       SELECT id FROM invoices
                       WHERE trans_type='SALE' AND cash_credit='CREDIT'
                         AND DATE_ADD(invoice_date, INTERVAL COALESCE(
                             (SELECT a.credit_period FROM accounts a WHERE a.id = invoices.account_id), 0
                         ) DAY) < CURDATE()
                   )
                ), 0
            ) AS overdue
            FROM invoices i
            WHERE i.trans_type='SALE' AND i.cash_credit='CREDIT'
              AND DATE_ADD(i.invoice_date, INTERVAL COALESCE(
                  (SELECT a.credit_period FROM accounts a WHERE a.id = i.account_id), 0
              ) DAY) < CURDATE()
        """;
        return fetchDouble(sql);
    }

    public double getDueToday() {
        String sql = """
            SELECT COALESCE(SUM(i.grand_total), 0) AS due_today
            FROM invoices i
            WHERE i.trans_type='SALE' AND i.cash_credit='CREDIT'
              AND DATE_ADD(i.invoice_date, INTERVAL COALESCE(
                  (SELECT a.credit_period FROM accounts a WHERE a.id = i.account_id), 0
              ) DAY) = CURDATE()
        """;
        return fetchDouble(sql);
    }

    public double getCollectedToday() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE voucher_type='RECEIPT' AND payment_date = CURDATE()";
        return fetchDouble(sql);
    }

    public double getCollectedThisMonth() {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM payments
            WHERE voucher_type='RECEIPT'
              AND MONTH(payment_date) = MONTH(CURDATE())
              AND YEAR(payment_date) = YEAR(CURDATE())
        """;
        return fetchDouble(sql);
    }

    public int getPendingInvoiceCount() {
        String sql = """
            SELECT COUNT(*) FROM invoices i
            WHERE i.trans_type='SALE' AND i.cash_credit='CREDIT'
              AND i.grand_total > COALESCE(
                  (SELECT SUM(p.amount) FROM payments p WHERE p.against_invoice_id = i.id), 0
              )
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            log.error("Failed to fetch pending invoice count", e);
        }
        return 0;
    }

    public List<OutstandingCustomer> getTopOutstandingCustomers(int limit) {
        String sql = """
            SELECT a.id, a.account_name, a.mobile, a.root_area_name,
                   COALESCE(SUM(i.grand_total), 0) -
                   COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.account_id = a.id AND p.voucher_type='RECEIPT'), 0)
                   AS outstanding
            FROM accounts a
            JOIN invoices i ON i.account_id = a.id AND i.trans_type='SALE' AND i.cash_credit='CREDIT'
            GROUP BY a.id, a.account_name, a.mobile, a.root_area_name
            HAVING outstanding > 0
            ORDER BY outstanding DESC
            LIMIT ?
        """;
        List<OutstandingCustomer> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OutstandingCustomer(
                            rs.getInt("id"),
                            rs.getString("account_name"),
                            rs.getString("mobile"),
                            rs.getString("root_area_name"),
                            rs.getDouble("outstanding")
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch top outstanding customers", e);
        }
        return list;
    }

    public List<Payment> getRecentPayments(int limit) {
        String sql = "SELECT * FROM payments WHERE voucher_type='RECEIPT' ORDER BY payment_date DESC, id DESC LIMIT ?";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setPaymentDate(rs.getDate("payment_date").toLocalDate());
                    p.setVoucherNo(rs.getString("voucher_no"));
                    p.setVoucherType(rs.getString("voucher_type"));
                    p.setAccountId(rs.getInt("account_id"));
                    p.setAccountName(rs.getString("account_name"));
                    p.setParticulars(rs.getString("particulars"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setAgainstInvoiceNo(rs.getString("against_invoice_no"));
                    p.setRemarks(rs.getString("remarks"));
                    list.add(p);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch recent payments", e);
        }
        return list;
    }

    private double fetchDouble(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            log.error("Dashboard query failed: {}", sql.substring(0, Math.min(60, sql.length())), e);
        }
        return 0;
    }

    public static class OutstandingCustomer {
        private final int accountId;
        private final String accountName;
        private final String mobile;
        private final String area;
        private final double outstanding;

        public OutstandingCustomer(int accountId, String accountName, String mobile, String area, double outstanding) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.mobile = mobile;
            this.area = area;
            this.outstanding = outstanding;
        }

        public int getAccountId() { return accountId; }
        public String getAccountName() { return accountName; }
        public String getMobile() { return mobile; }
        public String getArea() { return area; }
        public double getOutstanding() { return outstanding; }
    }
}
