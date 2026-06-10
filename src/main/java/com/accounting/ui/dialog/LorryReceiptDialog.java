package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.LorryReceiptDAO;
import com.accounting.dao.SettingsDAO;
import com.accounting.model.LorryReceipt;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.LorryReceiptPDFGenerator;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
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
import java.util.Objects;

public class LorryReceiptDialog {

    private static final Logger log = AppLogger.get(LorryReceiptDialog.class);
    private Stage stage;
    private LorryReceipt lr;
    private Runnable onClose;

    // Basic info
    private TextField lrNoField;
    private DatePicker lrDatePicker;
    private TextField vehicleNoField;
    private TextField fromField;
    private TextField toField;
    private TextField eWayBillNoField;

    // Consignor / Consignee
    private TextField consignorNameField;
    private TextField consignorGstinField;
    private TextField consigneeNameField;
    private TextField consigneeGstinField;

    // Package details
    private VBox packageRowsContainer;
    private TextField weightActualField;
    private TextField weightChargedField;
    private TextField rateField;
    private TextField freightToPayField;
    private TextField freightPaidField;

    // Amounts
    private TextField freightField;
    private TextField advanceField;
    private Label balanceLabel;
    private TextField aocField;
    private TextField stChargeField;
    private Label totalLabel;

    // Weights / ST / SH
    private TextField stNoField;
    private TextField shNoField;
    private TextField grossWeightField;
    private TextField tareWeightField;
    private TextField netWeightField;
    private TextField valueRsField;

    // Footer
    private TextField toPayRsField;
    private TextField advPaidRsField;
    private TextField invNoField;
    private DatePicker invDatePicker;

    // Insurance
    private TextField insuranceCompanyField;
    private TextField policyNoField;
    private DatePicker policyDatePicker;
    private TextField insuranceAmountField;
    private DatePicker insuranceDatePicker;
    private ComboBox<String> riskTypeCombo;

    public LorryReceiptDialog() {
        this.lr = new LorryReceipt();
    }

