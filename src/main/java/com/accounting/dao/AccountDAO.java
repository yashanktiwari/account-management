package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Account;
import com.accounting.model.AccountTransaction;
import com.accounting.model.BillDetail;
import com.accounting.model.OutstandingEntry;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AccountDAO {

    private static final Logger log = AppLogger.get(AccountDAO.class);

    public int save(Account a) {
        String sql = """
            INSERT INTO accounts (account_name, account_type, ac_as, ac_type, mailing_name, address,
                state_name, state_code, city_name, fax, pin_code, email, mobile, root_area_name,
                gstin, cst_no, tan_no, pan_no, tds_percent, tds_applicable, aadhar_no, drugs_lic_no,
                credit_period, credit_amt_limit, opening_balance, balance_type, nature_of_payment)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setAccountParams(ps, a);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Failed to save account: {}", a.getAccountName(), e);
        }
        return -1;
    }

    public boolean update(Account a) {
        String sql = """
            UPDATE accounts SET account_name=?, account_type=?, ac_as=?, ac_type=?, mailing_name=?,
                address=?, state_name=?, state_code=?, city_name=?, fax=?, pin_code=?, email=?,
                mobile=?, root_area_name=?, gstin=?, cst_no=?, tan_no=?, pan_no=?, tds_percent=?,
                tds_applicable=?, aadhar_no=?, drugs_lic_no=?, credit_period=?, credit_amt_limit=?,
                opening_balance=?, balance_type=?, nature_of_payment=?
            WHERE id=?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setAccountParams(ps, a);
            ps.setInt(28, a.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to update account: {}", a.getAccountName(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM accounts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to delete account id={}", id, e);
        }
        return false;
    }

    public Account findById(int id) {
        String sql = "SELECT * FROM accounts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        } catch (Exception e) {
            log.error("Failed to find account id={}", id, e);
        }
        return null;
    }

    public List<Account> findByName(String name, String accountType) {
        String sql = "SELECT * FROM accounts WHERE account_name LIKE ? AND account_type=? ORDER BY account_name";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            ps.setString(2, accountType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAccount(rs));
            }
        } catch (Exception e) {
            log.error("Failed to search accounts by name={}", name, e);
        }
        return list;
    }

    public List<Account> getAll(String accountType) {
        String sql = "SELECT * FROM accounts WHERE account_type=? ORDER BY account_name";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAccount(rs));
            }
        } catch (Exception e) {
            log.error("Failed to fetch all accounts type={}", accountType, e);
        }
        return list;
    }

    public List<Account> getAllAccounts() {
        String sql = "SELECT * FROM accounts ORDER BY account_name";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapAccount(rs));
        } catch (Exception e) {
            log.error("Failed to fetch all accounts", e);
        }
        return list;
    }

    public List<String> getAllAccountNames(String accountType) {
        String sql = "SELECT account_name FROM accounts WHERE account_type=? ORDER BY account_name";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("account_name"));
            }
        } catch (Exception e) {
            log.error("Failed to fetch account names", e);
        }
        return list;
    }

    public List<String> getAllRootAreas() {
        String sql = "SELECT area_name FROM root_areas ORDER BY area_name";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("area_name"));
        } catch (Exception e) {
            log.error("Failed to fetch root areas", e);
        }
        return list;
    }

    public void addRootArea(String areaName) {
        String sql = "INSERT IGNORE INTO root_areas (area_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, areaName);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Failed to add root area: {}", areaName, e);
        }
    }

    // ── Account Statement ──

    public List<AccountTransaction> getAccountStatement(
            int accountId, String accountType, String refType,
            LocalDate fromDate, LocalDate toDate
    ) {
        List<AccountTransaction> transactions = new ArrayList<>();

        // Get invoices
        String invoiceSql = """
            SELECT invoice_date, 'Sales A/c' AS particulars, 
                   CASE WHEN trans_type='SALE' THEN 'SALE' ELSE 'PURCHASE' END AS voucher_type,
                   invoice_no AS voucher_no, grand_total, trans_type, credit_period
            FROM invoices i
            LEFT JOIN accounts a ON i.account_id = a.id
            WHERE i.account_id = ? AND i.invoice_date BETWEEN ? AND ?
            ORDER BY i.invoice_date, i.id
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(invoiceSql)) {
            ps.setInt(1, accountId);
            ps.setDate(2, Date.valueOf(fromDate));
            ps.setDate(3, Date.valueOf(toDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AccountTransaction t = new AccountTransaction();
                    t.setDate(rs.getDate("invoice_date").toLocalDate());
                    t.setParticulars(rs.getString("particulars"));
                    t.setVoucherType(rs.getString("voucher_type"));
                    t.setVoucherNo(rs.getString("voucher_no"));

                    double total = rs.getDouble("grand_total");
                    String transType = rs.getString("trans_type");

                    if ("SALE".equals(transType)) {
                        t.setDebit(total);
                        t.setCredit(0);
                    } else {
                        t.setDebit(0);
                        t.setCredit(total);
                    }

                    long days = ChronoUnit.DAYS.between(t.getDate(), LocalDate.now());
                    t.setDueDays((int) days);
                    transactions.add(t);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get invoice transactions for account {}", accountId, e);
        }

        // Get payments
        String paymentSql = """
            SELECT payment_date, particulars, voucher_type, voucher_no, amount,
                   against_invoice_no
            FROM payments
            WHERE account_id = ? AND payment_date BETWEEN ? AND ?
            ORDER BY payment_date, id
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(paymentSql)) {
            ps.setInt(1, accountId);
            ps.setDate(2, Date.valueOf(fromDate));
            ps.setDate(3, Date.valueOf(toDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AccountTransaction t = new AccountTransaction();
                    t.setDate(rs.getDate("payment_date").toLocalDate());

                    String particulars = rs.getString("particulars");
                    t.setParticulars(particulars != null ? particulars : "");
                    t.setVoucherType(rs.getString("voucher_type"));
                    t.setVoucherNo(rs.getString("voucher_no"));

                    double amount = rs.getDouble("amount");
                    String vType = rs.getString("voucher_type");

                    if ("RECEIPT".equals(vType)) {
                        t.setDebit(0);
                        t.setCredit(amount);
                    } else {
                        t.setDebit(amount);
                        t.setCredit(0);
                    }

                    String againstNo = rs.getString("against_invoice_no");
                    if (againstNo != null && !againstNo.isBlank()) {
                        t.setRemarks("AGAINST INVOICE N... " + againstNo);
                    }

                    transactions.add(t);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get payment transactions for account {}", accountId, e);
        }

        // Sort by date
        transactions.sort(Comparator.comparing(AccountTransaction::getDate)
                .thenComparing(t -> t.getVoucherNo() != null ? t.getVoucherNo() : ""));

        // Calculate running balance
        double balance = 0;
        for (AccountTransaction t : transactions) {
            balance = balance + t.getDebit() - t.getCredit();
            t.setBalance(Math.abs(balance));
            t.setBalanceType(balance >= 0 ? "Dr" : "Cr");
        }

        return transactions;
    }

    // ── Outstanding Register ──

    public List<OutstandingEntry> getOutstanding(
            String acType, String rootCity, String natureOfPayment,
            int dueDaysFilter, LocalDate fromDate, LocalDate toDate
    ) {
        // acType: RECEIVABLE (customers/debtors) or PAYABLE (suppliers/creditors)
        String accountType = "RECEIVABLE".equals(acType) ? "CUSTOMER" : "SUPPLIER";

        List<OutstandingEntry> entries = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.account_name, a.root_area_name, a.mobile, a.credit_period,
                   COALESCE(SUM(CASE WHEN i.trans_type='SALE' THEN i.grand_total ELSE 0 END), 0) AS total_sales,
                   COALESCE(SUM(CASE WHEN i.trans_type='PURCHASE' THEN i.grand_total ELSE 0 END), 0) AS total_purchases,
                   COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.account_id = a.id
                        AND p.payment_date BETWEEN ? AND ?), 0) AS total_payments
            FROM accounts a
            LEFT JOIN invoices i ON i.account_id = a.id AND i.invoice_date BETWEEN ? AND ?
            WHERE a.account_type = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(fromDate));
        params.add(Date.valueOf(toDate));
        params.add(Date.valueOf(fromDate));
        params.add(Date.valueOf(toDate));
        params.add(accountType);

        if (rootCity != null && !rootCity.isBlank()) {
            sql.append(" AND a.root_area_name = ?");
            params.add(rootCity);
        }
        if (natureOfPayment != null && !natureOfPayment.isBlank()) {
            sql.append(" AND a.nature_of_payment = ?");
            params.add(natureOfPayment);
        }

        sql.append(" GROUP BY a.id ORDER BY a.account_name");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Date d) ps.setDate(i + 1, d);
                else ps.setString(i + 1, (String) p);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int accId = rs.getInt("id");
                    double totalSales = rs.getDouble("total_sales");
                    double totalPurchases = rs.getDouble("total_purchases");
                    double totalPayments = rs.getDouble("total_payments");

                    double outstanding;
                    String drCr;
                    if ("CUSTOMER".equals(accountType)) {
                        outstanding = totalSales - totalPayments;
                        drCr = outstanding >= 0 ? "Dr" : "Cr";
                    } else {
                        outstanding = totalPurchases - totalPayments;
                        drCr = outstanding >= 0 ? "Dr" : "Cr";
                    }

                    if (Math.abs(outstanding) < 0.01) continue;

                    OutstandingEntry entry = new OutstandingEntry();
                    entry.setAccountId(accId);
                    entry.setAccountName(rs.getString("account_name"));
                    entry.setRootArea(rs.getString("root_area_name"));
                    entry.setMobile(rs.getString("mobile"));
                    entry.setAmount(Math.abs(outstanding));
                    entry.setDrCr(drCr);

                    // Get bill details
                    entry.setBillDetails(getBillDetails(accId, accountType, fromDate, toDate));

                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get outstanding entries", e);
        }

        return entries;
    }

    private List<BillDetail> getBillDetails(int accountId, String accountType, LocalDate from, LocalDate to) {
        List<BillDetail> details = new ArrayList<>();
        String sql = """
            SELECT invoice_no, invoice_date, grand_total,
                   COALESCE((SELECT SUM(p.amount) FROM payments p 
                       WHERE p.against_invoice_id = i.id), 0) AS paid_amt
            FROM invoices i
            WHERE i.account_id = ? AND i.invoice_date BETWEEN ? AND ?
            ORDER BY i.invoice_date
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double total = rs.getDouble("grand_total");
                    double paid = rs.getDouble("paid_amt");
                    double pending = total - paid;
                    if (Math.abs(pending) < 0.01) continue;

                    BillDetail bd = new BillDetail();
                    bd.setBillNo(rs.getString("invoice_no"));
                    bd.setDate(rs.getDate("invoice_date").toLocalDate());
                    bd.setBillAmt(total);
                    bd.setPendingAmt(pending);

                    long days = ChronoUnit.DAYS.between(bd.getDate(), LocalDate.now());
                    bd.setDueDays((int) days);
                    details.add(bd);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get bill details for account {}", accountId, e);
        }
        return details;
    }

    private void setAccountParams(PreparedStatement ps, Account a) throws SQLException {
        ps.setString(1, a.getAccountName());
        ps.setString(2, a.getAccountType());
        ps.setString(3, a.getAcAs());
        ps.setString(4, a.getAcType());
        ps.setString(5, a.getMailingName());
        ps.setString(6, a.getAddress());
        ps.setString(7, a.getStateName());
        ps.setString(8, a.getStateCode());
        ps.setString(9, a.getCityName());
        ps.setString(10, a.getFax());
        ps.setString(11, a.getPinCode());
        ps.setString(12, a.getEmail());
        ps.setString(13, a.getMobile());
        ps.setString(14, a.getRootAreaName());
        ps.setString(15, a.getGstin());
        ps.setString(16, a.getCstNo());
        ps.setString(17, a.getTanNo());
        ps.setString(18, a.getPanNo());
        ps.setDouble(19, a.getTdsPercent());
        ps.setString(20, a.getTdsApplicable());
        ps.setString(21, a.getAadharNo());
        ps.setString(22, a.getDrugsLicNo());
        ps.setInt(23, a.getCreditPeriod());
        ps.setDouble(24, a.getCreditAmtLimit());
        ps.setDouble(25, a.getOpeningBalance());
        ps.setString(26, a.getBalanceType());
        ps.setString(27, a.getNatureOfPayment());
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("id"));
        a.setAccountName(rs.getString("account_name"));
        a.setAccountType(rs.getString("account_type"));
        a.setAcAs(rs.getString("ac_as"));
        a.setAcType(rs.getString("ac_type"));
        a.setMailingName(rs.getString("mailing_name"));
        a.setAddress(rs.getString("address"));
        a.setStateName(rs.getString("state_name"));
        a.setStateCode(rs.getString("state_code"));
        a.setCityName(rs.getString("city_name"));
        a.setFax(rs.getString("fax"));
        a.setPinCode(rs.getString("pin_code"));
        a.setEmail(rs.getString("email"));
        a.setMobile(rs.getString("mobile"));
        a.setRootAreaName(rs.getString("root_area_name"));
        a.setGstin(rs.getString("gstin"));
        a.setCstNo(rs.getString("cst_no"));
        a.setTanNo(rs.getString("tan_no"));
        a.setPanNo(rs.getString("pan_no"));
        a.setTdsPercent(rs.getDouble("tds_percent"));
        a.setTdsApplicable(rs.getString("tds_applicable"));
        a.setAadharNo(rs.getString("aadhar_no"));
        a.setDrugsLicNo(rs.getString("drugs_lic_no"));
        a.setCreditPeriod(rs.getInt("credit_period"));
        a.setCreditAmtLimit(rs.getDouble("credit_amt_limit"));
        a.setOpeningBalance(rs.getDouble("opening_balance"));
        a.setBalanceType(rs.getString("balance_type"));
        a.setNatureOfPayment(rs.getString("nature_of_payment"));
        a.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        a.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return a;
    }
}
