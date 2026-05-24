package com.accounting.ui.dialog;

import com.accounting.dao.InvoiceDAO;
import com.accounting.model.Invoice;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.ExportUtil;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class InvoiceRegisterDialog {

    private final InvoiceDAO dao = new InvoiceDAO();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private ComboBox<String> transTypeCombo;
    private ComboBox<String> searchByCombo;
    private ComboBox<String> orderWiseCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<String> cashCreditCombo;
    private TextField selectOneField;
    private TableView<Invoice> table;
    private ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();

    private Label totalRecordsLabel;
    private Label totalQtyLabel;
    private TextField totalField;

    private Stage stage;

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("Invoice Register");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(0);

        // Header
        Label header = new Label("INVOICE REGISTER");
        header.getStyleClass().add("section-header-red");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");
        closeBtn.setOnAction(e -> stage.close());

        HBox headerBar = new HBox(header, closeBtn);
        HBox.setHgrow(header, Priority.ALWAYS);
        headerBar.setAlignment(Pos.CENTER);
        headerBar.setStyle("-fx-background-color: #dc2626;");

        // Filter bar
        GridPane filters = new GridPane();
        filters.setHgap(10);
        filters.setVgap(8);
        filters.setPadding(new Insets(12));

        transTypeCombo = new ComboBox<>(FXCollections.observableArrayList("SALE", "PURCHASE", "All"));
        transTypeCombo.setValue("SALE");
        transTypeCombo.setPrefWidth(120);

        searchByCombo = new ComboBox<>(FXCollections.observableArrayList("Voucher_Type", "Account_Name"));
        searchByCombo.setValue("Voucher_Type");
        searchByCombo.setPrefWidth(130);

        orderWiseCombo = new ComboBox<>(FXCollections.observableArrayList("Date Wise", "Invoice No Wise"));
        orderWiseCombo.setValue("Date Wise");
        orderWiseCombo.setPrefWidth(120);

        fromDatePicker = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 1, 1));
        fromDatePicker.setPrefWidth(130);

        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(130);

        cashCreditCombo = new ComboBox<>(FXCollections.observableArrayList("All", "CASH", "CREDIT"));
        cashCreditCombo.setValue("All");
        cashCreditCombo.setPrefWidth(80);

        selectOneField = new TextField();
        selectOneField.setPrefWidth(200);
        selectOneField.setPromptText("T INVOICE");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> searchInvoices());

        Button excelBtn = new Button("Excel Export");
        excelBtn.setOnAction(e -> exportExcel());

        // Row 0
        filters.add(new Label("Trans Type"), 0, 0);
        filters.add(transTypeCombo, 1, 0);
        filters.add(new Label("Search By"), 2, 0);
        filters.add(searchByCombo, 3, 0);
        filters.add(new Label("Order Wise"), 4, 0);
        filters.add(orderWiseCombo, 5, 0);
        filters.add(new Label("From Date"), 6, 0);
        filters.add(fromDatePicker, 7, 0);

        // Row 1
        filters.add(new Label("Cash/Credit Type"), 0, 1);
        filters.add(cashCreditCombo, 1, 1);
        filters.add(new Label("Select One"), 2, 1);
        filters.add(selectOneField, 3, 1, 3, 1);
        filters.add(new Label("To Date"), 6, 1);
        filters.add(toDatePicker, 7, 1);

        HBox searchBox = new HBox(10, searchBtn, excelBtn);
        searchBox.setAlignment(Pos.CENTER_RIGHT);
        filters.add(searchBox, 8, 0, 1, 2);

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Footer
        totalRecordsLabel = new Label("Total Records : 0");
        totalQtyLabel = new Label("Total Qty : 0.000");
        totalField = new TextField("0.00");
        totalField.setEditable(false);
        totalField.setPrefWidth(120);

        Button settingBtn = new Button("Setting");

        HBox footer = new HBox(30, totalRecordsLabel, totalQtyLabel,
                new HBox(5, new Label("Total :"), totalField), settingBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.getStyleClass().add("footer-bar");

        root.getChildren().addAll(headerBar, filters, table, footer);

        Scene scene = new Scene(root, 1050, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        searchInvoices();
    }

    @SuppressWarnings("unchecked")
    private TableView<Invoice> buildTable() {
        TableView<Invoice> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Invoice, Boolean> selectCol = new TableColumn<>("");
        selectCol.setPrefWidth(30);
        selectCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : cb);
            }
        });

        TableColumn<Invoice, LocalDate> dateCol = new TableColumn<>("Invoice\nDate");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFmt));
            }
        });
        dateCol.setPrefWidth(100);

        TableColumn<Invoice, String> invNoCol = new TableColumn<>("Invoice No.");
        invNoCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
        invNoCol.setPrefWidth(90);

        TableColumn<Invoice, String> nameCol = new TableColumn<>("CustomerName");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        nameCol.setPrefWidth(250);

        TableColumn<Invoice, Double> qtyCol = new TableColumn<>("Total Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("totalQty"));
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.3f", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        qtyCol.setPrefWidth(80);

        TableColumn<Invoice, Double> amtCol = new TableColumn<>("Total Amt");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("totalAmt"));
        amtCol.setCellFactory(col -> numericCell());
        amtCol.setPrefWidth(100);

        TableColumn<Invoice, Double> taxCol = new TableColumn<>("Total Tax");
        taxCol.setCellValueFactory(new PropertyValueFactory<>("totalTax"));
        taxCol.setCellFactory(col -> numericCell());
        taxCol.setPrefWidth(80);

        TableColumn<Invoice, Double> grandCol = new TableColumn<>("Grand Total");
        grandCol.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        grandCol.setCellFactory(col -> numericCell());
        grandCol.setPrefWidth(100);

        TableColumn<Invoice, String> transCol = new TableColumn<>("Trans Type");
        transCol.setCellValueFactory(new PropertyValueFactory<>("transType"));
        transCol.setPrefWidth(80);

        TableColumn<Invoice, String> taxTypeCol = new TableColumn<>("Tax Type");
        taxTypeCol.setCellValueFactory(new PropertyValueFactory<>("taxType"));
        taxTypeCol.setPrefWidth(70);

        TableColumn<Invoice, String> vehCol = new TableColumn<>("Vehicle");
        vehCol.setCellValueFactory(new PropertyValueFactory<>("vehicleNo"));
        vehCol.setPrefWidth(80);

        tv.getColumns().addAll(selectCol, dateCol, invNoCol, nameCol, qtyCol, amtCol,
                taxCol, grandCol, transCol, taxTypeCol, vehCol);

        tv.setItems(invoiceList);
        return tv;
    }

    private TableCell<Invoice, Double> numericCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        };
    }

    private void searchInvoices() {
        String transType = transTypeCombo.getValue();
        String searchBy = searchByCombo.getValue();
        String selectOne = selectOneField.getText();
        String cashCredit = cashCreditCombo.getValue();
        String orderWise = orderWiseCombo.getValue();
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            AlertUtil.showWarning("Validation", "Please select both From and To dates.");
            return;
        }

        AppExecutor.submit(() -> {
            List<Invoice> results = dao.search(transType, searchBy, selectOne, cashCredit, orderWise, from, to);
            Platform.runLater(() -> {
                invoiceList.setAll(results);
                updateTotals();
            });
        });
    }

    private void updateTotals() {
        int count = invoiceList.size();
        double totalQty = invoiceList.stream().mapToDouble(Invoice::getTotalQty).sum();
        double totalAmt = invoiceList.stream().mapToDouble(Invoice::getGrandTotal).sum();

        totalRecordsLabel.setText("Total Records : " + count);
        totalQtyLabel.setText("Total Qty : " + String.format("%.3f", totalQty));
        totalField.setText(String.format("%.2f", totalAmt));
    }

    private void exportExcel() {
        if (invoiceList.isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Invoices to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("invoice_register.xlsx");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        AppExecutor.submit(() -> {
            try {
                ExportUtil.exportInvoicesToExcel(invoiceList, file.getAbsolutePath());
                Platform.runLater(() ->
                        NotificationUtil.showSuccess("Export", "Invoice register exported."));
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Export Failed", e.getMessage()));
            }
        });
    }
}
