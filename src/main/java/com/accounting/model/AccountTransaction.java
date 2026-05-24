package com.accounting.model;

import java.time.LocalDate;

public class AccountTransaction {

    private LocalDate date;
    private String particulars;
    private String voucherType;
    private String voucherNo;
    private double debit;
    private double credit;
    private double balance;
    private String balanceType; // Dr or Cr
    private int dueDays;
    private String remarks;

    public AccountTransaction() {}

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getParticulars() { return particulars; }
    public void setParticulars(String particulars) { this.particulars = particulars; }

    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }

    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }

    public double getDebit() { return debit; }
    public void setDebit(double debit) { this.debit = debit; }

    public double getCredit() { return credit; }
    public void setCredit(double credit) { this.credit = credit; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getBalanceType() { return balanceType; }
    public void setBalanceType(String balanceType) { this.balanceType = balanceType; }

    public int getDueDays() { return dueDays; }
    public void setDueDays(int dueDays) { this.dueDays = dueDays; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
