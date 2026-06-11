package com.accounting.ui.dialog;

import com.accounting.dao.PartyDAO;
import com.accounting.dao.SaleReceiptDAO;
import com.accounting.dao.SettingsDAO;
import com.accounting.model.Party;
import com.accounting.model.SaleReceipt;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Popup;
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
    private TextField partyField;
    private ObservableList<Party> allParties = FXCollections.observableArrayList();
    private Popup partyPopup;
    private ListView<Party> partyListView;
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

    public SaleReceiptDialog(SaleReceipt receipt) {
        this.receipt = receipt;
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

        Scene scene = new Scene(createContent(), 600, 600);
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
        grid.add(label("Receipt No"), 0, 0);
        grid.add(receiptNoField, 1, 0);

        receiptDatePicker = new DatePicker(LocalDate.now());
        grid.add(label("Receipt Date"), 0, 1);
        grid.add(receiptDatePicker, 1, 1);

        partyField = new TextField();
        partyField.setPrefWidth(250);
        partyField.setPromptText("Type to search customer...");
        setupPartyAutocomplete();
        grid.add(label("Customer"), 0, 2);
        grid.add(partyField, 1, 2);

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

    private void setupPartyAutocomplete() {
        partyPopup = new Popup();
        partyPopup.setAutoHide(true);

        partyListView = new ListView<>();
        partyListView.setFocusTraversable(false);
        partyListView.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1;");
        partyListView.setCellFactory(param -> new ListCell<Party>() {
            @Override
            protected void updateItem(Party party, boolean empty) {
                super.updateItem(party, empty);
                setText(empty || party == null ? "" : party.getName());
            }
        });

        partyPopup.getContent().add(partyListView);

        partyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                partyPopup.hide();
                return;
            }

            List<Party> filtered = allParties.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(newVal.toLowerCase()))
                    .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                    .collect(java.util.stream.Collectors.toList());

            if (filtered.isEmpty()) {
                partyPopup.hide();
                return;
            }

            partyListView.getItems().setAll(filtered);

            int visibleRows = Math.min(filtered.size(), 10);
            partyListView.setPrefHeight(visibleRows * 26 + 2);
            partyListView.setPrefWidth(partyField.getWidth());

            if (!partyPopup.isShowing()) {
                javafx.geometry.Point2D p = partyField.localToScreen(0, partyField.getHeight());
                if (p != null) {
                    partyPopup.show(partyField, p.getX(), p.getY());
                }
            }
        });

        // Mouse selection
        partyListView.setOnMouseClicked(e -> {
            Party selected = partyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                partyField.setText(selected.getName());
                partyPopup.hide();
            }
        });

        // Keyboard navigation
        partyField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN -> {
                    if (partyPopup.isShowing() && !partyListView.getItems().isEmpty()) {
                        partyListView.requestFocus();
                        if (partyListView.getSelectionModel().isEmpty()) {
                            partyListView.getSelectionModel().selectFirst();
                        }
                    }
                }
                case ESCAPE -> partyPopup.hide();
                case TAB -> {
                    if (partyPopup.isShowing() && !partyListView.getItems().isEmpty()) {
                        Party first = partyListView.getItems().get(0);
                        partyField.setText(first.getName());
                        partyPopup.hide();
                    }
                }
                case ENTER -> {
                    if (partyPopup.isShowing() && !partyListView.getItems().isEmpty()) {
                        Party selected = partyListView.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            partyField.setText(selected.getName());
                            partyPopup.hide();
                        } else {
                            Party first = partyListView.getItems().get(0);
                            partyField.setText(first.getName());
                            partyPopup.hide();
                        }
                    }
                }
            }
        });

        partyListView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    Party selected = partyListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        partyField.setText(selected.getName());
                        partyField.requestFocus();
                    }
                    partyPopup.hide();
                }
                case ESCAPE -> {
                    partyPopup.hide();
                    partyField.requestFocus();
                }
            }
        });
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<Party> customers = new PartyDAO().findByType("CUSTOMER");
                Platform.runLater(() -> {
                    allParties.setAll(customers);
                    // After loading parties, load receipt data if editing
                    if (receipt.getId() > 0) {
                        loadReceiptData();
                    } else {
                        // New receipt - auto-generate receipt number
                        generateNextReceiptNumber();
                    }
                });
            } catch (Exception e) {
                log.error("Failed to load customers", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load customers"));
            }
        });
    }

    private void generateNextReceiptNumber() {
        AppExecutor.submit(() -> {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                String startingNumberStr = settingsDAO.getSetting("sale_receipt_starting_number");
                int startingNumber = startingNumberStr != null ? Integer.parseInt(startingNumberStr) : 1;

                SaleReceiptDAO dao = new SaleReceiptDAO();
                String lastReceiptNo = dao.getLastReceiptNumber();
                int lastNumber = lastReceiptNo != null && !lastReceiptNo.isEmpty() ? Integer.parseInt(lastReceiptNo) : 0;

                int nextNumber = Math.max(startingNumber, lastNumber + 1);
                final String receiptNo = String.valueOf(nextNumber);

                Platform.runLater(() -> receiptNoField.setText(receiptNo));
            } catch (Exception e) {
                log.error("Failed to generate receipt number", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to generate receipt number"));
            }
        });
    }

    private void loadReceiptData() {
        receiptNoField.setText(receipt.getReceiptNo());
        receiptDatePicker.setValue(receipt.getReceiptDate());
        amountField.setText(String.valueOf(receipt.getAmount()));
        paymentModeCombo.setValue(receipt.getPaymentMode());
        chequeNoField.setText(receipt.getChequeNo());
        chequeDatePicker.setValue(receipt.getChequeDate());
        bankNameField.setText(receipt.getBankName());
        remarksField.setText(receipt.getRemarks());
        partyField.setText(receipt.getPartyName());
    }

    private void saveReceipt() {
        if (partyField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validation", "Please enter customer name");
            return;
        }
        if (amountField.getText().isEmpty()) {
            AlertUtil.showWarning("Validation", "Please enter amount");
            return;
        }

        try {
            // Generate receipt number if empty
            String receiptNo = receiptNoField.getText().trim();
            if (receiptNo.isEmpty()) {
                try {
                    SettingsDAO settingsDAO = new SettingsDAO();
                    String startingNumberStr = settingsDAO.getSetting("sale_receipt_starting_number");
                    int startingNumber = startingNumberStr != null ? Integer.parseInt(startingNumberStr) : 1;

                    SaleReceiptDAO dao = new SaleReceiptDAO();
                    String lastReceiptNo = dao.getLastReceiptNumber();
                    int lastNumber = lastReceiptNo != null && !lastReceiptNo.isEmpty() ? Integer.parseInt(lastReceiptNo) : 0;

                    int nextNumber = Math.max(startingNumber, lastNumber + 1);
                    receiptNo = String.valueOf(nextNumber);
                    receiptNoField.setText(receiptNo);
                } catch (Exception e) {
                    log.error("Failed to generate receipt number", e);
                    AlertUtil.showWarning("Validation", "Failed to auto-generate receipt number. Please enter it manually.");
                    return;
                }
            }

            receipt.setReceiptNo(receiptNo);
            receipt.setReceiptDate(receiptDatePicker.getValue());
            
            // Find party by name or use custom name
            String partyName = partyField.getText().trim();
            Party matchedParty = allParties.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(partyName))
                    .findFirst()
                    .orElse(null);
            
            if (matchedParty != null) {
                receipt.setPartyId(matchedParty.getId());
                receipt.setPartyName(matchedParty.getName());
            } else {
                receipt.setPartyId(0);
                receipt.setPartyName(partyName);
            }
            receipt.setAmount(Double.parseDouble(amountField.getText()));
            receipt.setPaymentMode(paymentModeCombo.getValue());
            receipt.setChequeNo(chequeNoField.getText());
            receipt.setChequeDate(chequeDatePicker.getValue());
            receipt.setBankName(bankNameField.getText());
            receipt.setRemarks(remarksField.getText());
            receipt.setStatus("SAVED");

            AppExecutor.submit(() -> {
                try {
                    SaleReceiptDAO dao = new SaleReceiptDAO();
                    if (receipt.getId() > 0) {
                        dao.update(receipt);
                    } else {
                        dao.save(receipt);

                        // Update the next receipt number in settings only for new receipts
                        try {
                            int currentReceiptNo = Integer.parseInt(receipt.getReceiptNo());
                            new SettingsDAO().saveSetting("sale_receipt_starting_number", String.valueOf(currentReceiptNo + 1));
                        } catch (Exception e) {
                            log.error("Failed to update receipt number in settings", e);
                        }
                    }
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Success", "Receipt saved successfully");
                        stage.close();
                        if (onClose != null) onClose.run();
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
