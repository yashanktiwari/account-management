package com.accounting.ui.dialog;

import com.accounting.dao.ReportDAO;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
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
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Color;
import java.io.File;
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
        generateBtn.setMinWidth(150);
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> generateReport());
        
        Button exportExcelBtn = new Button("Export to Excel");
        exportExcelBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        exportExcelBtn.setMinWidth(150);
        exportExcelBtn.setMaxWidth(Double.MAX_VALUE);
        exportExcelBtn.setOnAction(e -> exportToExcel());

        Button printPdfBtn = new Button("Print to PDF");
        printPdfBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        printPdfBtn.setMinWidth(130);
        printPdfBtn.setMaxWidth(Double.MAX_VALUE);
        printPdfBtn.setOnAction(e -> printToPdf());

        Button clearBtn = new Button("Clear Search");
        clearBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        clearBtn.setMinWidth(130);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applyFilters();
        });

        HBox buttonBox = new HBox(10, generateBtn, exportExcelBtn, printPdfBtn, clearBtn);
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
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        resultCountLabel.setText("Loading...");

        AppExecutor.submit(() -> {
            try {
                ReportDAO.ReportResult result = reportDAO.generateReport("All Transactions", from, to, null, null);

                Platform.runLater(() -> {
                    allTransactions.clear();
                    allTransactions.addAll(result.getRows());
                    applyFilters();
                    System.out.println("Loaded " + result.getRows().size() + " transactions");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtil.showError("Error", "Failed to generate report: " + e.getMessage());
                    e.printStackTrace();
                });
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

    private void exportToExcel() {
        if (filteredTransactions.isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("transactions_report.xlsx");

        Window window = resultTable.getScene().getWindow();
        File file = fc.showSaveDialog(window);

        if (file != null) {
            AppExecutor.submit(() -> {
                try {
                    // Simple Excel export using Apache POI
                    org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Transactions");

                    // Header row
                    org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                    String[] columns = {"Sr. No", "Type", "Transaction No", "Date", "Party", "Amount"};
                    
                    org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                    headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
                    headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                    org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerStyle.setFont(headerFont);

                    for (int i = 0; i < columns.length; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                        cell.setCellValue(columns[i]);
                        cell.setCellStyle(headerStyle);
                    }

                    // Data rows
                    int rowNum = 1;
                    for (ReportDAO.ReportRow row : filteredTransactions) {
                        org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowNum++);
                        dataRow.createCell(0).setCellValue(row.getSerialNo());
                        dataRow.createCell(1).setCellValue(formatTransactionType(row.getTransactionType()));
                        dataRow.createCell(2).setCellValue(row.getTransactionNo());
                        dataRow.createCell(3).setCellValue(row.getDate());
                        dataRow.createCell(4).setCellValue(row.getParty() != null ? row.getParty() : "");
                        dataRow.createCell(5).setCellValue(row.getAmount() != null ? row.getAmount() : 0);
                    }

                    // Auto-size columns
                    for (int i = 0; i < columns.length; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        workbook.write(fos);
                    }
                    workbook.close();

                    Platform.runLater(() -> AlertUtil.showInfo("Export", "Report exported successfully to " + file.getAbsolutePath()));
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtil.showError("Export Error", "Failed to export: " + e.getMessage()));
                }
            });
        }
    }

    private void printToPdf() {
        if (filteredTransactions.isEmpty()) {
            AlertUtil.showWarning("Print", "No data to print");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save as PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("transactions_report.pdf");

        Window window = resultTable.getScene().getWindow();
        File file = fc.showSaveDialog(window);

        if (file != null) {
            AppExecutor.submit(() -> {
                try {
                    com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());
                    com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                    document.open();

                    // Title
                    com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                    com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("All Transactions Report", titleFont);
                    title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    document.add(title);
                    document.add(new com.lowagie.text.Paragraph(" "));

                    // Date range
                    com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA, 10);
                    com.lowagie.text.Paragraph dateRange = new com.lowagie.text.Paragraph(
                        "From: " + fromDate.getValue().format(DATE_FORMATTER) + 
                        " To: " + toDate.getValue().format(DATE_FORMATTER), 
                        normalFont
                    );
                    document.add(dateRange);
                    document.add(new com.lowagie.text.Paragraph(" "));

                    // Table
                    com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
                    table.setWidthPercentage(100);

                    // Headers
                    com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
                    String[] headers = {"Sr. No", "Type", "Transaction No", "Date", "Party", "Amount"};
                    for (String header : headers) {
                        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(header, headerFont));
                        cell.setBackgroundColor(Color.LIGHT_GRAY);
                        table.addCell(cell);
                    }

                    // Data
                    com.lowagie.text.Font dataFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA, 9);
                    for (ReportDAO.ReportRow row : filteredTransactions) {
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.valueOf(row.getSerialNo()), dataFont)));
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(formatTransactionType(row.getTransactionType()), dataFont)));
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(row.getTransactionNo() != null ? row.getTransactionNo() : "", dataFont)));
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(row.getDate() != null ? row.getDate() : "", dataFont)));
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(row.getParty() != null ? row.getParty() : "", dataFont)));
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(row.getAmount() != null ? String.format("%.2f", row.getAmount()) : "0.00", dataFont)));
                    }

                    document.add(table);
                    document.close();

                    Platform.runLater(() -> AlertUtil.showInfo("Print", "PDF saved successfully to " + file.getAbsolutePath()));
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtil.showError("Print Error", "Failed to generate PDF: " + e.getMessage()));
                }
            });
        }
    }
}
