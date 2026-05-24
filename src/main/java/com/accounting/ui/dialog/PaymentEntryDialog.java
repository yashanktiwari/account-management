package com.accounting.ui.dialog;

import com.accounting.dao.AccountDAO;
import com.accounting.dao.InvoiceDAO;
import com.accounting.dao.PaymentDAO;
import com.accounting.model.Account;
import com.accounting.model.Invoice;
import com.accounting.model.Payment;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.textfield.TextFields;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class PaymentEntryDialog {

    private final AccountDAO accountDAO = new AccountDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TextField accountNameField;
    private DatePicker paymentDatePicker;
    private TextField voucherNoField;
    private ComboBox<String> voucherTypeCombo;
    private TextField amountField;
    private ComboBox<String> againstInvoiceCombo;
    private TextField remarksField;
    private Label pendingAmountLabel;

    private TableView<Payment> recentTable;
    private ObservableList<Payment> recentPayments = FXCollections.observableArrayList();

    private Account selectedAccount;
    private Stage stage;
    private Runnable onPaymentSaved;

    public PaymentEntryDialog() {}

    public PaymentEntryDialog(Runnable onPaymentSaved) {
        this.onPaymentSaved = onPaymentSaved;
    }

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("Payment Entry");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(0);

        // Header
        Label header = new Label("PAYMENT / RECEIPT ENTRY");
        header.getStyleClass().add("section-header");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");
        closeBtn.setOnAction(e -> stage.close());

        HBox headerBar = new HBox(header, closeBtn);
        HBox.setHgrow(header, Priority.ALWAYS);
        headerBar.setAlignment(Pos.CENTER);
        headerBar.setStyle("-fx-background-color: #16a34a;");

        // Form
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(16));

        int row = 0;

        // Voucher Type
        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList("RECEIPT", "PAYMENT"));
        voucherTypeCombo.setValue("RECEIPT");
        voucherTypeCombo.setPrefWidth(150);

        form.add(label("Voucher Type :"), 0, row);
        form.add(voucherTypeCombo, 1, row);

        // Voucher No
        voucherNoField = new TextField();
        voucherNoField.setPrefWidth(150);
        voucherNoField.setPromptText("Auto / Manual");
        form.add(label("Voucher No :"), 2, row);
        form.add(voucherNoField, 3, row);

        row++;

        // Date
        paymentDatePicker = new DatePicker(LocalDate.now());
        paymentDatePicker.setPrefWidth(150);
        form.add(label("Date :"), 0, row);
        form.add(paymentDatePicker, 1, row);

        row++;

        // Account Name with autocomplete
        accountNameField = new TextField();
        accountNameField.setPrefWidth(300);
        accountNameField.setPromptText("Type customer name...");
        form.add(label("Account Name :"), 0, row);
        form.add(accountNameField, 1, row, 3, 1);

        row++;

        // Pending amount display
        pendingAmountLabel = new Label("Pending: -");
        pendingAmountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
        form.add(new Label(""), 0, row);
        form.add(pendingAmountLabel, 1, row, 3, 1);

        row++;

        // Against Invoice
        againstInvoiceCombo = new ComboBox<>();
        againstInvoiceCombo.setEditable(true);
        againstInvoiceCombo.setPrefWidth(250);
        againstInvoiceCombo.setPromptText("Select invoice (optional)");
        form.add(label("Against Invoice :"), 0, row);
        form.add(againstInvoiceCombo, 1, row, 3, 1);

        row++;

        // Amount
        amountField = new TextField();
        amountField.setPrefWidth(150);
        amountField.setPromptText("0.00");
        amountField.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        form.add(label("Amount :"), 0, row);
        form.add(amountField, 1, row);

        row++;

        // Remarks
        remarksField = new TextField();
        remarksField.setPrefWidth(300);
        remarksField.setPromptText("Remarks (optional)");
        form.add(label("Remarks :"), 0, row);
        form.add(remarksField, 1, row, 3, 1);

        // Action buttons
        Button saveBtn = new Button("Save Payment");
        saveBtn.setPrefWidth(140);
        saveBtn.setPrefHeight(40);
        saveBtn.getStyleClass().add("success-button");
        saveBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> savePayment());

        Button clearBtn = new Button("Clear");
        clearBtn.setPrefWidth(80);
        clearBtn.setOnAction(e -> clearForm());

        HBox actionBar = new HBox(12, saveBtn, clearBtn);
        actionBar.setAlignment(Pos.CENTER);
        actionBar.setPadding(new Insets(12, 0, 12, 0));

        // Recent payments table
        Label recentLabel = new Label("Recent Payments");
        recentLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        recentTable = buildRecentTable();
        VBox.setVgrow(recentTable, Priority.ALWAYS);

        VBox tableSection = new VBox(6, recentLabel, recentTable);
        tableSection.setPadding(new Insets(0, 16, 16, 16));
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        root.getChildren().addAll(headerBar, form, actionBar, new Separator(), tableSection);

        Scene scene = new Scene(root, 700, 650);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        setupAutoComplete();
        loadRecentPayments();
    }

    private void setupAutoComplete() {
        AppExecutor.submit(() -> {
            List<Account> allAccounts = accountDAO.getAllAccounts();
            List<String> names = allAccounts.stream().map(Account::getAccountName).toList();
            Platform.runLater(() -> {
                TextFields.bindAutoCompletion(accountNameField, names);
                accountNameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (!isFocused) {
                        String typed = accountNameField.getText();
                        selectedAccount = allAccounts.stream()
                                .filter(a -> a.getAccountName().equalsIgnoreCase(typed))
                                .findFirst().orElse(null);
                        if (selectedAccount != null) {
                            loadInvoicesForAccount(selectedAccount.getId());
                        }
                    }
                });
            });
        });
    }

    private void loadInvoicesForAccount(int accountId) {
        AppExecutor.submit(() -> {
            List<Invoice> invoices = invoiceDAO.search("SALE", null, null, null, "Date Wise",
                    LocalDate.of(2000, 1, 1), LocalDate.now());

            // Filter for this account and CREDIT type
            List<Invoice> accountInvoices = invoices.stream()
                    .filter(i -> i.getAccountId() == accountId && "CREDIT".equals(i.getCashCredit()))
                    .toList();

            double totalInvoiced = accountInvoices.stream().mapToDouble(Invoice::getGrandTotal).sum();

            Platform.runLater(() -> {
                ObservableList<String> invoiceNos = FXCollections.observableArrayList();
                for (Invoice inv : accountInvoices) {
                    invoiceNos.add(inv.getInvoiceNo() + " (" + String.format("%.0f", inv.getGrandTotal()) + ")");
                }
                againstInvoiceCombo.setItems(invoiceNos);
                pendingAmountLabel.setText(String.format("Total Invoiced (Credit): %.2f", totalInvoiced));
            });
        });
    }

    private void savePayment() {
        if (selectedAccount == null) {
            AlertUtil.showWarning("Validation", "Please select a valid account name.");
            return;
        }

        String amtText = amountField.getText();
        if (amtText == null || amtText.isBlank()) {
            AlertUtil.showWarning("Validation", "Amount is required.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtText.trim());
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Validation", "Please enter a valid amount.");
            return;
        }

        if (amount <= 0) {
            AlertUtil.showWarning("Validation", "Amount must be greater than zero.");
            return;
        }

        Payment payment = new Payment();
        payment.setPaymentDate(paymentDatePicker.getValue());
        payment.setVoucherNo(voucherNoField.getText() != null ? voucherNoField.getText().trim() : "");
        payment.setVoucherType(voucherTypeCombo.getValue());
        payment.setAccountId(selectedAccount.getId());
        payment.setAccountName(selectedAccount.getAccountName());
        payment.setParticulars(voucherTypeCombo.getValue());
        payment.setAmount(amount);
        payment.setRemarks(remarksField.getText());

        // Parse against invoice
        String invoiceSelection = againstInvoiceCombo.getValue();
        if (invoiceSelection != null && !invoiceSelection.isBlank()) {
            String invoiceNo = invoiceSelection.contains("(")
                    ? invoiceSelection.substring(0, invoiceSelection.indexOf("(")).trim()
                    : invoiceSelection.trim();
            payment.setAgainstInvoiceNo(invoiceNo);
        }

        AppExecutor.submit(() -> {
            int id = paymentDAO.save(payment);
            Platform.runLater(() -> {
                if (id > 0) {
                    NotificationUtil.showSuccess("Payment Saved",
                            String.format("%.2f received from %s", amount, selectedAccount.getAccountName()));
                    clearForm();
                    loadRecentPayments();
                    if (onPaymentSaved != null) onPaymentSaved.run();
                } else {
                    AlertUtil.showError("Save Failed", "Could not save the payment. Please try again.");
                }
            });
        });
    }

    private void clearForm() {
        accountNameField.clear();
        amountField.clear();
        remarksField.clear();
        voucherNoField.clear();
        againstInvoiceCombo.getItems().clear();
        pendingAmountLabel.setText("Pending: -");
        paymentDatePicker.setValue(LocalDate.now());
        voucherTypeCombo.setValue("RECEIPT");
        selectedAccount = null;
        accountNameField.requestFocus();
    }

    private void loadRecentPayments() {
        AppExecutor.submit(() -> {
            List<Payment> payments = paymentDAO.search("RECEIPT",
                    LocalDate.now().minusDays(30), LocalDate.now());
            Platform.runLater(() -> recentPayments.setAll(payments));
        });
    }

    @SuppressWarnings("unchecked")
    private TableView<Payment> buildRecentTable() {
        TableView<Payment> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Payment, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFmt));
            }
        });
        dateCol.setPrefWidth(90);

        TableColumn<Payment, String> vNoCol = new TableColumn<>("Voucher No");
        vNoCol.setCellValueFactory(new PropertyValueFactory<>("voucherNo"));
        vNoCol.setPrefWidth(80);

        TableColumn<Payment, String> nameCol = new TableColumn<>("Account Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        nameCol.setPrefWidth(180);

        TableColumn<Payment, Double> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        amtCol.setPrefWidth(100);

        TableColumn<Payment, String> invCol = new TableColumn<>("Against Invoice");
        invCol.setCellValueFactory(new PropertyValueFactory<>("againstInvoiceNo"));
        invCol.setPrefWidth(100);

        TableColumn<Payment, String> remCol = new TableColumn<>("Remarks");
        remCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        remCol.setPrefWidth(120);

        tv.getColumns().addAll(dateCol, vNoCol, nameCol, amtCol, invCol, remCol);
        tv.setItems(recentPayments);
        return tv;
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        return lbl;
    }
}
