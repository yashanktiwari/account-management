package com.accounting.model;

import java.time.LocalDate;

/**
 * Unified transaction model representing all transaction types:
 * Purchase Invoice, Sale Invoice, Purchase Receipt, Sale Receipt, Loading Slip, Lorry Receipt
 */
public class UnifiedTransaction {

    private int serialNo;
    private String transactionType; // PURCHASE_INVOICE, SALE_INVOICE, PURCHASE_RECEIPT, SALE_RECEIPT, LOADING_SLIP, LORRY_RECEIPT
    private String transactionNo; // invoiceNo, receiptNo, slipNo, lrNo
    private LocalDate transactionDate;
    private String partyName;
    private String vehicleNo;
    private String fromLocation;
    private String toLocation;
    private String description;
    private String gst;
    private double taxableAmount;
    private double sgstAmount;
    private double cgstAmount;
    private double igstAmount;
    private double totalGst;
    private double netAmount;
    private double amount;
    private double freightAmount;
    private double advanceAmount;
    private double balanceAmount;
    private String paymentMode;
    private String chequeNo;
    private LocalDate chequeDate;
    private String bankName;
    private String remarks;
    private String status;

    public UnifiedTransaction() {}

    // Getters and Setters
    public int getSerialNo() { return serialNo; }
    public void setSerialNo(int serialNo) { this.serialNo = serialNo; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }

    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGst() { return gst; }
    public void setGst(String gst) { this.gst = gst; }

    public double getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(double taxableAmount) { this.taxableAmount = taxableAmount; }

    public double getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(double sgstAmount) { this.sgstAmount = sgstAmount; }

    public double getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(double cgstAmount) { this.cgstAmount = cgstAmount; }

    public double getIgstAmount() { return igstAmount; }
    public void setIgstAmount(double igstAmount) { this.igstAmount = igstAmount; }

    public double getTotalGst() { return totalGst; }
    public void setTotalGst(double totalGst) { this.totalGst = totalGst; }

    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getFreightAmount() { return freightAmount; }
    public void setFreightAmount(double freightAmount) { this.freightAmount = freightAmount; }

    public double getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(double advanceAmount) { this.advanceAmount = advanceAmount; }

    public double getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(double balanceAmount) { this.balanceAmount = balanceAmount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }

    public LocalDate getChequeDate() { return chequeDate; }
    public void setChequeDate(LocalDate chequeDate) { this.chequeDate = chequeDate; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
