package com.accounting.model;

import java.time.LocalDate;

public class LoadingSlip {

    private int id;
    private String slipNo;
    private LocalDate slipDate;
    private String partyName;
    private String vehicleNo;
    private String grNo;
    private String station;
    private String toLocation;
    private String weight;
    private String rate;
    private double freightAmount;
    private double advanceAmount;
    private double balanceAmount;
    private String bankName;
    private String accountNo;
    private String ifscCode;
    private String status;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public LoadingSlip() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) { this.slipNo = slipNo; }

    public LocalDate getSlipDate() { return slipDate; }
    public void setSlipDate(LocalDate slipDate) { this.slipDate = slipDate; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getGrNo() { return grNo; }
    public void setGrNo(String grNo) { this.grNo = grNo; }

    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }

    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getRate() { return rate; }
    public void setRate(String rate) { this.rate = rate; }

    public double getFreightAmount() { return freightAmount; }
    public void setFreightAmount(double freightAmount) { this.freightAmount = freightAmount; }

    public double getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(double advanceAmount) { this.advanceAmount = advanceAmount; }

    public double getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(double balanceAmount) { this.balanceAmount = balanceAmount; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}
