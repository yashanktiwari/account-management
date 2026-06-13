package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Party;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.accounting.util.AppLogger.get;

public class PartyDAO {

    private static final Logger log = get(PartyDAO.class);

    private void ensurePartyColumns(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<String> existing = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "parties", null)) {
            while (rs.next()) {
                existing.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }

        String[] requiredColumns = {
                "owner_name VARCHAR(255)",
                "cst_no VARCHAR(30)",
                "tan_no VARCHAR(30)",
                "tds VARCHAR(20)",
                "aadhar_no VARCHAR(20)",
                "routes TEXT"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String definition : requiredColumns) {
                String columnName = definition.substring(0, definition.indexOf(' ')).toLowerCase();
                if (!existing.contains(columnName)) {
                    stmt.executeUpdate("ALTER TABLE parties ADD COLUMN " + definition);
                }
            }
        }
    }

    public void save(Party party) throws Exception {
        String sql = """
                INSERT INTO parties (name, type, mailing_name, address, city, state, pincode, mobile, email,
                pan, gstin, credit_limit, opening_balance, balance_type, nature_of_payment, bank_name,
                bank_account, ifsc_code, remarks, owner_name, cst_no, tan_no, tds, aadhar_no, routes,
                created_at, updated_at)
                VALUES (?, 'CUSTOMER', '', ?, ?, ?, ?, ?, ?, ?, ?, '', '', '', '', '', '', '', '', ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ensurePartyColumns(conn);
            pstmt.setString(1, party.getName());
            pstmt.setString(2, party.getAddress());
            pstmt.setString(3, party.getCity());
            pstmt.setString(4, party.getState());
            pstmt.setString(5, party.getPincode());
            pstmt.setString(6, party.getMobile());
            pstmt.setString(7, party.getEmail());
            pstmt.setString(8, party.getPan());
            pstmt.setString(9, party.getGstin());
            pstmt.setString(10, "");
            pstmt.setString(11, party.getOwnerName());
            pstmt.setString(12, party.getCstNo());
            pstmt.setString(13, party.getTanNo());
            pstmt.setString(14, party.getTds());
            pstmt.setString(15, party.getAadharNo());
            pstmt.setString(16, party.getRoutes());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) party.setId(rs.getInt(1));
            }
            log.info("Party saved: {}", party.getName());
        }
    }

    public void update(Party party) throws Exception {
        String sql = """
                UPDATE parties SET name=?, type='CUSTOMER', mailing_name='', address=?, city=?, state=?, pincode=?,
                mobile=?, email=?, pan=?, gstin=?, credit_limit='', opening_balance='', balance_type='',
                nature_of_payment='', bank_name='', bank_account='', ifsc_code='', remarks='',
                owner_name=?, cst_no=?, tan_no=?, tds=?, aadhar_no=?, routes=?, updated_at=NOW()
                WHERE id=?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    ensurePartyColumns(conn);
            pstmt.setString(1, party.getName());
            pstmt.setString(2, party.getAddress());
            pstmt.setString(3, party.getCity());
            pstmt.setString(4, party.getState());
            pstmt.setString(5, party.getPincode());
            pstmt.setString(6, party.getMobile());
            pstmt.setString(7, party.getEmail());
            pstmt.setString(8, party.getPan());
            pstmt.setString(9, party.getGstin());
            pstmt.setString(10, "");
            pstmt.setString(11, party.getOwnerName());
            pstmt.setString(12, party.getCstNo());
            pstmt.setString(13, party.getTanNo());
            pstmt.setString(14, party.getTds());
            pstmt.setString(15, party.getAadharNo());
            pstmt.setString(16, party.getRoutes());
            pstmt.setInt(17, party.getId());

            pstmt.executeUpdate();
            log.info("Party updated: {}", party.getName());
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM parties WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            log.info("Party deleted: {}", id);
        }
    }

    public Party findById(int id) throws Exception {
        String sql = "SELECT * FROM parties WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapParty(rs);
            }
        }
        return null;
    }

    public Party findByName(String name) throws Exception {
        String sql = "SELECT * FROM parties WHERE name=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapParty(rs);
            }
        }
        return null;
    }

    public List<Party> findByType(String type) throws Exception {
        String sql = "SELECT * FROM parties WHERE type=? ORDER BY name";
        List<Party> parties = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) parties.add(mapParty(rs));
            }
        }
        return parties;
    }

    public List<Party> getAll() throws Exception {
        String sql = "SELECT * FROM parties ORDER BY name";
        List<Party> parties = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) parties.add(mapParty(rs));
        }
        return parties;
    }

    public List<Party> search(String keyword) throws Exception {
        String sql = "SELECT * FROM parties WHERE name LIKE ? OR mobile LIKE ? OR email LIKE ? ORDER BY name";
        List<Party> parties = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) parties.add(mapParty(rs));
            }
        }
        return parties;
    }

    public List<Party> searchAllColumns(String keyword) throws Exception {
        String sql = """
                SELECT *
                FROM parties
                WHERE CONCAT_WS(' ',
                    IFNULL(CAST(id AS CHAR), ''),
                    IFNULL(name, ''),
                    IFNULL(type, ''),
                    IFNULL(mailing_name, ''),
                    IFNULL(address, ''),
                    IFNULL(city, ''),
                    IFNULL(state, ''),
                    IFNULL(pincode, ''),
                    IFNULL(mobile, ''),
                    IFNULL(email, ''),
                    IFNULL(pan, ''),
                    IFNULL(gstin, ''),
                    IFNULL(credit_limit, ''),
                    IFNULL(opening_balance, ''),
                    IFNULL(balance_type, ''),
                    IFNULL(nature_of_payment, ''),
                    IFNULL(bank_name, ''),
                    IFNULL(bank_account, ''),
                    IFNULL(ifsc_code, ''),
                    IFNULL(remarks, ''),
                    IFNULL(owner_name, ''),
                    IFNULL(cst_no, ''),
                    IFNULL(tan_no, ''),
                    IFNULL(tds, ''),
                    IFNULL(aadhar_no, ''),
                    IFNULL(routes, ''),
                    IFNULL(CAST(created_at AS CHAR), ''),
                    IFNULL(CAST(updated_at AS CHAR), '')
                ) LIKE ?
                ORDER BY name
                """;
        List<Party> parties = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ensurePartyColumns(conn);
            pstmt.setString(1, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) parties.add(mapParty(rs));
            }
        }
        return parties;
    }

    /** Safely read a column that may not exist in older DB schemas */
    private String safeGet(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private Party mapParty(ResultSet rs) throws SQLException {
        Party p = new Party();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setType(rs.getString("type"));
        p.setMailingName(rs.getString("mailing_name"));
        p.setAddress(rs.getString("address"));
        p.setCity(rs.getString("city"));
        p.setState(rs.getString("state"));
        p.setPincode(rs.getString("pincode"));
        p.setMobile(rs.getString("mobile"));
        p.setEmail(rs.getString("email"));
        p.setPan(rs.getString("pan"));
        p.setGstin(rs.getString("gstin"));
        p.setCreditLimit(rs.getString("credit_limit"));
        p.setOpeningBalance(rs.getString("opening_balance"));
        p.setBalanceType(rs.getString("balance_type"));
        p.setNatureOfPayment(rs.getString("nature_of_payment"));
        p.setBankName(rs.getString("bank_name"));
        p.setBankAccount(rs.getString("bank_account"));
        p.setIfscCode(rs.getString("ifsc_code"));
        p.setRemarks(rs.getString("remarks"));
        p.setOwnerName(safeGet(rs, "owner_name"));
        p.setCstNo(safeGet(rs, "cst_no"));
        p.setTanNo(safeGet(rs, "tan_no"));
        p.setTds(safeGet(rs, "tds"));
        p.setAadharNo(safeGet(rs, "aadhar_no"));
        p.setRoutes(safeGet(rs, "routes"));
        p.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        p.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return p;
    }
}
