package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.util.*;

public class WidgetDAO {

    private static final Logger log = AppLogger.get(WidgetDAO.class);

    // ── Generic result structure for all widgets ────────────────────────────
    public static class WidgetData {
        private final List<String> headers;
        private final List<List<String>> rows;

        public WidgetData(List<String> headers, List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() { return headers; }
        public List<List<String>> getRows() { return rows; }
    }

    // ── Latest Purchase Invoices ────────────────────────────────────────────
    public WidgetData getLatestPurchaseInvoices(int limit) {
        String sql = "SELECT invoice_no, invoice_date, party_name, COALESCE(net_amount,0) FROM purchase_invoices ORDER BY invoice_date DESC, id DESC LIMIT ?";
        return fetchWidget(sql, List.of("Invoice No", "Date", "Party", "Amount"), limit);
    }

    // ── Latest Sale Invoices ────────────────────────────────────────────────
    public WidgetData getLatestSaleInvoices(int limit) {
        String sql = "SELECT invoice_no, invoice_date, party_name, COALESCE(net_amount,0) FROM sale_invoices ORDER BY invoice_date DESC, id DESC LIMIT ?";
        return fetchWidget(sql, List.of("Invoice No", "Date", "Party", "Amount"), limit);
    }

    // ── Latest Purchase Receipts ────────────────────────────────────────────
    public WidgetData getLatestPurchaseReceipts(int limit) {
        String sql = "SELECT receipt_no, receipt_date, party_name, COALESCE(amount,0) FROM purchase_receipts ORDER BY receipt_date DESC, id DESC LIMIT ?";
        return fetchWidget(sql, List.of("Receipt No", "Date", "Party", "Amount"), limit);
    }

    // ── Latest Sale Receipts ────────────────────────────────────────────────
    public WidgetData getLatestSaleReceipts(int limit) {
        String sql = "SELECT receipt_no, receipt_date, party_name, COALESCE(amount,0) FROM sale_receipts ORDER BY receipt_date DESC, id DESC LIMIT ?";
        return fetchWidget(sql, List.of("Receipt No", "Date", "Party", "Amount"), limit);
    }

    // ── Latest Lorry Receipts ───────────────────────────────────────────────
    public WidgetData getLatestLorryReceipts(int limit) {
        String sql = "SELECT lr_no, lr_date, vehicle_no, consignor_name, COALESCE(total,0) FROM lorry_receipts ORDER BY lr_date DESC, id DESC LIMIT ?";
        return fetchWidget(sql, List.of("LR No", "Date", "Vehicle", "Consignor", "Total"), limit);
    }

    // ── Latest Loading Slips ────────────────────────────────────────────────
    public WidgetData getLatestLoadingSlips(int limit) {
        String sql = "SELECT slip_no, slip_date, vehicle_no, party_name, COALESCE(freight_amount,0) FROM loading_slips ORDER BY slip_date DESC, id DESC LIMIT ?";
        return fetchWidget(sql, List.of("Slip No", "Date", "Vehicle", "Party", "Freight"), limit);
    }

    // ── Top Parties by Purchase Amount ──────────────────────────────────────
    public WidgetData getTopPartiesByPurchase(int limit) {
        String sql = "SELECT party_name, COUNT(*) as cnt, SUM(COALESCE(net_amount,0)) as total FROM purchase_invoices GROUP BY party_name ORDER BY total DESC LIMIT ?";
        return fetchWidget(sql, List.of("Party", "Invoices", "Total Amount"), limit);
    }

    // ── Top Parties by Sale Amount ──────────────────────────────────────────
    public WidgetData getTopPartiesBySale(int limit) {
        String sql = "SELECT party_name, COUNT(*) as cnt, SUM(COALESCE(net_amount,0)) as total FROM sale_invoices GROUP BY party_name ORDER BY total DESC LIMIT ?";
        return fetchWidget(sql, List.of("Party", "Invoices", "Total Amount"), limit);
    }

