package com.accounting.model;

import java.time.LocalDateTime;

public class Account {

    private int id;
    private String accountName;
    private String accountType; // CUSTOMER or SUPPLIER
    private String acAs;        // "Customer" or "Suppliers"
    private String acType;      // "SUNDRY DEBTORS" or "SUNDRY CREDITORS"
    private String mailingName;
    private String address;
    private String stateName;
    private String stateCode;
    private String cityName;
    private String fax;
    private String pinCode;
    private String email;
    private String mobile;
    private String rootAreaName;
    private String gstin;
    private String cstNo;
    private String tanNo;
    private String panNo;
    private double tdsPercent;
    private String tdsApplicable; // YES or NO
    private String aadharNo;
    private String drugsLicNo;
    private int creditPeriod;
    private double creditAmtLimit;
    private double openingBalance;
    private String balanceType; // Debit or Credit
    private String natureOfPayment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getAcAs() { return acAs; }
    public void setAcAs(String acAs) { this.acAs = acAs; }

    public String getAcType() { return acType; }
    public void setAcType(String acType) { this.acType = acType; }

    public String getMailingName() { return mailingName; }
    public void setMailingName(String mailingName) { this.mailingName = mailingName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRootAreaName() { return rootAreaName; }
    public void setRootAreaName(String rootAreaName) { this.rootAreaName = rootAreaName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getCstNo() { return cstNo; }
    public void setCstNo(String cstNo) { this.cstNo = cstNo; }

    public String getTanNo() { return tanNo; }
    public void setTanNo(String tanNo) { this.tanNo = tanNo; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }

    public double getTdsPercent() { return tdsPercent; }
    public void setTdsPercent(double tdsPercent) { this.tdsPercent = tdsPercent; }

    public String getTdsApplicable() { return tdsApplicable; }
    public void setTdsApplicable(String tdsApplicable) { this.tdsApplicable = tdsApplicable; }

    public String getAadharNo() { return aadharNo; }
    public void setAadharNo(String aadharNo) { this.aadharNo = aadharNo; }

    public String getDrugsLicNo() { return drugsLicNo; }
    public void setDrugsLicNo(String drugsLicNo) { this.drugsLicNo = drugsLicNo; }

    public int getCreditPeriod() { return creditPeriod; }
    public void setCreditPeriod(int creditPeriod) { this.creditPeriod = creditPeriod; }

    public double getCreditAmtLimit() { return creditAmtLimit; }
    public void setCreditAmtLimit(double creditAmtLimit) { this.creditAmtLimit = creditAmtLimit; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public String getBalanceType() { return balanceType; }
    public void setBalanceType(String balanceType) { this.balanceType = balanceType; }

    public String getNatureOfPayment() { return natureOfPayment; }
    public void setNatureOfPayment(String natureOfPayment) { this.natureOfPayment = natureOfPayment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return accountName != null ? accountName : "";
    }
}
