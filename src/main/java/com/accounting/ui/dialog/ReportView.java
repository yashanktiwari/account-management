package com.accounting.ui.dialog;

import com.accounting.dao.ReportDAO;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.ReportPDFGenerator;
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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

public class ReportView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");
    private final ReportDAO reportDAO = new ReportDAO();
    private final ObservableList<ReportDAO.ReportRow> allTransactions = FXCollections.observableArrayList();

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
                    Platform.runLater(ReportView.this::generateReport);
                }
            }, DEBOUNCE_DELAY);
        });

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchTerms.clear();
            searchTagsList.clear();
            generateReport();
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

        Button exportBtn = new Button("Export to Excel");
        exportBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        exportBtn.setOnAction(e -> exportReport());

        Button printPdfBtn = new Button("Print to PDF");
        printPdfBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        printPdfBtn.setOnAction(e -> printToPDF());

        HBox buttonBox = new HBox(10, generateBtn, exportBtn, printPdfBtn);
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

        resultTable.setItems(allTransactions);

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

            // Amount
            TableColumn<ReportDAO.ReportRow, Double> amountCol = new TableColumn<>("Amount");
            amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            amountCol.setCellFactory(col -> formatCurrencyCell());
            amountCol.setMinWidth(120);

            // Debit
            TableColumn<ReportDAO.ReportRow, Double> debitCol = new TableColumn<>("Debit");
            debitCol.setCellValueFactory(new PropertyValueFactory<>("debit"));
            debitCol.setCellFactory(col -> formatCurrencyCell());
            debitCol.setMinWidth(100);

            // Credit
            TableColumn<ReportDAO.ReportRow, Double> creditCol = new TableColumn<>("Credit");
            creditCol.setCellValueFactory(new PropertyValueFactory<>("credit"));
            creditCol.setCellFactory(col -> formatCurrencyCell());
            creditCol.setMinWidth(100);

            // Remarks
            TableColumn<ReportDAO.ReportRow, String> remarksCol = new TableColumn<>("Remarks");
            remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
            remarksCol.setMinWidth(180);

            resultTable.getColumns().addAll(
                serialCol, typeCol, noCol, dateCol, partyCol, amountCol, debitCol, creditCol, remarksCol
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

        // Combine search terms into a list
        java.util.List<String> searchTermsList = new java.util.ArrayList<>(searchTerms);
        if (!searchField.getText().trim().isEmpty()) {
            searchTermsList.add(searchField.getText().trim());
        }

        final java.util.List<String> searchTermsToUse = searchTermsList.isEmpty() ? null : searchTermsList;

        AppExecutor.submit(() -> {
            try {
                ReportDAO.ReportResult result = reportDAO.generateReport("All Transactions", from, to, null, null, searchTermsToUse);

                Platform.runLater(() -> {
                    allTransactions.clear();
                    allTransactions.addAll(result.getRows());
                    updateResultCount();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to generate report: " + e.getMessage()));
            }
        });
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
                    generateReport();
                }
            }

            private void removeSearchTerm(String term) {
                searchTerms.remove(term);
                searchTagsList.remove(term);
                generateReport();
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
                resultCountLabel.setText("Showing " + total + " transaction(s)");
            }

            private void exportReport() {
                if (allTransactions.isEmpty()) {
                    AlertUtil.showWarning("Export", "No data to export");
                    return;
                }

                // Create file chooser
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Export to Excel");
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
                );
                fileChooser.setInitialFileName("All_Transactions_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".xlsx");

                // Show save dialog
                File file = fileChooser.showSaveDialog(resultTable.getScene().getWindow());
                if (file == null) {
                    return; // User cancelled
                }

                // Export in background thread
                AppExecutor.submit(() -> {
                    try (Workbook workbook = new XSSFWorkbook();
                         FileOutputStream outputStream = new FileOutputStream(file)) {
                        
                        Sheet sheet = workbook.createSheet("All Transactions");
                        
                        // Define column widths
                        sheet.setColumnWidth(0, 5 * 256);  // S.No - 5 characters
                        sheet.setColumnWidth(1, 20 * 256); // Type - 20 characters
                        sheet.setColumnWidth(2, 18 * 256); // Transaction No - 18 characters
                        sheet.setColumnWidth(3, 15 * 256); // Date - 15 characters
                        sheet.setColumnWidth(4, 25 * 256); // Party - 25 characters
                        sheet.setColumnWidth(5, 12 * 256); // Debit - 12 characters
                        sheet.setColumnWidth(6, 12 * 256); // Credit - 12 characters
                        sheet.setColumnWidth(7, 30 * 256); // Remarks - 30 characters
                        
                        // Create header style
                        CellStyle headerStyle = workbook.createCellStyle();
                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerStyle.setFont(headerFont);
                        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        headerStyle.setBorderBottom(BorderStyle.THIN);
                        headerStyle.setBorderTop(BorderStyle.THIN);
                        headerStyle.setBorderLeft(BorderStyle.THIN);
                        headerStyle.setBorderRight(BorderStyle.THIN);
                        
                        // Create data style
                        CellStyle dataStyle = workbook.createCellStyle();
                        dataStyle.setBorderBottom(BorderStyle.THIN);
                        dataStyle.setBorderTop(BorderStyle.THIN);
                        dataStyle.setBorderLeft(BorderStyle.THIN);
                        dataStyle.setBorderRight(BorderStyle.THIN);
                        
                        // Create date style
                        CellStyle dateStyle = workbook.createCellStyle();
                        dateStyle.cloneStyleFrom(dataStyle);
                        CreationHelper createHelper = workbook.getCreationHelper();
                        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
                        
                        // Create number style
                        CellStyle numberStyle = workbook.createCellStyle();
                        numberStyle.cloneStyleFrom(dataStyle);
                        numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                        
                        // Define columns
                        String[] columns = {"S.No", "Type", "Transaction No", "Date", "Party", "Debit", "Credit", "Remarks"};
                        
                        // Create header row
                        Row headerRow = sheet.createRow(0);
                        for (int i = 0; i < columns.length; i++) {
                            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                            cell.setCellValue(columns[i]);
                            cell.setCellStyle(headerStyle);
                        }
                        
                        // Create data style for center alignment
                        CellStyle centerStyle = workbook.createCellStyle();
                        centerStyle.cloneStyleFrom(dataStyle);
                        centerStyle.setAlignment(HorizontalAlignment.CENTER);
                        
                        // Create data style for right alignment (numbers)
                        CellStyle rightStyle = workbook.createCellStyle();
                        rightStyle.cloneStyleFrom(numberStyle);
                        rightStyle.setAlignment(HorizontalAlignment.RIGHT);
                        
                        // Write data rows
                        int rowNum = 1;
                        for (ReportDAO.ReportRow row : allTransactions) {
                            Row dataRow = sheet.createRow(rowNum++);
                            
                            // S.No
                            org.apache.poi.ss.usermodel.Cell cell0 = dataRow.createCell(0);
                            cell0.setCellValue(row.getSerialNo());
                            cell0.setCellStyle(centerStyle);
                            
                            // Type
                            org.apache.poi.ss.usermodel.Cell cell1 = dataRow.createCell(1);
                            cell1.setCellValue(formatTransactionType(row.getTransactionType()));
                            cell1.setCellStyle(dataStyle);
                            
                            // Transaction No
                            org.apache.poi.ss.usermodel.Cell cell2 = dataRow.createCell(2);
                            cell2.setCellValue(row.getTransactionNo());
                            cell2.setCellStyle(dataStyle);
                            
                            // Date
                            org.apache.poi.ss.usermodel.Cell cell3 = dataRow.createCell(3);
                            try {
                                LocalDate date = LocalDate.parse(row.getDate());
                                cell3.setCellValue(date);
                                cell3.setCellStyle(dateStyle);
                            } catch (Exception e) {
                                cell3.setCellValue(row.getDate());
                                cell3.setCellStyle(dataStyle);
                            }
                            
                            // Party
                            org.apache.poi.ss.usermodel.Cell cell4 = dataRow.createCell(4);
                            cell4.setCellValue(row.getParty());
                            cell4.setCellStyle(dataStyle);
                            
                            // Debit
                            org.apache.poi.ss.usermodel.Cell cell5 = dataRow.createCell(5);
                            cell5.setCellValue(row.getDebit());
                            cell5.setCellStyle(rightStyle);
                            
                            // Credit
                            org.apache.poi.ss.usermodel.Cell cell6 = dataRow.createCell(6);
                            cell6.setCellValue(row.getCredit());
                            cell6.setCellStyle(rightStyle);
                            
                            // Remarks
                            org.apache.poi.ss.usermodel.Cell cell7 = dataRow.createCell(7);
                            cell7.setCellValue(row.getRemarks());
                            cell7.setCellStyle(dataStyle);
                        }
                        
                        workbook.write(outputStream);
                        
                        // Show success message on UI thread
                        Platform.runLater(() -> {
                            AlertUtil.showInfo("Export Successful", 
                                "Exported " + allTransactions.size() + " transactions to:\n" + file.getAbsolutePath());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            AlertUtil.showError("Export Failed", "Failed to export data: " + e.getMessage());
                        });
                    }
                });
            }

            private void printToPDF() {
                if (allTransactions.isEmpty()) {
                    AlertUtil.showWarning("Print to PDF", "No data to print");
                    return;
                }

                // Create file chooser
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Print to PDF");
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                );
                fileChooser.setInitialFileName("All_Transactions_Report_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".pdf");

                // Show save dialog
                File file = fileChooser.showSaveDialog(resultTable.getScene().getWindow());
                if (file == null) {
                    return; // User cancelled
                }

                // Generate PDF in background thread
                AppExecutor.submit(() -> {
                    try {
                        ReportPDFGenerator.generateReportPDF(allTransactions, fromDate.getValue(), toDate.getValue(), file.getAbsolutePath());
                        Platform.runLater(() -> {
                            AlertUtil.showInfo("PDF Generated", 
                                "Successfully generated PDF with " + allTransactions.size() + " transactions:\n" + file.getAbsolutePath());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            AlertUtil.showError("PDF Generation Failed", "Failed to generate PDF: " + e.getMessage());
                        });
                    }
                });
            }
}
