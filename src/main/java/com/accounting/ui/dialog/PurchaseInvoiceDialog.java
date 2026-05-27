package com.accounting.ui.dialog;

import com.accounting.dao.PartyDAO;
import com.accounting.dao.PurchaseInvoiceDAO;
import com.accounting.model.InvoiceLineItem;
import com.accounting.model.Party;
import com.accounting.model.PurchaseInvoice;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
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
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.StringConverter;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleIntegerProperty;

public class PurchaseInvoiceDialog {

    private static final Logger log = AppLogger.get(PurchaseInvoiceDialog.class);
    private Stage stage;
    private PurchaseInvoice invoice;
    private ObservableList<InvoiceLineItem> lineItems = FXCollections.observableArrayList();
    private TableView<InvoiceLineItem> lineItemTable;
    private Label totalLabel;
    private ComboBox<Party> partyCombo;
    private TextField invoiceNoField;
    private DatePicker invoiceDatePicker;
    private ComboBox<String> voucherTypeCombo;
    private TextField remarksField;
    private CheckBox sgstCheckBox;
    private CheckBox cgstCheckBox;
    private CheckBox igstCheckBox;
    private TextField sgstValueField;
    private TextField cgstValueField;
    private TextField igstValueField;
    private Label taxableValueLabel;
    private Label gstValueLabel;
    private ComboBox<String> creditDebitCombo;
    private TextField accountNameField;
    private ComboBox<String> paidByCombo;
    private TextField bankNameField;
    private TextField bankAccountField;
    private TextField ifscCodeField;
    private TextField supplierAddressField;
    private TextField supplierContactNumberField;
    private TextField supplierGstNoField;
    private TextField supplierSearchField;
    private ObservableList<Party> allParties = FXCollections.observableArrayList();
    private ObservableList<Party> filteredParties = FXCollections.observableArrayList();
    private Timer searchTimer;
    private Runnable onClose;

    public PurchaseInvoiceDialog() {
        this.invoice = new PurchaseInvoice();
    }

    public PurchaseInvoiceDialog(PurchaseInvoice invoice) {
        this.invoice = invoice;
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Purchase Invoice");
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

        // Bottom row: Payment details + Supplier details
        HBox bottomRow = new HBox(12);
        GridPane paymentGrid = createPaymentGrid();
        GridPane supplierGrid = createSupplierGrid();
        bottomRow.getChildren().addAll(paymentGrid, supplierGrid);
        HBox.setHgrow(paymentGrid, Priority.ALWAYS);
        HBox.setHgrow(supplierGrid, Priority.ALWAYS);

        // Footer with totals and buttons
        HBox footerBox = createFooterSection();

        root.getChildren().addAll(headerBox, topRow, lineItemsSection, bottomRow, footerBox);
        VBox.setVgrow(lineItemsSection, Priority.ALWAYS);

        // Load existing invoice data if editing
        loadInvoiceData();

        return root;
    }

    private void loadInvoiceData() {
        if (invoice.getId() > 0) {
            // Editing existing invoice
            invoiceNoField.setText(invoice.getInvoiceNo());
            invoiceDatePicker.setValue(invoice.getInvoiceDate());
            voucherTypeCombo.setValue(invoice.getVoucherType());
            remarksField.setText(invoice.getRemarks());
            
            // Credit/Debit
            creditDebitCombo.setValue(invoice.getCreditDebit() != null ? invoice.getCreditDebit() : "Debit");
            
            // Account Name
            accountNameField.setText(invoice.getAccountName());
            
            // Paid by
            paidByCombo.setValue(invoice.getPaidBy());
            
            // Bank details
            bankNameField.setText(invoice.getBankName());
            bankAccountField.setText(invoice.getBankAccount());
            ifscCodeField.setText(invoice.getIfscCode());
            
            // Supplier details
            supplierAddressField.setText(invoice.getSupplierAddress());
            supplierContactNumberField.setText(invoice.getSupplierContactNumber());
            supplierGstNoField.setText(invoice.getSupplierGstNo());
            
            // Set GST checkboxes based on values
            sgstCheckBox.setSelected(invoice.getSgstAmount() > 0);
            sgstValueField.setText(String.valueOf((int) invoice.getSgstAmount()));
            cgstCheckBox.setSelected(invoice.getCgstAmount() > 0);
            cgstValueField.setText(String.valueOf((int) invoice.getCgstAmount()));
            igstCheckBox.setSelected(invoice.getIgstAmount() > 0);
            igstValueField.setText(String.valueOf((int) invoice.getIgstAmount()));
            
            // Load line items
            lineItems.setAll(invoice.getLineItems());
            lineItemTable.setItems(lineItems);
            updateTotal();
        }
    }

