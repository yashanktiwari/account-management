package com.accounting.ui.dialog;

import com.accounting.dao.ReportDAO;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportView {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final ReportDAO reportDAO = new ReportDAO();
    private final ObservableList<ReportDAO.ReportRow> reportData = FXCollections.observableArrayList();
    private TableView<ReportDAO.ReportRow> resultTable;
    private ComboBox<String> reportTypeCombo;
    private DatePicker fromDate;
    private DatePicker toDate;
    private ComboBox<String> partyCombo;
    private ComboBox<String> vehicleCombo;
    private Label openingBalanceLabel;
    private Label closingBalanceLabel;

    public Parent createContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        // Title
        Label title = new Label("Reports");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        // Filter Section
        VBox filterSection = buildFilterSection();

        // Balance Summary Section
        HBox balanceSection = buildBalanceSection();

        // Results Table
        VBox tableSection = buildTableSection();

        root.getChildren().addAll(title, filterSection, balanceSection, tableSection);

        // Load initial data
        loadParties();
        loadVehicles();

        return root;
    }

    private VBox buildFilterSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        Label sectionTitle = new Label("Report Filters");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));

        // Report Type
        Label reportTypeLabel = new Label("Report Type:");
        reportTypeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        reportTypeCombo = new ComboBox<>();
        reportTypeCombo.getItems().addAll(
            "Party Transactions",
            "Vehicle Transactions",
            "GST Summary",
            "Invoice Summary",
            "Receipt Summary",
            "Loading Slip Summary",
            "Lorry Receipt Summary"
        );
        reportTypeCombo.setValue("Party Transactions");
        reportTypeCombo.setPrefWidth(200);
        reportTypeCombo.setOnAction(e -> updateFilterVisibility());

        // Date Range
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
        Label partyLabel = new Label("Party:");
        partyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        partyCombo = new ComboBox<>();
        partyCombo.setPrefWidth(200);
        partyCombo.setPromptText("All Parties");

        // Vehicle Filter
        Label vehicleLabel = new Label("Vehicle:");
        vehicleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        vehicleCombo = new ComboBox<>();
        vehicleCombo.setPrefWidth(200);
        vehicleCombo.setPromptText("All Vehicles");

        // Add to grid
        grid.add(reportTypeLabel, 0, 0);
        grid.add(reportTypeCombo, 1, 0);
        grid.add(fromDateLabel, 2, 0);
        grid.add(fromDate, 3, 0);
        grid.add(toDateLabel, 4, 0);
        grid.add(toDate, 5, 0);

        grid.add(partyLabel, 0, 1);
        grid.add(partyCombo, 1, 1);
        grid.add(vehicleLabel, 2, 1);
        grid.add(vehicleCombo, 3, 1);

        // Generate Button
        Button generateBtn = new Button("Generate Report");
        generateBtn.getStyleClass().add("primary-button");
        generateBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        generateBtn.setOnAction(e -> generateReport());

        Button exportBtn = new Button("Export");
        exportBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        exportBtn.setOnAction(e -> exportReport());

        HBox buttonBox = new HBox(10, generateBtn, exportBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(buttonBox, 0, 2, 6, 1);

        section.getChildren().addAll(sectionTitle, grid);
        return section;
    }

    private HBox buildBalanceSection() {
        HBox section = new HBox(20);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        section.setAlignment(Pos.CENTER_LEFT);

        Label openingLabel = new Label("Opening Balance:");
        openingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        openingBalanceLabel = new Label("0.00");
        openingBalanceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label closingLabel = new Label("Closing Balance:");
        closingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        closingBalanceLabel = new Label("0.00");
        closingBalanceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        section.getChildren().addAll(openingLabel, openingBalanceLabel, new Separator(Orientation.VERTICAL), closingLabel, closingBalanceLabel);
        return section;
    }

    private VBox buildTableSection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(section, Priority.ALWAYS);

        Label sectionTitle = new Label("Report Results");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        resultTable = new TableView<>();
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        resultTable.setItems(reportData);

        // Initial columns (will be updated based on report type)
        setupTableColumns("Party Transactions");

        VBox.setVgrow(resultTable, Priority.ALWAYS);

        section.getChildren().addAll(sectionTitle, resultTable);
        return section;
    }

    private void setupTableColumns(String reportType) {
        resultTable.getColumns().clear();

        switch (reportType) {
            case "Party Transactions":
                TableColumn<ReportDAO.ReportRow, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
                dateCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> partyCol = new TableColumn<>("Party");
                partyCol.setCellValueFactory(new PropertyValueFactory<>("party"));
                partyCol.setPrefWidth(200);

                TableColumn<ReportDAO.ReportRow, String> typeCol = new TableColumn<>("Type");
                typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
                typeCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> refCol = new TableColumn<>("Reference");
                refCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
                refCol.setPrefWidth(120);

                TableColumn<ReportDAO.ReportRow, Double> debitCol = new TableColumn<>("Debit");
                debitCol.setCellValueFactory(new PropertyValueFactory<>("debit"));
                debitCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null || item == 0 ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                debitCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, Double> creditCol = new TableColumn<>("Credit");
                creditCol.setCellValueFactory(new PropertyValueFactory<>("credit"));
                creditCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null || item == 0 ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                creditCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, Double> balanceCol = new TableColumn<>("Balance");
                balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
                balanceCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                balanceCol.setPrefWidth(100);

                resultTable.getColumns().addAll(dateCol, partyCol, typeCol, refCol, debitCol, creditCol, balanceCol);
                break;

            case "Vehicle Transactions":
                TableColumn<ReportDAO.ReportRow, String> vDateCol = new TableColumn<>("Date");
                vDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
                vDateCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> vehicleCol = new TableColumn<>("Vehicle");
                vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
                vehicleCol.setPrefWidth(150);

                TableColumn<ReportDAO.ReportRow, String> vTypeCol = new TableColumn<>("Type");
                vTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
                vTypeCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> vRefCol = new TableColumn<>("Reference");
                vRefCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
                vRefCol.setPrefWidth(120);

                TableColumn<ReportDAO.ReportRow, Double> vAmountCol = new TableColumn<>("Amount");
                vAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
                vAmountCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null || item == 0 ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                vAmountCol.setPrefWidth(100);

                resultTable.getColumns().addAll(vDateCol, vehicleCol, vTypeCol, vRefCol, vAmountCol);
                break;

            case "GST Summary":
                TableColumn<ReportDAO.ReportRow, String> gstDateCol = new TableColumn<>("Date");
                gstDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
                gstDateCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> gstPartyCol = new TableColumn<>("Party");
                gstPartyCol.setCellValueFactory(new PropertyValueFactory<>("party"));
                gstPartyCol.setPrefWidth(200);

                TableColumn<ReportDAO.ReportRow, String> gstTypeCol = new TableColumn<>("Type");
                gstTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
                gstTypeCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> gstinCol = new TableColumn<>("GSTIN");
                gstinCol.setCellValueFactory(new PropertyValueFactory<>("gstin"));
                gstinCol.setPrefWidth(150);

                TableColumn<ReportDAO.ReportRow, Double> gstAmountCol = new TableColumn<>("Taxable Amount");
                gstAmountCol.setCellValueFactory(new PropertyValueFactory<>("taxableAmount"));
                gstAmountCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null || item == 0 ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                gstAmountCol.setPrefWidth(120);

                TableColumn<ReportDAO.ReportRow, Double> gstPaidCol = new TableColumn<>("GST Paid");
                gstPaidCol.setCellValueFactory(new PropertyValueFactory<>("gstPaid"));
                gstPaidCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null || item == 0 ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                gstPaidCol.setPrefWidth(100);

                resultTable.getColumns().addAll(gstDateCol, gstPartyCol, gstTypeCol, gstinCol, gstAmountCol, gstPaidCol);
                break;

            default:
                // Generic columns for other report types
                TableColumn<ReportDAO.ReportRow, String> gDateCol = new TableColumn<>("Date");
                gDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
                gDateCol.setPrefWidth(100);

                TableColumn<ReportDAO.ReportRow, String> gRefCol = new TableColumn<>("Reference");
                gRefCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
                gRefCol.setPrefWidth(150);

                TableColumn<ReportDAO.ReportRow, String> gDescCol = new TableColumn<>("Description");
                gDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));
                gDescCol.setPrefWidth(200);

                TableColumn<ReportDAO.ReportRow, Double> gAmountCol = new TableColumn<>("Amount");
                gAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
                gAmountCol.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null || item == 0 ? null : String.format("%.2f", item));
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                });
                gAmountCol.setPrefWidth(100);

                resultTable.getColumns().addAll(gDateCol, gRefCol, gDescCol, gAmountCol);
        }
    }

    private void updateFilterVisibility() {
        String reportType = reportTypeCombo.getValue();
        if (reportType != null) {
            partyCombo.setVisible(reportType.equals("Party Transactions") || reportType.equals("GST Summary"));
            vehicleCombo.setVisible(reportType.equals("Vehicle Transactions"));
            setupTableColumns(reportType);
        }
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<String> parties = reportDAO.getAllParties();
                Platform.runLater(() -> {
                    partyCombo.getItems().clear();
                    partyCombo.getItems().add("All Parties");
                    partyCombo.getItems().addAll(parties);
                    partyCombo.setValue("All Parties");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load parties: " + e.getMessage()));
            }
        });
    }

    private void loadVehicles() {
        AppExecutor.submit(() -> {
            try {
                List<String> vehicles = reportDAO.getAllVehicles();
                Platform.runLater(() -> {
                    vehicleCombo.getItems().clear();
                    vehicleCombo.getItems().add("All Vehicles");
                    vehicleCombo.getItems().addAll(vehicles);
                    vehicleCombo.setValue("All Vehicles");
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load vehicles: " + e.getMessage()));
            }
        });
    }

    private void generateReport() {
        String reportType = reportTypeCombo.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        String party = partyCombo.getValue();
        String vehicle = vehicleCombo.getValue();

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
                ReportDAO.ReportResult result = reportDAO.generateReport(
                    reportType,
                    from,
                    to,
                    party != null && party.equals("All Parties") ? null : party,
                    vehicle != null && vehicle.equals("All Vehicles") ? null : vehicle
                );

                Platform.runLater(() -> {
                    reportData.clear();
                    reportData.addAll(result.getRows());
                    openingBalanceLabel.setText(String.format("%.2f", result.getOpeningBalance()));
                    closingBalanceLabel.setText(String.format("%.2f", result.getClosingBalance()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to generate report: " + e.getMessage()));
            }
        });
    }

    private void exportReport() {
        AlertUtil.showInfo("Export", "Export functionality will be implemented in the next phase.");
    }
}
