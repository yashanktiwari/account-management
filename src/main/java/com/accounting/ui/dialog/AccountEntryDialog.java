package com.accounting.ui.dialog;

import com.accounting.dao.AccountDAO;
import com.accounting.model.Account;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

public class AccountEntryDialog {

    private final String accountType; // "CUSTOMER" or "SUPPLIER"
    private final AccountDAO dao = new AccountDAO();
    private Account currentAccount;

    // Form fields
    private TextField nameField;
    private ComboBox<String> acAsCombo;
    private TextField acTypeField;
    private TextField mailingNameField;
    private TextArea addressField;
    private TextField stateNameField;
    private TextField stateCodeField;
    private TextField cityNameField;
    private TextField faxField;
    private TextField pinCodeField;
    private TextField emailField;
    private TextField mobileField;
    private ComboBox<String> rootAreaCombo;
    private TextField gstinField;
    private TextField cstField;
    private TextField tanField;
    private TextField panField;
    private TextField tdsField;
    private ComboBox<String> tdsApplicableCombo;
    private TextField aadharField;
    private TextField drugsLicField;
    private TextField creditPeriodField;
    private TextField creditAmtLimitField;
    private TextField openingBalanceField;
    private ComboBox<String> balanceTypeCombo;
    private ComboBox<String> natureOfPaymentCombo;

    private Button saveBtn;
    private Button deleteBtn;
    private Stage stage;

    public AccountEntryDialog(String accountType) {
        this.accountType = accountType;
    }

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle(isCustomer() ? "Customer" : "Supplier");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox();
        root.setSpacing(0);

        // Header
        Label header = new Label(isCustomer() ? "CUSTOMER ENTRY" : "SUPPLIER ENTRY");
        header.getStyleClass().add(isCustomer() ? "section-header" : "section-header-red");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");
        closeBtn.setOnAction(e -> stage.close());

        HBox headerBar = new HBox(header, closeBtn);
        HBox.setHgrow(header, Priority.ALWAYS);
        headerBar.setAlignment(Pos.CENTER);
        headerBar.setStyle("-fx-background-color: " + (isCustomer() ? "#16a34a" : "#dc2626") + ";");

        // Tab pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab ledgerTab = new Tab("Account/Ledger Details", buildLedgerDetailsPane());
        Tab otherTab = new Tab("Account's Other Details", buildOtherDetailsPane());
        Tab familyTab = new Tab("Account's Family Details", buildFamilyDetailsPane());

        tabPane.getTabs().addAll(ledgerTab, otherTab, familyTab);

        // Footer buttons
        HBox footer = buildFooter();

        root.getChildren().addAll(headerBar, tabPane, footer);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 780, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    private boolean isCustomer() {
        return "CUSTOMER".equals(accountType);
    }

    private Node buildLedgerDetailsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        int row = 0;

        // Name
        nameField = new TextField();
        nameField.setPrefWidth(350);
        grid.add(label(isCustomer() ? "Customer Name :" : "Suppliers Name :"), 0, row);
        grid.add(nameField, 1, row, 3, 1);

        row++;

