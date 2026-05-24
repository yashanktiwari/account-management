package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.model.Vehicle;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    private static final Logger log = AppLogger.get(VehicleDAO.class);

    public int save(Vehicle v) {
        String sql = "INSERT INTO vehicles (vehicle_no, vehicle_model, account_name) VALUES (?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getVehicleNo());
            ps.setString(2, v.getVehicleModel());
            ps.setString(3, v.getAccountName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("Failed to save vehicle: {}", v.getVehicleNo(), e);
        }
        return -1;
    }

    public boolean update(Vehicle v) {
        String sql = "UPDATE vehicles SET vehicle_no=?, vehicle_model=?, account_name=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getVehicleNo());
            ps.setString(2, v.getVehicleModel());
            ps.setString(3, v.getAccountName());
            ps.setInt(4, v.getId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to update vehicle: {}", v.getVehicleNo(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM vehicles WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Failed to delete vehicle id={}", id, e);
        }
        return false;
    }

    public List<Vehicle> getAll() {
        String sql = "SELECT * FROM vehicles ORDER BY vehicle_no";
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapVehicle(rs));
        } catch (Exception e) {
            log.error("Failed to fetch all vehicles", e);
        }
        return list;
    }

    public List<Vehicle> search(String keyword) {
        String sql = "SELECT * FROM vehicles WHERE vehicle_no LIKE ? OR vehicle_model LIKE ? OR account_name LIKE ? ORDER BY vehicle_no";
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = "%" + keyword + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapVehicle(rs));
            }
        } catch (Exception e) {
            log.error("Failed to search vehicles: {}", keyword, e);
        }
        return list;
    }

    public Vehicle findByVehicleNo(String vehicleNo) {
        String sql = "SELECT * FROM vehicles WHERE vehicle_no=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapVehicle(rs);
            }
        } catch (Exception e) {
            log.error("Failed to find vehicle: {}", vehicleNo, e);
        }
        return null;
    }

    private Vehicle mapVehicle(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setVehicleNo(rs.getString("vehicle_no"));
        v.setVehicleModel(rs.getString("vehicle_model"));
        v.setAccountName(rs.getString("account_name"));
        v.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        v.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return v;
    }
}
