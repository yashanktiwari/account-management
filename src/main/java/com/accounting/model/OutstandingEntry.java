package com.accounting.model;

import java.util.ArrayList;
import java.util.List;

public class OutstandingEntry {

    private int accountId;
    private String accountName;
    private String rootArea;
    private String mobile;
    private double amount;
    private String drCr; // Dr or Cr
    private List<BillDetail> billDetails = new ArrayList<>();

    public OutstandingEntry() {}

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getRootArea() { return rootArea; }
    public void setRootArea(String rootArea) { this.rootArea = rootArea; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDrCr() { return drCr; }
    public void setDrCr(String drCr) { this.drCr = drCr; }

    public List<BillDetail> getBillDetails() { return billDetails; }
    public void setBillDetails(List<BillDetail> billDetails) { this.billDetails = billDetails; }
}
