package com.accounting.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Invoice {

    private int id;
    private LocalDate invoiceDate;
    private String invoiceNo;
    private int accountId;
    private String accountName;
    private double totalQty;
    private double totalAmt;
    private double totalTax;
    private double grandTotal;
    private String transType;    // SALE or PURCHASE
    private String taxType;      // GST, IGST, N/A
    private String cashCredit;   // CASH or CREDIT
    private String voucherType;
    private String vehicleNo;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Invoice() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public double getTotalQty() { return totalQty; }
    public void setTotalQty(double totalQty) { this.totalQty = totalQty; }

    public double getTotalAmt() { return totalAmt; }
    public void setTotalAmt(double totalAmt) { this.totalAmt = totalAmt; }

    public double getTotalTax() { return totalTax; }
    public void setTotalTax(double totalTax) { this.totalTax = totalTax; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getTransType() { return transType; }
    public void setTransType(String transType) { this.transType = transType; }

    public String getTaxType() { return taxType; }
    public void setTaxType(String taxType) { this.taxType = taxType; }

    public String getCashCredit() { return cashCredit; }
    public void setCashCredit(String cashCredit) { this.cashCredit = cashCredit; }

    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
