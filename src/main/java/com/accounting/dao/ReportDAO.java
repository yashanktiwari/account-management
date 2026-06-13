package com.accounting.dao;

import com.accounting.database.DBConnection;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class ReportDAO {

    private static final Logger log = get(ReportDAO.class);

    public List<String> getAllParties() throws Exception {
        List<String> parties = new ArrayList<>();
        String sql = "SELECT DISTINCT name FROM parties ORDER BY name";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                parties.add(rs.getString("name"));
            }
        }
        return parties;
    }

    public List<String> getAllVehicles() throws Exception {
        List<String> vehicles = new ArrayList<>();
        String sql = """
            SELECT DISTINCT vehicle_no FROM (
                SELECT vehicle_no FROM loading_slips WHERE vehicle_no IS NOT NULL
                UNION ALL
                SELECT vehicle_no FROM lorry_receipts WHERE vehicle_no IS NOT NULL
            ) v ORDER BY vehicle_no
            """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                vehicles.add(rs.getString("vehicle_no"));
            }
        }
        return vehicles;
    }

    public ReportResult generateReport(String reportType, LocalDate fromDate, LocalDate toDate, String party, String vehicle) throws Exception {
        switch (reportType) {
            case "All Transactions":
                return generateAllTransactionsReport(fromDate, toDate);
            case "Party Transactions":
                return generatePartyTransactionsReport(fromDate, toDate, party);
            case "Vehicle Transactions":
                return generateVehicleTransactionsReport(fromDate, toDate, vehicle);
            case "GST Summary":
                return generateGSTSummaryReport(fromDate, toDate, party);
            case "Invoice Summary":
                return generateInvoiceSummaryReport(fromDate, toDate);
            case "Receipt Summary":
                return generateReceiptSummaryReport(fromDate, toDate);
            case "Loading Slip Summary":
                return generateLoadingSlipSummaryReport(fromDate, toDate);
            case "Lorry Receipt Summary":
                return generateLorryReceiptSummaryReport(fromDate, toDate);
            default:
                return new ReportResult(new ArrayList<>(), 0.0, 0.0);
        }
    }

    private ReportResult generateAllTransactionsReport(LocalDate fromDate, LocalDate toDate) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        // Combine all transactions from invoices, receipts, payments, loading slips, and lorry receipts
        String sql = """
            SELECT * FROM (
                SELECT 'Purchase Invoice' as transaction_type, invoice_no as transaction_no, invoice_date as date,
                       party_name as party, net_amount as amount, 'INVOICE' as type
                FROM purchase_invoices
                WHERE invoice_date BETWEEN ? AND ?
                UNION ALL
                SELECT 'Sale Invoice' as transaction_type, invoice_no as transaction_no, invoice_date as date,
                       account_name as party, net_amount as amount, 'INVOICE' as type
                FROM sale_invoices
                WHERE invoice_date BETWEEN ? AND ?
                UNION ALL
                SELECT 'Purchase Receipt' as transaction_type, receipt_no as transaction_no, receipt_date as date,
                       party_name as party, amount as amount, 'RECEIPT' as type
                FROM purchase_receipts
                WHERE receipt_date BETWEEN ? AND ?
                UNION ALL
                SELECT 'Sale Receipt' as transaction_type, receipt_no as transaction_no, receipt_date as date,
                       party_name as party, amount as amount, 'RECEIPT' as type
                FROM sale_receipts
                WHERE receipt_date BETWEEN ? AND ?
                UNION ALL
                SELECT 'Payment' as transaction_type, voucher_no as transaction_no, payment_date as date,
                       account_name as party, amount as amount, 'PAYMENT' as type
                FROM payments
                WHERE payment_date BETWEEN ? AND ?
                UNION ALL
                SELECT 'Loading Slip' as transaction_type, slip_no as transaction_no, slip_date as date,
                       NULL as party, freight_amount as amount, 'SLIP' as type
                FROM loading_slips
                WHERE slip_date BETWEEN ? AND ?
                UNION ALL
                SELECT 'Lorry Receipt' as transaction_type, lr_no as transaction_no, lr_date as date,
                       NULL as party, freight as amount, 'LR' as type
                FROM lorry_receipts
                WHERE lr_date BETWEEN ? AND ?
            ) combined
            ORDER BY date
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(fromDate));
            pstmt.setDate(2, Date.valueOf(toDate));
            pstmt.setDate(3, Date.valueOf(fromDate));
            pstmt.setDate(4, Date.valueOf(toDate));
            pstmt.setDate(5, Date.valueOf(fromDate));
            pstmt.setDate(6, Date.valueOf(toDate));
            pstmt.setDate(7, Date.valueOf(fromDate));
            pstmt.setDate(8, Date.valueOf(toDate));
            pstmt.setDate(9, Date.valueOf(fromDate));
            pstmt.setDate(10, Date.valueOf(toDate));
            pstmt.setDate(11, Date.valueOf(fromDate));
            pstmt.setDate(12, Date.valueOf(toDate));
            pstmt.setDate(13, Date.valueOf(fromDate));
            pstmt.setDate(14, Date.valueOf(toDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                int serialNo = 1;
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalAmount += amount;

                    ReportRow row = new ReportRow();
                    row.setSerialNo(serialNo++);
                    row.setTransactionType(rs.getString("transaction_type"));
                    row.setTransactionNo(rs.getString("transaction_no"));
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setParty(rs.getString("party"));
                    row.setAmount(amount);
                    row.setType(rs.getString("type"));
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }

    private ReportResult generatePartyTransactionsReport(LocalDate fromDate, LocalDate toDate, String party) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double openingBalance = 0.0;
        double runningBalance = 0.0;

        // Calculate opening balance (transactions before from date)
        String openingSql;
        if (party != null) {
            openingSql = """
                SELECT
                    COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) -
                    COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0) as balance
                FROM (
                    SELECT 'DEBIT' as type, net_amount as amount, invoice_date as date, account_name as party_name FROM sale_invoices
                    UNION ALL
                    SELECT 'CREDIT' as type, amount as amount, payment_date as date, account_name as party_name FROM payments
                ) combined
                WHERE date < ? AND party_name = ?
                """;
        } else {
            openingSql = """
                SELECT
                    COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) -
                    COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0) as balance
                FROM (
                    SELECT 'DEBIT' as type, net_amount as amount, invoice_date as date, account_name as party_name FROM sale_invoices
                    UNION ALL
                    SELECT 'CREDIT' as type, amount as amount, payment_date as date, account_name as party_name FROM payments
                ) combined
                WHERE date < ?
                """;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(openingSql)) {
            pstmt.setDate(1, Date.valueOf(fromDate));
            if (party != null) {
                pstmt.setString(2, party);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    openingBalance = rs.getDouble("balance");
                }
            }
        }

        runningBalance = openingBalance;

        // Get transactions in date range
        String sql;
        if (party != null) {
            sql = """
                SELECT date, party_name, type, reference,
                       CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END as debit,
                       CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END as credit
                FROM (
                    SELECT invoice_date as date, account_name as party_name, 'Invoice' as type,
                           invoice_no as reference, net_amount as amount
                    FROM sale_invoices
                    WHERE invoice_date BETWEEN ? AND ? AND account_name = ?

                    UNION ALL

                    SELECT payment_date as date, account_name as party_name, 'Receipt' as type,
                           CONCAT('PAY-', id) as reference, amount as amount
                    FROM payments
                    WHERE payment_date BETWEEN ? AND ? AND account_name = ?
                ) combined
                ORDER BY date
                """;
        } else {
            sql = """
                SELECT date, party_name, type, reference,
                       CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END as debit,
                       CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END as credit
                FROM (
                    SELECT invoice_date as date, account_name as party_name, 'Invoice' as type,
                           invoice_no as reference, net_amount as amount
                    FROM sale_invoices
                    WHERE invoice_date BETWEEN ? AND ?

                    UNION ALL

                    SELECT payment_date as date, account_name as party_name, 'Receipt' as type,
                           CONCAT('PAY-', id) as reference, amount as amount
                    FROM payments
                    WHERE payment_date BETWEEN ? AND ?
                ) combined
                ORDER BY date
                """;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            if (party != null) {
                pstmt.setString(paramIndex++, party);
            }
            pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            if (party != null) {
                pstmt.setString(paramIndex++, party);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double debit = rs.getDouble("debit");
                    double credit = rs.getDouble("credit");
                    runningBalance += debit - credit;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setParty(rs.getString("party_name"));
                    row.setType(rs.getString("type"));
                    row.setReference(rs.getString("reference"));
                    row.setDebit(debit);
                    row.setCredit(credit);
                    row.setBalance(runningBalance);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, openingBalance, runningBalance);
    }

    private ReportResult generateVehicleTransactionsReport(LocalDate fromDate, LocalDate toDate, String vehicle) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        String sql;
        if (vehicle != null) {
            sql = """
                SELECT slip_date as date, vehicle_no, 'Loading Slip' as type,
                       CONCAT('LS-', slip_no) as reference, freight_amount as amount
                FROM loading_slips
                WHERE slip_date BETWEEN ? AND ? AND vehicle_no = ?

                UNION ALL

                SELECT lr_date as date, vehicle_no, 'Lorry Receipt' as type,
                       CONCAT('LR-', lr_no) as reference, freight as amount
                FROM lorry_receipts
                WHERE lr_date BETWEEN ? AND ? AND vehicle_no = ?

                ORDER BY date
                """;
        } else {
            sql = """
                SELECT slip_date as date, vehicle_no, 'Loading Slip' as type,
                       CONCAT('LS-', slip_no) as reference, freight_amount as amount
                FROM loading_slips
                WHERE slip_date BETWEEN ? AND ?

                UNION ALL

                SELECT lr_date as date, vehicle_no, 'Lorry Receipt' as type,
                       CONCAT('LR-', lr_no) as reference, freight as amount
                FROM lorry_receipts
                WHERE lr_date BETWEEN ? AND ?

                ORDER BY date
                """;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            if (vehicle != null) {
                pstmt.setString(paramIndex++, vehicle);
            }
            pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            if (vehicle != null) {
                pstmt.setString(paramIndex++, vehicle);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalAmount += amount;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setVehicle(rs.getString("vehicle_no"));
                    row.setType(rs.getString("type"));
                    row.setReference(rs.getString("reference"));
                    row.setAmount(amount);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }

    private ReportResult generateGSTSummaryReport(LocalDate fromDate, LocalDate toDate, String party) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalGST = 0.0;

        String sql;
        if (party != null) {
            sql = """
                SELECT invoice_date as date, account_name as party_name, 'Sale Invoice' as type,
                       rcvr_gstin as gstin, taxable_amount, total_gst
                FROM sale_invoices
                WHERE invoice_date BETWEEN ? AND ? AND account_name = ?

                UNION ALL

                SELECT invoice_date as date, account_name as party_name, 'Purchase Invoice' as type,
                       supplier_gst_no as gstin, taxable_amount, total_gst
                FROM purchase_invoices
                WHERE invoice_date BETWEEN ? AND ? AND account_name = ?

                ORDER BY date
                """;
        } else {
            sql = """
                SELECT invoice_date as date, account_name as party_name, 'Sale Invoice' as type,
                       rcvr_gstin as gstin, taxable_amount, total_gst
                FROM sale_invoices
                WHERE invoice_date BETWEEN ? AND ?

                UNION ALL

                SELECT invoice_date as date, account_name as party_name, 'Purchase Invoice' as type,
                       supplier_gst_no as gstin, taxable_amount, total_gst
                FROM purchase_invoices
                WHERE invoice_date BETWEEN ? AND ?

                ORDER BY date
                """;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            if (party != null) {
                pstmt.setString(paramIndex++, party);
            }
            pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            if (party != null) {
                pstmt.setString(paramIndex++, party);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double gstPaid = rs.getDouble("total_gst");
                    totalGST += gstPaid;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setParty(rs.getString("party_name"));
                    row.setType(rs.getString("type"));
                    row.setGstin(rs.getString("gstin"));
                    row.setTaxableAmount(rs.getDouble("taxable_amount"));
                    row.setGstPaid(gstPaid);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalGST);
    }

    private ReportResult generateInvoiceSummaryReport(LocalDate fromDate, LocalDate toDate) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        String sql = """
            SELECT invoice_date as date, invoice_no as reference,
                   CONCAT('Sale - ', account_name) as description, net_amount as amount
            FROM sale_invoices
            WHERE invoice_date BETWEEN ? AND ?

            UNION ALL

            SELECT invoice_date as date, invoice_no as reference,
                   CONCAT('Purchase - ', account_name) as description, net_amount as amount
            FROM purchase_invoices
            WHERE invoice_date BETWEEN ? AND ?

            ORDER BY date
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(fromDate));
            pstmt.setDate(2, Date.valueOf(toDate));
            pstmt.setDate(3, Date.valueOf(fromDate));
            pstmt.setDate(4, Date.valueOf(toDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalAmount += amount;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setReference(rs.getString("reference"));
                    row.setDescription(rs.getString("description"));
                    row.setAmount(amount);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }

    private ReportResult generateReceiptSummaryReport(LocalDate fromDate, LocalDate toDate) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        String sql = """
            SELECT payment_date as date, CONCAT('PAY-', id) as reference,
                   CONCAT('Receipt - ', account_name) as description, amount
            FROM payments
            WHERE payment_date BETWEEN ? AND ?
            
            ORDER BY date
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(fromDate));
            pstmt.setDate(2, Date.valueOf(toDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalAmount += amount;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setReference(rs.getString("reference"));
                    row.setDescription(rs.getString("description"));
                    row.setAmount(amount);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }

    private ReportResult generateLoadingSlipSummaryReport(LocalDate fromDate, LocalDate toDate) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        String sql = """
            SELECT slip_date as date, CONCAT('LS-', slip_no) as reference,
                   CONCAT('Loading Slip - ', station, ' to ', to_location) as description, freight_amount as amount
            FROM loading_slips
            WHERE slip_date BETWEEN ? AND ?

            ORDER BY date
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(fromDate));
            pstmt.setDate(2, Date.valueOf(toDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalAmount += amount;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setReference(rs.getString("reference"));
                    row.setDescription(rs.getString("description"));
                    row.setAmount(amount);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }

    private ReportResult generateLorryReceiptSummaryReport(LocalDate fromDate, LocalDate toDate) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        String sql = """
            SELECT lr_date as date, CONCAT('LR-', lr_no) as reference,
                   CONCAT('Lorry Receipt - ', from_location, ' to ', to_location) as description, freight as amount
            FROM lorry_receipts
            WHERE lr_date BETWEEN ? AND ?

            ORDER BY date
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(fromDate));
            pstmt.setDate(2, Date.valueOf(toDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalAmount += amount;

                    ReportRow row = new ReportRow();
                    row.setDate(rs.getDate("date").toLocalDate().toString());
                    row.setReference(rs.getString("reference"));
                    row.setDescription(rs.getString("description"));
                    row.setAmount(amount);
                    rows.add(row);
                }
            }
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }

    public static class ReportRow {
        private String date;
        private String party;
        private String vehicle;
        private String type;
        private String reference;
        private String description;
        private String gstin;
            private int serialNo;
            private String transactionType;
            private String transactionNo;
            private String fromLocation;
            private String toLocation;
            private String gst;
            private Double sgst = 0.0;
            private Double cgst = 0.0;
            private Double igst = 0.0;
            private Double totalGst = 0.0;
            private Double advance = 0.0;
            private String paymentMode;
            private String chequeNo;
            private String chequeDate;
            private String bankName;
            private String remarks;
        private Double debit = 0.0;
        private Double credit = 0.0;
        private Double balance = 0.0;
        private Double amount = 0.0;
        private Double taxableAmount = 0.0;
        private Double gstPaid = 0.0;

        // Getters and Setters
    public int getSerialNo() { return serialNo; }
    public void setSerialNo(int serialNo) { this.serialNo = serialNo; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }
    public String getGst() { return gst; }
    public void setGst(String gst) { this.gst = gst; }
    public Double getSgst() { return sgst; }
    public void setSgst(Double sgst) { this.sgst = sgst; }
    public Double getCgst() { return cgst; }
    public void setCgst(Double cgst) { this.cgst = cgst; }
    public Double getIgst() { return igst; }
    public void setIgst(Double igst) { this.igst = igst; }
    public Double getTotalGst() { return totalGst; }
    public void setTotalGst(Double totalGst) { this.totalGst = totalGst; }
    public Double getAdvance() { return advance; }
    public void setAdvance(Double advance) { this.advance = advance; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }
    public String getChequeDate() { return chequeDate; }
    public void setChequeDate(String chequeDate) { this.chequeDate = chequeDate; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getParty() { return party; }
        public void setParty(String party) { this.party = party; }
        public String getVehicle() { return vehicle; }
        public void setVehicle(String vehicle) { this.vehicle = vehicle; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getGstin() { return gstin; }
        public void setGstin(String gstin) { this.gstin = gstin; }
        public Double getDebit() { return debit; }
        public void setDebit(Double debit) { this.debit = debit; }
        public Double getCredit() { return credit; }
        public void setCredit(Double credit) { this.credit = credit; }
        public Double getBalance() { return balance; }
        public void setBalance(Double balance) { this.balance = balance; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public Double getTaxableAmount() { return taxableAmount; }
        public void setTaxableAmount(Double taxableAmount) { this.taxableAmount = taxableAmount; }
        public Double getGstPaid() { return gstPaid; }
        public void setGstPaid(Double gstPaid) { this.gstPaid = gstPaid; }
    }

    public static class ReportResult {
        private final List<ReportRow> rows;
        private final double openingBalance;
        private final double closingBalance;

        public ReportResult(List<ReportRow> rows, double openingBalance, double closingBalance) {
            this.rows = rows;
            this.openingBalance = openingBalance;
            this.closingBalance = closingBalance;
        }

        public List<ReportRow> getRows() { return rows; }
        public double getOpeningBalance() { return openingBalance; }
        public double getClosingBalance() { return closingBalance; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Preset Reports - Simple table-based reports
    // ══════════════════════════════════════════════════════════════════════════

    public static class SimpleReportResult {
        private final String[] columnHeaders;
        private final List<String[]> rows;
        private final double totalAmount;

        public SimpleReportResult(String[] columnHeaders, List<String[]> rows, double totalAmount) {
            this.columnHeaders = columnHeaders;
            this.rows = rows;
            this.totalAmount = totalAmount;
        }

        public String[] getColumnHeaders() { return columnHeaders; }
        public List<String[]> getRows() { return rows; }
        public double getTotalAmount() { return totalAmount; }
    }

    public SimpleReportResult generatePresetReport(String reportType, LocalDate fromDate, LocalDate toDate, String party, String vehicle) throws Exception {
        return switch (reportType) {
            case "Purchase Invoices" -> presetPurchaseInvoices(fromDate, toDate, party);
            case "Sale Invoices" -> presetSaleInvoices(fromDate, toDate, party);
            case "Purchase Receipts" -> presetPurchaseReceipts(fromDate, toDate, party);
            case "Sale Receipts" -> presetSaleReceipts(fromDate, toDate, party);
            case "Payments" -> presetPayments(fromDate, toDate, party);
            case "Loading Slips" -> presetLoadingSlips(fromDate, toDate, vehicle);
            case "Lorry Receipts" -> presetLorryReceipts(fromDate, toDate, vehicle);
            case "Party-wise Summary" -> presetPartyWiseSummary(fromDate, toDate, party);
            case "Monthly Summary" -> presetMonthlySummary(fromDate, toDate);
            case "GST Report" -> presetGSTReport(fromDate, toDate, party);
            case "Vehicle-wise Summary" -> presetVehicleWiseSummary(fromDate, toDate, vehicle);
            case "Day Book" -> presetDayBook(fromDate, toDate);
            default -> new SimpleReportResult(new String[]{}, new ArrayList<>(), 0);
        };
    }

    public static List<String> getPresetReportTypes() {
        return List.of(
            "Purchase Invoices", "Sale Invoices",
            "Purchase Receipts", "Sale Receipts",
            "Payments",
            "Loading Slips", "Lorry Receipts",
            "Party-wise Summary", "Monthly Summary",
            "GST Report", "Vehicle-wise Summary", "Day Book"
        );
    }

    private SimpleReportResult presetPurchaseInvoices(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Date", "Invoice No", "Party", "Voucher Type", "Taxable Amt", "GST", "Net Amount", "Remarks"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT invoice_date, invoice_no, party_name, voucher_type, " +
                   "COALESCE(taxable_amount,0), COALESCE(total_gst,0), COALESCE(net_amount,0), COALESCE(remarks,'') " +
                   "FROM purchase_invoices WHERE invoice_date BETWEEN ? AND ?";
        if (party != null) sql += " AND party_name = ?";
        sql += " ORDER BY invoice_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (party != null) ps.setString(3, party);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double net = rs.getDouble(7);
                    total += net;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        fmt(rs.getDouble(5)), fmt(rs.getDouble(6)), fmt(net), rs.getString(8)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetSaleInvoices(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Date", "Invoice No", "Party", "Voucher Type", "Taxable Amt", "GST", "Net Amount", "Remarks"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT invoice_date, invoice_no, account_name, voucher_type, " +
                   "COALESCE(taxable_amount,0), COALESCE(total_gst,0), COALESCE(net_amount,0), COALESCE(remarks,'') " +
                   "FROM sale_invoices WHERE invoice_date BETWEEN ? AND ?";
        if (party != null) sql += " AND account_name = ?";
        sql += " ORDER BY invoice_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (party != null) ps.setString(3, party);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double net = rs.getDouble(7);
                    total += net;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        fmt(rs.getDouble(5)), fmt(rs.getDouble(6)), fmt(net), rs.getString(8)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetPurchaseReceipts(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Date", "Receipt No", "Party", "Amount", "Payment Mode", "Bank", "Remarks"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT receipt_date, receipt_no, party_name, COALESCE(amount,0), " +
                   "COALESCE(payment_mode,''), COALESCE(bank_name,''), COALESCE(remarks,'') " +
                   "FROM purchase_receipts WHERE receipt_date BETWEEN ? AND ?";
        if (party != null) sql += " AND party_name = ?";
        sql += " ORDER BY receipt_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (party != null) ps.setString(3, party);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double amt = rs.getDouble(4);
                    total += amt;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), fmt(amt),
                        rs.getString(5), rs.getString(6), rs.getString(7)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetSaleReceipts(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Date", "Receipt No", "Party", "Amount", "Payment Mode", "Bank", "Remarks"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT receipt_date, receipt_no, party_name, COALESCE(amount,0), " +
                   "COALESCE(payment_mode,''), COALESCE(bank_name,''), COALESCE(remarks,'') " +
                   "FROM sale_receipts WHERE receipt_date BETWEEN ? AND ?";
        if (party != null) sql += " AND party_name = ?";
        sql += " ORDER BY receipt_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (party != null) ps.setString(3, party);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double amt = rs.getDouble(4);
                    total += amt;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), fmt(amt),
                        rs.getString(5), rs.getString(6), rs.getString(7)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetPayments(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Date", "Voucher No", "Party", "Amount", "Voucher Type", "Particulars"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT payment_date, voucher_no, account_name, COALESCE(amount,0), " +
                   "COALESCE(voucher_type,''), COALESCE(particulars,'') " +
                   "FROM payments WHERE payment_date BETWEEN ? AND ?";
        if (party != null) sql += " AND account_name = ?";
        sql += " ORDER BY payment_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (party != null) ps.setString(3, party);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double amt = rs.getDouble(4);
                    total += amt;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), fmt(amt),
                        rs.getString(5), rs.getString(6)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetLoadingSlips(LocalDate from, LocalDate to, String vehicle) throws Exception {
        String[] headers = {"Sr.No", "Date", "Slip No", "Party", "Vehicle", "From", "To", "Freight", "Advance", "Balance"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT slip_date, slip_no, COALESCE(party_name,''), COALESCE(vehicle_no,''), " +
                   "COALESCE(station,''), COALESCE(to_location,''), " +
                   "COALESCE(freight_amount,0), COALESCE(advance_amount,0), COALESCE(balance_amount,0) " +
                   "FROM loading_slips WHERE slip_date BETWEEN ? AND ?";
        if (vehicle != null) sql += " AND vehicle_no = ?";
        sql += " ORDER BY slip_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (vehicle != null) ps.setString(3, vehicle);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double freight = rs.getDouble(7);
                    total += freight;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6),
                        fmt(freight), fmt(rs.getDouble(8)), fmt(rs.getDouble(9))});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetLorryReceipts(LocalDate from, LocalDate to, String vehicle) throws Exception {
        String[] headers = {"Sr.No", "Date", "LR No", "Vehicle", "From", "To", "Consignor", "Freight", "Advance", "Total"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = "SELECT lr_date, lr_no, COALESCE(vehicle_no,''), COALESCE(from_location,''), " +
                   "COALESCE(to_location,''), COALESCE(consignor_name,''), " +
                   "COALESCE(freight,0), COALESCE(advance,0), COALESCE(total,0) " +
                   "FROM lorry_receipts WHERE lr_date BETWEEN ? AND ?";
        if (vehicle != null) sql += " AND vehicle_no = ?";
        sql += " ORDER BY lr_date, id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            if (vehicle != null) ps.setString(3, vehicle);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double t = rs.getDouble(9);
                    total += t;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6),
                        fmt(rs.getDouble(7)), fmt(rs.getDouble(8)), fmt(t)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetPartyWiseSummary(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Party", "Purchase Invoices", "Sale Invoices", "Receipts", "Payments", "Net Amount"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        
        String sql = "SELECT party, SUM(purchase_amt) as purchases, SUM(sale_amt) as sales, " +
                   "SUM(receipt_amt) as receipts, SUM(payment_amt) as payments FROM (";
        
        String purchaseInvoices = "SELECT party_name as party, net_amount as purchase_amt, 0 as sale_amt, 0 as receipt_amt, 0 as payment_amt " +
                                "FROM purchase_invoices WHERE invoice_date BETWEEN ? AND ?";
        if (party != null) purchaseInvoices += " AND party_name = ?";
        
        String saleInvoices = "SELECT account_name as party, 0, net_amount, 0, 0 " +
                             "FROM sale_invoices WHERE invoice_date BETWEEN ? AND ?";
        if (party != null) saleInvoices += " AND account_name = ?";
        
        String purchaseReceipts = "SELECT party_name as party, 0, 0, amount, 0 " +
                                "FROM purchase_receipts WHERE receipt_date BETWEEN ? AND ?";
        if (party != null) purchaseReceipts += " AND party_name = ?";
        
        String saleReceipts = "SELECT party_name as party, 0, 0, amount, 0 " +
                             "FROM sale_receipts WHERE receipt_date BETWEEN ? AND ?";
        if (party != null) saleReceipts += " AND party_name = ?";
        
        String payments = "SELECT account_name as party, 0, 0, 0, amount " +
                         "FROM payments WHERE payment_date BETWEEN ? AND ?";
        if (party != null) payments += " AND account_name = ?";
        
        sql += purchaseInvoices + " UNION ALL " + saleInvoices + " UNION ALL " + 
               purchaseReceipts + " UNION ALL " + saleReceipts + " UNION ALL " + payments +
               ") combined WHERE party IS NOT NULL GROUP BY party ORDER BY (SUM(purchase_amt) + SUM(sale_amt)) DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (int i = 0; i < 5; i++) {
                ps.setDate(paramIndex++, Date.valueOf(from));
                ps.setDate(paramIndex++, Date.valueOf(to));
                if (party != null) ps.setString(paramIndex++, party);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double purchases = rs.getDouble("purchases");
                    double sales = rs.getDouble("sales");
                    double receipts = rs.getDouble("receipts");
                    double paymentAmt = rs.getDouble("payments");
                    double net = (purchases + sales) - (receipts + paymentAmt);
                    total += net;
                    rows.add(new String[]{String.valueOf(sr++), rs.getString("party"),
                        fmt(purchases), fmt(sales), fmt(receipts), fmt(paymentAmt), fmt(net)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetMonthlySummary(LocalDate from, LocalDate to) throws Exception {
        String[] headers = {"Sr.No", "Month", "Purchase Amt", "Sale Amt", "Receipt Amt", "Payment Amt", "Net"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        String sql = """
            SELECT month_label,
                   SUM(purchase_amt) as purchases, SUM(sale_amt) as sales,
                   SUM(receipt_amt) as receipts, SUM(payment_amt) as payments
            FROM (
                SELECT DATE_FORMAT(invoice_date, '%b %Y') as month_label, CONCAT(YEAR(invoice_date),'-',LPAD(MONTH(invoice_date),2,'0')) as sort_key,
                       net_amount as purchase_amt, 0 as sale_amt, 0 as receipt_amt, 0 as payment_amt
                FROM purchase_invoices WHERE invoice_date BETWEEN ? AND ?
                UNION ALL
                SELECT DATE_FORMAT(invoice_date, '%b %Y'), CONCAT(YEAR(invoice_date),'-',LPAD(MONTH(invoice_date),2,'0')),
                       0, net_amount, 0, 0
                FROM sale_invoices WHERE invoice_date BETWEEN ? AND ?
                UNION ALL
                SELECT DATE_FORMAT(receipt_date, '%b %Y'), CONCAT(YEAR(receipt_date),'-',LPAD(MONTH(receipt_date),2,'0')),
                       0, 0, amount, 0
                FROM purchase_receipts WHERE receipt_date BETWEEN ? AND ?
                UNION ALL
                SELECT DATE_FORMAT(receipt_date, '%b %Y'), CONCAT(YEAR(receipt_date),'-',LPAD(MONTH(receipt_date),2,'0')),
                       0, 0, amount, 0
                FROM sale_receipts WHERE receipt_date BETWEEN ? AND ?
                UNION ALL
                SELECT DATE_FORMAT(payment_date, '%b %Y'), CONCAT(YEAR(payment_date),'-',LPAD(MONTH(payment_date),2,'0')),
                       0, 0, 0, amount
                FROM payments WHERE payment_date BETWEEN ? AND ?
            ) combined
            GROUP BY month_label, sort_key
            ORDER BY sort_key
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < 5; i++) {
                ps.setDate(i * 2 + 1, Date.valueOf(from));
                ps.setDate(i * 2 + 2, Date.valueOf(to));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double purchases = rs.getDouble("purchases");
                    double sales = rs.getDouble("sales");
                    double receipts = rs.getDouble("receipts");
                    double payments = rs.getDouble("payments");
                    double net = (sales - purchases) - (payments - receipts);
                    total += net;
                    rows.add(new String[]{String.valueOf(sr++), rs.getString("month_label"),
                        fmt(purchases), fmt(sales), fmt(receipts), fmt(payments), fmt(net)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetGSTReport(LocalDate from, LocalDate to, String party) throws Exception {
        String[] headers = {"Sr.No", "Date", "Type", "Party", "GSTIN", "Taxable Amt", "SGST", "CGST", "IGST", "Total GST"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        
        String saleInvoices = "SELECT invoice_date, 'Sale' as type, account_name as party, COALESCE(rcvr_gstin,'') as gstin, " +
                             "COALESCE(taxable_amount,0), COALESCE(sgst_amount,0), COALESCE(cgst_amount,0), " +
                             "COALESCE(igst_amount,0), COALESCE(total_gst,0) FROM sale_invoices " +
                             "WHERE invoice_date BETWEEN ? AND ?";
        if (party != null) saleInvoices += " AND account_name = ?";
        
        String purchaseInvoices = "SELECT invoice_date, 'Purchase' as type, party_name as party, COALESCE(supplier_gst_no,'') as gstin, " +
                                "COALESCE(taxable_amount,0), COALESCE(sgst_amount,0), COALESCE(cgst_amount,0), " +
                                "COALESCE(igst_amount,0), COALESCE(total_gst,0) FROM purchase_invoices " +
                                "WHERE invoice_date BETWEEN ? AND ?";
        if (party != null) purchaseInvoices += " AND party_name = ?";
        
        String sql = saleInvoices + " UNION ALL " + purchaseInvoices + " ORDER BY 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            ps.setDate(paramIndex++, Date.valueOf(from)); ps.setDate(paramIndex++, Date.valueOf(to));
            if (party != null) ps.setString(paramIndex++, party);
            ps.setDate(paramIndex++, Date.valueOf(from)); ps.setDate(paramIndex++, Date.valueOf(to));
            if (party != null) ps.setString(paramIndex++, party);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double gst = rs.getDouble(9);
                    total += gst;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate(1).toLocalDate().toString(),
                        rs.getString(2), rs.getString(3), rs.getString(4),
                        fmt(rs.getDouble(5)), fmt(rs.getDouble(6)), fmt(rs.getDouble(7)),
                        fmt(rs.getDouble(8)), fmt(gst)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetVehicleWiseSummary(LocalDate from, LocalDate to, String vehicle) throws Exception {
        String[] headers = {"Sr.No", "Vehicle No", "Loading Slips", "Lorry Receipts", "Total Freight"};
        List<String[]> rows = new ArrayList<>();
        double total = 0;
        
        String loadingSlips = "SELECT vehicle_no, 'LS' as type, COALESCE(freight_amount,0) as amount " +
                             "FROM loading_slips WHERE slip_date BETWEEN ? AND ?";
        if (vehicle != null) loadingSlips += " AND vehicle_no = ?";
        
        String lorryReceipts = "SELECT vehicle_no, 'LR' as type, COALESCE(freight,0) as amount " +
                              "FROM lorry_receipts WHERE lr_date BETWEEN ? AND ?";
        if (vehicle != null) lorryReceipts += " AND vehicle_no = ?";
        
        String sql = "SELECT vehicle_no, SUM(CASE WHEN type='LS' THEN 1 ELSE 0 END) as ls_count, " +
                   "SUM(CASE WHEN type='LR' THEN 1 ELSE 0 END) as lr_count, SUM(amount) as total_freight " +
                   "FROM (" + loadingSlips + " UNION ALL " + lorryReceipts + ") combined " +
                   "WHERE vehicle_no IS NOT NULL AND vehicle_no != '' " +
                   "GROUP BY vehicle_no ORDER BY total_freight DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            ps.setDate(paramIndex++, Date.valueOf(from)); ps.setDate(paramIndex++, Date.valueOf(to));
            if (vehicle != null) ps.setString(paramIndex++, vehicle);
            ps.setDate(paramIndex++, Date.valueOf(from)); ps.setDate(paramIndex++, Date.valueOf(to));
            if (vehicle != null) ps.setString(paramIndex++, vehicle);
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double freight = rs.getDouble("total_freight");
                    total += freight;
                    rows.add(new String[]{String.valueOf(sr++), rs.getString("vehicle_no"),
                        String.valueOf(rs.getInt("ls_count")), String.valueOf(rs.getInt("lr_count")),
                        fmt(freight)});
                }
            }
        }
        return new SimpleReportResult(headers, rows, total);
    }

    private SimpleReportResult presetDayBook(LocalDate from, LocalDate to) throws Exception {
        String[] headers = {"Sr.No", "Date", "Type", "Reference", "Party", "Debit", "Credit"};
        List<String[]> rows = new ArrayList<>();
        double totalDebit = 0, totalCredit = 0;
        String sql = """
            SELECT * FROM (
                SELECT invoice_date as txn_date, 'Purchase Invoice' as type, invoice_no as reference,
                       party_name as party, net_amount as debit, 0 as credit
                FROM purchase_invoices WHERE invoice_date BETWEEN ? AND ?
                UNION ALL
                SELECT invoice_date, 'Sale Invoice', invoice_no, account_name, 0, net_amount
                FROM sale_invoices WHERE invoice_date BETWEEN ? AND ?
                UNION ALL
                SELECT receipt_date, 'Purchase Receipt', receipt_no, party_name, 0, amount
                FROM purchase_receipts WHERE receipt_date BETWEEN ? AND ?
                UNION ALL
                SELECT receipt_date, 'Sale Receipt', receipt_no, party_name, amount, 0
                FROM sale_receipts WHERE receipt_date BETWEEN ? AND ?
                UNION ALL
                SELECT payment_date, 'Payment', voucher_no, account_name, amount, 0
                FROM payments WHERE payment_date BETWEEN ? AND ?
                UNION ALL
                SELECT slip_date, 'Loading Slip', slip_no, party_name, freight_amount, 0
                FROM loading_slips WHERE slip_date BETWEEN ? AND ?
                UNION ALL
                SELECT lr_date, 'Lorry Receipt', lr_no, COALESCE(consignor_name,''), freight, 0
                FROM lorry_receipts WHERE lr_date BETWEEN ? AND ?
            ) combined
            ORDER BY txn_date, type
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < 7; i++) {
                ps.setDate(i * 2 + 1, Date.valueOf(from));
                ps.setDate(i * 2 + 2, Date.valueOf(to));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int sr = 1;
                while (rs.next()) {
                    double debit = rs.getDouble("debit");
                    double credit = rs.getDouble("credit");
                    totalDebit += debit;
                    totalCredit += credit;
                    rows.add(new String[]{String.valueOf(sr++), rs.getDate("txn_date").toLocalDate().toString(),
                        rs.getString("type"), rs.getString("reference"),
                        rs.getString("party") != null ? rs.getString("party") : "",
                        debit > 0 ? fmt(debit) : "", credit > 0 ? fmt(credit) : ""});
                }
            }
        }
        return new SimpleReportResult(headers, rows, totalDebit - totalCredit);
    }

    private static String fmt(double val) {
        if (val == 0) return "0.00";
        return String.format("%.2f", val);
    }
}
