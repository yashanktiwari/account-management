package com.accounting.ui.dialog;

import com.accounting.dao.PartyDAO;
import com.accounting.dao.SaleReceiptDAO;
import com.accounting.model.Party;
import com.accounting.model.SaleReceipt;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class SaleReceiptDialog {

    private static final Logger log = AppLogger.get(SaleReceiptDialog.class);
    private Stage stage;
    private SaleReceipt receipt;
    private ComboBox<Party> partyCombo;
    private TextField receiptNoField;
    private DatePicker receiptDatePicker;
    private TextField amountField;
    private ComboBox<String> paymentModeCombo;
    private TextField chequeNoField;
    private DatePicker chequeDatePicker;
    private TextField bankNameField;
    private TextField remarksField;
    private Runnable onClose;

    public SaleReceiptDialog() {
        this.receipt = new SaleReceipt();
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Sale Receipt");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Scene scene = new Scene(createContent(), 600, 500);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label title = new Label("SALE RECEIPT");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        GridPane form = createForm();

        HBox buttonsBox = createButtonsBox();

        root.getChildren().addAll(title, form, buttonsBox);
        VBox.setVgrow(form, Priority.ALWAYS);

        return root;
    }

    private GridPane createForm() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        receiptNoField = new TextField();
        receiptNoField.setPromptText("Auto-generated");
        receiptNoField.setDisable(true);
        grid.add(label("Receipt No"), 0, 0);
        grid.add(receiptNoField, 1, 0);

        receiptDatePicker = new DatePicker(LocalDate.now());
        grid.add(label("Receipt Date"), 0, 1);
        grid.add(receiptDatePicker, 1, 1);

        partyCombo = new ComboBox<>();
        partyCombo.setPrefWidth(250);
        grid.add(label("Customer"), 0, 2);
        grid.add(partyCombo, 1, 2);

        amountField = new TextField();
        amountField.setPromptText("0.00");
        grid.add(label("Amount"), 0, 3);
        grid.add(amountField, 1, 3);

        paymentModeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "CASH", "CHEQUE", "BANK_TRANSFER"
        ));
        paymentModeCombo.setValue("CASH");
        grid.add(label("Payment Mode"), 0, 4);
        grid.add(paymentModeCombo, 1, 4);

        chequeNoField = new TextField();
        chequeNoField.setDisable(true);
        grid.add(label("Cheque No"), 0, 5);
        grid.add(chequeNoField, 1, 5);

        chequeDatePicker = new DatePicker();
        chequeDatePicker.setDisable(true);
        grid.add(label("Cheque Date"), 0, 6);
        grid.add(chequeDatePicker, 1, 6);

        bankNameField = new TextField();
        bankNameField.setDisable(true);
        grid.add(label("Bank Name"), 0, 7);
        grid.add(bankNameField, 1, 7);

        remarksField = new TextField();
        grid.add(label("Remarks"), 0, 8);
        grid.add(remarksField, 1, 8);

        paymentModeCombo.setOnAction(e -> {
            boolean isCheque = "CHEQUE".equals(paymentModeCombo.getValue());
            chequeNoField.setDisable(!isCheque);
            chequeDatePicker.setDisable(!isCheque);
            bankNameField.setDisable(!isCheque);
        });

        loadParties();
        return grid;
    }

    private HBox createButtonsBox() {
        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveReceipt());

        Button printBtn = new Button("Print");
        printBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #0891b2; -fx-text-fill: white;");
        printBtn.setOnAction(e -> printReceipt());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox box = new HBox(10, saveBtn, printBtn, closeBtn);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return box;
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

    private void saveReceipt() {
        if (partyCombo.getValue() == null) {
            AlertUtil.showWarning("Validation", "Please select a customer");
            return;
        }
        if (amountField.getText().isEmpty()) {
            AlertUtil.showWarning("Validation", "Please enter amount");
            return;
        }

        try {
            receipt.setReceiptDate(receiptDatePicker.getValue());
            receipt.setPartyId(partyCombo.getValue().getId());
            receipt.setPartyName(partyCombo.getValue().getName());
            receipt.setAmount(Double.parseDouble(amountField.getText()));
            receipt.setPaymentMode(paymentModeCombo.getValue());
            receipt.setChequeNo(chequeNoField.getText());
            receipt.setChequeDate(chequeDatePicker.getValue());
            receipt.setBankName(bankNameField.getText());
            receipt.setRemarks(remarksField.getText());
            receipt.setStatus("SAVED");

            AppExecutor.submit(() -> {
                try {
                    new SaleReceiptDAO().save(receipt);
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Success", "Receipt saved successfully");
                        stage.close();
                    });
                } catch (Exception e) {
                    log.error("Failed to save receipt", e);
                    Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save receipt: " + e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Validation", "Please enter a valid amount");
        }
    }

    private void printReceipt() {
        AlertUtil.showInfo("Info", "Print functionality will be implemented with PDF export");
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        return lbl;
    }
}
