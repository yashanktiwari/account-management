package com.accounting.model;

import java.time.LocalDateTime;

public class Vehicle {

    private int id;
    private String vehicleNo;
    private String vehicleModel;
    private String accountName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Vehicle() {}

    public Vehicle(String vehicleNo, String vehicleModel, String accountName) {
        this.vehicleNo = vehicleNo;
        this.vehicleModel = vehicleModel;
        this.accountName = accountName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return vehicleNo != null ? vehicleNo : "";
    }
}
