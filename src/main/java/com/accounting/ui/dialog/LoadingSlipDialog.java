package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.LoadingSlipDAO;
import com.accounting.dao.SettingsDAO;
import com.accounting.model.LoadingSlip;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.ScreenUtil;
import com.accounting.util.LoadingSlipPDFGenerator;
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

public class LoadingSlipDialog {

    private static final Logger log = AppLogger.get(LoadingSlipDialog.class);
    private Stage stage;
    private LoadingSlip slip;
    private Runnable onClose;

    private TextField slipNoField;
    private DatePicker slipDatePicker;
    private TextField partyNameField;
    private TextField vehicleNoField;
    private TextField grNoField;
    private TextField stationField;
    private TextField toLocationField;
    private TextField weightField;
    private TextField rateField;
    private TextField freightAmountField;
    private TextField advanceAmountField;
    private Label balanceAmountLabel;
    private TextField bankNameField;
    private TextField accountNoField;
    private TextField ifscCodeField;
    private TextArea remarksField;

    public LoadingSlipDialog() {
        this.slip = new LoadingSlip();
    }

    public LoadingSlipDialog(LoadingSlip slip) {
        this.slip = slip;
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Loading Slip");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        ScreenUtil.DialogSize size = ScreenUtil.getResponsiveSize(950, 620);
        
        Parent content = createContent();
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        Scene scene = new Scene(scrollPane, size.width, size.height);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        if (slip.getId() > 0) {
            loadSlipData();
        } else {
            generateNextSlipNumber();
        }
    }

