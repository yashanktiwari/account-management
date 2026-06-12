package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Party;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class LedgerDAO {

    private static final Logger log = get(LedgerDAO.class);

    /**
     * Represents a single ledger entry (transaction row).
     */
    public static class LedgerEntry {
        private LocalDate date;
        private String transactionType;
        private String transactionNo;
        private double debit;
        private double credit;
        private double runningBalance;
        private String remarks;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

        public String getTransactionNo() { return transactionNo; }
        public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

        public double getDebit() { return debit; }
        public void setDebit(double debit) { this.debit = debit; }

        public double getCredit() { return credit; }
        public void setCredit(double credit) { this.credit = credit; }

        public double getRunningBalance() { return runningBalance; }
        public void setRunningBalance(double runningBalance) { this.runningBalance = runningBalance; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    /**
     * Result object containing opening balance, closing balance, and all ledger entries.
     */
    public static class LedgerResult {
        private double openingBalance;
        private double closingBalance;
        private final List<LedgerEntry> entries = new ArrayList<>();

        public double getOpeningBalance() { return openingBalance; }
        public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

        public double getClosingBalance() { return closingBalance; }
        public void setClosingBalance(double closingBalance) { this.closingBalance = closingBalance; }

        public List<LedgerEntry> getEntries() { return entries; }
    }

    /**
     * Get the current balance for a party (opening balance + all transactions).
     */
    public double getCurrentBalance(int partyId) throws Exception {
        double partyOpeningBalance = getPartyOpeningBalance(partyId);
        double allTransactionsBalance = getAllTransactionsBalance(partyId);
        return partyOpeningBalance + allTransactionsBalance;
    }

    /**
     * Generate a ledger statement for a party within a date range.
     *
     * Balance logic (as per business rules):
     * - Purchase Invoice  -> Credit (balance increases, we owe the party)
     * - Sale Invoice      -> Debit  (balance decreases, party owes us)
     * - Purchase Receipt  -> Credit (balance increases)
     * - Sale Receipt      -> Debit  (balance decreases)
     *
     * Positive balance = we owe the party (Cr)
     * Negative balance = party owes us (Dr)
     */
    public LedgerResult generateLedger(int partyId, LocalDate fromDate, LocalDate toDate) throws Exception {
        LedgerResult result = new LedgerResult();

        // 1. Get party's opening balance from the parties table
        double partyOpeningBalance = getPartyOpeningBalance(partyId);

        // 2. Calculate the sum of all transactions BEFORE the fromDate to get the effective opening balance
        double prePeriodBalance = getPrePeriodBalance(partyId, fromDate);
        double openingBalance = partyOpeningBalance + prePeriodBalance;
        result.setOpeningBalance(openingBalance);

        // 3. Get all transactions within the date range
        List<LedgerEntry> entries = getTransactionsInRange(partyId, fromDate, toDate);

        // 4. Compute running balance for each entry
        double runningBalance = openingBalance;
        for (LedgerEntry entry : entries) {
            runningBalance += entry.getCredit() - entry.getDebit();
            entry.setRunningBalance(runningBalance);
        }

        result.getEntries().addAll(entries);
        result.setClosingBalance(runningBalance);

        return result;
    }

    /**
     * Get the party's opening balance from the parties table.
     * balance_type: "Cr" means positive, "Dr" means negative.
     */
    private double getPartyOpeningBalance(int partyId) throws Exception {
        String sql = "SELECT opening_balance, balance_type FROM parties WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, partyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String balStr = rs.getString("opening_balance");
                    String balType = rs.getString("balance_type");
                    double amount = 0;
                    if (balStr != null && !balStr.trim().isEmpty()) {
                        try {
                            amount = Double.parseDouble(balStr.trim());
                        } catch (NumberFormatException e) {
                            amount = 0;
                        }
                    }
                    if ("Dr".equalsIgnoreCase(balType)) {
                        amount = -amount;
                    }
                    return amount;
                }
            }
        }
        return 0;
    }

    /**
     * Calculate the net balance from all transactions for a party BEFORE a given date.
     * This is used to compute the effective opening balance for the ledger period.
     */
    private double getPrePeriodBalance(int partyId, LocalDate beforeDate) throws Exception {
        double balance = 0;

        // Purchase Invoices before fromDate -> Credit (+)
        balance += sumAmount(
            "SELECT COALESCE(SUM(net_amount), 0) FROM purchase_invoices WHERE party_id = ? AND invoice_date < ?",
            partyId, beforeDate);

        // Sale Invoices before fromDate -> Debit (-)
        balance -= sumAmount(
            "SELECT COALESCE(SUM(net_amount), 0) FROM sale_invoices WHERE party_id = ? AND invoice_date < ?",
            partyId, beforeDate);

        // Purchase Receipts before fromDate -> Credit (+)
        balance += sumAmount(
            "SELECT COALESCE(SUM(amount), 0) FROM purchase_receipts WHERE party_id = ? AND receipt_date < ?",
            partyId, beforeDate);

        // Sale Receipts before fromDate -> Debit (-)
        balance -= sumAmount(
            "SELECT COALESCE(SUM(amount), 0) FROM sale_receipts WHERE party_id = ? AND receipt_date < ?",
            partyId, beforeDate);

        return balance;
    }

    /**
     * Calculate the net balance from all transactions for a party (all time).
     */
    private double getAllTransactionsBalance(int partyId) throws Exception {
        double balance = 0;

        // Purchase Invoices -> Credit (+)
        balance += sumAmount(
            "SELECT COALESCE(SUM(net_amount), 0) FROM purchase_invoices WHERE party_id = ?",
            partyId);

        // Sale Invoices -> Debit (-)
        balance -= sumAmount(
            "SELECT COALESCE(SUM(net_amount), 0) FROM sale_invoices WHERE party_id = ?",
            partyId);

        // Purchase Receipts -> Credit (+)
        balance += sumAmount(
            "SELECT COALESCE(SUM(amount), 0) FROM purchase_receipts WHERE party_id = ?",
            partyId);

        // Sale Receipts -> Debit (-)
        balance -= sumAmount(
            "SELECT COALESCE(SUM(amount), 0) FROM sale_receipts WHERE party_id = ?",
            partyId);

        return balance;
    }

    private double sumAmount(String sql, int partyId) throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, partyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    private double sumAmount(String sql, int partyId, LocalDate beforeDate) throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, partyId);
            pstmt.setDate(2, java.sql.Date.valueOf(beforeDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    /**
     * Get all transactions for a party within a date range, ordered by date.
     */
    private List<LedgerEntry> getTransactionsInRange(int partyId, LocalDate fromDate, LocalDate toDate) throws Exception {
        String sql = """
            SELECT * FROM (
                SELECT invoice_date as txn_date, 'Purchase Invoice' as txn_type,
                       COALESCE(invoice_no, '') as txn_no,
                       0 as debit_amt,
                       COALESCE(net_amount, 0) as credit_amt,
                       COALESCE(remarks, '') as remarks
                FROM purchase_invoices
                WHERE party_id = ? AND invoice_date BETWEEN ? AND ?
                
                UNION ALL
                
                SELECT invoice_date as txn_date, 'Sale Invoice' as txn_type,
                       COALESCE(invoice_no, '') as txn_no,
                       COALESCE(net_amount, 0) as debit_amt,
                       0 as credit_amt,
                       COALESCE(remarks, '') as remarks
                FROM sale_invoices
                WHERE party_id = ? AND invoice_date BETWEEN ? AND ?
                
                UNION ALL
                
                SELECT receipt_date as txn_date, 'Purchase Receipt' as txn_type,
                       COALESCE(receipt_no, '') as txn_no,
                       0 as debit_amt,
                       COALESCE(amount, 0) as credit_amt,
                       COALESCE(remarks, '') as remarks
                FROM purchase_receipts
                WHERE party_id = ? AND receipt_date BETWEEN ? AND ?
                
                UNION ALL
                
                SELECT receipt_date as txn_date, 'Sale Receipt' as txn_type,
                       COALESCE(receipt_no, '') as txn_no,
                       COALESCE(amount, 0) as debit_amt,
                       0 as credit_amt,
                       COALESCE(remarks, '') as remarks
                FROM sale_receipts
                WHERE party_id = ? AND receipt_date BETWEEN ? AND ?
            ) combined
            ORDER BY txn_date ASC, txn_type ASC
            """;

        List<LedgerEntry> entries = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idx = 1;
            for (int i = 0; i < 4; i++) {
                pstmt.setInt(idx++, partyId);
                pstmt.setDate(idx++, java.sql.Date.valueOf(fromDate));
                pstmt.setDate(idx++, java.sql.Date.valueOf(toDate));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LedgerEntry entry = new LedgerEntry();
                    entry.setDate(rs.getDate("txn_date").toLocalDate());
                    entry.setTransactionType(rs.getString("txn_type"));
                    entry.setTransactionNo(rs.getString("txn_no"));
                    entry.setDebit(rs.getDouble("debit_amt"));
                    entry.setCredit(rs.getDouble("credit_amt"));
                    entry.setRemarks(rs.getString("remarks"));
                    entries.add(entry);
                }
            }
        }
        return entries;
    }
}
