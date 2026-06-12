package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.LorryReceipt;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class LorryReceiptDAO {

    private static final Logger log = get(LorryReceiptDAO.class);
    private static Boolean freightWatermarkColumnExists = null;

    private boolean checkFreightWatermarkColumn() throws Exception {
        if (freightWatermarkColumnExists != null) {
            return freightWatermarkColumnExists;
        }
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM lorry_receipts LIKE 'freight_watermark'");
            freightWatermarkColumnExists = rs.next();
            log.info("freight_watermark column exists: {}", freightWatermarkColumnExists);
            return freightWatermarkColumnExists;
        }
    }

    public void ensureTable() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lorry_receipts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    lr_no VARCHAR(50) NOT NULL UNIQUE,
                    lr_date DATE NOT NULL,
                    vehicle_no VARCHAR(50),
                    from_location VARCHAR(255),
                    to_location VARCHAR(255),
                    e_way_bill_no VARCHAR(100),
                    consignor_name VARCHAR(255),
                    consignor_gstin VARCHAR(20),
                    consignee_name VARCHAR(255),
                    consignee_gstin VARCHAR(20),
                    no_of_packages VARCHAR(50),
                    method_of_packing VARCHAR(100),
                    description TEXT,
                    weight_actual VARCHAR(50),
                    weight_charged VARCHAR(50),
                    rate VARCHAR(50),
                    freight_to_pay DECIMAL(15,2) DEFAULT 0,
                    freight_paid DECIMAL(15,2) DEFAULT 0,
                    freight DECIMAL(15,2) DEFAULT 0,
                    freight_watermark VARCHAR(255),
                    advance DECIMAL(15,2) DEFAULT 0,
                    balance DECIMAL(15,2) DEFAULT 0,
                    aoc DECIMAL(15,2) DEFAULT 0,
                    st_charge DECIMAL(15,2) DEFAULT 0,
                    total DECIMAL(15,2) DEFAULT 0,
                    st_no VARCHAR(50),
                    sh_no VARCHAR(50),
                    gross_weight VARCHAR(50),
                    tare_weight VARCHAR(50),
                    net_weight VARCHAR(50),
                    value_rs VARCHAR(50),
                    to_pay_rs DECIMAL(15,2) DEFAULT 0,
                    adv_paid_rs DECIMAL(15,2) DEFAULT 0,
                    inv_no VARCHAR(50),
                    inv_date DATE,
                    insurance_company VARCHAR(255),
                    policy_no VARCHAR(100),
                    policy_date DATE,
                    insurance_amount VARCHAR(50),
                    insurance_date DATE,
                    risk_type VARCHAR(50),
                    remarks TEXT,
                    status VARCHAR(20) DEFAULT 'SAVED',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);
            // Add freight_watermark column if it doesn't exist (for existing databases)
            try {
                ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM lorry_receipts LIKE 'freight_watermark'");
                if (!rs.next()) {
                    stmt.executeUpdate("ALTER TABLE lorry_receipts ADD COLUMN freight_watermark VARCHAR(255) AFTER freight");
                    log.info("freight_watermark column added");
                } else {
                    log.info("freight_watermark column already exists");
                }
            } catch (SQLException e) {
                log.warn("Could not check/add freight_watermark column: {}", e.getMessage());
            }
            // Add remarks column if it doesn't exist (for existing databases)
            try {
                stmt.executeUpdate("ALTER TABLE lorry_receipts ADD COLUMN remarks TEXT AFTER risk_type");
            } catch (SQLException e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.warn("Could not add remarks column: {}", e.getMessage());
                }
            }

            // Reset the cache after ensuring table and adding columns
            freightWatermarkColumnExists = null;
        }
    }

    public void save(LorryReceipt lr) throws Exception {
        ensureTable();
        boolean hasFreightWatermark = checkFreightWatermarkColumn();
        String sql;
        if (hasFreightWatermark) {
            sql = """
                    INSERT INTO lorry_receipts (lr_no, lr_date, vehicle_no, from_location, to_location,
                    e_way_bill_no, consignor_name, consignor_gstin, consignee_name, consignee_gstin,
                    no_of_packages, method_of_packing, description, weight_actual, weight_charged, rate,
                    freight_to_pay, freight_paid, freight, freight_watermark, advance, balance, aoc, st_charge, total,
                    st_no, sh_no, gross_weight, tare_weight, net_weight, value_rs, to_pay_rs, adv_paid_rs,
                    inv_no, inv_date, insurance_company, policy_no, policy_date, insurance_amount,
                    insurance_date, risk_type, remarks, status)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """;
        } else {
            sql = """
                    INSERT INTO lorry_receipts (lr_no, lr_date, vehicle_no, from_location, to_location,
                    e_way_bill_no, consignor_name, consignor_gstin, consignee_name, consignee_gstin,
                    no_of_packages, method_of_packing, description, weight_actual, weight_charged, rate,
                    freight_to_pay, freight_paid, freight, advance, balance, aoc, st_charge, total,
                    st_no, sh_no, gross_weight, tare_weight, net_weight, value_rs, to_pay_rs, adv_paid_rs,
                    inv_no, inv_date, insurance_company, policy_no, policy_date, insurance_amount,
                    insurance_date, risk_type, remarks, status)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setAllParams(p, lr, hasFreightWatermark);
            p.executeUpdate();
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) lr.setId(rs.getInt(1));
            }
            log.info("Lorry receipt saved: {}", lr.getLrNo());
        }
    }

    public void update(LorryReceipt lr) throws Exception {
        ensureTable();
        boolean hasFreightWatermark = checkFreightWatermarkColumn();
        String sql;
        if (hasFreightWatermark) {
            sql = """
                    UPDATE lorry_receipts SET lr_no=?, lr_date=?, vehicle_no=?, from_location=?, to_location=?,
                    e_way_bill_no=?, consignor_name=?, consignor_gstin=?, consignee_name=?, consignee_gstin=?,
                    no_of_packages=?, method_of_packing=?, description=?, weight_actual=?, weight_charged=?, rate=?,
                    freight_to_pay=?, freight_paid=?, freight=?, freight_watermark=?, advance=?, balance=?, aoc=?, st_charge=?, total=?,
                    st_no=?, sh_no=?, gross_weight=?, tare_weight=?, net_weight=?, value_rs=?, to_pay_rs=?, adv_paid_rs=?,
                    inv_no=?, inv_date=?, insurance_company=?, policy_no=?, policy_date=?, insurance_amount=?,
                    insurance_date=?, risk_type=?, remarks=?, status=?, updated_at=NOW()
                    WHERE id=?
                    """;
        } else {
            sql = """
                    UPDATE lorry_receipts SET lr_no=?, lr_date=?, vehicle_no=?, from_location=?, to_location=?,
                    e_way_bill_no=?, consignor_name=?, consignor_gstin=?, consignee_name=?, consignee_gstin=?,
                    no_of_packages=?, method_of_packing=?, description=?, weight_actual=?, weight_charged=?, rate=?,
                    freight_to_pay=?, freight_paid=?, freight=?, advance=?, balance=?, aoc=?, st_charge=?, total=?,
                    st_no=?, sh_no=?, gross_weight=?, tare_weight=?, net_weight=?, value_rs=?, to_pay_rs=?, adv_paid_rs=?,
                    inv_no=?, inv_date=?, insurance_company=?, policy_no=?, policy_date=?, insurance_amount=?,
                    insurance_date=?, risk_type=?, remarks=?, status=?, updated_at=NOW()
                    WHERE id=?
                    """;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            setAllParams(p, lr, hasFreightWatermark);
            p.setInt(hasFreightWatermark ? 44 : 43, lr.getId());
            p.executeUpdate();
            log.info("Lorry receipt updated: {}", lr.getLrNo());
        }
    }

    private void setAllParams(PreparedStatement p, LorryReceipt lr, boolean hasFreightWatermark) throws SQLException {
        p.setString(1, lr.getLrNo());
        p.setDate(2, java.sql.Date.valueOf(lr.getLrDate()));
        p.setString(3, lr.getVehicleNo());
        p.setString(4, lr.getFromLocation());
        p.setString(5, lr.getToLocation());
        p.setString(6, lr.getEWayBillNo());
        p.setString(7, lr.getConsignorName());
        p.setString(8, lr.getConsignorGstin());
        p.setString(9, lr.getConsigneeName());
        p.setString(10, lr.getConsigneeGstin());
        p.setString(11, lr.getNoOfPackages());
        p.setString(12, lr.getMethodOfPacking());
        p.setString(13, lr.getDescription());
        p.setString(14, lr.getWeightActual());
        p.setString(15, lr.getWeightCharged());
        p.setString(16, lr.getRate());
        p.setDouble(17, lr.getFreightToPay());
        p.setDouble(18, lr.getFreightPaid());
        p.setDouble(19, lr.getFreight());
        if (hasFreightWatermark) {
            p.setString(20, lr.getFreightWatermark());
        }
        p.setDouble(hasFreightWatermark ? 21 : 20, lr.getAdvance());
        p.setDouble(hasFreightWatermark ? 22 : 21, lr.getBalance());
        p.setDouble(hasFreightWatermark ? 23 : 22, lr.getAoc());
        p.setDouble(hasFreightWatermark ? 24 : 23, lr.getStCharge());
        p.setDouble(hasFreightWatermark ? 25 : 24, lr.getTotal());
        p.setString(hasFreightWatermark ? 26 : 25, lr.getStNo());
        p.setString(hasFreightWatermark ? 27 : 26, lr.getShNo());
        p.setString(hasFreightWatermark ? 28 : 27, lr.getGrossWeight());
        p.setString(hasFreightWatermark ? 29 : 28, lr.getTareWeight());
        p.setString(hasFreightWatermark ? 30 : 29, lr.getNetWeight());
        p.setString(hasFreightWatermark ? 31 : 30, lr.getValueRs());
        p.setDouble(hasFreightWatermark ? 32 : 31, lr.getToPayRs());
        p.setDouble(hasFreightWatermark ? 33 : 32, lr.getAdvPaidRs());
        p.setString(hasFreightWatermark ? 34 : 33, lr.getInvNo());
        p.setDate(hasFreightWatermark ? 35 : 34, lr.getInvDate() != null ? java.sql.Date.valueOf(lr.getInvDate()) : null);
        p.setString(hasFreightWatermark ? 36 : 35, lr.getInsuranceCompany());
        p.setString(hasFreightWatermark ? 37 : 36, lr.getPolicyNo());
        p.setDate(hasFreightWatermark ? 38 : 37, lr.getPolicyDate() != null ? java.sql.Date.valueOf(lr.getPolicyDate()) : null);
        p.setString(hasFreightWatermark ? 39 : 38, lr.getInsuranceAmount());
        p.setDate(hasFreightWatermark ? 40 : 39, lr.getInsuranceDate() != null ? java.sql.Date.valueOf(lr.getInsuranceDate()) : null);
        p.setString(hasFreightWatermark ? 41 : 40, lr.getRiskType());
        p.setString(hasFreightWatermark ? 42 : 41, lr.getRemarks());
        p.setString(hasFreightWatermark ? 43 : 42, lr.getStatus());
    }

    public void delete(int id) throws Exception {
        ensureTable();
        String sql = "DELETE FROM lorry_receipts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
            log.info("Lorry receipt deleted: {}", id);
        }
    }

    public LorryReceipt findById(int id) throws Exception {
        ensureTable();
        String sql = "SELECT * FROM lorry_receipts WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return mapLR(rs);
            }
        }
        return null;
    }

    public List<LorryReceipt> getAll() throws Exception {
        ensureTable();
        String sql = "SELECT * FROM lorry_receipts ORDER BY id DESC";
        List<LorryReceipt> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapLR(rs));
        }
        return list;
    }

    public List<LorryReceipt> searchAllColumns(String keyword) throws Exception {
        ensureTable();
        String sql = """
                SELECT * FROM lorry_receipts
                WHERE CONCAT_WS(' ',
                    IFNULL(CAST(id AS CHAR),''), IFNULL(lr_no,''), IFNULL(CAST(lr_date AS CHAR),''),
                    IFNULL(vehicle_no,''), IFNULL(from_location,''), IFNULL(to_location,''),
                    IFNULL(e_way_bill_no,''), IFNULL(consignor_name,''), IFNULL(consignor_gstin,''),
                    IFNULL(consignee_name,''), IFNULL(consignee_gstin,''), IFNULL(no_of_packages,''),
                    IFNULL(description,''), IFNULL(CAST(freight AS CHAR),''), IFNULL(CAST(total AS CHAR),''),
                    IFNULL(inv_no,''), IFNULL(remarks,''), IFNULL(status,'')
                ) LIKE ?
                ORDER BY id DESC
                """;
        List<LorryReceipt> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, "%" + keyword + "%");
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) list.add(mapLR(rs));
            }
        }
        return list;
    }

    public String getLastLrNumber() throws Exception {
        ensureTable();
        String sql = "SELECT lr_no FROM lorry_receipts ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("lr_no");
        }
        return null;
    }

    private LorryReceipt mapLR(ResultSet rs) throws SQLException {
        LorryReceipt lr = new LorryReceipt();
        lr.setId(rs.getInt("id"));
        lr.setLrNo(rs.getString("lr_no"));
        lr.setLrDate(rs.getDate("lr_date").toLocalDate());
        lr.setVehicleNo(rs.getString("vehicle_no"));
        lr.setFromLocation(rs.getString("from_location"));
        lr.setToLocation(rs.getString("to_location"));
        lr.setEWayBillNo(rs.getString("e_way_bill_no"));
        lr.setConsignorName(rs.getString("consignor_name"));
        lr.setConsignorGstin(rs.getString("consignor_gstin"));
        lr.setConsigneeName(rs.getString("consignee_name"));
        lr.setConsigneeGstin(rs.getString("consignee_gstin"));
        lr.setNoOfPackages(rs.getString("no_of_packages"));
        lr.setMethodOfPacking(rs.getString("method_of_packing"));
        lr.setDescription(rs.getString("description"));
        lr.setWeightActual(rs.getString("weight_actual"));
        lr.setWeightCharged(rs.getString("weight_charged"));
        lr.setRate(rs.getString("rate"));
        lr.setFreightToPay(rs.getDouble("freight_to_pay"));
        lr.setFreightPaid(rs.getDouble("freight_paid"));
        lr.setFreight(rs.getDouble("freight"));
        try {
            lr.setFreightWatermark(rs.getString("freight_watermark"));
        } catch (SQLException e) {
            // Column might not exist in older databases
            lr.setFreightWatermark(null);
        }
        lr.setAdvance(rs.getDouble("advance"));
        lr.setBalance(rs.getDouble("balance"));
        lr.setAoc(rs.getDouble("aoc"));
        lr.setStCharge(rs.getDouble("st_charge"));
        lr.setTotal(rs.getDouble("total"));
        lr.setStNo(rs.getString("st_no"));
        lr.setShNo(rs.getString("sh_no"));
        lr.setGrossWeight(rs.getString("gross_weight"));
        lr.setTareWeight(rs.getString("tare_weight"));
        lr.setNetWeight(rs.getString("net_weight"));
        lr.setValueRs(rs.getString("value_rs"));
        lr.setToPayRs(rs.getDouble("to_pay_rs"));
        lr.setAdvPaidRs(rs.getDouble("adv_paid_rs"));
        lr.setInvNo(rs.getString("inv_no"));
        lr.setInvDate(rs.getDate("inv_date") != null ? rs.getDate("inv_date").toLocalDate() : null);
        lr.setInsuranceCompany(rs.getString("insurance_company"));
        lr.setPolicyNo(rs.getString("policy_no"));
        lr.setPolicyDate(rs.getDate("policy_date") != null ? rs.getDate("policy_date").toLocalDate() : null);
        lr.setInsuranceAmount(rs.getString("insurance_amount"));
        lr.setInsuranceDate(rs.getDate("insurance_date") != null ? rs.getDate("insurance_date").toLocalDate() : null);
        lr.setRiskType(rs.getString("risk_type"));
        lr.setRemarks(rs.getString("remarks"));
        lr.setStatus(rs.getString("status"));
        lr.setCreatedAt(rs.getDate("created_at").toLocalDate());
        lr.setUpdatedAt(rs.getDate("updated_at").toLocalDate());
        return lr;
    }
}
