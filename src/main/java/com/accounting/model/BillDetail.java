package com.accounting.model;

import java.time.LocalDate;

public class BillDetail {

    private String billNo;
    private LocalDate date;
    private int dueDays;
    private double billAmt;
    private double pendingAmt;

    public BillDetail() {}

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getDueDays() { return dueDays; }
    public void setDueDays(int dueDays) { this.dueDays = dueDays; }

    public double getBillAmt() { return billAmt; }
    public void setBillAmt(double billAmt) { this.billAmt = billAmt; }

    public double getPendingAmt() { return pendingAmt; }
    public void setPendingAmt(double pendingAmt) { this.pendingAmt = pendingAmt; }
}
