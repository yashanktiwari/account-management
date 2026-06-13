package com.accounting.ui.dialog;

import com.accounting.dao.ReportDAO;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final ReportDAO reportDAO = new ReportDAO();
    private final ObservableList<ReportDAO.ReportRow> allTransactions = FXCollections.observableArrayList();
    private FilteredList<ReportDAO.ReportRow> filteredTransactions;

    private TableView<ReportDAO.ReportRow> resultTable;
    private DatePicker fromDate;
    private DatePicker toDate;
    private TextField searchField;
    private Label resultCountLabel;

    public Parent createContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("All Transactions Report");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        VBox filterSection = buildFilterSection();

        VBox tableSection = buildTableSection();

        root.getChildren().addAll(title, filterSection, tableSection);

        // Auto-generate report on load
        generateReport();

        return root;
    }

    private VBox buildFilterSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        Label sectionTitle = new Label("Filters & Search");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));


        Label fromDateLabel = new Label("From Date:");
        fromDateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        fromDate = new DatePicker();
        fromDate.setPrefWidth(150);
        fromDate.setValue(LocalDate.now().minusDays(30));

        Label toDateLabel = new Label("To Date:");
        toDateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        toDate = new DatePicker();
        toDate.setPrefWidth(150);
        toDate.setValue(LocalDate.now());

        // Party Filter
        Label searchLabel = new Label("Search:");
        searchLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        searchField = new TextField();
        searchField.setPromptText("Search by Type, Transaction No, Party, Date, or Amount");
        searchField.setPrefWidth(400);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Add to grid
        grid.add(fromDateLabel, 0, 0);
        grid.add(fromDate, 1, 0);
        grid.add(toDateLabel, 2, 0);
        grid.add(toDate, 3, 0);

        grid.add(searchLabel, 0, 1);
        grid.add(searchField, 1, 1, 3, 1);
        
        Button generateBtn = new Button("Generate Report");
        generateBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        generateBtn.setOnAction(e -> generateReport());
        
        Button exportBtn = new Button("Export to CSV");
        exportBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        exportBtn.setOnAction(e -> exportReport());

        Button clearBtn = new Button("Clear Search");
        clearBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applyFilters();
        });

        HBox buttonBox = new HBox(10, generateBtn, exportBtn, clearBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(buttonBox, 0, 2, 4, 1);

        resultCountLabel = new Label("No results");
        resultCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        grid.add(resultCountLabel, 0, 3, 4, 1);
        
        return section;
    }

    private VBox buildTableSection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(section, Priority.ALWAYS);

        Label sectionTitle = new Label("All Transactions");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        resultTable = new TableView<>();
        resultTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        filteredTransactions = new FilteredList<>(allTransactions);
        resultTable.setItems(filteredTransactions);

        setupTableColumns("All Transactions");

        // Wrap table in ScrollPane for proper scrolling
        ScrollPane scrollPane = new ScrollPane(resultTable);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        section.getChildren().addAll(sectionTitle, scrollPane);
        return section;
    }

    private void setupTableColumns(String reportType) {
        resultTable.getColumns().clear();

        // Serial No
        TableColumn<ReportDAO.ReportRow, Integer> serialCol = new TableColumn<>("Sr. No");
        serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNo"));
        serialCol.setPrefWidth(60);
        serialCol.setStyle("-fx-alignment: CENTER;");

        // Transaction Type
        TableColumn<ReportDAO.ReportRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
        typeCol.setPrefWidth(150);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String formatted = formatTransactionType(item);
                    setText(formatted);
                }
            }
        });

        // Transaction No
        TableColumn<ReportDAO.ReportRow, String> noCol = new TableColumn<>("Transaction No");
        noCol.setCellValueFactory(new PropertyValueFactory<>("transactionNo"));
        noCol.setPrefWidth(120);

        // Date
        TableColumn<ReportDAO.ReportRow, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);

        // Party Name
        TableColumn<ReportDAO.ReportRow, String> partyCol = new TableColumn<>("Party");
        partyCol.setCellValueFactory(new PropertyValueFactory<>("party"));
        partyCol.setPrefWidth(200);

        // Amount
        TableColumn<ReportDAO.ReportRow, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> formatCurrencyCell());
        amountCol.setPrefWidth(120);

        resultTable.getColumns().addAll(
            serialCol, typeCol, noCol, dateCol, partyCol, amountCol
        );
    }

    private TableCell<ReportDAO.ReportRow, Double> formatCurrencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private String formatTransactionType(String type) {
        if (type == null) return "";
        switch (type) {
            case "PURCHASE_INVOICE": return "Purchase Invoice";
            case "SALE_INVOICE": return "Sale Invoice";
            case "PURCHASE_RECEIPT": return "Purchase Receipt";
            case "SALE_RECEIPT": return "Sale Receipt";
            case "LOADING_SLIP": return "Loading Slip";
            case "LORRY_RECEIPT": return "Lorry Receipt";
            default: return type;
        }
    }

    private void generateReport() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (from == null || to == null) {
            AlertUtil.showWarning("Validation", "Please select both from and to dates");
            return;
        }

        if (from.isAfter(to)) {
            AlertUtil.showWarning("Validation", "From date cannot be after to date");
            return;
        }

        AppExecutor.submit(() -> {
            try {
                ReportDAO.ReportResult result = reportDAO.generateReport("All Transactions", from, to, null, null);

                Platform.runLater(() -> {
                    allTransactions.clear();
                    allTransactions.addAll(result.getRows());
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to generate report: " + e.getMessage()));
            }
        });
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            filteredTransactions.setPredicate(null);
        } else {
            filteredTransactions.setPredicate(row -> {
                // Match transaction number
                if (row.getTransactionNo() != null && row.getTransactionNo().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match transaction type
                String typeFormatted = formatTransactionType(row.getTransactionType()).toLowerCase();
                if (typeFormatted.contains(searchText)) {
                    return true;
                }

                // Match party name
                if (row.getParty() != null && row.getParty().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match date
                if (row.getDate() != null && row.getDate().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match amount
                if (row.getAmount() != null && row.getAmount().toString().contains(searchText)) {
                    return true;
                }

                return false;
            });
        }

        updateResultCount();
    }

    private void updateResultCount() {
        int total = allTransactions.size();
        int filtered = filteredTransactions.size();
        if (filtered == total) {
            resultCountLabel.setText("Showing " + total + " transaction(s)");
        } else {
            resultCountLabel.setText("Showing " + filtered + " of " + total + " transaction(s)");
        }
    }

    private void exportReport() {
        if (filteredTransactions.isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export");
            return;
        }

        // Simple CSV export
        StringBuilder csv = new StringBuilder();
        csv.append("Sr. No,Type,Transaction No,Date,Party,Amount\n");

        for (ReportDAO.ReportRow row : filteredTransactions) {
            csv.append(row.getSerialNo()).append(",");
            csv.append(formatTransactionType(row.getTransactionType())).append(",");
            csv.append(row.getTransactionNo()).append(",");
            csv.append(row.getDate()).append(",");
            csv.append(escapeCSV(row.getParty())).append(",");
            csv.append(row.getAmount() != null ? row.getAmount() : "").append("\n");
        }

        AlertUtil.showInfo("Export", "CSV export:\n\n" + csv.toString().substring(0, Math.min(500, csv.length())) + "...\n\n(Export to CSV feature would save to file)");
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
