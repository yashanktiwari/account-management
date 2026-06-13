package com.accounting.ui.dialog;

import com.accounting.dao.ReportDAO;
import com.accounting.dao.ReportDAO.SimpleReportResult;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.lowagie.text.FontFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QueryBuilderView {

    private static final Logger log = AppLogger.get(QueryBuilderView.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ReportDAO reportDAO = new ReportDAO();

    private ComboBox<String> reportTypeCombo;
    private ComboBox<String> partyCombo;
    private ComboBox<String> vehicleCombo;
    private DatePicker fromDate, toDate;
    private TextField searchField;
    private TableView<String[]> resultsTable;
    private Label statusLabel;
    private String currentReportTitle = "";

    private final ObservableList<String[]> allData = FXCollections.observableArrayList();
    private FilteredList<String[]> filteredData;
    private String[] currentHeaders = {};

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label heading = new Label("Custom Reports");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        VBox filterSection = buildFilterSection();
        VBox resultsSection = buildResultsSection();

        root.getChildren().addAll(heading, filterSection, resultsSection);
        VBox.setVgrow(resultsSection, Priority.ALWAYS);

        return root;
    }

    private VBox buildFilterSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));

        // Report Type
        Label typeLabel = new Label("Report Type:");
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
        reportTypeCombo = new ComboBox<>(FXCollections.observableArrayList(ReportDAO.getPresetReportTypes()));
        reportTypeCombo.setPromptText("Select a report...");
        reportTypeCombo.setPrefWidth(220);
        reportTypeCombo.setOnAction(e -> onReportTypeChanged());

        // Date Range
        Label fromLabel = new Label("From Date:");
        fromLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        fromDate = new DatePicker(LocalDate.now().minusDays(30));
        fromDate.setPrefWidth(150);

        Label toLabel = new Label("To Date:");
        toLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        toDate = new DatePicker(LocalDate.now());
        toDate.setPrefWidth(150);

        // Party Filter
        Label partyLabel = new Label("Party:");
        partyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        partyCombo = new ComboBox<>();
        partyCombo.setPromptText("(Optional)");
        partyCombo.setPrefWidth(200);
        partyCombo.setVisible(false);
        partyCombo.setManaged(false);

        // Vehicle Filter
        Label vehicleLabel = new Label("Vehicle:");
        vehicleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        vehicleCombo = new ComboBox<>();
        vehicleCombo.setPromptText("(Optional)");
        vehicleCombo.setPrefWidth(200);
        vehicleCombo.setVisible(false);
        vehicleCombo.setManaged(false);

        // Search
        Label searchLabel = new Label("Search:");
        searchLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        searchField = new TextField();
        searchField.setPromptText("Type to filter results...");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        grid.add(typeLabel, 0, 0);
        grid.add(reportTypeCombo, 1, 0);
        grid.add(fromLabel, 2, 0);
        grid.add(fromDate, 3, 0);
        grid.add(toLabel, 4, 0);
        grid.add(toDate, 5, 0);

        grid.add(partyLabel, 0, 1);
        grid.add(partyCombo, 1, 1);
        grid.add(vehicleLabel, 2, 1);
        grid.add(vehicleCombo, 3, 1);

        grid.add(searchLabel, 0, 2);
        grid.add(searchField, 1, 2, 5, 1);

        // Buttons
        Button runBtn = new Button("Generate Report");
        runBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        runBtn.setMinWidth(150);
        runBtn.setOnAction(e -> runReport());

        Button excelBtn = new Button("Export to Excel");
        excelBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        excelBtn.setMinWidth(150);
        excelBtn.setOnAction(e -> exportToExcel());

        Button pdfBtn = new Button("Print to PDF");
        pdfBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        pdfBtn.setMinWidth(130);
        pdfBtn.setOnAction(e -> printToPdf());

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        clearBtn.setMinWidth(80);
        clearBtn.setOnAction(e -> {
            searchField.clear();
            resultsTable.getColumns().clear();
            resultsTable.getItems().clear();
            allData.clear();
            statusLabel.setText("Select a report and click Generate");
        });

        HBox buttonBox = new HBox(10, runBtn, excelBtn, pdfBtn, clearBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(buttonBox, 0, 3, 6, 1);

        statusLabel = new Label("Select a report and click Generate");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        grid.add(statusLabel, 0, 4, 6, 1);

        section.getChildren().add(grid);
        return section;
    }

    private void onReportTypeChanged() {
        String reportType = reportTypeCombo.getValue();
        if (reportType == null) return;

        // Show/hide party filter
        boolean needsParty = List.of("Purchase Invoices", "Sale Invoices", "Purchase Receipts", 
            "Sale Receipts", "Payments", "Party-wise Summary", "GST Report").contains(reportType);
        partyCombo.setVisible(needsParty);
        partyCombo.setManaged(needsParty);

        // Show/hide vehicle filter
        boolean needsVehicle = List.of("Loading Slips", "Lorry Receipts", "Vehicle-wise Summary").contains(reportType);
        vehicleCombo.setVisible(needsVehicle);
        vehicleCombo.setManaged(needsVehicle);

        // Load parties if needed
        if (needsParty && partyCombo.getItems().isEmpty()) {
            loadParties();
        }

        // Load vehicles if needed
        if (needsVehicle && vehicleCombo.getItems().isEmpty()) {
            loadVehicles();
        }
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<String> parties = reportDAO.getAllParties();
                Platform.runLater(() -> {
                    partyCombo.setItems(FXCollections.observableArrayList(parties));
                });
            } catch (Exception e) {
                log.error("Failed to load parties", e);
            }
        });
    }

    private void loadVehicles() {
        AppExecutor.submit(() -> {
            try {
                List<String> vehicles = reportDAO.getAllVehicles();
                Platform.runLater(() -> {
                    vehicleCombo.setItems(FXCollections.observableArrayList(vehicles));
                });
            } catch (Exception e) {
                log.error("Failed to load vehicles", e);
            }
        });
    }

    private VBox buildResultsSection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(section, Priority.ALWAYS);

        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("Select a report type and click 'Generate Report'"));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        filteredData = new FilteredList<>(allData);
        resultsTable.setItems(filteredData);

        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        section.getChildren().add(resultsTable);
        return section;
    }

    private void runReport() {
        String reportType = reportTypeCombo.getValue();
        if (reportType == null || reportType.isEmpty()) {
            AlertUtil.showWarning("Report", "Please select a report type");
            return;
        }

        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null) {
            AlertUtil.showWarning("Report", "Please select both from and to dates");
            return;
        }
        if (from.isAfter(to)) {
            AlertUtil.showWarning("Report", "From date cannot be after to date");
            return;
        }

        currentReportTitle = reportType;
        statusLabel.setText("Loading...");

        String party = partyCombo.isVisible() ? partyCombo.getValue() : null;
        String vehicle = vehicleCombo.isVisible() ? vehicleCombo.getValue() : null;

        AppExecutor.submit(() -> {
            try {
                SimpleReportResult result = reportDAO.generatePresetReport(reportType, from, to, party, vehicle);

                Platform.runLater(() -> {
                    currentHeaders = result.getColumnHeaders();
                    buildDynamicColumns(currentHeaders);
                    allData.clear();
                    allData.addAll(result.getRows());
                    searchField.clear();
                    applyFilter();

                    String totalLabel = result.getTotalAmount() != 0
                        ? String.format("  |  Total: %,.2f", result.getTotalAmount()) : "";
                    statusLabel.setText("Showing " + result.getRows().size() + " row(s)" + totalLabel);
                    statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e3a5f; -fx-font-weight: bold;");
                });
            } catch (Exception e) {
                log.error("Report generation failed", e);
                Platform.runLater(() -> {
                    AlertUtil.showError("Error", "Failed to generate report: " + e.getMessage());
                    statusLabel.setText("Error");
                    statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626;");
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void buildDynamicColumns(String[] headers) {
        resultsTable.getColumns().clear();

        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(param -> {
                String[] row = param.getValue();
                String val = colIndex < row.length ? row[colIndex] : "";
                return new javafx.beans.property.SimpleStringProperty(val);
            });

            // Right-align numeric columns (check if header suggests amount/number)
            String h = headers[i].toLowerCase();
            if (h.contains("amt") || h.contains("amount") || h.contains("gst") || h.contains("freight")
                || h.contains("advance") || h.contains("balance") || h.contains("total") || h.contains("net")
                || h.contains("debit") || h.contains("credit") || h.contains("purchase") || h.contains("sale")
                || h.contains("receipt") || h.contains("payment")) {
                col.setStyle("-fx-alignment: CENTER-RIGHT;");
            }

            if (h.equals("sr.no") || h.equals("sr no")) {
                col.setPrefWidth(50);
                col.setStyle("-fx-alignment: CENTER;");
            }

            resultsTable.getColumns().add(col);
        }
    }

    private void applyFilter() {
        String text = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        if (text.isEmpty()) {
            filteredData.setPredicate(null);
        } else {
            final String searchText = text;
            filteredData.setPredicate(row -> {
                for (String cell : row) {
                    if (cell != null && cell.toLowerCase().contains(searchText)) return true;
                }
                return false;
            });
        }

        int total = allData.size();
        int filtered = filteredData.size();
        if (total > 0) {
            String countText = filtered == total
                ? "Showing " + total + " row(s)"
                : "Showing " + filtered + " of " + total + " row(s)";
            statusLabel.setText(countText);
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e3a5f; -fx-font-weight: bold;");
        }
    }

    private void exportToExcel() {
        if (filteredData.isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName(currentReportTitle.replaceAll("\\s+", "_") + ".xlsx");

        Window window = resultsTable.getScene().getWindow();
        File file = fc.showSaveDialog(window);
        if (file == null) return;

        AppExecutor.submit(() -> {
            try {
                org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(currentReportTitle);

                org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                org.apache.poi.ss.usermodel.Font hf = workbook.createFont();
                hf.setBold(true);
                headerStyle.setFont(hf);

                org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                for (int i = 0; i < currentHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                    cell.setCellValue(currentHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowNum = 1;
                for (String[] row : filteredData) {
                    org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < row.length; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = dataRow.createCell(i);
                        try {
                            double val = Double.parseDouble(row[i]);
                            cell.setCellValue(val);
                        } catch (NumberFormatException e) {
                            cell.setCellValue(row[i] != null ? row[i] : "");
                        }
                    }
                }

                for (int i = 0; i < currentHeaders.length; i++) sheet.autoSizeColumn(i);

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    workbook.write(fos);
                }
                workbook.close();

                Platform.runLater(() -> AlertUtil.showInfo("Export", "Exported successfully to " + file.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Export Error", "Failed to export: " + e.getMessage()));
            }
        });
    }

    private void printToPdf() {
        if (filteredData.isEmpty()) {
            AlertUtil.showWarning("Print", "No data to print");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save as PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName(currentReportTitle.replaceAll("\\s+", "_") + ".pdf");

        Window window = resultsTable.getScene().getWindow();
        File file = fc.showSaveDialog(window);
        if (file == null) return;

        AppExecutor.submit(() -> {
            try {
                com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());
                com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                document.open();

                com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(currentReportTitle, titleFont);
                title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                document.add(title);

                com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA, 10);
                document.add(new com.lowagie.text.Paragraph(
                    "From: " + fromDate.getValue().format(DATE_FMT) + "  To: " + toDate.getValue().format(DATE_FMT), normalFont));
                document.add(new com.lowagie.text.Paragraph(" "));

                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(currentHeaders.length);
                table.setWidthPercentage(100);

                com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
                for (String h : currentHeaders) {
                    com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, headerFont));
                    cell.setBackgroundColor(Color.LIGHT_GRAY);
                    table.addCell(cell);
                }

                com.lowagie.text.Font dataFont = com.lowagie.text.FontFactory.getFont(FontFactory.HELVETICA, 8);
                for (String[] row : filteredData) {
                    for (int i = 0; i < currentHeaders.length; i++) {
                        String val = i < row.length && row[i] != null ? row[i] : "";
                        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(val, dataFont)));
                    }
                }

                document.add(table);
                document.close();

                Platform.runLater(() -> AlertUtil.showInfo("Print", "PDF saved to " + file.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Print Error", "Failed to generate PDF: " + e.getMessage()));
            }
        });
    }
}