    // Helper method to trigger auto-fill from party selection
    private void autofillFromParty(Party party) {
        if (party != null) {
            supplierAddressField.setText(party.getAddress());
            supplierContactNumberField.setText(party.getMobile());
            supplierGstNoField.setText(party.getGstin());
            // Don't auto-fill paidBy - let user choose
            bankNameField.setText(party.getBankName());
            bankAccountField.setText(party.getBankAccount());
            ifscCodeField.setText(party.getIfscCode());
        }
    }

    private void filterSuppliers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredParties.setAll(allParties);
        } else {
            String lower = searchText.toLowerCase();
            filteredParties.setAll(allParties.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(lower))
                    .collect(java.util.stream.Collectors.toList()));
        }
    }

    private HBox createHeaderSection() {
        Label title = new Label("PURCHASE INVOICE");
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

        // Invoice No
        invoiceNoField = new TextField();
        invoiceNoField.setPromptText("Auto-generated");
        invoiceNoField.setDisable(true);
        grid.add(label("Invoice No"), 0, 0);
        grid.add(invoiceNoField, 1, 0);

        // Invoice Date
        invoiceDatePicker = new DatePicker(LocalDate.now());
        grid.add(label("Invoice Date"), 2, 0);
        grid.add(invoiceDatePicker, 3, 0);

        // Voucher Type
        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "PURCHASE", "PURCHASE RETURN", "DEBIT NOTE"
        ));
        voucherTypeCombo.setValue("PURCHASE");
        grid.add(label("Voucher Type"), 0, 1);
        grid.add(voucherTypeCombo, 1, 1);

        // Credit/Debit
        creditDebitCombo = new ComboBox<>(FXCollections.observableArrayList("Credit", "Debit"));
        creditDebitCombo.setValue("Debit");
        grid.add(label("Credit/Debit"), 2, 1);
        grid.add(creditDebitCombo, 3, 1);

        // Account Name
        accountNameField = new TextField();
        grid.add(label("Account Name"), 0, 2);
        grid.add(accountNameField, 1, 2);

        // Party (Supplier) with search
        VBox supplierBox = new VBox(4);
        supplierSearchField = new TextField();
        supplierSearchField.setPromptText("Search supplier...");
        partyCombo = new ComboBox<>();
        partyCombo.setPrefWidth(250);
        partyCombo.setItems(filteredParties);
        partyCombo.setCellFactory(param -> new ListCell<Party>() {
            @Override
            protected void updateItem(Party party, boolean empty) {
                super.updateItem(party, empty);
                setText(empty || party == null ? "" : party.getName());
            }
        });
        partyCombo.setConverter(new StringConverter<Party>() {
            @Override
            public String toString(Party party) {
                return party == null ? "" : party.getName();
            }
            @Override
            public Party fromString(String string) {
                return null;
            }
        });

        // Search debouncing
        supplierSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (searchTimer != null) {
                searchTimer.cancel();
            }
            searchTimer = new Timer();
            searchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> filterSuppliers(newVal));
                }
            }, 300);
        });

        supplierBox.getChildren().addAll(supplierSearchField, partyCombo);
        grid.add(label("Supplier"), 2, 2);
        grid.add(supplierBox, 3, 2);

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

        // Paid by
        paidByCombo = new ComboBox<>(FXCollections.observableArrayList("Cash", "Bank Transfer", "UPI", "Cheque"));
        grid.add(label("Paid by"), 0, 0);
        grid.add(paidByCombo, 1, 0);

        // Bank Name
        bankNameField = new TextField();
        grid.add(label("Bank Name"), 2, 0);
        grid.add(bankNameField, 3, 0);

        // Bank Account
        bankAccountField = new TextField();
        grid.add(label("Bank A/c"), 0, 1);
        grid.add(bankAccountField, 1, 1);

        // IFSC Code
        ifscCodeField = new TextField();
        grid.add(label("IFSC Code"), 2, 1);
        grid.add(ifscCodeField, 3, 1);

        // Remarks
        remarksField = new TextField();
        grid.add(label("Remarks"), 0, 2);
        grid.add(remarksField, 1, 2, 3, 1);

        return grid;
    }

    private GridPane createSupplierGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Supplier Address
        supplierAddressField = new TextField();
        supplierAddressField.setPrefWidth(300);
        grid.add(label("Supplier Address"), 0, 0);
        grid.add(supplierAddressField, 1, 0, 3, 1);

        // Contact Number
        supplierContactNumberField = new TextField();
        supplierContactNumberField.setPrefWidth(150);
        grid.add(label("Contact Number"), 0, 1);
        grid.add(supplierContactNumberField, 1, 1);

        // GSTIN No.
        supplierGstNoField = new TextField();
        supplierGstNoField.setPrefWidth(150);
        grid.add(label("GSTIN No."), 2, 1);
        grid.add(supplierGstNoField, 3, 1);

        return grid;
    }

    private VBox createLineItemsSection() {
        Label sectionTitle = new Label("Line Items");
        sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        lineItemTable = new TableView<>(lineItems);
        lineItemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        lineItemTable.setEditable(false);

        // Sr. No. column (auto-filled, read-only)
        TableColumn<InvoiceLineItem, Integer> srNoCol = new TableColumn<>("Sr. No");
        srNoCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(lineItems.indexOf(cellData.getValue()) + 1).asObject());
        srNoCol.setPrefWidth(60);

        // Date column (auto-filled, read-only)
        TableColumn<InvoiceLineItem, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> lrNoCol = new TableColumn<>("LR No");
        lrNoCol.setCellValueFactory(new PropertyValueFactory<>("lrNo"));
        lrNoCol.setPrefWidth(80);

        TableColumn<InvoiceLineItem, String> containerCol = new TableColumn<>("Container No");
        containerCol.setCellValueFactory(new PropertyValueFactory<>("containerNo"));
        containerCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> vehicleCol = new TableColumn<>("Vehicle No");
        vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleNo"));
        vehicleCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("from"));
        fromCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(new PropertyValueFactory<>("to"));
        toCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);

        TableColumn<InvoiceLineItem, Double> freightCol = new TableColumn<>("Basic Freight");
        freightCol.setCellValueFactory(new PropertyValueFactory<>("basicFreight"));
        freightCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, Double> detentionCol = new TableColumn<>("Detention Charge");
        detentionCol.setCellValueFactory(new PropertyValueFactory<>("detentionCharge"));
        detentionCol.setPrefWidth(120);

        TableColumn<InvoiceLineItem, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);

        lineItemTable.getColumns().addAll(srNoCol, dateCol, lrNoCol, containerCol, vehicleCol, fromCol, toCol, typeCol,
                freightCol, detentionCol, totalCol);

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

        Button addRowBtn = new Button("+ Add Row");
        addRowBtn.setStyle("-fx-padding: 6 12 6 12; -fx-font-size: 12px;");
        addRowBtn.setOnAction(e -> showAddLineItemDialog());

        VBox section = new VBox(8, sectionTitle, addRowBtn, lineItemTable);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(lineItemTable, Priority.ALWAYS);
        return section;
    }

    private void showAddLineItemDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Add Line Item");
        dialogStage.initOwner(stage);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField lrNoField = new TextField();
        TextField containerField = new TextField();
        TextField vehicleField = new TextField();
        TextField fromField = new TextField();
        TextField toField = new TextField();
        TextField typeField = new TextField();
        TextField freightField = new TextField();
        TextField detentionField = new TextField();

        grid.add(new Label("LR No:"), 0, 0);
        grid.add(lrNoField, 1, 0);
        grid.add(new Label("Container No:"), 2, 0);
        grid.add(containerField, 3, 0);
        grid.add(new Label("Vehicle No:"), 0, 1);
        grid.add(vehicleField, 1, 1);
        grid.add(new Label("From:"), 2, 1);
        grid.add(fromField, 3, 1);
        grid.add(new Label("To:"), 0, 2);
        grid.add(toField, 1, 2);
        grid.add(new Label("Type:"), 2, 2);
        grid.add(typeField, 3, 2);
        grid.add(new Label("Basic Freight:"), 0, 3);
        grid.add(freightField, 1, 3);
        grid.add(new Label("Detention Charge:"), 2, 3);
        grid.add(detentionField, 3, 3);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 6, 2, 1);

        saveBtn.setOnAction(e -> {
            try {
                double freight = freightField.getText().isEmpty() ? 0 : Double.parseDouble(freightField.getText());
                double detention = detentionField.getText().isEmpty() ? 0 : Double.parseDouble(detentionField.getText());

                InvoiceLineItem item = new InvoiceLineItem();
                item.setDate(java.time.LocalDate.now().toString());
                item.setLrNo(lrNoField.getText());
                item.setContainerNo(containerField.getText());
                item.setVehicleNo(vehicleField.getText());
                item.setFrom(fromField.getText());
                item.setTo(toField.getText());
                item.setType(typeField.getText());
                item.setBasicFreight(freight);
                item.setDetentionCharge(detention);
                item.setTotal(freight + detention);

                lineItems.add(item);
                updateTotal();
                dialogStage.close();
            } catch (NumberFormatException ex) {
                AlertUtil.showError("Error", "Please enter valid numbers for freight and detention charges");
            }
        });

        cancelBtn.setOnAction(e -> dialogStage.close());

        Scene scene = new Scene(grid, 500, 350);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private HBox createFooterSection() {
        Label taxableLbl = new Label("Taxable Amount:");
        taxableLbl.setStyle("-fx-font-size: 12px;");
        taxableValueLabel = new Label("0.00");
        taxableValueLabel.setStyle("-fx-font-size: 12px;");
        
        Label gstLbl = new Label("GST:");
        gstLbl.setStyle("-fx-font-size: 12px;");
        gstValueLabel = new Label("0.00");
        gstValueLabel.setStyle("-fx-font-size: 12px;");
        
        Label netLbl = new Label("Net Amount:");
        netLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        totalLabel = new Label("0.00");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

        VBox taxableBox = new VBox(2, taxableLbl, taxableValueLabel);
        VBox gstBox = new VBox(2, gstLbl, gstValueLabel);
        VBox netBox = new VBox(2, netLbl, totalLabel);
        
        HBox totalsBox = new HBox(20, taxableBox, gstBox, netBox);
        totalsBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(totalsBox, Priority.ALWAYS);

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveInvoice());

        Button printBtn = new Button("Print");
        printBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #0891b2; -fx-text-fill: white;");
        printBtn.setOnAction(e -> printInvoice());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox buttonsBox = new HBox(10, saveBtn, printBtn, closeBtn);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox footer = new HBox(20, totalsBox, buttonsBox);
        footer.setPadding(new Insets(12));
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return footer;
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<Party> parties = new PartyDAO().getAll();
                Platform.runLater(() -> {
                    allParties.setAll(parties);
                    filteredParties.setAll(parties);

                    // If editing, select the invoice's party
                    if (invoice.getId() > 0) {
                        for (Party p : parties) {
                            if (p.getId() == invoice.getPartyId()) {
                                partyCombo.setValue(p);
                                autofillFromParty(p);
                                break;
                            }
                        }
                    }
                    // No default selection for new invoices

                    // Add listener to auto-fill fields when supplier is selected
                    partyCombo.setOnAction(e -> {
                        Party selected = partyCombo.getValue();
                        autofillFromParty(selected);
                    });
                });
            } catch (Exception e) {
                log.error("Failed to load parties", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load parties"));
            }
        });
    }

    private void updateTotal() {
        double taxable = lineItems.stream().mapToDouble(InvoiceLineItem::getTotal).sum();
        
        // Calculate GST based on checkbox states
        double sgst = sgstCheckBox.isSelected() ? taxable * 0.09 : 0;
        double cgst = cgstCheckBox.isSelected() ? taxable * 0.09 : 0;
        double igst = igstCheckBox.isSelected() ? taxable * 0.18 : 0;
        double totalGst = sgst + cgst + igst;
        double netAmount = taxable + totalGst;
        
        taxableValueLabel.setText(String.format("%.2f", taxable));
        gstValueLabel.setText(String.format("%.2f", totalGst));
        totalLabel.setText(String.format("%.2f", netAmount));
    }

    private void saveInvoice() {
        if (partyCombo.getValue() == null) {
            AlertUtil.showWarning("Validation", "Please select a supplier");
            return;
        }
        if (lineItems.isEmpty()) {
            AlertUtil.showWarning("Validation", "Please add at least one line item");
            return;
        }

        invoice.setInvoiceDate(invoiceDatePicker.getValue());
        invoice.setPartyId(partyCombo.getValue().getId());
        invoice.setPartyName(partyCombo.getValue().getName());
        invoice.setVoucherType(voucherTypeCombo.getValue());
        invoice.setRemarks(remarksField.getText());
        invoice.setLineItems(new java.util.ArrayList<>(lineItems));
        invoice.setStatus("SAVED");

        // New fields
        invoice.setCreditDebit(creditDebitCombo.getValue());
        invoice.setAccountName(accountNameField.getText());
        invoice.setPaidBy(paidByCombo.getValue());
        invoice.setBankName(bankNameField.getText());
        invoice.setBankAccount(bankAccountField.getText());
        invoice.setIfscCode(ifscCodeField.getText());
        invoice.setSupplierAddress(supplierAddressField.getText());
        invoice.setSupplierContactNumber(supplierContactNumberField.getText());
        invoice.setSupplierGstNo(supplierGstNoField.getText());

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
                new PurchaseInvoiceDAO().save(invoice);
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
        AlertUtil.showInfo("Info", "Print functionality will be implemented with PDF export");
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        return lbl;
    }
}