    // ── Top Vehicles by Revenue ─────────────────────────────────────────────
    public WidgetData getTopVehiclesByRevenue(int limit) {
        String sql = "SELECT vehicle_no, COUNT(*) as trips, SUM(COALESCE(total,0)) as revenue FROM lorry_receipts GROUP BY vehicle_no ORDER BY revenue DESC LIMIT ?";
        return fetchWidget(sql, List.of("Vehicle", "Trips", "Revenue"), limit);
    }

    // ── Parties Least Interacted With ───────────────────────────────────────
    public WidgetData getLeastInteractedParties(int limit) {
        String sql = """
            SELECT p.name, p.type, COALESCE(t.txn_count, 0) as txn_count
            FROM parties p
            LEFT JOIN (
                SELECT party_name, COUNT(*) as txn_count FROM (
                    SELECT party_name FROM purchase_invoices
                    UNION ALL SELECT party_name FROM sale_invoices
                    UNION ALL SELECT party_name FROM purchase_receipts
                    UNION ALL SELECT party_name FROM sale_receipts
                ) all_txns GROUP BY party_name
            ) t ON p.name = t.party_name
            ORDER BY txn_count ASC
            LIMIT ?
            """;
        return fetchWidget(sql, List.of("Party", "Type", "Transactions"), limit);
    }

    // ── Parties Not Done Business for Longest Period ────────────────────────
    public WidgetData getLongestInactiveParties(int limit) {
        String sql = """
            SELECT p.name, p.type, COALESCE(t.last_txn, 'Never') as last_txn,
                   CASE WHEN t.last_txn IS NOT NULL THEN DATEDIFF(CURDATE(), t.last_txn) ELSE 9999 END as days_ago
            FROM parties p
            LEFT JOIN (
                SELECT party_name, MAX(txn_date) as last_txn FROM (
                    SELECT party_name, invoice_date as txn_date FROM purchase_invoices
                    UNION ALL SELECT party_name, invoice_date FROM sale_invoices
                    UNION ALL SELECT party_name, receipt_date FROM purchase_receipts
                    UNION ALL SELECT party_name, receipt_date FROM sale_receipts
                ) all_txns GROUP BY party_name
            ) t ON p.name = t.party_name
            ORDER BY days_ago DESC
            LIMIT ?
            """;
        return fetchWidget(sql, List.of("Party", "Type", "Last Transaction", "Days Ago"), limit);
    }

