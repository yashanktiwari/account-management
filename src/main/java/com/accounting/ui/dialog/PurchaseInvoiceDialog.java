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
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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

        Scene scene = new Scene(createContent(), 1200, 700);
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

        // Party and invoice details
        GridPane detailsGrid = createDetailsGrid();

        // Line items table
        VBox lineItemsSection = createLineItemsSection();

        // Footer with totals and buttons
        HBox footerBox = createFooterSection();

        root.getChildren().addAll(headerBox, detailsGrid, lineItemsSection, footerBox);
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

    private GridPane createDetailsGrid() {
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

        // Party
        partyCombo = new ComboBox<>();
        partyCombo.setPrefWidth(250);
        grid.add(label("Supplier"), 0, 1);
        grid.add(partyCombo, 1, 1);

        // Voucher Type
        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "PURCHASE", "PURCHASE RETURN", "DEBIT NOTE"
        ));
        voucherTypeCombo.setValue("PURCHASE");
        grid.add(label("Voucher Type"), 2, 1);
        grid.add(voucherTypeCombo, 3, 1);

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
        grid.add(label("GST"), 0, 2);
        grid.add(sgstBox, 1, 2);

        cgstCheckBox = new CheckBox("CGST");
        cgstValueField = new TextField("0");
        cgstValueField.setPrefWidth(80);
        cgstValueField.setEditable(false);
        cgstCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            cgstValueField.setText(newVal ? "9" : "0");
            updateTotal();
        });
        HBox cgstBox = new HBox(10, cgstCheckBox, cgstValueField);
        grid.add(label(""), 2, 2);
        grid.add(cgstBox, 3, 2);

        igstCheckBox = new CheckBox("IGST");
        igstValueField = new TextField("0");
        igstValueField.setPrefWidth(80);
        igstValueField.setEditable(false);
        igstCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            igstValueField.setText(newVal ? "18" : "0");
            updateTotal();
        });
        HBox igstBox = new HBox(10, igstCheckBox, igstValueField);
        grid.add(label(""), 0, 3);
        grid.add(igstBox, 1, 3);

        // Remarks
        remarksField = new TextField();
        grid.add(label("Remarks"), 2, 3);
        grid.add(remarksField, 3, 3);

        loadParties();
        return grid;
    }

    private VBox createLineItemsSection() {
        Label sectionTitle = new Label("Line Items");
        sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        lineItemTable = new TableView<>(lineItems);
        lineItemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        lineItemTable.setEditable(true);

        TableColumn<InvoiceLineItem, String> lrNoCol = new TableColumn<>("LR No");
        lrNoCol.setCellValueFactory(new PropertyValueFactory<>("lrNo"));
        lrNoCol.setCellFactory(TextFieldTableCell.forTableColumn());
        lrNoCol.setOnEditCommit(e -> e.getRowValue().setLrNo(e.getNewValue()));
        lrNoCol.setPrefWidth(80);

        TableColumn<InvoiceLineItem, String> containerCol = new TableColumn<>("Container No");
        containerCol.setCellValueFactory(new PropertyValueFactory<>("containerNo"));
        containerCol.setCellFactory(TextFieldTableCell.forTableColumn());
        containerCol.setOnEditCommit(e -> e.getRowValue().setContainerNo(e.getNewValue()));
        containerCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> vehicleCol = new TableColumn<>("Vehicle No");
        vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleNo"));
        vehicleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        vehicleCol.setOnEditCommit(e -> e.getRowValue().setVehicleNo(e.getNewValue()));
        vehicleCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("from"));
        fromCol.setCellFactory(TextFieldTableCell.forTableColumn());
        fromCol.setOnEditCommit(e -> e.getRowValue().setFrom(e.getNewValue()));
        fromCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(new PropertyValueFactory<>("to"));
        toCol.setCellFactory(TextFieldTableCell.forTableColumn());
        toCol.setOnEditCommit(e -> e.getRowValue().setTo(e.getNewValue()));
        toCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        typeCol.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));
        typeCol.setPrefWidth(80);

        TableColumn<InvoiceLineItem, Double> freightCol = new TableColumn<>("Basic Freight");
        freightCol.setCellValueFactory(new PropertyValueFactory<>("basicFreight"));
        freightCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        freightCol.setOnEditCommit(e -> {
            e.getRowValue().setBasicFreight(e.getNewValue());
            updateTotal();
        });
        freightCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, Double> detentionCol = new TableColumn<>("Detention Charge");
        detentionCol.setCellValueFactory(new PropertyValueFactory<>("detentionCharge"));
        detentionCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        detentionCol.setOnEditCommit(e -> {
            e.getRowValue().setDetentionCharge(e.getNewValue());
            updateTotal();
        });
        detentionCol.setPrefWidth(120);

        TableColumn<InvoiceLineItem, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);

        TableColumn<InvoiceLineItem, Void> deleteCol = new TableColumn<>("Action");
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-padding: 4 8 4 8; -fx-font-size: 11px;");
                deleteBtn.setOnAction(e -> lineItems.remove(getIndex()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
        deleteCol.setPrefWidth(80);

        lineItemTable.getColumns().addAll(lrNoCol, containerCol, vehicleCol, fromCol, toCol, typeCol,
                freightCol, detentionCol, totalCol, deleteCol);

        Button addRowBtn = new Button("+ Add Row");
        addRowBtn.setStyle("-fx-padding: 6 12 6 12; -fx-font-size: 12px;");
        addRowBtn.setOnAction(e -> lineItems.add(new InvoiceLineItem()));

        VBox section = new VBox(8, sectionTitle, addRowBtn, lineItemTable);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(lineItemTable, Priority.ALWAYS);
        return section;
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
                List<Party> suppliers = new PartyDAO().findByType("SUPPLIER");
                Platform.runLater(() -> {
                    partyCombo.setItems(FXCollections.observableArrayList(suppliers));
                    if (!suppliers.isEmpty()) {
                        // If editing, select the invoice's party, otherwise select first
                        if (invoice.getId() > 0) {
                            for (Party p : suppliers) {
                                if (p.getId() == invoice.getPartyId()) {
                                    partyCombo.setValue(p);
                                    break;
                                }
                            }
                        } else {
                            partyCombo.setValue(suppliers.get(0));
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Failed to load suppliers", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load suppliers"));
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