    private void generateNextSlipNumber() {
        AppExecutor.submit(() -> {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                String startingNumberStr = settingsDAO.getSetting("loading_slip_starting_number");
                int startingNumber = startingNumberStr != null ? Integer.parseInt(startingNumberStr) : 1;

                LoadingSlipDAO dao = new LoadingSlipDAO();
                String lastSlipNo = dao.getLastSlipNumber();
                int lastNumber = 0;
                if (lastSlipNo != null && lastSlipNo.matches("^[0-9]+$")) {
                    lastNumber = Integer.parseInt(lastSlipNo);
                }

                int nextNumber = Math.max(startingNumber, lastNumber + 1);
                final String slipNo = String.valueOf(nextNumber);
                Platform.runLater(() -> slipNoField.setText(slipNo));
            } catch (Exception e) {
                log.error("Failed to generate slip number", e);
                Platform.runLater(() -> slipNoField.setText(""));
            }
        });
    }

    private void loadSlipData() {
        slipNoField.setText(slip.getSlipNo());
        slipDatePicker.setValue(slip.getSlipDate());
        partyNameField.setText(slip.getPartyName());
        vehicleNoField.setText(slip.getVehicleNo());
        grNoField.setText(slip.getGrNo());
        stationField.setText(slip.getStation());
        toLocationField.setText(slip.getToLocation());
        weightField.setText(slip.getWeight());
        rateField.setText(slip.getRate());
        freightAmountField.setText(String.format("%.2f", slip.getFreightAmount()));
        advanceAmountField.setText(String.format("%.2f", slip.getAdvanceAmount()));
        updateBalance();
        bankNameField.setText(slip.getBankName());
        accountNoField.setText(slip.getAccountNo());
        ifscCodeField.setText(slip.getIfscCode());
        remarksField.setText(slip.getRemarks());
    }

    private Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        // Header
        Label title = new Label("LOADING SLIP");
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

        // Slip details grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        slipNoField = new TextField();
        slipNoField.setPromptText("Auto-generated");
        slipNoField.setDisable(false);
        grid.add(label("Slip No"), 0, 0);
        grid.add(slipNoField, 1, 0);

        slipDatePicker = new DatePicker(LocalDate.now());
        grid.add(label("Date"), 2, 0);
        grid.add(slipDatePicker, 3, 0);

        partyNameField = new TextField();
        setupUppercaseListener(partyNameField);
        partyNameField.setPrefWidth(300);
        grid.add(label("Party Name"), 0, 1);
        grid.add(partyNameField, 1, 1, 3, 1);

        vehicleNoField = new TextField();
        setupUppercaseListener(vehicleNoField);
        grid.add(label("Vehicle No"), 0, 2);
        grid.add(vehicleNoField, 1, 2);

        grNoField = new TextField();
        setupUppercaseListener(grNoField);
        grid.add(label("G.R. No"), 2, 2);
        grid.add(grNoField, 3, 2);

        stationField = new TextField();
        setupUppercaseListener(stationField);
        grid.add(label("Station (From)"), 0, 3);
        grid.add(stationField, 1, 3);

        toLocationField = new TextField();
        setupUppercaseListener(toLocationField);
        grid.add(label("To"), 2, 3);
        grid.add(toLocationField, 3, 3);

        weightField = new TextField();
        setupUppercaseListener(weightField);
        grid.add(label("Weight"), 0, 4);
        grid.add(weightField, 1, 4);

        rateField = new TextField();
        setupUppercaseListener(rateField);
        grid.add(label("Rate"), 2, 4);
        grid.add(rateField, 3, 4);

        // Amounts section
        GridPane amountsGrid = new GridPane();
        amountsGrid.setHgap(16);
        amountsGrid.setVgap(12);
        amountsGrid.setPadding(new Insets(12));
        amountsGrid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        freightAmountField = new TextField();
        freightAmountField.setPromptText("0.00");
        freightAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("\\d*\\.?\\d*")) {
                freightAmountField.setText(oldVal);
            }
            updateBalance();
        });
        amountsGrid.add(label("Freight Amount (Rs.)"), 0, 0);
        amountsGrid.add(freightAmountField, 1, 0);

        advanceAmountField = new TextField();
        advanceAmountField.setPromptText("0.00");
        advanceAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("\\d*\\.?\\d*")) {
                advanceAmountField.setText(oldVal);
            }
            updateBalance();
        });
        amountsGrid.add(label("Advance Amount (Rs.)"), 2, 0);
        amountsGrid.add(advanceAmountField, 3, 0);

        balanceAmountLabel = new Label("0.00");
        balanceAmountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
        amountsGrid.add(label("Balance Amount (Rs.)"), 0, 1);
        amountsGrid.add(balanceAmountLabel, 1, 1);

        // Bank details section
        GridPane bankGrid = new GridPane();
        bankGrid.setHgap(16);
        bankGrid.setVgap(12);
        bankGrid.setPadding(new Insets(12));
        bankGrid.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        bankNameField = new TextField();
        setupUppercaseListener(bankNameField);
        bankGrid.add(label("Bank Name"), 0, 0);
        bankGrid.add(bankNameField, 1, 0);

        accountNoField = new TextField();
        setupUppercaseListener(accountNoField);
        bankGrid.add(label("A/C No"), 2, 0);
        bankGrid.add(accountNoField, 3, 0);

        ifscCodeField = new TextField();
        setupUppercaseListener(ifscCodeField);
        bankGrid.add(label("IFSC Code"), 0, 1);
        bankGrid.add(ifscCodeField, 1, 1);

        remarksField = new TextArea();
        remarksField.setPrefRowCount(2);
        remarksField.setPromptText("Remarks...");
        bankGrid.add(label("Remarks"), 2, 0, 1, 2);
        bankGrid.add(remarksField, 3, 0, 1, 2);

        // Footer with buttons
        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveSlip());

        Button printBtn = new Button("Print");
        printBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #2563eb; -fx-text-fill: white;");
        printBtn.setOnAction(e -> printSlip());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox buttonsBox = new HBox(10, saveBtn, printBtn, closeBtn);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox footer = new HBox(20, buttonsBox);
        footer.setPadding(new Insets(12));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        root.getChildren().addAll(header, grid, amountsGrid, bankGrid, footer);
        return root;
    }

    private void updateBalance() {
        double freight = 0;
        double advance = 0;
        try {
            if (freightAmountField != null && !freightAmountField.getText().trim().isEmpty()) {
                freight = Double.parseDouble(freightAmountField.getText().trim());
            }
        } catch (NumberFormatException ignored) {}
        try {
            if (advanceAmountField != null && !advanceAmountField.getText().trim().isEmpty()) {
                advance = Double.parseDouble(advanceAmountField.getText().trim());
            }
        } catch (NumberFormatException ignored) {}
        double balance = freight - advance;
        if (balanceAmountLabel != null) {
            balanceAmountLabel.setText(String.format("%.2f", balance));
        }
    }

    private void saveSlip() {
        if (partyNameField.getText() == null || partyNameField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validation", "Please enter a party name");
            return;
        }
        if (slipDatePicker.getValue() == null) {
            AlertUtil.showWarning("Validation", "Please select a date");
            return;
        }

        double freight = 0;
        double advance = 0;
        try {
            if (!freightAmountField.getText().trim().isEmpty())
                freight = Double.parseDouble(freightAmountField.getText().trim());
        } catch (NumberFormatException ignored) {}
        try {
            if (!advanceAmountField.getText().trim().isEmpty())
                advance = Double.parseDouble(advanceAmountField.getText().trim());
        } catch (NumberFormatException ignored) {}

        slip.setSlipNo(slipNoField.getText());
        slip.setSlipDate(slipDatePicker.getValue());
        slip.setPartyName(partyNameField.getText().trim());
        slip.setVehicleNo(vehicleNoField.getText().trim());
        slip.setGrNo(grNoField.getText().trim());
        slip.setStation(stationField.getText().trim());
        slip.setToLocation(toLocationField.getText().trim());
        slip.setWeight(weightField.getText().trim());
        slip.setRate(rateField.getText().trim());
        slip.setFreightAmount(freight);
        slip.setAdvanceAmount(advance);
        slip.setBalanceAmount(freight - advance);
        slip.setBankName(bankNameField.getText().trim());
        slip.setAccountNo(accountNoField.getText().trim());
        slip.setIfscCode(ifscCodeField.getText().trim());
        slip.setRemarks(remarksField.getText().trim());
        slip.setStatus("SAVED");

        AppExecutor.submit(() -> {
            try {
                LoadingSlipDAO dao = new LoadingSlipDAO();
                if (slip.getId() > 0) {
                    dao.update(slip);
                } else {
                    dao.save(slip);

                    // Update the next slip number in settings
                    try {
                        int currentSlipNo = Integer.parseInt(slip.getSlipNo());
                        new SettingsDAO().saveSetting("loading_slip_starting_number", String.valueOf(currentSlipNo + 1));
                    } catch (Exception e) {
                        log.error("Failed to update slip number in settings", e);
                    }
                }
                Platform.runLater(() -> {
                    NotificationUtil.showSuccess("Success", "Loading slip saved successfully");
                    stage.close();
                });
            } catch (Exception e) {
                log.error("Failed to save loading slip", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save loading slip: " + e.getMessage()));
            }
        });
    }

    private void printSlip() {
        if (slip.getId() == 0) {
            AlertUtil.showWarning("Warning", "Please save the loading slip before printing");
            return;
        }

        try {
            // Use system temp directory for initial preview (not saved to app folder)
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            String fileName = tempDir.getAbsolutePath() + "/Loading_Slip_" + slip.getSlipNo() + "_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            LoadingSlipPDFGenerator.generateLoadingSlipPDF(slip, fileName, "Original");

            stage.close();
            new PrintPreviewDialog(fileName, (copyLabel, outputPath) ->
                    LoadingSlipPDFGenerator.generateLoadingSlipPDF(slip, outputPath, copyLabel)
            ).showInApp(() -> MainApp.showContentInApp(new LoadingSlipListView().createContent()));
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
                int caretPosition = textField.getCaretPosition();
                textField.setText(newVal.toUpperCase());
                textField.positionCaret(Math.min(caretPosition, textField.getLength()));
            }
        });
    }
}
