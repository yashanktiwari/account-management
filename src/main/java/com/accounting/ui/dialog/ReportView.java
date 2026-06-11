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
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

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
    private final Set<String> searchTerms = new HashSet<>();
    private final ObservableList<String> searchTagsList = FXCollections.observableArrayList();
    private HBox tagsContainer;
    private Timer debounceTimer;
    private static final int DEBOUNCE_DELAY = 500;
    private static final int MAX_SEARCH_TERMS = 5;

    public Parent createContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("All Transactions Report");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        VBox filterSection = buildFilterSection();


        VBox tableSection = buildTableSection();

        root.getChildren().addAll(title, filterSection, tableSection);

        // Load initial data
        Platform.runLater(this::generateReport);

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

        Label searchLabel = new Label("Search:");
        searchLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        searchField = new TextField();
        searchField.setPromptText("Type and press Enter to add search term...");
        searchField.setPrefWidth(300);
        searchField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                addSearchTerm();
                e.consume();
            }
        });
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (debounceTimer != null) {
                debounceTimer.cancel();
            }
            debounceTimer = new Timer();
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(ReportView.this::applyFilters);
                }
            }, DEBOUNCE_DELAY);
        });

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchTerms.clear();
            searchTagsList.clear();
            applyFilters();
        });

        HBox searchControls = new HBox(10, searchLabel, searchField, clearBtn);
        searchControls.setAlignment(Pos.CENTER_LEFT);

        tagsContainer = new HBox(8);
        tagsContainer.setAlignment(Pos.CENTER_LEFT);
        tagsContainer.setPrefHeight(32);
        searchTagsList.addListener((javafx.collections.ListChangeListener<String>) change -> {
            tagsContainer.getChildren().clear();
            for (String term : searchTagsList) {
                HBox tag = new HBox(5);
                tag.setStyle("-fx-padding: 4px 8px; -fx-background-color: #e3f2fd; -fx-border-color: #1976d2; -fx-border-radius: 4; -fx-alignment: CENTER;");
                Label label = new Label(term);
                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-padding: 0; -fx-font-size: 12px;");
                removeBtn.setOnAction(e -> removeSearchTerm(term));
                tag.getChildren().addAll(label, removeBtn);
                tagsContainer.getChildren().add(tag);
            }
        });

        VBox searchRow = new VBox(8, searchControls, tagsContainer);

        // Add to grid
        grid.add(fromDateLabel, 0, 0);
        grid.add(fromDate, 1, 0);
        grid.add(toDateLabel, 2, 0);
        grid.add(toDate, 3, 0);

        grid.add(searchRow, 0, 1, 4, 1);
        Button generateBtn = new Button("Generate Report");
        generateBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        generateBtn.setOnAction(e -> generateReport());

        Button exportBtn = new Button("Export to CSV");
        exportBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        exportBtn.setOnAction(e -> exportReport());

        HBox buttonBox = new HBox(10, generateBtn, exportBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(buttonBox, 0, 2, 4, 1);

        resultCountLabel = new Label("No results");
        resultCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        grid.add(resultCountLabel, 0, 3, 4, 1);

        section.getChildren().addAll(sectionTitle, grid);
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
        resultTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    filteredTransactions = new FilteredList<>(allTransactions);
    resultTable.setItems(filteredTransactions);

    setupTableColumns("All Transactions");

        VBox.setVgrow(resultTable, Priority.ALWAYS);

        section.getChildren().addAll(sectionTitle, resultTable);
        return section;
    }

    private void setupTableColumns(String reportType) {
        resultTable.getColumns().clear();

            // Serial No
            TableColumn<ReportDAO.ReportRow, Integer> serialCol = new TableColumn<>("S.No");
            serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNo"));
            serialCol.setMinWidth(50);
            serialCol.setMaxWidth(70);
            serialCol.setStyle("-fx-alignment: CENTER;");

            // Transaction Type
            TableColumn<ReportDAO.ReportRow, String> typeCol = new TableColumn<>("Type");
            typeCol.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
            typeCol.setMinWidth(150);
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
            noCol.setMinWidth(130);

            // Date
            TableColumn<ReportDAO.ReportRow, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            dateCol.setMinWidth(110);
            dateCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        try {
                            // Parse the date from database format and format as dd-mm-yyyy
                            LocalDate date = LocalDate.parse(item);
                            setText(date.format(DATE_FORMATTER));
                        } catch (Exception e) {
                            setText(item); // If parsing fails, show as-is
                        }
                    }
                }
            });

            // Party Name
            TableColumn<ReportDAO.ReportRow, String> partyCol = new TableColumn<>("Party");
            partyCol.setCellValueFactory(new PropertyValueFactory<>("party"));
            partyCol.setMinWidth(150);

            // Vehicle No
            TableColumn<ReportDAO.ReportRow, String> vehicleCol = new TableColumn<>("Vehicle No");
            vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            vehicleCol.setMinWidth(120);

            // From Location
            TableColumn<ReportDAO.ReportRow, String> fromLocCol = new TableColumn<>("From Location");
            fromLocCol.setCellValueFactory(new PropertyValueFactory<>("fromLocation"));
            fromLocCol.setMinWidth(130);

            // To Location
            TableColumn<ReportDAO.ReportRow, String> toLocCol = new TableColumn<>("To Location");
            toLocCol.setCellValueFactory(new PropertyValueFactory<>("toLocation"));
            toLocCol.setMinWidth(130);

            // Description
            TableColumn<ReportDAO.ReportRow, String> descCol = new TableColumn<>("Description");
            descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
            descCol.setMinWidth(180);

            // GST
            TableColumn<ReportDAO.ReportRow, String> gstCol = new TableColumn<>("GST %");
            gstCol.setCellValueFactory(new PropertyValueFactory<>("gst"));
            gstCol.setMinWidth(80);

            // Taxable Amount
            TableColumn<ReportDAO.ReportRow, Double> taxableCol = new TableColumn<>("Taxable Amount");
            taxableCol.setCellValueFactory(new PropertyValueFactory<>("taxableAmount"));
            taxableCol.setCellFactory(col -> formatCurrencyCell());
            taxableCol.setMinWidth(130);

            // SGST
            TableColumn<ReportDAO.ReportRow, Double> sgstCol = new TableColumn<>("SGST");
            sgstCol.setCellValueFactory(new PropertyValueFactory<>("sgst"));
            sgstCol.setCellFactory(col -> formatCurrencyCell());
            sgstCol.setMinWidth(100);

            // CGST
            TableColumn<ReportDAO.ReportRow, Double> cgstCol = new TableColumn<>("CGST");
            cgstCol.setCellValueFactory(new PropertyValueFactory<>("cgst"));
            cgstCol.setCellFactory(col -> formatCurrencyCell());
            cgstCol.setMinWidth(100);

            // IGST
            TableColumn<ReportDAO.ReportRow, Double> igstCol = new TableColumn<>("IGST");
            igstCol.setCellValueFactory(new PropertyValueFactory<>("igst"));
            igstCol.setCellFactory(col -> formatCurrencyCell());
            igstCol.setMinWidth(100);

            // Total GST
            TableColumn<ReportDAO.ReportRow, Double> totalGstCol = new TableColumn<>("Total GST");
            totalGstCol.setCellValueFactory(new PropertyValueFactory<>("totalGst"));
            totalGstCol.setCellFactory(col -> formatCurrencyCell());
            totalGstCol.setMinWidth(110);

            // Amount
            TableColumn<ReportDAO.ReportRow, Double> amountCol = new TableColumn<>("Amount");
            amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            amountCol.setCellFactory(col -> formatCurrencyCell());
            amountCol.setMinWidth(120);

            // Advance
            TableColumn<ReportDAO.ReportRow, Double> advanceCol = new TableColumn<>("Advance");
            advanceCol.setCellValueFactory(new PropertyValueFactory<>("advance"));
            advanceCol.setCellFactory(col -> formatCurrencyCell());
            advanceCol.setMinWidth(100);

            // Balance
            TableColumn<ReportDAO.ReportRow, Double> balanceCol = new TableColumn<>("Balance");
            balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
            balanceCol.setCellFactory(col -> formatCurrencyCell());
            balanceCol.setMinWidth(100);

            // Payment Mode
            TableColumn<ReportDAO.ReportRow, String> paymentModeCol = new TableColumn<>("Payment Mode");
            paymentModeCol.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));
            paymentModeCol.setMinWidth(120);

            // Cheque No
            TableColumn<ReportDAO.ReportRow, String> chequeNoCol = new TableColumn<>("Cheque No");
            chequeNoCol.setCellValueFactory(new PropertyValueFactory<>("chequeNo"));
            chequeNoCol.setMinWidth(110);

            // Cheque Date
            TableColumn<ReportDAO.ReportRow, String> chequeDateCol = new TableColumn<>("Cheque Date");
            chequeDateCol.setCellValueFactory(new PropertyValueFactory<>("chequeDate"));
            chequeDateCol.setMinWidth(110);

            // Bank Name
            TableColumn<ReportDAO.ReportRow, String> bankCol = new TableColumn<>("Bank Name");
            bankCol.setCellValueFactory(new PropertyValueFactory<>("bankName"));
            bankCol.setMinWidth(130);

            // Remarks
            TableColumn<ReportDAO.ReportRow, String> remarksCol = new TableColumn<>("Remarks");
            remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
            remarksCol.setMinWidth(180);

            // Status
            TableColumn<ReportDAO.ReportRow, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusCol.setMinWidth(90);

            resultTable.getColumns().addAll(
                serialCol, typeCol, noCol, dateCol, partyCol, vehicleCol, fromLocCol, toLocCol,
                descCol, gstCol, taxableCol, sgstCol, cgstCol, igstCol, totalGstCol, amountCol,
                advanceCol, balanceCol, paymentModeCol, chequeNoCol, chequeDateCol, bankCol,
                remarksCol, statusCol
            );

        // Save column state when columns change
        resultTable.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<ReportDAO.ReportRow, ?>>) change -> saveColumnState());

        // Load saved column state
        loadColumnState();
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
                String liveSearchText = searchField.getText().trim().toLowerCase();

                if (searchTerms.isEmpty() && liveSearchText.isEmpty()) {
                    filteredTransactions.setPredicate(null);
                } else {
                    filteredTransactions.setPredicate(row -> {
                        // Check all search terms (AND logic)
                        for (String term : searchTerms) {
                            if (!matchesSearchTerm(row, term.toLowerCase())) {
                                return false;
                            }
                        }

                        // Check live search text if present
                        if (!liveSearchText.isEmpty() && !matchesSearchTerm(row, liveSearchText)) {
                            return false;
                        }

                        return true;
                    });
                }

                updateResultCount();
            }

            private boolean matchesSearchTerm(ReportDAO.ReportRow row, String searchText) {
                // Match transaction number (invoice no, receipt no, etc.)
                if (row.getTransactionNo() != null && row.getTransactionNo().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match transaction type (e.g., "Invoice", "Receipt", "LR", "Slip")
                String typeFormatted = formatTransactionType(row.getTransactionType()).toLowerCase();
                if (typeFormatted.contains(searchText)) {
                    return true;
                }

                // Match party name
                if (row.getParty() != null && row.getParty().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match vehicle number
                if (row.getVehicle() != null && row.getVehicle().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match from/to locations
                if (row.getFromLocation() != null && row.getFromLocation().toLowerCase().contains(searchText)) {
                    return true;
                }
                if (row.getToLocation() != null && row.getToLocation().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match description
                if (row.getDescription() != null && row.getDescription().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match remarks
                if (row.getRemarks() != null && row.getRemarks().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match payment mode
                if (row.getPaymentMode() != null && row.getPaymentMode().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match cheque number
                if (row.getChequeNo() != null && row.getChequeNo().toLowerCase().contains(searchText)) {
                    return true;
                }

                // Match bank name
                if (row.getBankName() != null && row.getBankName().toLowerCase().contains(searchText)) {
                    return true;
                }

                return false;
            }

            private void addSearchTerm() {
                String term = searchField.getText().trim();
                if (term.isEmpty() || searchTerms.size() >= MAX_SEARCH_TERMS) {
                    return;
                }
                if (!searchTerms.contains(term)) {
                    searchTerms.add(term);
                    searchTagsList.add(term);
                    searchField.clear();
                    applyFilters();
                }
            }

            private void removeSearchTerm(String term) {
                searchTerms.remove(term);
                searchTagsList.remove(term);
                applyFilters();
            }

            private void saveColumnState() {
                Preferences prefs = Preferences.userNodeForPackage(ReportView.class);
                StringBuilder columnOrder = new StringBuilder();
                StringBuilder columnWidths = new StringBuilder();

                for (int i = 0; i < resultTable.getColumns().size(); i++) {
                    TableColumn<ReportDAO.ReportRow, ?> col = resultTable.getColumns().get(i);
                    if (i > 0) {
                        columnOrder.append(",");
                        columnWidths.append(",");
                    }
                    columnOrder.append(col.getText());
                    columnWidths.append((int) col.getWidth());
                }

                prefs.put("reportTable_columnOrder", columnOrder.toString());
                prefs.put("reportTable_columnWidths", columnWidths.toString());
            }

            private void loadColumnState() {
                Preferences prefs = Preferences.userNodeForPackage(ReportView.class);
                String columnOrderStr = prefs.get("reportTable_columnOrder", "");
                String columnWidthsStr = prefs.get("reportTable_columnWidths", "");

                try {
                    // Restore column order
                    if (!columnOrderStr.isEmpty()) {
                        String[] columnNames = columnOrderStr.split(",");
                        List<TableColumn<ReportDAO.ReportRow, ?>> currentColumns = new java.util.ArrayList<>(resultTable.getColumns());

                        // Reorder columns based on saved order
                        for (int i = 0; i < columnNames.length && i < currentColumns.size(); i++) {
                            String targetName = columnNames[i];
                            for (int j = i; j < currentColumns.size(); j++) {
                                if (currentColumns.get(j).getText().equals(targetName)) {
                                    // Swap columns
                                    TableColumn<ReportDAO.ReportRow, ?> temp = currentColumns.get(i);
                                    currentColumns.set(i, currentColumns.get(j));
                                    currentColumns.set(j, temp);
                                    break;
                                }
                            }
                        }

                        // Clear and re-add columns in correct order
                        resultTable.getColumns().clear();
                        resultTable.getColumns().addAll(currentColumns);
                    }

                    // Restore column widths
                    if (!columnWidthsStr.isEmpty()) {
                        String[] widths = columnWidthsStr.split(",");
                        for (int i = 0; i < widths.length && i < resultTable.getColumns().size(); i++) {
                            int width = Integer.parseInt(widths[i]);
                            if (width > 0) {
                                resultTable.getColumns().get(i).setPrefWidth(width);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore if preferences are corrupted
                    e.printStackTrace();
                }
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
                csv.append("S.No,Type,Transaction No,Date,Party,Vehicle No,From Location,To Location,");
                csv.append("Description,GST %,Taxable Amount,SGST,CGST,IGST,Total GST,Amount,");
                csv.append("Advance,Balance,Payment Mode,Cheque No,Cheque Date,Bank Name,Remarks,Status\n");

                for (ReportDAO.ReportRow row : filteredTransactions) {
                    csv.append(row.getSerialNo()).append(",");
                    csv.append(formatTransactionType(row.getTransactionType())).append(",");
                    csv.append(row.getTransactionNo()).append(",");
                    csv.append(row.getDate()).append(",");
                    csv.append(escapeCSV(row.getParty())).append(",");
                    csv.append(row.getVehicle()).append(",");
                    csv.append(row.getFromLocation()).append(",");
                    csv.append(row.getToLocation()).append(",");
                    csv.append(escapeCSV(row.getDescription())).append(",");
                    csv.append(row.getGst()).append(",");
                    csv.append(row.getTaxableAmount() != null ? row.getTaxableAmount() : "").append(",");
                    csv.append(row.getSgst() != null ? row.getSgst() : "").append(",");
                    csv.append(row.getCgst() != null ? row.getCgst() : "").append(",");
                    csv.append(row.getIgst() != null ? row.getIgst() : "").append(",");
                    csv.append(row.getTotalGst() != null ? row.getTotalGst() : "").append(",");
                    csv.append(row.getAmount() != null ? row.getAmount() : "").append(",");
                    csv.append(row.getAdvance() != null ? row.getAdvance() : "").append(",");
                    csv.append(row.getBalance() != null ? row.getBalance() : "").append(",");
                    csv.append(row.getPaymentMode()).append(",");
                    csv.append(row.getChequeNo()).append(",");
                    csv.append(row.getChequeDate()).append(",");
                    csv.append(row.getBankName()).append(",");
                    csv.append(escapeCSV(row.getRemarks())).append("\n");
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
