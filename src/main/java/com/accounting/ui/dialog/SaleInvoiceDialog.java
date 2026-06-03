package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.PartyDAO;
import com.accounting.dao.SaleInvoiceDAO;
import com.accounting.dao.SettingsDAO;
import com.accounting.database.DBConnection;
import com.accounting.model.InvoiceLineItem;
import com.accounting.model.Party;
import com.accounting.model.SaleInvoice;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class SaleInvoiceDialog {

    private static final Logger log = AppLogger.get(SaleInvoiceDialog.class);
    private Stage stage;
    private SaleInvoice invoice;
    private ObservableList<InvoiceLineItem> lineItems = FXCollections.observableArrayList();
    private TableView<InvoiceLineItem> lineItemTable;
    private Label totalLabel;
    private TextField partyField;
    private ObservableList<Party> allCustomers = FXCollections.observableArrayList();
    private Popup customerPopup;
    private ListView<Party> customerListView;
    private TextField invoiceNoField;
    private DatePicker invoiceDatePicker;
    private ComboBox<String> voucherTypeCombo;
    private CheckBox sgstCheckBox;
    private CheckBox cgstCheckBox;
    private CheckBox igstCheckBox;
    private TextField sgstValueField;
    private TextField cgstValueField;
    private TextField igstValueField;
    private TextField remarksField;
    private TextField rcvrAddressField;
    private TextField rcvrContactField;
    private TextField rcvrGstinField;
    private ComboBox<String> creditDebitCombo;
    private TextField accountNameField;
    private TextField paidByField;
    private ComboBox<String> paymentModeCombo;
    private TextField bankNameField;
    private TextField bankAccountField;
    private TextField ifscCodeField;
    private Runnable onClose;

    public SaleInvoiceDialog() {
        this.invoice = new SaleInvoice();
    }

    public SaleInvoiceDialog(SaleInvoice invoice) {
        this.invoice = invoice;
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Sale Invoice");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Scene scene = new Scene(createContent(), 1200, 850);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        // Load invoice data if editing
        if (invoice.getId() > 0) {
            loadInvoiceData();
        } else {
            // New invoice - auto-generate invoice number
            generateNextInvoiceNumber();
        }
    }

    private void generateNextInvoiceNumber() {
        AppExecutor.submit(() -> {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                String startingNumberStr = settingsDAO.getSetting("global_invoice_starting_number");
                int startingNumber = startingNumberStr != null ? Integer.parseInt(startingNumberStr) : 1;

                // Get the last invoice number from all invoice/receipt tables
                int lastNumber = getLastGlobalInvoiceNumber();
                int nextNumber = Math.max(startingNumber, lastNumber + 1);

                final String invoiceNo = String.valueOf(nextNumber);
                Platform.runLater(() -> invoiceNoField.setText(invoiceNo));
            } catch (Exception e) {
                log.error("Failed to generate invoice number", e);
                Platform.runLater(() -> invoiceNoField.setText(""));
            }
        });
    }

    private int getLastGlobalInvoiceNumber() throws Exception {
        int maxNumber = 0;

        // Check purchase invoices
        String purchaseSql = "SELECT CAST(invoice_no AS INTEGER) as num FROM purchase_invoices WHERE invoice_no GLOB '^[0-9]+$' ORDER BY CAST(invoice_no AS INTEGER) DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(purchaseSql)) {
            if (rs.next()) {
                maxNumber = Math.max(maxNumber, rs.getInt("num"));
            }
        }

        // Check sale invoices
        String saleSql = "SELECT CAST(invoice_no AS INTEGER) as num FROM sale_invoices WHERE invoice_no GLOB '^[0-9]+$' ORDER BY CAST(invoice_no AS INTEGER) DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(saleSql)) {
            if (rs.next()) {
                maxNumber = Math.max(maxNumber, rs.getInt("num"));
            }
        }

        // Check purchase receipts
        String purchaseReceiptSql = "SELECT CAST(invoice_no AS INTEGER) as num FROM purchase_receipts WHERE invoice_no GLOB '^[0-9]+$' ORDER BY CAST(invoice_no AS INTEGER) DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(purchaseReceiptSql)) {
            if (rs.next()) {
                maxNumber = Math.max(maxNumber, rs.getInt("num"));
            }
        }

        // Check sale receipts
        String saleReceiptSql = "SELECT CAST(invoice_no AS INTEGER) as num FROM sale_receipts WHERE invoice_no GLOB '^[0-9]+$' ORDER BY CAST(invoice_no AS INTEGER) DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(saleReceiptSql)) {
            if (rs.next()) {
                maxNumber = Math.max(maxNumber, rs.getInt("num"));
            }
        }

        return maxNumber;
    }

    private void loadInvoiceData() {
        invoiceNoField.setText(invoice.getInvoiceNo());
        invoiceDatePicker.setValue(invoice.getInvoiceDate());
        voucherTypeCombo.setValue(invoice.getVoucherType());
        remarksField.setText(invoice.getRemarks());

        // Credit/Debit
        creditDebitCombo.setValue(invoice.getCreditDebit() != null ? invoice.getCreditDebit() : "Credit");

        // Account Name
        accountNameField.setText(invoice.getAccountName());

        // Paid by (Person name)
        paidByField.setText(invoice.getPaidBy());

        // Payment Mode
        paymentModeCombo.setValue(invoice.getPaymentMode() != null ? invoice.getPaymentMode() : "Cash");

        // Bank details
        bankNameField.setText(invoice.getBankName());
        bankAccountField.setText(invoice.getBankAccount());
        ifscCodeField.setText(invoice.getIfscCode());

        // Receiver details
        rcvrAddressField.setText(invoice.getRcvrAddress());
        rcvrContactField.setText(invoice.getRcvrContactNo());
        rcvrGstinField.setText(invoice.getRcvrGstin());

        // Set GST checkboxes based on values
        sgstCheckBox.setSelected(invoice.getSgstAmount() > 0);
        sgstValueField.setText(sgstCheckBox.isSelected() ? "9" : "0");
        cgstCheckBox.setSelected(invoice.getCgstAmount() > 0);
        cgstValueField.setText(cgstCheckBox.isSelected() ? "9" : "0");
        igstCheckBox.setSelected(invoice.getIgstAmount() > 0);
        igstValueField.setText(igstCheckBox.isSelected() ? "18" : "0");

        // Load line items
        lineItems.setAll(invoice.getLineItems());
        lineItemTable.setItems(lineItems);
        updateTotal();
    }

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        // Header section
        HBox headerBox = createHeaderSection();

        // Top row: Invoice details + GST
        HBox topRow = new HBox(12);
        GridPane invoiceDetailsGrid = createInvoiceDetailsGrid();
        GridPane gstGrid = createGstGrid();
        topRow.getChildren().addAll(invoiceDetailsGrid, gstGrid);
        HBox.setHgrow(invoiceDetailsGrid, Priority.ALWAYS);
        HBox.setHgrow(gstGrid, Priority.ALWAYS);

        // Line items table
        VBox lineItemsSection = createLineItemsSection();

        // Bottom row: Payment details + Receiver details
        HBox bottomRow = new HBox(12);
        GridPane paymentGrid = createPaymentGrid();
        GridPane receiverGrid = createReceiverGrid();
        bottomRow.getChildren().addAll(paymentGrid, receiverGrid);
        HBox.setHgrow(paymentGrid, Priority.ALWAYS);
        HBox.setHgrow(receiverGrid, Priority.ALWAYS);

        // Footer with totals and buttons
        HBox footerBox = createFooterSection();

        root.getChildren().addAll(headerBox, topRow, lineItemsSection, bottomRow, footerBox);
        VBox.setVgrow(lineItemsSection, Priority.ALWAYS);

        return root;
    }

    private HBox createHeaderSection() {
        Label title = new Label("SALE INVOICE");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
        
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));
        header.setBorder(new Border(new BorderStroke(
                javafx.scene.paint.Color.web("#e2e8f0"),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0)
        )));
        return header;
    }

    private GridPane createInvoiceDetailsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        invoiceNoField = new TextField();
        invoiceNoField.setPromptText("Auto-generated");
        invoiceNoField.setDisable(true);
        grid.add(label("Invoice No"), 0, 0);
        grid.add(invoiceNoField, 1, 0);

        invoiceDatePicker = new DatePicker(LocalDate.now());
        grid.add(label("Invoice Date"), 2, 0);
        grid.add(invoiceDatePicker, 3, 0);

        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "SALE", "SALE RETURN", "CREDIT NOTE"
        ));
        voucherTypeCombo.setValue("SALE");
        grid.add(label("Voucher Type"), 0, 1);
        grid.add(voucherTypeCombo, 1, 1);

        creditDebitCombo = new ComboBox<>(FXCollections.observableArrayList("Credit", "Debit"));
        creditDebitCombo.setValue("Credit");
        grid.add(label("Credit/Debit"), 2, 1);
        grid.add(creditDebitCombo, 3, 1);

        accountNameField = new TextField();
        setupUppercaseListener(accountNameField);
        grid.add(label("Account Name"), 0, 2);
        grid.add(accountNameField, 1, 2);

        partyField = new TextField();
        partyField.setPrefWidth(250);
        setupUppercaseListener(partyField);
        setupCustomerAutocomplete();
        grid.add(label("Customer"), 2, 2);
        grid.add(partyField, 3, 2);

        loadParties();
        return grid;
    }

    private GridPane createGstGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // GST Checkboxes
        sgstCheckBox = new CheckBox("SGST");
        sgstValueField = new TextField("0");
        sgstValueField.setPrefWidth(80);
        sgstValueField.setEditable(false);
        sgstCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            sgstValueField.setText(newVal ? "9" : "0");
            updateTotal();
        });
        HBox sgstBox = new HBox(10, sgstCheckBox, sgstValueField);
        grid.add(label("GST"), 0, 0);
        grid.add(sgstBox, 1, 0);

        cgstCheckBox = new CheckBox("CGST");
        cgstValueField = new TextField("0");
        cgstValueField.setPrefWidth(80);
        cgstValueField.setEditable(false);
        cgstCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            cgstValueField.setText(newVal ? "9" : "0");
            updateTotal();
        });
        HBox cgstBox = new HBox(10, cgstCheckBox, cgstValueField);
        grid.add(label(""), 2, 0);
        grid.add(cgstBox, 3, 0);

        igstCheckBox = new CheckBox("IGST");
        igstValueField = new TextField("0");
        igstValueField.setPrefWidth(80);
        igstValueField.setEditable(false);
        igstCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            igstValueField.setText(newVal ? "18" : "0");
            updateTotal();
        });
        HBox igstBox = new HBox(10, igstCheckBox, igstValueField);
        grid.add(label(""), 0, 1);
        grid.add(igstBox, 1, 1);

        return grid;
    }

    private GridPane createPaymentGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Paid by (Person name)
        paidByField = new TextField();
        setupUppercaseListener(paidByField);
        grid.add(label("Paid by"), 0, 0);
        grid.add(paidByField, 1, 0);

        // Payment Mode
        paymentModeCombo = new ComboBox<>(FXCollections.observableArrayList("Cash", "Bank Transfer", "UPI", "Cheque"));
        paymentModeCombo.setValue("Cash");
        grid.add(label("Mode"), 2, 0);
        grid.add(paymentModeCombo, 3, 0);

        // Bank Name
        bankNameField = new TextField();
        setupUppercaseListener(bankNameField);
        grid.add(label("Bank Name"), 0, 1);
        grid.add(bankNameField, 1, 1);

        // Bank Account
        bankAccountField = new TextField();
        setupUppercaseListener(bankAccountField);
        grid.add(label("Bank A/c"), 2, 1);
        grid.add(bankAccountField, 3, 1);

        // IFSC Code
        ifscCodeField = new TextField();
        setupUppercaseListener(ifscCodeField);
        grid.add(label("IFSC Code"), 0, 2);
        grid.add(ifscCodeField, 1, 2);

        // Remarks
        remarksField = new TextField();
        setupUppercaseListener(remarksField);
        grid.add(label("Remarks"), 2, 2);
        grid.add(remarksField, 3, 2);

        return grid;
    }

    private GridPane createReceiverGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Receiver Address
        rcvrAddressField = new TextField();
        setupUppercaseListener(rcvrAddressField);
        grid.add(label("Receiver Address"), 0, 0);
        grid.add(rcvrAddressField, 1, 0, 3, 1);

        // Contact Number
        rcvrContactField = new TextField();
        setupUppercaseListener(rcvrContactField);
        grid.add(label("Contact No"), 0, 1);
        grid.add(rcvrContactField, 1, 1);

        // GSTIN No.
        rcvrGstinField = new TextField();
        setupUppercaseListener(rcvrGstinField);
        grid.add(label("GSTIN"), 2, 1);
        grid.add(rcvrGstinField, 3, 1);

        return grid;
    }

    private VBox createLineItemsSection() {
        Label sectionTitle = new Label("Line Items");
        sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        // Input fields for new line item
        TextField lrNoField = new TextField();
        setupUppercaseListener(lrNoField);
        VBox lrNoBox = new VBox(4, new Label("LR No"), lrNoField);
        HBox.setHgrow(lrNoBox, Priority.ALWAYS);

        TextField containerNoField = new TextField();
        setupUppercaseListener(containerNoField);
        VBox containerBox = new VBox(4, new Label("Container No"), containerNoField);
        HBox.setHgrow(containerBox, Priority.ALWAYS);

        TextField vehicleNoField = new TextField();
        setupUppercaseListener(vehicleNoField);
        VBox vehicleBox = new VBox(4, new Label("Vehicle No"), vehicleNoField);
        HBox.setHgrow(vehicleBox, Priority.ALWAYS);

        TextField fromField = new TextField();
        setupUppercaseListener(fromField);
        VBox fromBox = new VBox(4, new Label("From"), fromField);
        HBox.setHgrow(fromBox, Priority.ALWAYS);

        TextField toField = new TextField();
        setupUppercaseListener(toField);
        VBox toBox = new VBox(4, new Label("To"), toField);
        HBox.setHgrow(toBox, Priority.ALWAYS);

        TextField typeField = new TextField();
        setupUppercaseListener(typeField);
        VBox typeBox = new VBox(4, new Label("Type"), typeField);
        HBox.setHgrow(typeBox, Priority.ALWAYS);

        TextField freightField = new TextField();
        freightField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("\\d*\\.?\\d*")) {
                freightField.setText(oldVal);
            }
        });
        VBox freightBox = new VBox(4, new Label("Basic Freight"), freightField);
        HBox.setHgrow(freightBox, Priority.ALWAYS);

        TextField detentionField = new TextField();
        detentionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("\\d*\\.?\\d*")) {
                detentionField.setText(oldVal);
            }
        });
        VBox detentionBox = new VBox(4, new Label("Detention Charge"), detentionField);
        HBox.setHgrow(detentionBox, Priority.ALWAYS);

        TextField otherChargesField = new TextField();
        otherChargesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("\\d*\\.?\\d*")) {
                otherChargesField.setText(oldVal);
            }
        });
        VBox otherChargesBox = new VBox(4, new Label("Other Charges"), otherChargesField);
        HBox.setHgrow(otherChargesBox, Priority.ALWAYS);

        Button addRowBtn = new Button("+ Add Row");
        addRowBtn.setStyle("-fx-padding: 4 12 4 12; -fx-font-size: 12px;");
        addRowBtn.setPrefHeight(35);
        addRowBtn.setMinWidth(90);
        addRowBtn.setWrapText(false);
        VBox.setVgrow(addRowBtn, Priority.ALWAYS);

        HBox inputRow = new HBox(8, lrNoBox, containerBox, vehicleBox, fromBox, toBox, typeBox, freightBox, detentionBox, otherChargesBox, addRowBtn);
        inputRow.setPadding(new Insets(12));
        inputRow.setAlignment(Pos.BOTTOM_CENTER);
        inputRow.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e2e8f0; -fx-border-radius: 4;");

        lineItemTable = new TableView<>(lineItems);
        lineItemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        lineItemTable.setEditable(false);

        // Sr. No. column (auto-filled, read-only)
        TableColumn<InvoiceLineItem, Integer> srNoCol = new TableColumn<>("Sr. No");
        srNoCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(lineItems.indexOf(cellData.getValue()) + 1).asObject());
        srNoCol.setPrefWidth(60);
        srNoCol.setEditable(false);

        // Date column (auto-filled, read-only)
        TableColumn<InvoiceLineItem, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);
        dateCol.setEditable(false);

        TableColumn<InvoiceLineItem, String> lrNoCol = new TableColumn<>("LR No");
        lrNoCol.setCellValueFactory(new PropertyValueFactory<>("lrNo"));
        lrNoCol.setCellFactory(TextFieldTableCell.forTableColumn());
        lrNoCol.setOnEditCommit(e -> e.getRowValue().setLrNo(e.getNewValue()));
        lrNoCol.setPrefWidth(80);
        lrNoCol.setEditable(true);

        TableColumn<InvoiceLineItem, String> containerCol = new TableColumn<>("Container No");
        containerCol.setCellValueFactory(new PropertyValueFactory<>("containerNo"));
        containerCol.setCellFactory(TextFieldTableCell.forTableColumn());
        containerCol.setOnEditCommit(e -> e.getRowValue().setContainerNo(e.getNewValue()));
        containerCol.setPrefWidth(100);
        containerCol.setEditable(true);

        TableColumn<InvoiceLineItem, String> vehicleCol = new TableColumn<>("Vehicle No");
        vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleNo"));
        vehicleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        vehicleCol.setOnEditCommit(e -> e.getRowValue().setVehicleNo(e.getNewValue()));
        vehicleCol.setPrefWidth(100);
        vehicleCol.setEditable(true);

        TableColumn<InvoiceLineItem, String> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("from"));
        fromCol.setCellFactory(TextFieldTableCell.forTableColumn());
        fromCol.setOnEditCommit(e -> e.getRowValue().setFrom(e.getNewValue()));
        fromCol.setPrefWidth(100);
        fromCol.setEditable(true);

        TableColumn<InvoiceLineItem, String> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(new PropertyValueFactory<>("to"));
        toCol.setCellFactory(TextFieldTableCell.forTableColumn());
        toCol.setOnEditCommit(e -> e.getRowValue().setTo(e.getNewValue()));
        toCol.setPrefWidth(100);
        toCol.setEditable(true);

        TableColumn<InvoiceLineItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        typeCol.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));
        typeCol.setPrefWidth(80);
        typeCol.setEditable(true);

        TableColumn<InvoiceLineItem, Double> freightCol = new TableColumn<>("Basic Freight");
        freightCol.setCellValueFactory(new PropertyValueFactory<>("basicFreight"));
        freightCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        freightCol.setOnEditCommit(e -> {
            e.getRowValue().setBasicFreight(e.getNewValue());
            updateTotal();
        });
        freightCol.setPrefWidth(100);
        freightCol.setEditable(true);

        TableColumn<InvoiceLineItem, Double> detentionCol = new TableColumn<>("Detention Charge");
        detentionCol.setCellValueFactory(new PropertyValueFactory<>("detentionCharge"));
        detentionCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        detentionCol.setOnEditCommit(e -> {
            e.getRowValue().setDetentionCharge(e.getNewValue());
            updateTotal();
        });
        detentionCol.setPrefWidth(120);
        detentionCol.setEditable(true);

        TableColumn<InvoiceLineItem, Double> otherChargesCol = new TableColumn<>("Other Charges");
        otherChargesCol.setCellValueFactory(new PropertyValueFactory<>("otherCharges"));
        otherChargesCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        otherChargesCol.setOnEditCommit(e -> {
            e.getRowValue().setOtherCharges(e.getNewValue());
            updateTotal();
        });
        otherChargesCol.setPrefWidth(100);
        otherChargesCol.setEditable(true);

        TableColumn<InvoiceLineItem, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);

        lineItemTable.getColumns().addAll(srNoCol, dateCol, lrNoCol, containerCol, vehicleCol, fromCol, toCol, typeCol,
                freightCol, detentionCol, otherChargesCol, totalCol);

        // Right-click context menu for deleting rows
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Row");
        deleteItem.setOnAction(e -> {
            InvoiceLineItem selected = lineItemTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                lineItems.remove(selected);
                updateTotal();
            }
        });
        contextMenu.getItems().add(deleteItem);

        lineItemTable.setRowFactory(tv -> {
            TableRow<InvoiceLineItem> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    lineItemTable.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, e.getScreenX(), e.getScreenY());
                }
                e.consume();
            });
            return row;
        });

        // Set up the Add Row button action
        addRowBtn.setOnAction(e -> {
            if (lineItems.size() >= 7) {
                AlertUtil.showWarning("Validation", "Maximum 7 line items are allowed per invoice");
                return;
            }

            InvoiceLineItem item = new InvoiceLineItem();
            item.setDate(java.time.LocalDate.now().toString());
            item.setLrNo(lrNoField.getText().trim());
            item.setContainerNo(containerNoField.getText().trim());
            item.setVehicleNo(vehicleNoField.getText().trim());
            item.setFrom(fromField.getText().trim());
            item.setTo(toField.getText().trim());
            item.setType(typeField.getText().trim());

            // Parse numeric fields (default to 0 if empty or invalid)
            double freight = 0;
            double detention = 0;
            try {
                if (!freightField.getText().trim().isEmpty()) {
                    freight = Double.parseDouble(freightField.getText().trim());
                }
            } catch (NumberFormatException ex) {
                // Ignore invalid input, default to 0
            }
            try {
                if (!detentionField.getText().trim().isEmpty()) {
                    detention = Double.parseDouble(detentionField.getText().trim());
                }
            } catch (NumberFormatException ex) {
                // Ignore invalid input, default to 0
            }
            double otherChg = 0;
            try {
                if (!otherChargesField.getText().trim().isEmpty()) {
                    otherChg = Double.parseDouble(otherChargesField.getText().trim());
                }
            } catch (NumberFormatException ex) {
                // Ignore invalid input, default to 0
            }
            item.setBasicFreight(freight);
            item.setDetentionCharge(detention);
            item.setOtherCharges(otherChg);

            lineItems.add(item);
            updateTotal();

            // Clear input fields
            lrNoField.clear();
            containerNoField.clear();
            vehicleNoField.clear();
            fromField.clear();
            toField.clear();
            typeField.clear();
            freightField.clear();
            detentionField.clear();
            otherChargesField.clear();
        });

        VBox section = new VBox(8, sectionTitle, inputRow, lineItemTable);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(lineItemTable, Priority.ALWAYS);
        return section;
    }

    private HBox createFooterSection() {
        Label totalLbl = new Label("Total Amount:");
        totalLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        totalLabel = new Label("0.00");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

        HBox totalsBox = new HBox(12, totalLbl, totalLabel);
        totalsBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(totalsBox, Priority.ALWAYS);

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveInvoice());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox buttonsBox = new HBox(10, saveBtn, closeBtn);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox footer = new HBox(20, totalsBox, buttonsBox);
        footer.setPadding(new Insets(12));
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return footer;
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<Party> customers = new PartyDAO().findByType("CUSTOMER");
                Platform.runLater(() -> {
                    allCustomers.setAll(customers);

                    // If editing, select the invoice's party
                    if (invoice.getId() > 0) {
                        for (Party p : customers) {
                            if (p.getId() == invoice.getPartyId()) {
                                partyField.setText(p.getName());
                                autofillFromParty(p);
                                break;
                            }
                        }
                    }
                    // No default selection for new invoices
                });
            } catch (Exception e) {
                log.error("Failed to load customers", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load customers"));
            }
        });
    }

    // Helper method to trigger auto-fill from party selection
    private void autofillFromParty(Party party) {
        if (party != null) {
            partyField.setText(party.getName());
            rcvrAddressField.setText(party.getAddress());
            rcvrContactField.setText(party.getMobile());
            rcvrGstinField.setText(party.getGstin());
            // Don't auto-fill paidBy, bank details - let user choose
        }
    }

    private void setupCustomerAutocomplete() {
        customerPopup = new Popup();
        customerPopup.setAutoHide(true);

        customerListView = new ListView<>();
        customerListView.setFocusTraversable(false);
        customerListView.setCellFactory(param -> new ListCell<Party>() {
            @Override
            protected void updateItem(Party party, boolean empty) {
                super.updateItem(party, empty);
                setText(empty || party == null ? "" : party.getName());
            }
        });

        customerPopup.getContent().add(customerListView);

        partyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                customerPopup.hide();
                return;
            }

            List<Party> filtered = allCustomers.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(newVal.toLowerCase()))
                    .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                    .collect(java.util.stream.Collectors.toList());

            if (filtered.isEmpty()) {
                customerPopup.hide();
                return;
            }

            customerListView.getItems().setAll(filtered);

            int visibleRows = Math.min(filtered.size(), 10);
            customerListView.setPrefHeight(visibleRows * 26 + 2);
            customerListView.setPrefWidth(partyField.getWidth());

            if (!customerPopup.isShowing()) {
                javafx.geometry.Point2D p = partyField.localToScreen(0, partyField.getHeight());
                if (p != null) {
                    customerPopup.show(partyField, p.getX(), p.getY());
                }
            }
        });

        // Mouse selection
        customerListView.setOnMouseClicked(e -> {
            Party selected = customerListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                partyField.setText(selected.getName());
                customerPopup.hide();
                autofillFromParty(selected);
            }
        });

        // Keyboard navigation
        partyField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN -> {
                    if (customerPopup.isShowing() && !customerListView.getItems().isEmpty()) {
                        customerListView.requestFocus();
                        if (customerListView.getSelectionModel().isEmpty()) {
                            customerListView.getSelectionModel().selectFirst();
                        }
                    }
                }
                case ESCAPE -> customerPopup.hide();
                case TAB -> {
                    if (customerPopup.isShowing()) {
                        Party selected = customerListView.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            partyField.setText(selected.getName());
                            autofillFromParty(selected);
                        }
                        customerPopup.hide();
                    }
                }
            }
        });

        customerListView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    Party selected = customerListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        partyField.setText(selected.getName());
                        autofillFromParty(selected);
                        partyField.requestFocus();
                    }
                    customerPopup.hide();
                }
                case ESCAPE -> {
                    customerPopup.hide();
                    partyField.requestFocus();
                }
            }
        });
    }

    private void updateTotal() {
        double taxable = lineItems.stream().mapToDouble(InvoiceLineItem::getTotal).sum();
        double sgst = sgstCheckBox.isSelected() ? taxable * 0.09 : 0;
        double cgst = cgstCheckBox.isSelected() ? taxable * 0.09 : 0;
        double igst = igstCheckBox.isSelected() ? taxable * 0.18 : 0;
        double totalGst = sgst + cgst + igst;
        double netAmount = taxable + totalGst;

        totalLabel.setText(String.format("%.2f", netAmount));
    }

    private void saveInvoice() {
        String customerName = partyField.getText();
        if (customerName == null || customerName.trim().isEmpty()) {
            AlertUtil.showWarning("Validation", "Please select a customer");
            return;
        }

        // Find the party by name
        Party selectedParty = allCustomers.stream()
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(customerName.trim()))
                .findFirst()
                .orElse(null);

        if (selectedParty == null) {
            AlertUtil.showWarning("Validation", "Invalid customer selected");
            return;
        }

        if (lineItems.isEmpty()) {
            AlertUtil.showWarning("Validation", "Please add at least one line item");
            return;
        }

        if (lineItems.size() > 7) {
            AlertUtil.showWarning("Validation", "Maximum 7 line items are allowed per invoice");
            return;
        }

        invoice.setInvoiceDate(invoiceDatePicker.getValue());
        invoice.setInvoiceNo(invoiceNoField.getText());
        invoice.setPartyId(selectedParty.getId());
        invoice.setPartyName(selectedParty.getName());
        invoice.setVoucherType(voucherTypeCombo.getValue());
        invoice.setRemarks(remarksField.getText());
        invoice.setRcvrAddress(rcvrAddressField.getText());
        invoice.setRcvrContactNo(rcvrContactField.getText());
        invoice.setRcvrGstin(rcvrGstinField.getText());
        invoice.setCreditDebit(creditDebitCombo.getValue());
        invoice.setAccountName(accountNameField.getText());
        invoice.setPaidBy(paidByField.getText());
        invoice.setPaymentMode(paymentModeCombo.getValue());
        invoice.setBankName(bankNameField.getText());
        invoice.setBankAccount(bankAccountField.getText());
        invoice.setIfscCode(ifscCodeField.getText());
        invoice.setLineItems(new java.util.ArrayList<>(lineItems));
        invoice.setStatus("SAVED");

        double total = lineItems.stream().mapToDouble(InvoiceLineItem::getTotal).sum();
        invoice.setTaxableAmount(total);

        // Calculate GST amounts based on checkbox states
        double sgst = sgstCheckBox.isSelected() ? total * 0.09 : 0;
        double cgst = cgstCheckBox.isSelected() ? total * 0.09 : 0;
        double igst = igstCheckBox.isSelected() ? total * 0.18 : 0;
        double totalGst = sgst + cgst + igst;

        invoice.setSgstAmount(sgst);
        invoice.setCgstAmount(cgst);
        invoice.setIgstAmount(igst);
        invoice.setTotalGst(totalGst);
        invoice.setNetAmount(total + totalGst);

        AppExecutor.submit(() -> {
            try {
                SaleInvoiceDAO dao = new SaleInvoiceDAO();
                if (invoice.getId() > 0) {
                    dao.update(invoice);
                } else {
                    dao.save(invoice);

                    // Update the next invoice number in settings
                    try {
                        int currentInvoiceNo = Integer.parseInt(invoice.getInvoiceNo());
                        new SettingsDAO().saveSetting("global_invoice_starting_number", String.valueOf(currentInvoiceNo + 1));
                    } catch (Exception e) {
                        log.error("Failed to update invoice number in settings", e);
                    }
                }
                Platform.runLater(() -> {
                    NotificationUtil.showSuccess("Success", "Invoice saved successfully");
                    stage.close();
                });
            } catch (Exception e) {
                log.error("Failed to save invoice", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save invoice: " + e.getMessage()));
            }
        });
    }

    private void printInvoice() {
        if (invoice.getId() == 0) {
            AlertUtil.showWarning("Warning", "Please save the invoice before printing");
            return;
        }

        try {
            // Create invoices directory if it doesn't exist
            java.io.File invoicesDir = new java.io.File("invoices");
            if (!invoicesDir.exists()) {
                invoicesDir.mkdirs();
            }

            // Generate PDF file name
            String fileName = "invoices/Sale_Invoice_" + invoice.getInvoiceNo() + "_" + 
                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            // Generate Original PDF
            com.accounting.util.InvoicePDFGenerator.generateSaleInvoicePDF(invoice, fileName, "Original");

            // Show print preview embedded in app; Close returns to sale invoice list
            stage.close();
            new PrintPreviewDialog(fileName, (copyLabel, outputPath) ->
                    com.accounting.util.InvoicePDFGenerator.generateSaleInvoicePDF(invoice, outputPath, copyLabel)
            ).showInApp(() -> MainApp.showContentInApp(new SaleInvoiceListView().createContent()));
        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            AlertUtil.showError("Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        return lbl;
    }

    private void setupUppercaseListener(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                // Convert to uppercase, preserving cursor position
                int caretPosition = textField.getCaretPosition();
                textField.setText(newVal.toUpperCase());
                textField.positionCaret(Math.min(caretPosition, textField.getLength()));
            }
        });
    }
}
