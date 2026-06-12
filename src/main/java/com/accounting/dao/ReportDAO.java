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
        String sql = "SELECT DISTINCT vehicle_no FROM loading_slips WHERE vehicle_no IS NOT NULL ORDER BY vehicle_no";

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
        return generateReport(reportType, fromDate, toDate, party, vehicle, null);
    }

    public ReportResult generateReport(String reportType, LocalDate fromDate, LocalDate toDate, String party, String vehicle, java.util.List<String> searchTerms) throws Exception {
        switch (reportType) {
            case "All Transactions":
                return generateAllTransactionsReport(fromDate, toDate, searchTerms);
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
        return generateAllTransactionsReport(fromDate, toDate, null);
    }

    private ReportResult generateAllTransactionsReport(LocalDate fromDate, LocalDate toDate, java.util.List<String> searchTerms) throws Exception {
        List<ReportRow> rows = new ArrayList<>();
        double totalAmount = 0.0;

        // First, get all transactions in the date range
        String sql = buildAllTransactionsSQL(false);
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            
            // Set date parameters for all 7 transaction types
            for (int i = 0; i < 7; i++) {
                pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
                pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            }

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
                    row.setDebit(rs.getDouble("debit"));
                    row.setCredit(rs.getDouble("credit"));
                    row.setRemarks(rs.getString("remarks"));
                    row.setType(rs.getString("type"));
                    rows.add(row);
                }
            }
        }
        
        // Apply client-side filtering if search terms exist
        if (searchTerms != null && !searchTerms.isEmpty()) {
            List<ReportRow> filteredRows = new ArrayList<>(rows);
            
            // Apply each search term with AND logic
            for (String term : searchTerms) {
                String lowerTerm = term.trim().toLowerCase();
                filteredRows.retainAll(rows.stream()
                    .filter(row -> matchesSearchTerm(row, lowerTerm))
                    .collect(java.util.stream.Collectors.toList()));
            }
            
            rows = filteredRows;
        }

        return new ReportResult(rows, 0.0, totalAmount);
    }
    
    private boolean matchesSearchTerm(ReportRow row, String searchTerm) {
        if (row.getTransactionNo() != null && row.getTransactionNo().toLowerCase().contains(searchTerm)) {
            return true;
        }
        if (row.getTransactionType() != null && row.getTransactionType().toLowerCase().contains(searchTerm)) {
            return true;
        }
        if (row.getParty() != null && row.getParty().toLowerCase().contains(searchTerm)) {
            return true;
        }
        if (row.getRemarks() != null && row.getRemarks().toLowerCase().contains(searchTerm)) {
            return true;
        }
        return false;
    }
    
    private String buildAllTransactionsSQL(boolean withSearch) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM (");
        
        // Purchase Invoices
        sqlBuilder.append("""
            SELECT 'Purchase Invoice' as transaction_type, 
                   COALESCE(invoice_no, '') as transaction_no, 
                   invoice_date as date,
                   COALESCE(party_name, '') as party, 
                   COALESCE(net_amount, 0) as amount, 
                   COALESCE(net_amount, 0) as debit,
                   0 as credit,
                   COALESCE(remarks, '') as remarks,
                   'INVOICE' as type
            FROM purchase_invoices
            WHERE invoice_date BETWEEN ? AND ?
            """);
        
        // Sale Invoices
        sqlBuilder.append("""
            UNION ALL
            SELECT 'Sale Invoice' as transaction_type, 
                   COALESCE(invoice_no, '') as transaction_no, 
                   invoice_date as date,
                   COALESCE(party_name, '') as party, 
                   COALESCE(net_amount, 0) as amount, 
                   0 as debit,
                   COALESCE(net_amount, 0) as credit,
                   COALESCE(remarks, '') as remarks,
                   'INVOICE' as type
            FROM sale_invoices
            WHERE invoice_date BETWEEN ? AND ?
            """);
        
        // Purchase Receipts
        sqlBuilder.append("""
            UNION ALL
            SELECT 'Purchase Receipt' as transaction_type, 
                   COALESCE(receipt_no, '') as transaction_no, 
                   receipt_date as date,
                   COALESCE(party_name, '') as party, 
                   COALESCE(amount, 0) as amount, 
                   COALESCE(amount, 0) as debit,
                   0 as credit,
                   COALESCE(remarks, '') as remarks,
                   'RECEIPT' as type
            FROM purchase_receipts
            WHERE receipt_date BETWEEN ? AND ?
            """);
        
        // Sale Receipts
        sqlBuilder.append("""
            UNION ALL
            SELECT 'Sale Receipt' as transaction_type, 
                   COALESCE(receipt_no, '') as transaction_no, 
                   receipt_date as date,
                   COALESCE(party_name, '') as party, 
                   COALESCE(amount, 0) as amount, 
                   0 as debit,
                   COALESCE(amount, 0) as credit,
                   COALESCE(remarks, '') as remarks,
                   'RECEIPT' as type
            FROM sale_receipts
            WHERE receipt_date BETWEEN ? AND ?
            """);
        
        // Payments (assume payments are debits - outgoing)
        sqlBuilder.append("""
            UNION ALL
            SELECT 'Payment' as transaction_type, 
                   COALESCE(voucher_no, '') as transaction_no, 
                   payment_date as date,
                   COALESCE(account_name, '') as party, 
                   COALESCE(amount, 0) as amount, 
                   COALESCE(amount, 0) as debit,
                   0 as credit,
                   COALESCE(remarks, '') as remarks,
                   'PAYMENT' as type
            FROM payments
            WHERE payment_date BETWEEN ? AND ?
            """);
        
        // Loading Slips - use freight_amount instead of freight (assume debits - expense)
        sqlBuilder.append("""
            UNION ALL
            SELECT 'Loading Slip' as transaction_type, 
                   COALESCE(slip_no, '') as transaction_no, 
                   slip_date as date,
                   COALESCE(party_name, '') as party, 
                   COALESCE(freight_amount, 0) as amount, 
                   COALESCE(freight_amount, 0) as debit,
                   0 as credit,
                   COALESCE(remarks, '') as remarks,
                   'SLIP' as type
            FROM loading_slips
            WHERE slip_date BETWEEN ? AND ?
            """);
        
        // Lorry Receipts - use total instead of freight (assume debits - expense)
        sqlBuilder.append("""
            UNION ALL
            SELECT 'Lorry Receipt' as transaction_type, 
                   COALESCE(lr_no, '') as transaction_no, 
                   lr_date as date,
                   COALESCE(consignor_name, '') as party, 
                   COALESCE(total, 0) as amount, 
                   COALESCE(total, 0) as debit,
                   0 as credit,
                   COALESCE(remarks, '') as remarks,
                   'LR' as type
            FROM lorry_receipts
            WHERE lr_date BETWEEN ? AND ?
            """);
        
        sqlBuilder.append(") combined ORDER BY date DESC");
        
        return sqlBuilder.toString();
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
}
