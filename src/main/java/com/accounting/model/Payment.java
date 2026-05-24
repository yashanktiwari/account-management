package com.accounting.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Payment {

    private int id;
    private LocalDate paymentDate;
    private String voucherNo;
    private String voucherType;   // RECEIPT or PAYMENT
    private int accountId;
    private String accountName;
    private String particulars;
    private double amount;
    private Integer againstInvoiceId;
    private String againstInvoiceNo;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }

    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getParticulars() { return particulars; }
    public void setParticulars(String particulars) { this.particulars = particulars; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Integer getAgainstInvoiceId() { return againstInvoiceId; }
    public void setAgainstInvoiceId(Integer againstInvoiceId) { this.againstInvoiceId = againstInvoiceId; }

    public String getAgainstInvoiceNo() { return againstInvoiceNo; }
    public void setAgainstInvoiceNo(String againstInvoiceNo) { this.againstInvoiceNo = againstInvoiceNo; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
