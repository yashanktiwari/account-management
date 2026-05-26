package com.accounting.model;

import java.time.LocalDateTime;

public class Party {

    private int id;
    private String name;         // company name
    private String type;         // kept for DAO backward compat
    private String mailingName;  // kept for DAO backward compat
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String mobile;
    private String email;
    private String pan;
    private String gstin;
    private String creditLimit;
    private String openingBalance;
    private String balanceType;
    private String natureOfPayment;
    private String bankName;
    private String bankAccount;
    private String ifscCode;
    private String remarks;
    // New fields
    private String ownerName;
    private String cstNo;
    private String tanNo;
    private String tds;
    private String aadharNo;
    private String routes;  // pipe-separated list of route values
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Party() {}

    public Party(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /** Alias: company name is stored in the 'name' field */
    public String getCompanyName() { return name; }
    public void setCompanyName(String companyName) { this.name = companyName; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMailingName() { return mailingName; }
    public void setMailingName(String mailingName) { this.mailingName = mailingName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getCreditLimit() { return creditLimit; }
    public void setCreditLimit(String creditLimit) { this.creditLimit = creditLimit; }

    public String getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(String openingBalance) { this.openingBalance = openingBalance; }

    public String getBalanceType() { return balanceType; }
    public void setBalanceType(String balanceType) { this.balanceType = balanceType; }

    public String getNatureOfPayment() { return natureOfPayment; }
    public void setNatureOfPayment(String natureOfPayment) { this.natureOfPayment = natureOfPayment; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getCstNo() { return cstNo; }
    public void setCstNo(String cstNo) { this.cstNo = cstNo; }

    public String getTanNo() { return tanNo; }
    public void setTanNo(String tanNo) { this.tanNo = tanNo; }

    public String getTds() { return tds; }
    public void setTds(String tds) { this.tds = tds; }

    public String getAadharNo() { return aadharNo; }
    public void setAadharNo(String aadharNo) { this.aadharNo = aadharNo; }

    public String getRoutes() { return routes; }
    public void setRoutes(String routes) { this.routes = routes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