        // Use A/c As & A/c Type
        acAsCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Customer", "Supplier", "Bank", "Cash", "General"
        ));
        acAsCombo.setValue(isCustomer() ? "Customer" : "Supplier");
        acAsCombo.setPrefWidth(160);

        acTypeField = new TextField(isCustomer() ? "SUNDRY DEBTORS" : "SUNDRY CREDITORS");
        acTypeField.setPrefWidth(180);
        acTypeField.setEditable(false);

        grid.add(label("Use A/c As"), 0, row);
        grid.add(acAsCombo, 1, row);
        grid.add(label("A/c Type"), 2, row);
        grid.add(acTypeField, 3, row);

        row++;

        // Mailing Name & State
        mailingNameField = new TextField();
        mailingNameField.setPrefWidth(220);
        stateNameField = new TextField();
        stateNameField.setPrefWidth(180);
        stateCodeField = new TextField();
        stateCodeField.setPrefWidth(40);

        grid.add(label("Mailing Name"), 0, row);
        grid.add(mailingNameField, 1, row);
        grid.add(label("State Name"), 2, row);
        HBox stateBox = new HBox(5, stateNameField, stateCodeField);
        grid.add(stateBox, 3, row);

        row++;

        // Address & City
        addressField = new TextArea();
        addressField.setPrefRowCount(2);
        addressField.setPrefWidth(220);
        addressField.setWrapText(true);
        cityNameField = new TextField();
        cityNameField.setPrefWidth(180);

        grid.add(label("Address"), 0, row);
        grid.add(addressField, 1, row);
        grid.add(label("City Name"), 2, row);
        grid.add(cityNameField, 3, row);

        row++;

        // Fax
        faxField = new TextField();
        grid.add(label("Fax"), 2, row);
        grid.add(faxField, 3, row);

        row++;

        // Pin Code & Email
        pinCodeField = new TextField();
        pinCodeField.setPrefWidth(220);
        emailField = new TextField();
        emailField.setPrefWidth(220);

        grid.add(label("Pin Code"), 0, row);
        grid.add(pinCodeField, 1, row);
        grid.add(label("Email Address"), 2, row);
        grid.add(emailField, 3, row);

        row++;

        // Mobile & Root/Area
        mobileField = new TextField();
        mobileField.setPrefWidth(220);

        rootAreaCombo = new ComboBox<>();
        rootAreaCombo.setEditable(true);
        rootAreaCombo.setPrefWidth(160);

        Button addAreaBtn = new Button("+");
        addAreaBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        addAreaBtn.setOnAction(e -> {
            String area = rootAreaCombo.getEditor().getText();
            if (area != null && !area.isBlank()) {
                AppExecutor.submit(() -> {
                    dao.addRootArea(area.trim());
                    List<String> areas = dao.getAllRootAreas();
                    Platform.runLater(() -> {
                        rootAreaCombo.setItems(FXCollections.observableArrayList(areas));
                        rootAreaCombo.setValue(area.trim());
                    });
                });
            }
        });

        grid.add(label("Mobile No."), 0, row);
        grid.add(mobileField, 1, row);
        grid.add(label("Root/Area Name"), 2, row);
        HBox areaBox = new HBox(5, rootAreaCombo, addAreaBtn);
        grid.add(areaBox, 3, row);

        row++;

        // Separator
        Separator sep1 = new Separator();
        grid.add(sep1, 0, row, 4, 1);

        row++;

        // GSTIN & CST/TAN
        gstinField = new TextField();
        gstinField.setPrefWidth(180);
        cstField = new TextField();
        cstField.setPrefWidth(80);
        tanField = new TextField();
        tanField.setPrefWidth(80);

        Button gstVerifyBtn = new Button("...");
        gstVerifyBtn.setStyle("-fx-padding: 2 8 2 8;");

        grid.add(label("GSTIN No."), 0, row);
        HBox gstBox = new HBox(5, gstinField, gstVerifyBtn);
        grid.add(gstBox, 1, row);
        grid.add(label("CST & TAN No."), 2, row);
        HBox cstTanBox = new HBox(5, cstField, tanField);
        grid.add(cstTanBox, 3, row);

        row++;

        // PAN & TDS
        panField = new TextField();
        panField.setPrefWidth(220);
        tdsField = new TextField("0");
        tdsField.setPrefWidth(80);
        tdsApplicableCombo = new ComboBox<>(FXCollections.observableArrayList("YES", "NO"));
        tdsApplicableCombo.setValue("NO");
        tdsApplicableCombo.setPrefWidth(80);

        grid.add(label("PAN No."), 0, row);
        grid.add(panField, 1, row);
        grid.add(label("TDS(%)"), 2, row);
        HBox tdsBox = new HBox(5, tdsField, tdsApplicableCombo);
        grid.add(tdsBox, 3, row);

        row++;

        // Aadhar & Drugs Lic
        aadharField = new TextField();
        aadharField.setPrefWidth(220);
        drugsLicField = new TextField();
        drugsLicField.setPrefWidth(220);

        grid.add(label("Aadhar No."), 0, row);
        grid.add(aadharField, 1, row);
        grid.add(label("Drugs Lic. No."), 2, row);
        grid.add(drugsLicField, 3, row);

        row++;

        // Separator
        Separator sep2 = new Separator();
        grid.add(sep2, 0, row, 4, 1);

        row++;

        // Credit Period & Credit Amt Limit
        creditPeriodField = new TextField("0");
        creditPeriodField.setPrefWidth(100);
        creditAmtLimitField = new TextField("0.00");
        creditAmtLimitField.setPrefWidth(100);

        grid.add(label("Credit Period"), 0, row);
        grid.add(creditPeriodField, 1, row);
        grid.add(label("Credit Amt Limit"), 2, row);
        grid.add(creditAmtLimitField, 3, row);

        row++;

        // Opening Balance & Nature of Payment
        openingBalanceField = new TextField("0");
        openingBalanceField.setPrefWidth(100);
        balanceTypeCombo = new ComboBox<>(FXCollections.observableArrayList("Debit", "Credit"));
        balanceTypeCombo.setValue("Debit");
        balanceTypeCombo.setPrefWidth(80);

        natureOfPaymentCombo = new ComboBox<>(FXCollections.observableArrayList(
                "", "Cash", "Cheque", "NEFT", "RTGS", "Online"
        ));
        natureOfPaymentCombo.setPrefWidth(160);

        grid.add(label("Opening Balance"), 0, row);
        HBox balBox = new HBox(5, openingBalanceField, balanceTypeCombo);
        grid.add(balBox, 1, row);
        grid.add(label("Nature of Payment"), 2, row);
        grid.add(natureOfPaymentCombo, 3, row);

        // Load root areas
        AppExecutor.submit(() -> {
            List<String> areas = dao.getAllRootAreas();
            Platform.runLater(() -> rootAreaCombo.setItems(FXCollections.observableArrayList(areas)));
        });

        // Column constraints
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPrefWidth(120);
        col0.setHalignment(HPos.RIGHT);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(230);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(120);
        col2.setHalignment(HPos.RIGHT);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPrefWidth(230);
        grid.getColumnConstraints().addAll(col0, col1, col2, col3);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private Node buildOtherDetailsPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.getChildren().add(new Label("Additional account details can be added here."));
        return box;
    }

    private Node buildFamilyDetailsPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.getChildren().add(new Label("Family details of the account holder can be added here."));
        return box;
    }

    private HBox buildFooter() {
        Button newBtn = new Button("New");
        newBtn.setPrefWidth(80);
        newBtn.setOnAction(e -> clearForm());

        saveBtn = new Button("Save");
        saveBtn.setPrefWidth(80);
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> saveAccount());

        deleteBtn = new Button("Delete");
        deleteBtn.setPrefWidth(80);
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> deleteAccount());

        Button findBtn = new Button("Find");
        findBtn.setPrefWidth(80);
        findBtn.setOnAction(e -> findAccount());

        Button b2bBtn = new Button("B2b OpBal");
        b2bBtn.setPrefWidth(90);

        Button printBtn = new Button("Print Envelope");
        printBtn.setPrefWidth(110);

        Button exitBtn = new Button("Exit");
        exitBtn.setPrefWidth(80);
        exitBtn.setOnAction(e -> stage.close());

        HBox footer = new HBox(10, newBtn, saveBtn, deleteBtn, findBtn, b2bBtn, printBtn, exitBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12));
        footer.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb transparent transparent transparent;");

        return footer;
    }

    private void saveAccount() {
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            AlertUtil.showWarning("Validation", "Account name is required.");
            return;
        }

        Account a = currentAccount != null ? currentAccount : new Account();
        populateAccountFromForm(a);

        AppExecutor.submit(() -> {
            boolean success;
            if (a.getId() > 0) {
                success = dao.update(a);
            } else {
                int id = dao.save(a);
                success = id > 0;
                if (success) a.setId(id);
            }

            Platform.runLater(() -> {
                if (success) {
                    currentAccount = a;
                    saveBtn.setText("Update");
                    NotificationUtil.showSuccess("Saved", a.getAccountName() + " saved successfully.");
                } else {
                    AlertUtil.showError("Save Failed", "Could not save the account.");
                }
            });
        });
    }

    private void deleteAccount() {
        if (currentAccount == null || currentAccount.getId() <= 0) {
            AlertUtil.showWarning("Delete", "No account selected to delete.");
            return;
        }

        boolean confirm = AlertUtil.showConfirmation("Delete Account",
                "Are you sure you want to delete " + currentAccount.getAccountName() + "?");
        if (!confirm) return;

        AppExecutor.submit(() -> {
            boolean success = dao.delete(currentAccount.getId());
            Platform.runLater(() -> {
                if (success) {
                    NotificationUtil.showSuccess("Deleted", currentAccount.getAccountName() + " deleted.");
                    clearForm();
                } else {
                    AlertUtil.showError("Delete Failed", "Could not delete the account.");
                }
            });
        });
    }

    private void findAccount() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Find Account");
        dialog.setHeaderText("Search " + (isCustomer() ? "Customer" : "Supplier"));
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            AppExecutor.submit(() -> {
                List<Account> results = dao.findByName(name, accountType);
                Platform.runLater(() -> {
                    if (results.isEmpty()) {
                        AlertUtil.showInfo("Not Found", "No accounts found matching: " + name);
                    } else if (results.size() == 1) {
                        loadAccount(results.get(0));
                    } else {
                        showSearchResults(results);
                    }
                });
            });
        });
    }

    private void showSearchResults(List<Account> results) {
        Dialog<Account> dialog = new Dialog<>();
        dialog.setTitle("Search Results");
        dialog.setHeaderText("Select an account");

        ListView<Account> listView = new ListView<>(FXCollections.observableArrayList(results));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getAccountName() + " | " + safe(item.getCityName()) + " | " + safe(item.getMobile()));
            }
        });
        listView.setPrefHeight(300);
        listView.setPrefWidth(500);

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) return listView.getSelectionModel().getSelectedItem();
            return null;
        });

        dialog.showAndWait().ifPresent(this::loadAccount);
    }

    private void loadAccount(Account a) {
        currentAccount = a;
        nameField.setText(safe(a.getAccountName()));
        acAsCombo.setValue(safe(a.getAcAs()));
        acTypeField.setText(safe(a.getAcType()));
        mailingNameField.setText(safe(a.getMailingName()));
        addressField.setText(safe(a.getAddress()));
        stateNameField.setText(safe(a.getStateName()));
        stateCodeField.setText(safe(a.getStateCode()));
        cityNameField.setText(safe(a.getCityName()));
        faxField.setText(safe(a.getFax()));
        pinCodeField.setText(safe(a.getPinCode()));
        emailField.setText(safe(a.getEmail()));
        mobileField.setText(safe(a.getMobile()));
        rootAreaCombo.setValue(safe(a.getRootAreaName()));
        gstinField.setText(safe(a.getGstin()));
        cstField.setText(safe(a.getCstNo()));
        tanField.setText(safe(a.getTanNo()));
        panField.setText(safe(a.getPanNo()));
        tdsField.setText(String.valueOf(a.getTdsPercent()));
        tdsApplicableCombo.setValue(safe(a.getTdsApplicable()));
        aadharField.setText(safe(a.getAadharNo()));
        drugsLicField.setText(safe(a.getDrugsLicNo()));
        creditPeriodField.setText(String.valueOf(a.getCreditPeriod()));
        creditAmtLimitField.setText(String.format("%.2f", a.getCreditAmtLimit()));
        openingBalanceField.setText(String.valueOf(a.getOpeningBalance()));
        balanceTypeCombo.setValue(safe(a.getBalanceType()));
        natureOfPaymentCombo.setValue(safe(a.getNatureOfPayment()));

        saveBtn.setText("Update");
    }

    private void clearForm() {
        currentAccount = null;
        nameField.clear();
        acAsCombo.setValue(isCustomer() ? "Customer" : "Supplier");
        acTypeField.setText(isCustomer() ? "SUNDRY DEBTORS" : "SUNDRY CREDITORS");
        mailingNameField.clear();
        addressField.clear();
        stateNameField.clear();
        stateCodeField.clear();
        cityNameField.clear();
        faxField.clear();
        pinCodeField.clear();
        emailField.clear();
        mobileField.clear();
        rootAreaCombo.setValue(null);
        gstinField.clear();
        cstField.clear();
        tanField.clear();
        panField.clear();
        tdsField.setText("0");
        tdsApplicableCombo.setValue("NO");
        aadharField.clear();
        drugsLicField.clear();
        creditPeriodField.setText("0");
        creditAmtLimitField.setText("0.00");
        openingBalanceField.setText("0");
        balanceTypeCombo.setValue("Debit");
        natureOfPaymentCombo.setValue(null);
        saveBtn.setText("Save");
        nameField.requestFocus();
    }

    private void populateAccountFromForm(Account a) {
        a.setAccountName(nameField.getText().trim());
        a.setAccountType(accountType);
        a.setAcAs(acAsCombo.getValue());
        a.setAcType(acTypeField.getText());
        a.setMailingName(mailingNameField.getText());
        a.setAddress(addressField.getText());
        a.setStateName(stateNameField.getText());
        a.setStateCode(stateCodeField.getText());
        a.setCityName(cityNameField.getText());
        a.setFax(faxField.getText());
        a.setPinCode(pinCodeField.getText());
        a.setEmail(emailField.getText());
        a.setMobile(mobileField.getText());
        a.setRootAreaName(rootAreaCombo.getValue());
        a.setGstin(gstinField.getText());
        a.setCstNo(cstField.getText());
        a.setTanNo(tanField.getText());
        a.setPanNo(panField.getText());
        a.setTdsApplicable(tdsApplicableCombo.getValue());
        a.setAadharNo(aadharField.getText());
        a.setDrugsLicNo(drugsLicField.getText());
        a.setNatureOfPayment(natureOfPaymentCombo.getValue());

        try { a.setTdsPercent(Double.parseDouble(tdsField.getText())); } catch (NumberFormatException e) { a.setTdsPercent(0); }
        try { a.setCreditPeriod(Integer.parseInt(creditPeriodField.getText())); } catch (NumberFormatException e) { a.setCreditPeriod(0); }
        try { a.setCreditAmtLimit(Double.parseDouble(creditAmtLimitField.getText())); } catch (NumberFormatException e) { a.setCreditAmtLimit(0); }
        try { a.setOpeningBalance(Double.parseDouble(openingBalanceField.getText())); } catch (NumberFormatException e) { a.setOpeningBalance(0); }
        a.setBalanceType(balanceTypeCombo.getValue());
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
