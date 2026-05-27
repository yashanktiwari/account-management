package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.InvoiceLineItem;
import com.accounting.model.SaleInvoice;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class SaleInvoiceDAO {

    private static final Logger log = get(SaleInvoiceDAO.class);

    private void ensureInvoiceColumns() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(sale_invoices)")) {
            java.util.Set<String> existing = new java.util.HashSet<>();
            while (rs.next()) {
                existing.add(rs.getString("name").toLowerCase());
            }

            try (Statement stmt2 = conn.createStatement()) {
                if (!existing.contains("credit_debit")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN credit_debit VARCHAR(20)");
                }
                if (!existing.contains("account_name")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN account_name VARCHAR(255)");
                }
                if (!existing.contains("paid_by")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN paid_by VARCHAR(255)");
                }
                if (!existing.contains("payment_mode")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN payment_mode VARCHAR(50)");
                }
                if (!existing.contains("bank_name")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN bank_name VARCHAR(255)");
                }
                if (!existing.contains("bank_account")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN bank_account VARCHAR(255)");
                }
                if (!existing.contains("ifsc_code")) {
                    stmt2.executeUpdate("ALTER TABLE sale_invoices ADD COLUMN ifsc_code VARCHAR(20)");
                }
            }
        }
    }

    public void save(SaleInvoice invoice) throws Exception {
        ensureInvoiceColumns();
        String sql = """
                INSERT INTO sale_invoices (invoice_no, invoice_date, delivery_date, party_id, party_name,
                voucher_type, gst, taxable_amount, sgst_amount, cgst_amount, igst_amount, total_gst,
                net_amount, remarks, rcvr_name, rcvr_address, rcvr_contact_no, rcvr_gstin,
                credit_debit, account_name, paid_by, payment_mode, bank_name, bank_account, ifsc_code,
                status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, invoice.getInvoiceNo());
            pstmt.setDate(2, java.sql.Date.valueOf(invoice.getInvoiceDate()));
            pstmt.setDate(3, java.sql.Date.valueOf(invoice.getDeliveryDate()));
            pstmt.setInt(4, invoice.getPartyId());
            pstmt.setString(5, invoice.getPartyName());
            pstmt.setString(6, invoice.getVoucherType());
            pstmt.setString(7, invoice.getGst());
            pstmt.setDouble(8, invoice.getTaxableAmount());
            pstmt.setDouble(9, invoice.getSgstAmount());
            pstmt.setDouble(10, invoice.getCgstAmount());
            pstmt.setDouble(11, invoice.getIgstAmount());
            pstmt.setDouble(12, invoice.getTotalGst());
            pstmt.setDouble(13, invoice.getNetAmount());
            pstmt.setString(14, invoice.getRemarks());
            pstmt.setString(15, invoice.getRcvrName());
            pstmt.setString(16, invoice.getRcvrAddress());
            pstmt.setString(17, invoice.getRcvrContactNo());
            pstmt.setString(18, invoice.getRcvrGstin());
            pstmt.setString(19, invoice.getCreditDebit());
            pstmt.setString(20, invoice.getAccountName());
            pstmt.setString(21, invoice.getPaidBy());
            pstmt.setString(22, invoice.getPaymentMode());
            pstmt.setString(23, invoice.getBankName());
            pstmt.setString(24, invoice.getBankAccount());
            pstmt.setString(25, invoice.getIfscCode());
            pstmt.setString(26, invoice.getStatus());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) invoice.setId(rs.getInt(1));
            }

            for (InvoiceLineItem item : invoice.getLineItems()) {
                saveLineItem(invoice.getId(), item);
            }
            log.info("Sale invoice saved: {}", invoice.getInvoiceNo());
        }
    }

    public void update(SaleInvoice invoice) throws Exception {
        ensureInvoiceColumns();
        String sql = """
                UPDATE sale_invoices SET invoice_no=?, invoice_date=?, delivery_date=?, party_id=?,
                party_name=?, voucher_type=?, gst=?, taxable_amount=?, sgst_amount=?, cgst_amount=?,
                igst_amount=?, total_gst=?, net_amount=?, remarks=?, rcvr_name=?, rcvr_address=?,
                rcvr_contact_no=?, rcvr_gstin=?, credit_debit=?, account_name=?, paid_by=?, payment_mode=?,
                bank_name=?, bank_account=?, ifsc_code=?, status=?, updated_at=NOW() WHERE id=?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoice.getInvoiceNo());
            pstmt.setDate(2, java.sql.Date.valueOf(invoice.getInvoiceDate()));
            pstmt.setDate(3, java.sql.Date.valueOf(invoice.getDeliveryDate()));
            pstmt.setInt(4, invoice.getPartyId());
            pstmt.setString(5, invoice.getPartyName());
            pstmt.setString(6, invoice.getVoucherType());
            pstmt.setString(7, invoice.getGst());
            pstmt.setDouble(8, invoice.getTaxableAmount());
            pstmt.setDouble(9, invoice.getSgstAmount());
            pstmt.setDouble(10, invoice.getCgstAmount());
            pstmt.setDouble(11, invoice.getIgstAmount());
            pstmt.setDouble(12, invoice.getTotalGst());
            pstmt.setDouble(13, invoice.getNetAmount());
            pstmt.setString(14, invoice.getRemarks());
            pstmt.setString(15, invoice.getRcvrName());
            pstmt.setString(16, invoice.getRcvrAddress());
            pstmt.setString(17, invoice.getRcvrContactNo());
            pstmt.setString(18, invoice.getRcvrGstin());
            pstmt.setString(19, invoice.getCreditDebit());
            pstmt.setString(20, invoice.getAccountName());
            pstmt.setString(21, invoice.getPaidBy());
            pstmt.setString(22, invoice.getPaymentMode());
            pstmt.setString(23, invoice.getBankName());
            pstmt.setString(24, invoice.getBankAccount());
            pstmt.setString(25, invoice.getIfscCode());
            pstmt.setString(26, invoice.getStatus());
            pstmt.setInt(27, invoice.getId());

            pstmt.executeUpdate();
            deleteLineItems(invoice.getId());
            for (InvoiceLineItem item : invoice.getLineItems()) {
                saveLineItem(invoice.getId(), item);
            }
            log.info("Sale invoice updated: {}", invoice.getInvoiceNo());
        }
    }

    public void delete(int id) throws Exception {
        deleteLineItems(id);
        String sql = "DELETE FROM sale_invoices WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            log.info("Sale invoice deleted: {}", id);
        }
    }

    public SaleInvoice findById(int id) throws Exception {
        String sql = "SELECT * FROM sale_invoices WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    SaleInvoice inv = mapInvoice(rs);
                    inv.setLineItems(getLineItems(id));
                    return inv;
                }
            }
        }
        return null;
    }

    public List<SaleInvoice> getAll() throws Exception {
        String sql = "SELECT * FROM sale_invoices ORDER BY invoice_date DESC";
        List<SaleInvoice> invoices = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SaleInvoice inv = mapInvoice(rs);
                inv.setLineItems(getLineItems(inv.getId()));
                invoices.add(inv);
            }
        }
        return invoices;
    }

    private void saveLineItem(int invoiceId, InvoiceLineItem item) throws Exception {
        String sql = """
                INSERT INTO invoice_line_items (invoice_id, lr_no, container_no, vehicle_no, from_location,
                to_location, type, basic_freight, detention_charge, total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            pstmt.setString(2, item.getLrNo());
            pstmt.setString(3, item.getContainerNo());
            pstmt.setString(4, item.getVehicleNo());
            pstmt.setString(5, item.getFrom());
            pstmt.setString(6, item.getTo());
            pstmt.setString(7, item.getType());
            pstmt.setDouble(8, item.getBasicFreight());
            pstmt.setDouble(9, item.getDetentionCharge());
            pstmt.setDouble(10, item.getTotal());
            pstmt.executeUpdate();
        }
    }

    private List<InvoiceLineItem> getLineItems(int invoiceId) throws Exception {
        String sql = "SELECT * FROM invoice_line_items WHERE invoice_id=? ORDER BY id";
        List<InvoiceLineItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) items.add(mapLineItem(rs));
            }
        }
        return items;
    }

    private void deleteLineItems(int invoiceId) throws Exception {
        String sql = "DELETE FROM invoice_line_items WHERE invoice_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            pstmt.executeUpdate();
        }
    }

    private SaleInvoice mapInvoice(ResultSet rs) throws SQLException {
        SaleInvoice inv = new SaleInvoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceNo(rs.getString("invoice_no"));
        inv.setInvoiceDate(rs.getDate("invoice_date").toLocalDate());
        inv.setDeliveryDate(rs.getDate("delivery_date").toLocalDate());
        inv.setPartyId(rs.getInt("party_id"));
        inv.setPartyName(rs.getString("party_name"));
        inv.setVoucherType(rs.getString("voucher_type"));
        inv.setGst(rs.getString("gst"));
        inv.setTaxableAmount(rs.getDouble("taxable_amount"));
        inv.setSgstAmount(rs.getDouble("sgst_amount"));
        inv.setCgstAmount(rs.getDouble("cgst_amount"));
        inv.setIgstAmount(rs.getDouble("igst_amount"));
        inv.setTotalGst(rs.getDouble("total_gst"));
        inv.setNetAmount(rs.getDouble("net_amount"));
        inv.setRemarks(rs.getString("remarks"));
        inv.setRcvrName(rs.getString("rcvr_name"));
        inv.setRcvrAddress(rs.getString("rcvr_address"));
        inv.setRcvrContactNo(rs.getString("rcvr_contact_no"));
        inv.setRcvrGstin(rs.getString("rcvr_gstin"));
        inv.setCreditDebit(rs.getString("credit_debit"));
        inv.setAccountName(rs.getString("account_name"));
        inv.setPaidBy(rs.getString("paid_by"));
        inv.setPaymentMode(rs.getString("payment_mode"));
        inv.setBankName(rs.getString("bank_name"));
        inv.setBankAccount(rs.getString("bank_account"));
        inv.setIfscCode(rs.getString("ifsc_code"));
        inv.setStatus(rs.getString("status"));
        inv.setCreatedAt(rs.getDate("created_at").toLocalDate());
        inv.setUpdatedAt(rs.getDate("updated_at").toLocalDate());
        return inv;
    }

    private InvoiceLineItem mapLineItem(ResultSet rs) throws SQLException {
        InvoiceLineItem item = new InvoiceLineItem();
        item.setId(rs.getInt("id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setLrNo(rs.getString("lr_no"));
        item.setContainerNo(rs.getString("container_no"));
        item.setVehicleNo(rs.getString("vehicle_no"));
        item.setFrom(rs.getString("from_location"));
        item.setTo(rs.getString("to_location"));
        item.setType(rs.getString("type"));
        item.setBasicFreight(rs.getDouble("basic_freight"));
        item.setDetentionCharge(rs.getDouble("detention_charge"));
        item.setTotal(rs.getDouble("total"));
        return item;
    }
}