    // ── Purchase GST Summary (This Year) ────────────────────────────────────
    public WidgetData getPurchaseGSTSummary() {
        String sql = """
            SELECT
                COALESCE(SUM(taxable_amount), 0),
                COALESCE(SUM(cgst_amount), 0),
                COALESCE(SUM(sgst_amount), 0),
                COALESCE(SUM(igst_amount), 0),
                COALESCE(SUM(total_gst), 0),
                COALESCE(SUM(net_amount), 0)
            FROM purchase_invoices
            WHERE YEAR(invoice_date) = YEAR(CURDATE())
            """;
        List<List<String>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                rows.add(List.of("Taxable", fmt(rs.getDouble(1))));
                rows.add(List.of("CGST", fmt(rs.getDouble(2))));
                rows.add(List.of("SGST", fmt(rs.getDouble(3))));
                rows.add(List.of("IGST", fmt(rs.getDouble(4))));
                rows.add(List.of("Total GST", fmt(rs.getDouble(5))));
                rows.add(List.of("Net Amount", fmt(rs.getDouble(6))));
            }
        } catch (Exception e) {
            log.error("Failed to fetch purchase GST summary", e);
            rows.add(List.of("Error", e.getMessage()));
        }
        return new WidgetData(List.of("Component", "Amount"), rows);
    }

    // ── Sale GST Summary (This Year) ────────────────────────────────────────
    public WidgetData getSaleGSTSummary() {
        String sql = """
            SELECT
                COALESCE(SUM(taxable_amount), 0),
                COALESCE(SUM(cgst_amount), 0),
                COALESCE(SUM(sgst_amount), 0),
                COALESCE(SUM(igst_amount), 0),
                COALESCE(SUM(total_gst), 0),
                COALESCE(SUM(net_amount), 0)
            FROM sale_invoices
            WHERE YEAR(invoice_date) = YEAR(CURDATE())
            """;
        List<List<String>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                rows.add(List.of("Taxable", fmt(rs.getDouble(1))));
                rows.add(List.of("CGST", fmt(rs.getDouble(2))));
                rows.add(List.of("SGST", fmt(rs.getDouble(3))));
                rows.add(List.of("IGST", fmt(rs.getDouble(4))));
                rows.add(List.of("Total GST", fmt(rs.getDouble(5))));
                rows.add(List.of("Net Amount", fmt(rs.getDouble(6))));
            }
        } catch (Exception e) {
            log.error("Failed to fetch sale GST summary", e);
            rows.add(List.of("Error", e.getMessage()));
        }
        return new WidgetData(List.of("Component", "Amount"), rows);
    }

    // ── Monthly Purchase Summary ────────────────────────────────────────────
    public WidgetData getMonthlyPurchaseSummary() {
        String sql = """
            SELECT DATE_FORMAT(invoice_date, '%b %Y') as month_label,
                   COUNT(*) as cnt,
                   SUM(COALESCE(net_amount, 0)) as total
            FROM purchase_invoices
            WHERE YEAR(invoice_date) = YEAR(CURDATE())
            GROUP BY YEAR(invoice_date), MONTH(invoice_date), month_label
            ORDER BY YEAR(invoice_date), MONTH(invoice_date)
            """;
        return fetchWidgetNoLimit(sql, List.of("Month", "Invoices", "Total"));
    }

    // ── Monthly Sale Summary ────────────────────────────────────────────────
    public WidgetData getMonthlySaleSummary() {
        String sql = """
            SELECT DATE_FORMAT(invoice_date, '%b %Y') as month_label,
                   COUNT(*) as cnt,
                   SUM(COALESCE(net_amount, 0)) as total
            FROM sale_invoices
            WHERE YEAR(invoice_date) = YEAR(CURDATE())
            GROUP BY YEAR(invoice_date), MONTH(invoice_date), month_label
            ORDER BY YEAR(invoice_date), MONTH(invoice_date)
            """;
        return fetchWidgetNoLimit(sql, List.of("Month", "Invoices", "Total"));
    }

    // ── Party Count by Type ─────────────────────────────────────────────────
    public WidgetData getPartyCountByType() {
        String sql = "SELECT type, COUNT(*) as cnt FROM parties GROUP BY type ORDER BY cnt DESC";
        return fetchWidgetNoLimit(sql, List.of("Type", "Count"));
    }

    // ── Helper: fetch with limit parameter ──────────────────────────────────
    private WidgetData fetchWidget(String sql, List<String> headers, int limit) {
        List<List<String>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) {
                        Object val = rs.getObject(i);
                        if (val instanceof Number num) {
                            row.add(String.format("%,.2f", num.doubleValue()));
                        } else {
                            row.add(val != null ? val.toString() : "");
                        }
                    }
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            log.error("Widget query failed", e);
            rows.add(headers.stream().map(h -> "—").toList());
        }
        return new WidgetData(headers, rows);
    }

    // ── Helper: fetch without limit parameter ───────────────────────────────
    private WidgetData fetchWidgetNoLimit(String sql, List<String> headers) {
        List<List<String>> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    Object val = rs.getObject(i);
                    if (val instanceof Number num) {
                        row.add(String.format("%,.2f", num.doubleValue()));
                    } else {
                        row.add(val != null ? val.toString() : "");
                    }
                }
                rows.add(row);
            }
        } catch (Exception e) {
            log.error("Widget query failed", e);
            rows.add(headers.stream().map(h -> "—").toList());
        }
        return new WidgetData(headers, rows);
    }

    private String fmt(double val) {
        return String.format("%,.2f", val);
    }
}
