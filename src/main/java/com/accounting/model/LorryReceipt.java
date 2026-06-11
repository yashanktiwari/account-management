package com.accounting.model;

import java.time.LocalDate;

public class LorryReceipt {

    private int id;
    private String lrNo;
    private LocalDate lrDate;
    private String vehicleNo;
    private String fromLocation;
    private String toLocation;
    private String eWayBillNo;
    private String consignorName;
    private String consignorGstin;
    private String consigneeName;
    private String consigneeGstin;
    private String noOfPackages;
    private String methodOfPacking;
    private String description;
    private String weightActual;
    private String weightCharged;
    private String rate;
    private double freightToPay;
    private double freightPaid;
    private double freight;
    private String freightWatermark;
    private double advance;
    private double balance;
    private double aoc;
    private double stCharge;
    private double total;
    private String stNo;
    private String shNo;
    private String grossWeight;
    private String tareWeight;
    private String netWeight;
    private String valueRs;
    private double toPayRs;
    private double advPaidRs;
    private String invNo;
    private LocalDate invDate;
    private String insuranceCompany;
    private String policyNo;
    private LocalDate policyDate;
    private String insuranceAmount;
    private LocalDate insuranceDate;
    private String riskType;
    private String status;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public LorryReceipt() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLrNo() { return lrNo; }
    public void setLrNo(String lrNo) { this.lrNo = lrNo; }

    public LocalDate getLrDate() { return lrDate; }
    public void setLrDate(LocalDate lrDate) { this.lrDate = lrDate; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }

    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }

    public String getEWayBillNo() { return eWayBillNo; }
    public void setEWayBillNo(String eWayBillNo) { this.eWayBillNo = eWayBillNo; }

    public String getConsignorName() { return consignorName; }
    public void setConsignorName(String consignorName) { this.consignorName = consignorName; }

    public String getConsignorGstin() { return consignorGstin; }
    public void setConsignorGstin(String consignorGstin) { this.consignorGstin = consignorGstin; }

    public String getConsigneeName() { return consigneeName; }
    public void setConsigneeName(String consigneeName) { this.consigneeName = consigneeName; }

    public String getConsigneeGstin() { return consigneeGstin; }
    public void setConsigneeGstin(String consigneeGstin) { this.consigneeGstin = consigneeGstin; }

    public String getNoOfPackages() { return noOfPackages; }
    public void setNoOfPackages(String noOfPackages) { this.noOfPackages = noOfPackages; }

    public String getMethodOfPacking() { return methodOfPacking; }
    public void setMethodOfPacking(String methodOfPacking) { this.methodOfPacking = methodOfPacking; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWeightActual() { return weightActual; }
    public void setWeightActual(String weightActual) { this.weightActual = weightActual; }

    public String getWeightCharged() { return weightCharged; }
    public void setWeightCharged(String weightCharged) { this.weightCharged = weightCharged; }

    public String getRate() { return rate; }
    public void setRate(String rate) { this.rate = rate; }

    public double getFreightToPay() { return freightToPay; }
    public void setFreightToPay(double freightToPay) { this.freightToPay = freightToPay; }

    public double getFreightPaid() { return freightPaid; }
    public void setFreightPaid(double freightPaid) { this.freightPaid = freightPaid; }

    public double getFreight() { return freight; }
    public void setFreight(double freight) { this.freight = freight; }

    public String getFreightWatermark() { return freightWatermark; }
    public void setFreightWatermark(String freightWatermark) { this.freightWatermark = freightWatermark; }

    public double getAdvance() { return advance; }
    public void setAdvance(double advance) { this.advance = advance; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getAoc() { return aoc; }
    public void setAoc(double aoc) { this.aoc = aoc; }

    public double getStCharge() { return stCharge; }
    public void setStCharge(double stCharge) { this.stCharge = stCharge; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStNo() { return stNo; }
    public void setStNo(String stNo) { this.stNo = stNo; }

    public String getShNo() { return shNo; }
    public void setShNo(String shNo) { this.shNo = shNo; }

    public String getGrossWeight() { return grossWeight; }
    public void setGrossWeight(String grossWeight) { this.grossWeight = grossWeight; }

    public String getTareWeight() { return tareWeight; }
    public void setTareWeight(String tareWeight) { this.tareWeight = tareWeight; }

    public String getNetWeight() { return netWeight; }
    public void setNetWeight(String netWeight) { this.netWeight = netWeight; }

    public String getValueRs() { return valueRs; }
    public void setValueRs(String valueRs) { this.valueRs = valueRs; }

    public double getToPayRs() { return toPayRs; }
    public void setToPayRs(double toPayRs) { this.toPayRs = toPayRs; }

    public double getAdvPaidRs() { return advPaidRs; }
    public void setAdvPaidRs(double advPaidRs) { this.advPaidRs = advPaidRs; }

    public String getInvNo() { return invNo; }
    public void setInvNo(String invNo) { this.invNo = invNo; }

    public LocalDate getInvDate() { return invDate; }
    public void setInvDate(LocalDate invDate) { this.invDate = invDate; }

    public String getInsuranceCompany() { return insuranceCompany; }
    public void setInsuranceCompany(String insuranceCompany) { this.insuranceCompany = insuranceCompany; }

    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }

    public LocalDate getPolicyDate() { return policyDate; }
    public void setPolicyDate(LocalDate policyDate) { this.policyDate = policyDate; }

    public String getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(String insuranceAmount) { this.insuranceAmount = insuranceAmount; }

    public LocalDate getInsuranceDate() { return insuranceDate; }
    public void setInsuranceDate(LocalDate insuranceDate) { this.insuranceDate = insuranceDate; }

    public String getRiskType() { return riskType; }
    public void setRiskType(String riskType) { this.riskType = riskType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
}
