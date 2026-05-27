package com.accounting.model;

public class InvoiceLineItem {

    private int id;
    private int invoiceId;
    private String date;
    private String lrNo;
    private String containerNo;
    private String vehicleNo;
    private String from;
    private String to;
    private String type;
    private double basicFreight;
    private double detentionCharge;
    private double total;

    public InvoiceLineItem() {}

    public InvoiceLineItem(String lrNo, String containerNo, String vehicleNo, String from, String to,
                          String type, double basicFreight, double detentionCharge) {
        this.lrNo = lrNo;
        this.containerNo = containerNo;
        this.vehicleNo = vehicleNo;
        this.from = from;
        this.to = to;
        this.type = type;
        this.basicFreight = basicFreight;
        this.detentionCharge = detentionCharge;
        this.total = basicFreight + detentionCharge;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLrNo() { return lrNo; }
    public void setLrNo(String lrNo) { this.lrNo = lrNo; }

    public String getContainerNo() { return containerNo; }
    public void setContainerNo(String containerNo) { this.containerNo = containerNo; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getBasicFreight() { return basicFreight; }
    public void setBasicFreight(double basicFreight) {
        this.basicFreight = basicFreight;
        recalculateTotal();
    }

    public double getDetentionCharge() { return detentionCharge; }
    public void setDetentionCharge(double detentionCharge) {
        this.detentionCharge = detentionCharge;
        recalculateTotal();
    }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    private void recalculateTotal() {
        this.total = basicFreight + detentionCharge;
    }
}
