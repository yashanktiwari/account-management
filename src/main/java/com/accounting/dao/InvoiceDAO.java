package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Invoice;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private static final Logger log = AppLogger.get(InvoiceDAO.class);

    public int save(Invoice inv) {
        String sql = """
            INSERT INTO invoices (invoice_date, invoice_no, account_id, account_name, total_qty,
                total_amt, total_tax, grand_total, trans_type, tax_type, cash_credit, voucher_type,
                vehicle_no, remarks)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, Date.valueOf(inv.getInvoiceDate()));
            ps.setString(2, inv.getInvoiceNo());
            ps.setInt(3, inv.getAccountId());
            ps.setString(4, inv.getAccountName());
            ps.setDouble(5, inv.getTotalQty());
            ps.setDouble(6, inv.getTotalAmt());
            ps.setDouble(7, inv.getTotalTax());
            ps.setDouble(8, inv.getGrandTotal());
            ps.setString(9, inv.getTransType());
            ps.setString(10, inv.getTaxType());
            ps.setString(11, inv.getCashCredit());
            ps.setString(12, inv.getVoucherType());
            ps.setString(13, inv.getVehicleNo());
            ps.setString(14, inv.getRemarks());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Failed to save invoice: {}", inv.getInvoiceNo(), e);
        }
        return -1;
    }

    public boolean update(Invoice inv) {
        String sql = """
            UPDATE invoices SET invoice_date=?, invoice_no=?, account_id=?, account_name=?,
                total_qty=?, total_amt=?, total_tax=?, grand_total=?, trans_type=?, tax_type=?,
                cash_credit=?, voucher_type=?, vehicle_no=?, remarks=?
            WHERE id=?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(inv.getInvoiceDate()));
            ps.setString(2, inv.getInvoiceNo());
            ps.setInt(3, inv.getAccountId());
            ps.setString(4, inv.getAccountName());
            ps.setDouble(5, inv.getTotalQty());
            ps.setDouble(6, inv.getTotalAmt());
            ps.setDouble(7, inv.getTotalTax());
            ps.setDouble(8, inv.getGrandTotal());
            ps.setString(9, inv.getTransType());
            ps.setString(10, inv.getTaxType());
            ps.setString(11, inv.getCashCredit());
            ps.setString(12, inv.getVoucherType());
            ps.setString(13, inv.getVehicleNo());
            ps.setString(14, inv.getRemarks());
            ps.setInt(15, inv.getId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to update invoice: {}", inv.getInvoiceNo(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM invoices WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to delete invoice id={}", id, e);
        }
        return false;
    }

    public List<Invoice> search(String transType, String searchBy, String selectOne,
                                 String cashCredit, String orderWise,
                                 LocalDate fromDate, LocalDate toDate) {

        List<Invoice> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM invoices WHERE invoice_date BETWEEN ? AND ?");
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(fromDate));
        params.add(Date.valueOf(toDate));

        if (transType != null && !transType.isBlank() && !"All".equals(transType)) {
            sql.append(" AND trans_type=?");
            params.add(transType);
        }

        if (cashCredit != null && !cashCredit.isBlank() && !"All".equals(cashCredit)) {
            sql.append(" AND cash_credit=?");
            params.add(cashCredit);
        }

        if (selectOne != null && !selectOne.isBlank()) {
            if ("Voucher_Type".equals(searchBy)) {
                sql.append(" AND voucher_type LIKE ?");
                params.add("%" + selectOne + "%");
            } else if ("Account_Name".equals(searchBy)) {
                sql.append(" AND account_name LIKE ?");
                params.add("%" + selectOne + "%");
            }
        }

        if ("Date Wise".equals(orderWise)) {
            sql.append(" ORDER BY invoice_date DESC, id DESC");
        } else {
            sql.append(" ORDER BY invoice_no DESC");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Date d) ps.setDate(i + 1, d);
                else ps.setString(i + 1, (String) p);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvoice(rs));
            }
        } catch (Exception e) {
            log.error("Failed to search invoices", e);
        }
        return list;
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceDate(rs.getDate("invoice_date").toLocalDate());
        inv.setInvoiceNo(rs.getString("invoice_no"));
        inv.setAccountId(rs.getInt("account_id"));
        inv.setAccountName(rs.getString("account_name"));
        inv.setTotalQty(rs.getDouble("total_qty"));
        inv.setTotalAmt(rs.getDouble("total_amt"));
        inv.setTotalTax(rs.getDouble("total_tax"));
        inv.setGrandTotal(rs.getDouble("grand_total"));
        inv.setTransType(rs.getString("trans_type"));
        inv.setTaxType(rs.getString("tax_type"));
        inv.setCashCredit(rs.getString("cash_credit"));
        inv.setVoucherType(rs.getString("voucher_type"));
        inv.setVehicleNo(rs.getString("vehicle_no"));
        inv.setRemarks(rs.getString("remarks"));
        inv.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        inv.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return inv;
    }
}
