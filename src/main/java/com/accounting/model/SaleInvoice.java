package com.accounting.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SaleInvoice {

    private int id;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private LocalDate deliveryDate;
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
    private String rcvrName;
    private String rcvrAddress;
    private String rcvrContactNo;
    private String rcvrGstin;
    private String status; // DRAFT, SAVED, PRINTED
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    public SaleInvoice() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

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

    public String getRcvrName() { return rcvrName; }
    public void setRcvrName(String rcvrName) { this.rcvrName = rcvrName; }

    public String getRcvrAddress() { return rcvrAddress; }
    public void setRcvrAddress(String rcvrAddress) { this.rcvrAddress = rcvrAddress; }

    public String getRcvrContactNo() { return rcvrContactNo; }
    public void setRcvrContactNo(String rcvrContactNo) { this.rcvrContactNo = rcvrContactNo; }

    public String getRcvrGstin() { return rcvrGstin; }
    public void setRcvrGstin(String rcvrGstin) { this.rcvrGstin = rcvrGstin; }

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
}
