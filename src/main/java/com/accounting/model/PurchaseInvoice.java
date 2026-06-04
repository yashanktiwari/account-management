package com.accounting.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseInvoice {

    private int id;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private int partyId;
    private String partyName;
    private String voucherType;
    private String gst;
    private double taxableAmount;
    private double sgstAmount;
    private double cgstAmount;
    private double igstAmount;
    private double totalGst;
    private double netAmount;
    private String remarks;
    private String bankName;
    private String bankAccount;
    private String ifscCode;
    private String creditDebit;
    private String accountName;
    private String paidBy;
    private String paymentMode;
    private String supplierAddress;
    private String supplierContactNumber;
    private String supplierGstNo;
    private double loadingUnloadingCharges;
    private double weighBridgeCharges;
    private String status; // DRAFT, SAVED, PRINTED
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    public PurchaseInvoice() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public int getPartyId() { return partyId; }
    public void setPartyId(int partyId) { this.partyId = partyId; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }

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

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getCreditDebit() { return creditDebit; }
    public void setCreditDebit(String creditDebit) { this.creditDebit = creditDebit; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getSupplierAddress() { return supplierAddress; }
    public void setSupplierAddress(String supplierAddress) { this.supplierAddress = supplierAddress; }

    public String getSupplierContactNumber() { return supplierContactNumber; }
    public void setSupplierContactNumber(String supplierContactNumber) { this.supplierContactNumber = supplierContactNumber; }

    public String getSupplierGstNo() { return supplierGstNo; }
    public void setSupplierGstNo(String supplierGstNo) { this.supplierGstNo = supplierGstNo; }

    public double getLoadingUnloadingCharges() { return loadingUnloadingCharges; }
    public void setLoadingUnloadingCharges(double loadingUnloadingCharges) { this.loadingUnloadingCharges = loadingUnloadingCharges; }

    public double getWeighBridgeCharges() { return weighBridgeCharges; }
    public void setWeighBridgeCharges(double weighBridgeCharges) { this.weighBridgeCharges = weighBridgeCharges; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }

    public List<InvoiceLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<InvoiceLineItem> lineItems) { this.lineItems = lineItems; }

    public void addLineItem(InvoiceLineItem item) {
        item.setInvoiceId(this.id);
        this.lineItems.add(item);
    }

    public void removeLineItem(int index) {
        if (index >= 0 && index < lineItems.size()) {
            lineItems.remove(index);
        }
    }

    public double calculateTotal() {
        return lineItems.stream().mapToDouble(InvoiceLineItem::getTotal).sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseInvoice that = (PurchaseInvoice) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
