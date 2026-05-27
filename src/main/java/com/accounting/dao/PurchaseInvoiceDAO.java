package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.InvoiceLineItem;
import com.accounting.model.PurchaseInvoice;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class PurchaseInvoiceDAO {

    private static final Logger log = get(PurchaseInvoiceDAO.class);

    public void save(PurchaseInvoice invoice) throws Exception {
        ensureInvoiceColumns();
        String sql = """
                INSERT INTO purchase_invoices (invoice_no, invoice_date, party_id, party_name, voucher_type,
                gst, taxable_amount, sgst_amount, cgst_amount, igst_amount, total_gst, net_amount, remarks,
                bank_name, bank_account, ifsc_code, credit_debit, account_name, paid_by, supplier_address,
                supplier_contact_number, supplier_gst_no, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, invoice.getInvoiceNo());
            pstmt.setDate(2, java.sql.Date.valueOf(invoice.getInvoiceDate()));
            pstmt.setInt(3, invoice.getPartyId());
            pstmt.setString(4, invoice.getPartyName());
            pstmt.setString(5, invoice.getVoucherType());
            pstmt.setString(6, invoice.getGst());
            pstmt.setDouble(7, invoice.getTaxableAmount());
            pstmt.setDouble(8, invoice.getSgstAmount());
            pstmt.setDouble(9, invoice.getCgstAmount());
            pstmt.setDouble(10, invoice.getIgstAmount());
            pstmt.setDouble(11, invoice.getTotalGst());
            pstmt.setDouble(12, invoice.getNetAmount());
            pstmt.setString(13, invoice.getRemarks());
            pstmt.setString(14, invoice.getBankName());
            pstmt.setString(15, invoice.getBankAccount());
            pstmt.setString(16, invoice.getIfscCode());
            pstmt.setString(17, invoice.getCreditDebit());
            pstmt.setString(18, invoice.getAccountName());
            pstmt.setString(19, invoice.getPaidBy());
            pstmt.setString(20, invoice.getSupplierAddress());
            pstmt.setString(21, invoice.getSupplierContactNumber());
            pstmt.setString(22, invoice.getSupplierGstNo());
            pstmt.setString(23, invoice.getStatus());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) invoice.setId(rs.getInt(1));
            }

            for (InvoiceLineItem item : invoice.getLineItems()) {
                saveLineItem(invoice.getId(), item);
            }
            log.info("Purchase invoice saved: {}", invoice.getInvoiceNo());
        }
    }

    public void update(PurchaseInvoice invoice) throws Exception {
        ensureInvoiceColumns();
        String sql = """
                UPDATE purchase_invoices SET invoice_no=?, invoice_date=?, party_id=?, party_name=?,
                voucher_type=?, gst=?, taxable_amount=?, sgst_amount=?, cgst_amount=?, igst_amount=?,
                total_gst=?, net_amount=?, remarks=?, bank_name=?, bank_account=?, ifsc_code=?,
                credit_debit=?, account_name=?, paid_by=?, supplier_address=?, supplier_contact_number=?,
                supplier_gst_no=?, status=?, updated_at=NOW() WHERE id=?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoice.getInvoiceNo());
            pstmt.setDate(2, java.sql.Date.valueOf(invoice.getInvoiceDate()));
            pstmt.setInt(3, invoice.getPartyId());
            pstmt.setString(4, invoice.getPartyName());
            pstmt.setString(5, invoice.getVoucherType());
            pstmt.setString(6, invoice.getGst());
            pstmt.setDouble(7, invoice.getTaxableAmount());
            pstmt.setDouble(8, invoice.getSgstAmount());
            pstmt.setDouble(9, invoice.getCgstAmount());
            pstmt.setDouble(10, invoice.getIgstAmount());
            pstmt.setDouble(11, invoice.getTotalGst());
            pstmt.setDouble(12, invoice.getNetAmount());
            pstmt.setString(13, invoice.getRemarks());
            pstmt.setString(14, invoice.getBankName());
            pstmt.setString(15, invoice.getBankAccount());
            pstmt.setString(16, invoice.getIfscCode());
            pstmt.setString(17, invoice.getCreditDebit());
            pstmt.setString(18, invoice.getAccountName());
            pstmt.setString(19, invoice.getPaidBy());
            pstmt.setString(20, invoice.getSupplierAddress());
            pstmt.setString(21, invoice.getSupplierContactNumber());
            pstmt.setString(22, invoice.getSupplierGstNo());
            pstmt.setString(23, invoice.getStatus());
            pstmt.setInt(24, invoice.getId());

            pstmt.executeUpdate();
            deleteLineItems(invoice.getId());
            for (InvoiceLineItem item : invoice.getLineItems()) {
                saveLineItem(invoice.getId(), item);
            }
            log.info("Purchase invoice updated: {}", invoice.getInvoiceNo());
        }
    }

    public void delete(int id) throws Exception {
        deleteLineItems(id);
        String sql = "DELETE FROM purchase_invoices WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            log.info("Purchase invoice deleted: {}", id);
        }
    }

    public PurchaseInvoice findById(int id) throws Exception {
        String sql = "SELECT * FROM purchase_invoices WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    PurchaseInvoice inv = mapInvoice(rs);
                    inv.setLineItems(getLineItems(id));
                    return inv;
                }
            }
        }
        return null;
    }

    public List<PurchaseInvoice> getAll() throws Exception {
        String sql = "SELECT * FROM purchase_invoices ORDER BY invoice_date DESC";
        List<PurchaseInvoice> invoices = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PurchaseInvoice inv = mapInvoice(rs);
                inv.setLineItems(getLineItems(inv.getId()));
                invoices.add(inv);
            }
        }
        return invoices;
    }

    public List<PurchaseInvoice> searchAllColumns(String keyword) throws Exception {
        String sql = """
                SELECT *
                FROM purchase_invoices
                WHERE CONCAT_WS(' ',
                    IFNULL(CAST(id AS CHAR), ''),
                    IFNULL(invoice_no, ''),
                    IFNULL(CAST(invoice_date AS CHAR), ''),
                    IFNULL(CAST(party_id AS CHAR), ''),
                    IFNULL(party_name, ''),
                    IFNULL(voucher_type, ''),
                    IFNULL(gst, ''),
                    IFNULL(CAST(taxable_amount AS CHAR), ''),
                    IFNULL(CAST(sgst_amount AS CHAR), ''),
                    IFNULL(CAST(cgst_amount AS CHAR), ''),
                    IFNULL(CAST(igst_amount AS CHAR), ''),
                    IFNULL(CAST(total_gst AS CHAR), ''),
                    IFNULL(CAST(net_amount AS CHAR), ''),
                    IFNULL(remarks, ''),
                    IFNULL(bank_name, ''),
                    IFNULL(bank_account, ''),
                    IFNULL(ifsc_code, ''),
                    IFNULL(status, ''),
                    IFNULL(CAST(created_at AS CHAR), ''),
                    IFNULL(CAST(updated_at AS CHAR), '')
                ) LIKE ?
                ORDER BY invoice_date DESC
                """;
        List<PurchaseInvoice> invoices = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PurchaseInvoice inv = mapInvoice(rs);
                    inv.setLineItems(getLineItems(inv.getId()));
                    invoices.add(inv);
                }
            }
        }
        return invoices;
    }

    private void saveLineItem(int invoiceId, InvoiceLineItem item) throws Exception {
        ensureLineItemColumns();
        String sql = """
                INSERT INTO invoice_line_items (invoice_id, date, lr_no, container_no, vehicle_no, from_location,
                to_location, type, basic_freight, detention_charge, total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, invoiceId);
            pstmt.setString(2, item.getDate());
            pstmt.setString(3, item.getLrNo());
            pstmt.setString(4, item.getContainerNo());
            pstmt.setString(5, item.getVehicleNo());
            pstmt.setString(6, item.getFrom());
            pstmt.setString(7, item.getTo());
            pstmt.setString(8, item.getType());
            pstmt.setDouble(9, item.getBasicFreight());
            pstmt.setDouble(10, item.getDetentionCharge());
            pstmt.setDouble(11, item.getTotal());
            pstmt.executeUpdate();
        }
    }

    private void ensureLineItemColumns() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> existing = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "invoice_line_items", null)) {
                while (rs.next()) {
                    existing.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            try (Statement stmt = conn.createStatement()) {
                if (!existing.contains("date")) {
                    stmt.executeUpdate("ALTER TABLE invoice_line_items ADD COLUMN date VARCHAR(20)");
                }
            }
        }
    }

    private void ensureInvoiceColumns() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> existing = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "purchase_invoices", null)) {
                while (rs.next()) {
                    existing.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            try (Statement stmt = conn.createStatement()) {
                if (!existing.contains("credit_debit")) {
                    stmt.executeUpdate("ALTER TABLE purchase_invoices ADD COLUMN credit_debit VARCHAR(20)");
                }
                if (!existing.contains("account_name")) {
                    stmt.executeUpdate("ALTER TABLE purchase_invoices ADD COLUMN account_name VARCHAR(255)");
                }
                if (!existing.contains("paid_by")) {
                    stmt.executeUpdate("ALTER TABLE purchase_invoices ADD COLUMN paid_by VARCHAR(100)");
                }
                if (!existing.contains("supplier_address")) {
                    stmt.executeUpdate("ALTER TABLE purchase_invoices ADD COLUMN supplier_address VARCHAR(255)");
                }
                if (!existing.contains("supplier_contact_number")) {
                    stmt.executeUpdate("ALTER TABLE purchase_invoices ADD COLUMN supplier_contact_number VARCHAR(20)");
                }
                if (!existing.contains("supplier_gst_no")) {
                    stmt.executeUpdate("ALTER TABLE purchase_invoices ADD COLUMN supplier_gst_no VARCHAR(20)");
                }
            }
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

    private PurchaseInvoice mapInvoice(ResultSet rs) throws SQLException {
        PurchaseInvoice inv = new PurchaseInvoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceNo(rs.getString("invoice_no"));
        inv.setInvoiceDate(rs.getDate("invoice_date").toLocalDate());
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
        inv.setBankName(rs.getString("bank_name"));
        inv.setBankAccount(rs.getString("bank_account"));
        inv.setIfscCode(rs.getString("ifsc_code"));
        inv.setCreditDebit(rs.getString("credit_debit"));
        inv.setAccountName(rs.getString("account_name"));
        inv.setPaidBy(rs.getString("paid_by"));
        inv.setSupplierAddress(rs.getString("supplier_address"));
        inv.setSupplierContactNumber(rs.getString("supplier_contact_number"));
        inv.setSupplierGstNo(rs.getString("supplier_gst_no"));
        inv.setStatus(rs.getString("status"));
        inv.setCreatedAt(rs.getDate("created_at").toLocalDate());
        inv.setUpdatedAt(rs.getDate("updated_at").toLocalDate());
        return inv;
    }

    private InvoiceLineItem mapLineItem(ResultSet rs) throws SQLException {
        InvoiceLineItem item = new InvoiceLineItem();
        item.setId(rs.getInt("id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setDate(rs.getString("date"));
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