    public LorryReceiptDialog(LorryReceipt lr) {
        this.lr = lr;
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Lorry Receipt (LR)");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Parent content = createContent();
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane, 1450, 950);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        if (lr.getId() > 0) {
            loadLrData();
        } else {
            generateNextLrNumber();
            // Add two empty rows for new LR
            addPackageRow();
            addPackageRow();
        }
    }

    private void generateNextLrNumber() {
        AppExecutor.submit(() -> {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                String startingNumberStr = settingsDAO.getSetting("lr_starting_number");
                int startingNumber = startingNumberStr != null ? Integer.parseInt(startingNumberStr) : 1;

                LorryReceiptDAO dao = new LorryReceiptDAO();
                String lastLrNo = dao.getLastLrNumber();
                int lastNumber = 0;
                if (lastLrNo != null && lastLrNo.matches("^[0-9]+$")) {
                    lastNumber = Integer.parseInt(lastLrNo);
                }

                int nextNumber = Math.max(startingNumber, lastNumber + 1);
                final String lrNo = String.valueOf(nextNumber);
                Platform.runLater(() -> lrNoField.setText(lrNo));
            } catch (Exception e) {
                log.error("Failed to generate LR number", e);
                Platform.runLater(() -> lrNoField.setText(""));
            }
        });
    }

    private void loadLrData() {
        lrNoField.setText(lr.getLrNo());
        lrDatePicker.setValue(lr.getLrDate());
        vehicleNoField.setText(lr.getVehicleNo());
        fromField.setText(lr.getFromLocation());
        toField.setText(lr.getToLocation());
        eWayBillNoField.setText(lr.getEWayBillNo());
        consignorNameField.setText(lr.getConsignorName());
        consignorGstinField.setText(lr.getConsignorGstin());
        consigneeNameField.setText(lr.getConsigneeName());
        consigneeGstinField.setText(lr.getConsigneeGstin());

        // Split package data into rows
        packageRowsContainer.getChildren().clear();
        String[] noPkgs = lr.getNoOfPackages() != null ? lr.getNoOfPackages().split(" \\| ") : new String[]{""};
        String[] methods = lr.getMethodOfPacking() != null ? lr.getMethodOfPacking().split(" \\| ") : new String[]{""};
        String[] descs = lr.getDescription() != null ? lr.getDescription().split(" \\| ") : new String[]{""};

        int maxRows = Math.max(Math.max(noPkgs.length, methods.length), descs.length);
        for (int i = 0; i < maxRows; i++) {
            addPackageRow();
            HBox row = (HBox) packageRowsContainer.getChildren().get(i);
            TextField noPkgField = (TextField) row.getChildren().get(0);
            TextField methodField = (TextField) row.getChildren().get(1);
            TextField descField = (TextField) row.getChildren().get(2);

            if (i < noPkgs.length) noPkgField.setText(noPkgs[i].trim());
            if (i < methods.length) methodField.setText(methods[i].trim());
            if (i < descs.length) descField.setText(descs[i].trim());
        }

        weightActualField.setText(lr.getWeightActual());
        weightChargedField.setText(lr.getWeightCharged());
        rateField.setText(lr.getRate());
        freightToPayField.setText(fmt(lr.getFreightToPay()));
        freightPaidField.setText(fmt(lr.getFreightPaid()));
        freightField.setText(fmt(lr.getFreight()));
        advanceField.setText(fmt(lr.getAdvance()));
        aocField.setText(fmt(lr.getAoc()));
        stChargeField.setText(fmt(lr.getStCharge()));
        updateTotals();
        stNoField.setText(lr.getStNo());
        shNoField.setText(lr.getShNo());
        grossWeightField.setText(lr.getGrossWeight());
        tareWeightField.setText(lr.getTareWeight());
        netWeightField.setText(lr.getNetWeight());
        valueRsField.setText(lr.getValueRs());
        toPayRsField.setText(fmt(lr.getToPayRs()));
        advPaidRsField.setText(fmt(lr.getAdvPaidRs()));
        invNoField.setText(lr.getInvNo());
        invDatePicker.setValue(lr.getInvDate());
        insuranceCompanyField.setText(lr.getInsuranceCompany());
        policyNoField.setText(lr.getPolicyNo());
        policyDatePicker.setValue(lr.getPolicyDate());
        insuranceAmountField.setText(lr.getInsuranceAmount());
        insuranceDatePicker.setValue(lr.getInsuranceDate());
        if (lr.getRiskType() != null) riskTypeCombo.setValue(lr.getRiskType());
    }

    private Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        // ── Header ──
        Label title = new Label("LORRY RECEIPT (LR)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));
        header.setBorder(new Border(new BorderStroke(
                javafx.scene.paint.Color.web("#e2e8f0"), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 0, 1, 0))));

        // ── Section 1: Basic Info ──
        GridPane basicGrid = sectionGrid();
        lrNoField = new TextField();
        lrNoField.setPromptText("Auto-generated");
        lrNoField.setDisable(true);
        basicGrid.add(label("LR No"), 0, 0);
        basicGrid.add(lrNoField, 1, 0);

        lrDatePicker = new DatePicker(LocalDate.now());
        basicGrid.add(label("LR Date"), 2, 0);
        basicGrid.add(lrDatePicker, 3, 0);

        vehicleNoField = tf();
        basicGrid.add(label("Vehicle No"), 0, 1);
        basicGrid.add(vehicleNoField, 1, 1);

        eWayBillNoField = tf();
        basicGrid.add(label("E-Way Bill No"), 2, 1);
        basicGrid.add(eWayBillNoField, 3, 1);

        fromField = tf();
        basicGrid.add(label("From"), 0, 2);
        basicGrid.add(fromField, 1, 2);

        toField = tf();
        basicGrid.add(label("To"), 2, 2);
        basicGrid.add(toField, 3, 2);

        riskTypeCombo = new ComboBox<>();
        riskTypeCombo.getItems().addAll("OWNER'S RISK", "CARRIER'S RISK");
        riskTypeCombo.setValue("OWNER'S RISK");
        basicGrid.add(label("Risk Type"), 0, 3);
        basicGrid.add(riskTypeCombo, 1, 3);

        // ── Section 2: Consignor / Consignee ──
        GridPane partyGrid = sectionGrid();
        consignorNameField = tf();
        consignorNameField.setPrefWidth(300);
        partyGrid.add(label("Consignor"), 0, 0);
        partyGrid.add(consignorNameField, 1, 0, 3, 1);

        consignorGstinField = tf();
        partyGrid.add(label("Consignor GSTIN"), 0, 1);
        partyGrid.add(consignorGstinField, 1, 1, 3, 1);

        consigneeNameField = tf();
        consigneeNameField.setPrefWidth(300);
        partyGrid.add(label("Consignee"), 0, 2);
        partyGrid.add(consigneeNameField, 1, 2, 3, 1);

        consigneeGstinField = tf();
        partyGrid.add(label("Consignee GSTIN"), 0, 3);
        partyGrid.add(consigneeGstinField, 1, 3, 3, 1);

        // ── Section 3: Package & Description ──
        VBox pkgSection = new VBox(10);

        // Package rows container with scroll
        packageRowsContainer = new VBox(5);
        ScrollPane scrollPane = new ScrollPane(packageRowsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(80);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 4;");
        pkgSection.getChildren().add(scrollPane);

        // Add row button
        Button addPackageBtn = new Button("+ Add Package Row");
        addPackageBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-padding: 5 10;");
        addPackageBtn.setOnAction(e -> addPackageRow());
        pkgSection.getChildren().add(addPackageBtn);

        // Other package fields
        GridPane pkgGrid = sectionGrid();
        weightActualField = tf();
        pkgGrid.add(label("Weight Actual"), 0, 0);
        pkgGrid.add(weightActualField, 1, 0);

        weightChargedField = tf();
        pkgGrid.add(label("Weight Charged"), 2, 0);
        pkgGrid.add(weightChargedField, 3, 0);

        rateField = tf();
        pkgGrid.add(label("Rate"), 0, 1);
        pkgGrid.add(rateField, 1, 1);

        freightToPayField = numField();
        pkgGrid.add(label("Freight To Pay"), 2, 1);
        pkgGrid.add(freightToPayField, 3, 1);

        freightPaidField = numField();
        pkgGrid.add(label("Freight Paid"), 0, 2);
        pkgGrid.add(freightPaidField, 1, 2);

        pkgSection.getChildren().add(pkgGrid);

        // ── Section 4: Amounts (right side in image) ──
        GridPane amountGrid = sectionGrid();
        freightField = numField();
        freightField.textProperty().addListener((o, ov, nv) -> updateTotals());
        amountGrid.add(label("Freight"), 0, 0);
        amountGrid.add(freightField, 1, 0);

        advanceField = numField();
        advanceField.textProperty().addListener((o, ov, nv) -> updateTotals());
        amountGrid.add(label("Advance"), 2, 0);
        amountGrid.add(advanceField, 3, 0);

        balanceLabel = new Label("0.00");
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
        amountGrid.add(label("Balance"), 0, 1);
        amountGrid.add(balanceLabel, 1, 1);

        aocField = numField();
        aocField.textProperty().addListener((o, ov, nv) -> updateTotals());
        amountGrid.add(label("A.O.C."), 2, 1);
        amountGrid.add(aocField, 3, 1);

        stChargeField = numField();
        stChargeField.textProperty().addListener((o, ov, nv) -> updateTotals());
        amountGrid.add(label("S.T. Charge"), 0, 2);
        amountGrid.add(stChargeField, 1, 2);

        totalLabel = new Label("0.00");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        amountGrid.add(label("Total"), 2, 2);
        amountGrid.add(totalLabel, 3, 2);

        // ── Section 5: ST/SH/Weights ──
        GridPane weightsGrid = sectionGrid();
        stNoField = tf();
        weightsGrid.add(label("S.T. No"), 0, 0);
        weightsGrid.add(stNoField, 1, 0);

        shNoField = tf();
        weightsGrid.add(label("S.H. No"), 2, 0);
        weightsGrid.add(shNoField, 3, 0);

        grossWeightField = tf();
        weightsGrid.add(label("G. Wt."), 0, 1);
        weightsGrid.add(grossWeightField, 1, 1);

        tareWeightField = tf();
        weightsGrid.add(label("T. Wt."), 2, 1);
        weightsGrid.add(tareWeightField, 3, 1);

        netWeightField = tf();
        weightsGrid.add(label("N. Wt."), 0, 2);
        weightsGrid.add(netWeightField, 1, 2);

        valueRsField = tf();
        weightsGrid.add(label("Value Rs."), 2, 2);
        weightsGrid.add(valueRsField, 3, 2);

        // ── Section 6: Insurance ──
        GridPane insGrid = sectionGrid();
        insuranceCompanyField = tf();
        insGrid.add(label("Insurance Company"), 0, 0);
        insGrid.add(insuranceCompanyField, 1, 0, 3, 1);

        policyNoField = tf();
        insGrid.add(label("Policy No"), 0, 1);
        insGrid.add(policyNoField, 1, 1);

        policyDatePicker = new DatePicker();
        insGrid.add(label("Policy Date"), 2, 1);
        insGrid.add(policyDatePicker, 3, 1);

        insuranceAmountField = tf();
        insGrid.add(label("Insurance Amount"), 0, 2);
        insGrid.add(insuranceAmountField, 1, 2);

        insuranceDatePicker = new DatePicker();
        insGrid.add(label("Insurance Date"), 2, 2);
        insGrid.add(insuranceDatePicker, 3, 2);

        // ── Section 7: Footer fields ──
        GridPane footerGrid = sectionGrid();
        toPayRsField = numField();
        footerGrid.add(label("To Pay Rs."), 0, 0);
        footerGrid.add(toPayRsField, 1, 0);

        advPaidRsField = numField();
        footerGrid.add(label("Adv. Paid Rs."), 2, 0);
        footerGrid.add(advPaidRsField, 3, 0);

        invNoField = tf();
        footerGrid.add(label("Inv. No"), 0, 1);
        footerGrid.add(invNoField, 1, 1);

        invDatePicker = new DatePicker();
        footerGrid.add(label("Inv. Date"), 2, 1);
        footerGrid.add(invDatePicker, 3, 1);

        // ── Buttons ──
        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveLR());

        Button printBtn = new Button("Print");
        printBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #2563eb; -fx-text-fill: white;");
        printBtn.setOnAction(e -> printLR());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox buttonsBox = new HBox(10, saveBtn, printBtn, closeBtn);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);
        buttonsBox.setPadding(new Insets(12));
        buttonsBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Left column
        VBox leftColumn = new VBox(10);
        leftColumn.getChildren().addAll(
                sectionLabel("Basic Info"), basicGrid,
                sectionLabel("Consignor / Consignee"), partyGrid,
                sectionLabel("Package & Description"), pkgSection
        );

        // Right column
        VBox rightColumn = new VBox(10);
        rightColumn.getChildren().addAll(
                sectionLabel("Freight Amounts"), amountGrid,
                sectionLabel("S.T. / S.H. / Weights"), weightsGrid,
                sectionLabel("Insurance Details"), insGrid,
                sectionLabel("Invoice / Payment"), footerGrid
        );

        // Two-column layout
        HBox contentColumns = new HBox(20, leftColumn, rightColumn);
        contentColumns.setPadding(new Insets(10));

        root.getChildren().addAll(header, contentColumns, buttonsBox);
        return root;
    }

    private void addPackageRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField noOfPackages = tf();
        noOfPackages.setPrefWidth(120);
        noOfPackages.setPromptText("No. of Packages");

        TextField methodOfPacking = tf();
        methodOfPacking.setPrefWidth(150);
        methodOfPacking.setPromptText("Method of Packing");

        TextField description = new TextField();
        description.setPrefWidth(300);
        description.setPromptText("Description");

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-padding: 5 10;");
        removeBtn.setOnAction(e -> packageRowsContainer.getChildren().remove(row));

        row.getChildren().addAll(noOfPackages, methodOfPacking, description, removeBtn);
        packageRowsContainer.getChildren().add(row);
    }

    private void updateTotals() {
        double freight = parseNum(freightField);
        double advance = parseNum(advanceField);
        double aoc = parseNum(aocField);
        double stChg = parseNum(stChargeField);
        double balance = freight - advance;
        double total = freight + aoc + stChg;
        if (balanceLabel != null) balanceLabel.setText(String.format("%.2f", balance));
        if (totalLabel != null) totalLabel.setText(String.format("%.2f", total));
    }

    private void saveLR() {
        if (lrDatePicker.getValue() == null) {
            AlertUtil.showWarning("Validation", "Please select an LR date");
            return;
        }

        lr.setLrNo(lrNoField.getText());
        lr.setLrDate(lrDatePicker.getValue());
        lr.setVehicleNo(vehicleNoField.getText().trim());
        lr.setFromLocation(fromField.getText().trim());
        lr.setToLocation(toField.getText().trim());
        lr.setEWayBillNo(eWayBillNoField.getText().trim());
        lr.setConsignorName(consignorNameField.getText().trim());
        lr.setConsignorGstin(consignorGstinField.getText().trim());
        lr.setConsigneeName(consigneeNameField.getText().trim());
        lr.setConsigneeGstin(consigneeGstinField.getText().trim());

        // Concatenate package rows
        StringBuilder noOfPackages = new StringBuilder();
        StringBuilder methodOfPacking = new StringBuilder();
        StringBuilder description = new StringBuilder();

        for (int i = 0; i < packageRowsContainer.getChildren().size(); i++) {
            HBox row = (HBox) packageRowsContainer.getChildren().get(i);
            TextField noPkgField = (TextField) row.getChildren().get(0);
            TextField methodField = (TextField) row.getChildren().get(1);
            TextField descField = (TextField) row.getChildren().get(2);

            if (i > 0) {
                noOfPackages.append(" | ");
                methodOfPacking.append(" | ");
                description.append(" | ");
            }
            noOfPackages.append(noPkgField.getText().trim());
            methodOfPacking.append(methodField.getText().trim());
            description.append(descField.getText().trim());
        }

        lr.setNoOfPackages(noOfPackages.toString());
        lr.setMethodOfPacking(methodOfPacking.toString());
        lr.setDescription(description.toString());

        lr.setWeightActual(weightActualField.getText().trim());
        lr.setWeightCharged(weightChargedField.getText().trim());
        lr.setRate(rateField.getText().trim());
        lr.setFreightToPay(parseNum(freightToPayField));
        lr.setFreightPaid(parseNum(freightPaidField));
        lr.setFreight(parseNum(freightField));
        lr.setAdvance(parseNum(advanceField));
        lr.setBalance(parseNum(freightField) - parseNum(advanceField));
        lr.setAoc(parseNum(aocField));
        lr.setStCharge(parseNum(stChargeField));
        lr.setTotal(parseNum(freightField) + parseNum(aocField) + parseNum(stChargeField));
        lr.setStNo(stNoField.getText().trim());
        lr.setShNo(shNoField.getText().trim());
        lr.setGrossWeight(grossWeightField.getText().trim());
        lr.setTareWeight(tareWeightField.getText().trim());
        lr.setNetWeight(netWeightField.getText().trim());
        lr.setValueRs(valueRsField.getText().trim());
        lr.setToPayRs(parseNum(toPayRsField));
        lr.setAdvPaidRs(parseNum(advPaidRsField));
        lr.setInvNo(invNoField.getText().trim());
        lr.setInvDate(invDatePicker.getValue());
        lr.setInsuranceCompany(insuranceCompanyField.getText().trim());
        lr.setPolicyNo(policyNoField.getText().trim());
        lr.setPolicyDate(policyDatePicker.getValue());
        lr.setInsuranceAmount(insuranceAmountField.getText().trim());
        lr.setInsuranceDate(insuranceDatePicker.getValue());
        lr.setRiskType(riskTypeCombo.getValue());
        lr.setStatus("SAVED");

        AppExecutor.submit(() -> {
            try {
                LorryReceiptDAO dao = new LorryReceiptDAO();
                if (lr.getId() > 0) {
                    dao.update(lr);
                } else {
                    dao.save(lr);
                    try {
                        int currentLrNo = Integer.parseInt(lr.getLrNo());
                        new SettingsDAO().saveSetting("lr_starting_number", String.valueOf(currentLrNo + 1));
                    } catch (Exception e) {
                        log.error("Failed to update LR number in settings", e);
                    }
                }
                Platform.runLater(() -> {
                    NotificationUtil.showSuccess("Success", "Lorry Receipt saved successfully");
                    stage.close();
                });
            } catch (Exception e) {
                log.error("Failed to save lorry receipt", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save LR: " + e.getMessage()));
            }
        });
    }

    private void printLR() {
        if (lr.getId() == 0) {
            AlertUtil.showWarning("Warning", "Please save the LR before printing");
            return;
        }
        try {
            java.io.File dir = new java.io.File("lorry_receipts");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "lorry_receipts/LR_" + lr.getLrNo() + "_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            LorryReceiptPDFGenerator.generateLorryReceiptPDF(lr, fileName, "Original");

            stage.close();
            new PrintPreviewDialog(fileName, (copyLabel, outputPath) ->
                    LorryReceiptPDFGenerator.generateLorryReceiptPDF(lr, outputPath, copyLabel)
            ).showInApp(() -> MainApp.showContentInApp(new LorryReceiptListView().createContent()));
        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            AlertUtil.showError("Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    // ── Helpers ──
    private GridPane sectionGrid() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(10);
        g.setPadding(new Insets(10));
        g.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return g;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 6 0 0 0;");
        return l;
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        return lbl;
    }

    private TextField tf() {
        TextField t = new TextField();
        setupUppercaseListener(t);
        return t;
    }

    private TextField numField() {
        TextField t = new TextField();
        t.setPromptText("0.00");
        t.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("\\d*\\.?\\d*")) {
                t.setText(oldVal);
            }
        });
        return t;
    }

    private double parseNum(TextField field) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) return 0;
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private String fmt(double val) {
        return val == 0 ? "" : String.format("%.2f", val);
    }

    private void setupUppercaseListener(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                int caretPosition = textField.getCaretPosition();
                textField.setText(newVal.toUpperCase());
                textField.positionCaret(Math.min(caretPosition, textField.getLength()));
            }
        });
    }
}
