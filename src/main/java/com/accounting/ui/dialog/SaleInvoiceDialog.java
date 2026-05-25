package com.accounting.ui.dialog;

import com.accounting.dao.PartyDAO;
import com.accounting.dao.SaleInvoiceDAO;
import com.accounting.model.InvoiceLineItem;
import com.accounting.model.Party;
import com.accounting.model.SaleInvoice;
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
import org.slf4j.Logger;

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
    private ComboBox<Party> partyCombo;
    private TextField invoiceNoField;
    private DatePicker invoiceDatePicker;
    private DatePicker deliveryDatePicker;
    private ComboBox<String> voucherTypeCombo;
    private ComboBox<String> gstCombo;
    private TextField remarksField;
    private TextField rcvrNameField;
    private TextField rcvrAddressField;
    private TextField rcvrContactField;
    private TextField rcvrGstinField;
    private Runnable onClose;

    public SaleInvoiceDialog() {
        this.invoice = new SaleInvoice();
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

        Scene scene = new Scene(createContent(), 1200, 750);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        HBox headerBox = createHeaderSection();
        GridPane detailsGrid = createDetailsGrid();
        VBox lineItemsSection = createLineItemsSection();
        HBox footerBox = createFooterSection();

        root.getChildren().addAll(headerBox, detailsGrid, lineItemsSection, footerBox);
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

    private GridPane createDetailsGrid() {
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

        deliveryDatePicker = new DatePicker(LocalDate.now());
        grid.add(label("Delivery Date"), 4, 0);
        grid.add(deliveryDatePicker, 5, 0);

        partyCombo = new ComboBox<>();
        partyCombo.setPrefWidth(250);
        grid.add(label("Customer"), 0, 1);
        grid.add(partyCombo, 1, 1);

        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "SALE", "SALE RETURN", "CREDIT NOTE"
        ));
        voucherTypeCombo.setValue("SALE");
        grid.add(label("Voucher Type"), 2, 1);
        grid.add(voucherTypeCombo, 3, 1);

        gstCombo = new ComboBox<>(FXCollections.observableArrayList(
                "5%", "12%", "18%", "28%"
        ));
        gstCombo.setValue("18%");
        grid.add(label("GST"), 4, 1);
        grid.add(gstCombo, 5, 1);

        // Receiver details
        rcvrNameField = new TextField();
        grid.add(label("Receiver Name"), 0, 2);
        grid.add(rcvrNameField, 1, 2);

        rcvrAddressField = new TextField();
        grid.add(label("Receiver Address"), 2, 2);
        grid.add(rcvrAddressField, 3, 2);

        rcvrContactField = new TextField();
        grid.add(label("Contact No"), 4, 2);
        grid.add(rcvrContactField, 5, 2);

        rcvrGstinField = new TextField();
        grid.add(label("GSTIN"), 0, 3);
        grid.add(rcvrGstinField, 1, 3);

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
                List<Party> customers = new PartyDAO().findByType("CUSTOMER");
                Platform.runLater(() -> {
                    partyCombo.setItems(FXCollections.observableArrayList(customers));
                    if (!customers.isEmpty()) {
                        partyCombo.setValue(customers.get(0));
                    }
                });
            } catch (Exception e) {
                log.error("Failed to load customers", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load customers"));
            }
        });
    }

    private void updateTotal() {
        double total = lineItems.stream().mapToDouble(InvoiceLineItem::getTotal).sum();
        totalLabel.setText(String.format("%.2f", total));
    }

    private void saveInvoice() {
        if (partyCombo.getValue() == null) {
            AlertUtil.showWarning("Validation", "Please select a customer");
            return;
        }
        if (lineItems.isEmpty()) {
            AlertUtil.showWarning("Validation", "Please add at least one line item");
            return;
        }

        invoice.setInvoiceDate(invoiceDatePicker.getValue());
        invoice.setDeliveryDate(deliveryDatePicker.getValue());
        invoice.setPartyId(partyCombo.getValue().getId());
        invoice.setPartyName(partyCombo.getValue().getName());
        invoice.setVoucherType(voucherTypeCombo.getValue());
        invoice.setGst(gstCombo.getValue());
        invoice.setRemarks(remarksField.getText());
        invoice.setRcvrName(rcvrNameField.getText());
        invoice.setRcvrAddress(rcvrAddressField.getText());
        invoice.setRcvrContactNo(rcvrContactField.getText());
        invoice.setRcvrGstin(rcvrGstinField.getText());
        invoice.setLineItems(new java.util.ArrayList<>(lineItems));
        invoice.setStatus("SAVED");

        double total = lineItems.stream().mapToDouble(InvoiceLineItem::getTotal).sum();
        invoice.setTaxableAmount(total);
        invoice.setNetAmount(total);

        AppExecutor.submit(() -> {
            try {
                new SaleInvoiceDAO().save(invoice);
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
