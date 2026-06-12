package com.accounting.dao;

import com.accounting.database.DBConnection;
import com.accounting.util.AppLogger;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QueryBuilderDAO {

    private static final Logger log = AppLogger.get(QueryBuilderDAO.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Field type enum ─────────────────────────────────────────────────────
    public enum FieldType { TEXT, NUMBER, DATE, CURRENCY }

    // ── Field definition ────────────────────────────────────────────────────
    public static class FieldDef {
        private final String column, display;
        private final FieldType type;

        public FieldDef(String column, String display, FieldType type) {
            this.column = column; this.display = display; this.type = type;
        }

        public String getColumn() { return column; }
        public String getDisplay() { return display; }
        public FieldType getType() { return type; }
        @Override public String toString() { return display; }
    }

    // ── Data source definition ──────────────────────────────────────────────
    public static class DataSourceDef {
        private final String name, tableName;
        private final List<FieldDef> fields;

        public DataSourceDef(String name, String tableName, List<FieldDef> fields) {
            this.name = name; this.tableName = tableName; this.fields = fields;
        }

        public String getName() { return name; }
        public String getTableName() { return tableName; }
        public List<FieldDef> getFields() { return fields; }
        public List<FieldDef> getNumericFields() {
            return fields.stream().filter(f -> f.type == FieldType.NUMBER || f.type == FieldType.CURRENCY).toList();
        }
        @Override public String toString() { return name; }
    }

    // ── Filter definition ───────────────────────────────────────────────────
    public static class FilterDef {
        private String connector; // AND / OR
        private FieldDef field;
        private String operator;
        private String value;

        public FilterDef(String connector, FieldDef field, String operator, String value) {
            this.connector = connector; this.field = field; this.operator = operator; this.value = value;
        }

        public String getConnector() { return connector; }
        public FieldDef getField() { return field; }
        public String getOperator() { return operator; }
        public String getValue() { return value; }
    }

    // ── Operators per field type ─────────────────────────────────────────────
    public static List<String> getOperators(FieldType type) {
        return switch (type) {
            case TEXT -> List.of("Equals", "Not Equals", "Contains", "Starts With", "Ends With", "Is Empty", "Is Not Empty");
            case NUMBER, CURRENCY -> List.of("=", "≠", ">", "<", "≥", "≤");
            case DATE -> List.of("Equals", "Before", "After", "This Month", "Last Month", "This Year", "Last Year");
        };
    }

    public static boolean operatorNeedsValue(String operator) {
        return !Set.of("Is Empty", "Is Not Empty", "This Month", "Last Month", "This Year", "Last Year").contains(operator);
    }

    // ── Data sources ────────────────────────────────────────────────────────
    public static List<DataSourceDef> getDataSources() {
        List<DataSourceDef> sources = new ArrayList<>();

        sources.add(new DataSourceDef("Purchase Invoices", "purchase_invoices", List.of(
            new FieldDef("invoice_no", "Invoice No", FieldType.TEXT),
            new FieldDef("invoice_date", "Date", FieldType.DATE),
            new FieldDef("party_name", "Party Name", FieldType.TEXT),
            new FieldDef("gst", "GST %", FieldType.TEXT),
            new FieldDef("taxable_amount", "Taxable Amount", FieldType.CURRENCY),
            new FieldDef("sgst_amount", "SGST", FieldType.CURRENCY),
            new FieldDef("cgst_amount", "CGST", FieldType.CURRENCY),
            new FieldDef("igst_amount", "IGST", FieldType.CURRENCY),
            new FieldDef("total_gst", "Total GST", FieldType.CURRENCY),
            new FieldDef("net_amount", "Net Amount", FieldType.CURRENCY),
            new FieldDef("remarks", "Remarks", FieldType.TEXT),
            new FieldDef("status", "Status", FieldType.TEXT)
        )));

        sources.add(new DataSourceDef("Sale Invoices", "sale_invoices", List.of(
            new FieldDef("invoice_no", "Invoice No", FieldType.TEXT),
            new FieldDef("invoice_date", "Date", FieldType.DATE),
            new FieldDef("delivery_date", "Delivery Date", FieldType.DATE),
            new FieldDef("party_name", "Party Name", FieldType.TEXT),
            new FieldDef("gst", "GST %", FieldType.TEXT),
            new FieldDef("taxable_amount", "Taxable Amount", FieldType.CURRENCY),
            new FieldDef("sgst_amount", "SGST", FieldType.CURRENCY),
            new FieldDef("cgst_amount", "CGST", FieldType.CURRENCY),
            new FieldDef("igst_amount", "IGST", FieldType.CURRENCY),
            new FieldDef("total_gst", "Total GST", FieldType.CURRENCY),
            new FieldDef("net_amount", "Net Amount", FieldType.CURRENCY),
            new FieldDef("remarks", "Remarks", FieldType.TEXT),
            new FieldDef("status", "Status", FieldType.TEXT)
        )));

        sources.add(new DataSourceDef("Purchase Receipts", "purchase_receipts", List.of(
            new FieldDef("receipt_no", "Receipt No", FieldType.TEXT),
            new FieldDef("receipt_date", "Date", FieldType.DATE),
            new FieldDef("party_name", "Party Name", FieldType.TEXT),
            new FieldDef("amount", "Amount", FieldType.CURRENCY),
            new FieldDef("payment_mode", "Payment Mode", FieldType.TEXT),
            new FieldDef("bank_name", "Bank Name", FieldType.TEXT),
            new FieldDef("remarks", "Remarks", FieldType.TEXT),
            new FieldDef("status", "Status", FieldType.TEXT)
        )));

        sources.add(new DataSourceDef("Sale Receipts", "sale_receipts", List.of(
            new FieldDef("receipt_no", "Receipt No", FieldType.TEXT),
            new FieldDef("receipt_date", "Date", FieldType.DATE),
            new FieldDef("party_name", "Party Name", FieldType.TEXT),
            new FieldDef("amount", "Amount", FieldType.CURRENCY),
            new FieldDef("payment_mode", "Payment Mode", FieldType.TEXT),
            new FieldDef("bank_name", "Bank Name", FieldType.TEXT),
            new FieldDef("remarks", "Remarks", FieldType.TEXT),
            new FieldDef("status", "Status", FieldType.TEXT)
        )));

        sources.add(new DataSourceDef("Loading Slips", "loading_slips", List.of(
            new FieldDef("slip_no", "Slip No", FieldType.TEXT),
            new FieldDef("slip_date", "Date", FieldType.DATE),
            new FieldDef("party_name", "Party Name", FieldType.TEXT),
            new FieldDef("vehicle_no", "Vehicle No", FieldType.TEXT),
            new FieldDef("station", "Station", FieldType.TEXT),
            new FieldDef("to_location", "To", FieldType.TEXT),
            new FieldDef("weight", "Weight", FieldType.TEXT),
            new FieldDef("rate", "Rate", FieldType.TEXT),
            new FieldDef("freight_amount", "Freight", FieldType.CURRENCY),
            new FieldDef("advance_amount", "Advance", FieldType.CURRENCY),
            new FieldDef("balance_amount", "Balance", FieldType.CURRENCY),
            new FieldDef("status", "Status", FieldType.TEXT)
        )));

        sources.add(new DataSourceDef("Lorry Receipts", "lorry_receipts", List.of(
            new FieldDef("lr_no", "LR No", FieldType.TEXT),
            new FieldDef("lr_date", "Date", FieldType.DATE),
            new FieldDef("vehicle_no", "Vehicle No", FieldType.TEXT),
            new FieldDef("from_location", "From", FieldType.TEXT),
            new FieldDef("to_location", "To", FieldType.TEXT),
            new FieldDef("consignor_name", "Consignor", FieldType.TEXT),
            new FieldDef("consignee_name", "Consignee", FieldType.TEXT),
            new FieldDef("freight", "Freight", FieldType.CURRENCY),
            new FieldDef("advance", "Advance", FieldType.CURRENCY),
            new FieldDef("balance", "Balance", FieldType.CURRENCY),
            new FieldDef("total", "Total", FieldType.CURRENCY),
            new FieldDef("status", "Status", FieldType.TEXT)
        )));

        sources.add(new DataSourceDef("Parties", "parties", List.of(
            new FieldDef("name", "Name", FieldType.TEXT),
            new FieldDef("type", "Type", FieldType.TEXT),
            new FieldDef("city", "City", FieldType.TEXT),
            new FieldDef("state", "State", FieldType.TEXT),
            new FieldDef("mobile", "Mobile", FieldType.TEXT),
            new FieldDef("email", "Email", FieldType.TEXT),
            new FieldDef("gstin", "GSTIN", FieldType.TEXT),
            new FieldDef("opening_balance", "Opening Balance", FieldType.TEXT),
            new FieldDef("balance_type", "Balance Type", FieldType.TEXT),
            new FieldDef("owner_name", "Owner Name", FieldType.TEXT),
            new FieldDef("routes", "Routes", FieldType.TEXT)
        )));

        return sources;
    }

    // ── Execute detail query (flat rows) ────────────────────────────────────
    public List<Map<String, Object>> executeDetailQuery(DataSourceDef source, List<FilterDef> filters) throws Exception {
        List<Object> params = new ArrayList<>();
        String where = buildWhereClause(filters, params);

        StringBuilder sql = new StringBuilder("SELECT ");
        StringJoiner cols = new StringJoiner(", ");
        for (FieldDef f : source.getFields()) cols.add(f.getColumn());
        sql.append(cols).append(" FROM ").append(source.getTableName());
        if (!where.isEmpty()) sql.append(" WHERE ").append(where);
        sql.append(" ORDER BY 1 DESC LIMIT 5000");

        log.info("Detail query: {}", sql);
        return executeSQL(sql.toString(), params, source.getFields());
    }

    // ── Execute summary query (grouped + aggregated) ────────────────────────
    public List<Map<String, Object>> executeSummaryQuery(DataSourceDef source, List<FilterDef> filters,
                                                          FieldDef groupByField, String aggFunction,
                                                          List<FieldDef> aggFields) throws Exception {
        List<Object> params = new ArrayList<>();
        String where = buildWhereClause(filters, params);

        StringBuilder sql = new StringBuilder("SELECT ");
        List<FieldDef> resultFields = new ArrayList<>();

        if (groupByField != null) {
            sql.append(groupByField.getColumn()).append(", ");
            resultFields.add(groupByField);
        }

        StringJoiner aggCols = new StringJoiner(", ");
        for (FieldDef f : aggFields) {
            String alias = aggFunction + "(" + f.getDisplay() + ")";
            aggCols.add(aggFunction + "(" + f.getColumn() + ") AS `" + alias + "`");
            resultFields.add(new FieldDef(alias, alias, FieldType.CURRENCY));
        }
        // Also add COUNT(*)
        resultFields.add(new FieldDef("COUNT(*)", "Count", FieldType.NUMBER));
        sql.append(aggCols).append(", COUNT(*) AS `Count`");

        sql.append(" FROM ").append(source.getTableName());
        if (!where.isEmpty()) sql.append(" WHERE ").append(where);

        if (groupByField != null) {
            sql.append(" GROUP BY ").append(groupByField.getColumn());
            // Order by first aggregate DESC
            if (!aggFields.isEmpty()) {
                sql.append(" ORDER BY ").append(aggFunction).append("(").append(aggFields.get(0).getColumn()).append(") DESC");
            }
        }
        sql.append(" LIMIT 5000");

        log.info("Summary query: {}", sql);
        return executeSQL(sql.toString(), params, resultFields);
    }

    // ── Build WHERE clause ──────────────────────────────────────────────────
    private String buildWhereClause(List<FilterDef> filters, List<Object> params) {
        if (filters == null || filters.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            FilterDef f = filters.get(i);
            if (f.getField() == null || f.getOperator() == null) continue;

            if (sb.length() > 0) sb.append(" ").append(f.getConnector()).append(" ");

            String col = f.getField().getColumn();
            String op = f.getOperator();
            String val = f.getValue();

            switch (op) {
                // Text operators
                case "Equals" -> { sb.append(col).append(" = ?"); params.add(val); }
                case "Not Equals" -> { sb.append(col).append(" != ?"); params.add(val); }
                case "Contains" -> { sb.append(col).append(" LIKE ?"); params.add("%" + val + "%"); }
                case "Starts With" -> { sb.append(col).append(" LIKE ?"); params.add(val + "%"); }
                case "Ends With" -> { sb.append(col).append(" LIKE ?"); params.add("%" + val); }
                case "Is Empty" -> sb.append("(").append(col).append(" IS NULL OR ").append(col).append(" = '')");
                case "Is Not Empty" -> sb.append("(").append(col).append(" IS NOT NULL AND ").append(col).append(" != '')");

                // Number operators
                case "=" -> { sb.append(col).append(" = ?"); params.add(Double.parseDouble(val)); }
                case "≠" -> { sb.append(col).append(" != ?"); params.add(Double.parseDouble(val)); }
                case ">" -> { sb.append(col).append(" > ?"); params.add(Double.parseDouble(val)); }
                case "<" -> { sb.append(col).append(" < ?"); params.add(Double.parseDouble(val)); }
                case "≥" -> { sb.append(col).append(" >= ?"); params.add(Double.parseDouble(val)); }
                case "≤" -> { sb.append(col).append(" <= ?"); params.add(Double.parseDouble(val)); }

                // Date operators
                case "Before" -> { sb.append(col).append(" < ?"); params.add(Date.valueOf(LocalDate.parse(val, DATE_FMT))); }
                case "After" -> { sb.append(col).append(" > ?"); params.add(Date.valueOf(LocalDate.parse(val, DATE_FMT))); }
                case "This Month" -> sb.append("YEAR(").append(col).append(") = YEAR(CURDATE()) AND MONTH(").append(col).append(") = MONTH(CURDATE())");
                case "Last Month" -> sb.append(col).append(" >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 1 MONTH) AND ").append(col).append(" < DATE_FORMAT(CURDATE(),'%Y-%m-01')");
                case "This Year" -> sb.append("YEAR(").append(col).append(") = YEAR(CURDATE())");
                case "Last Year" -> sb.append("YEAR(").append(col).append(") = YEAR(CURDATE()) - 1");

                default -> { sb.append(col).append(" = ?"); params.add(val); }
            }
        }
        return sb.toString();
    }

    // ── Execute SQL and return results ──────────────────────────────────────
    private List<Map<String, Object>> executeSQL(String sql, List<Object> params, List<FieldDef> fields) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Double) pstmt.setDouble(i + 1, (Double) p);
                else if (p instanceof Date) pstmt.setDate(i + 1, (Date) p);
                else pstmt.setString(i + 1, p.toString());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colLabel = meta.getColumnLabel(i);
                        row.put(colLabel, rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        log.info("Query returned {} rows", results.size());
        return results;
    }
}
