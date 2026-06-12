package com.accounting.ui.dialog;

import com.accounting.dao.LedgerDAO;
import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.LedgerPDFGenerator;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LedgerView {

    private static final Logger log = AppLogger.get(LedgerView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final LedgerDAO ledgerDAO = new LedgerDAO();
    private final PartyDAO partyDAO = new PartyDAO();

    private ComboBox<Party> partyComboBox;
    private DatePicker fromDate;
    private DatePicker toDate;
    private TableView<LedgerDAO.LedgerEntry> ledgerTable;
    private final ObservableList<LedgerDAO.LedgerEntry> ledgerEntries = FXCollections.observableArrayList();

    private Label openingBalanceLabel;
    private Label closingBalanceLabel;
    private Label totalDebitLabel;
    private Label totalCreditLabel;
    
    private LedgerDAO.LedgerResult currentLedgerResult;
    private Party currentParty;
    private LocalDate currentFromDate;
    private LocalDate currentToDate;

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label heading = new Label("Party Ledger Statement");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        // Filter section
        VBox filterSection = buildFilterSection();

        // Summary cards
        HBox summaryCards = buildSummaryCards();

        // Table section
        VBox tableSection = buildTableSection();
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        root.getChildren().addAll(heading, filterSection, summaryCards, tableSection);
        loadParties();
        return root;
    }

    private VBox buildFilterSection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Party selector
        Label partyLabel = new Label("Party:");
        partyLabel.setStyle("-fx-font-weight: bold;");
        partyComboBox = new ComboBox<>();
        partyComboBox.setPromptText("Select a party...");
        partyComboBox.setPrefWidth(350);

        // Custom cell factory to show party name
        partyComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Party item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        partyComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Party item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        // Date pickers
        Label fromLabel = new Label("From:");
        fromLabel.setStyle("-fx-font-weight: bold;");
        fromDate = new DatePicker(LocalDate.now().withDayOfMonth(1));
        fromDate.setPrefWidth(150);

        Label toLabel = new Label("To:");
        toLabel.setStyle("-fx-font-weight: bold;");
        toDate = new DatePicker(LocalDate.now());
        toDate.setPrefWidth(150);

        // Generate button
        Button generateBtn = new Button("Generate Ledger");
        generateBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        generateBtn.setOnAction(e -> generateLedger());
        
        // Print PDF button
        Button printBtn = new Button("Print PDF");
        printBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        printBtn.setOnAction(e -> exportToPDF());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(partyLabel, 0, 0);
        grid.add(partyComboBox, 1, 0, 3, 1);
        grid.add(fromLabel, 0, 1);
        grid.add(fromDate, 1, 1);
        grid.add(toLabel, 2, 1);
        grid.add(toDate, 3, 1);
        grid.add(generateBtn, 4, 1);
        grid.add(printBtn, 5, 1);

        section.getChildren().add(grid);
        return section;
    }

    private HBox buildSummaryCards() {
        openingBalanceLabel = new Label("0.00");
        closingBalanceLabel = new Label("0.00");
        totalDebitLabel = new Label("0.00");
        totalCreditLabel = new Label("0.00");

        HBox cards = new HBox(12);
        cards.getChildren().addAll(
            summaryCard("Opening Balance", openingBalanceLabel, "#7c3aed"),
            summaryCard("Total Debit", totalDebitLabel, "#dc2626"),
            summaryCard("Total Credit", totalCreditLabel, "#16a34a"),
            summaryCard("Closing Balance", closingBalanceLabel, "#2563eb")
        );
        return cards;
    }

    private VBox summaryCard(String title, Label valueLabel, String color) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        VBox card = new VBox(4, titleLbl, valueLabel);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);");
        card.setPrefWidth(200);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildTableSection() {
        VBox section = new VBox(8);

        ledgerTable = new TableView<>();
        ledgerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ledgerTable.setItems(ledgerEntries);

        // Serial No
        TableColumn<LedgerDAO.LedgerEntry, Integer> serialCol = new TableColumn<>("S.No");
        serialCol.setCellValueFactory(cellData -> {
            int index = ledgerTable.getItems().indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleObjectProperty<>(index + 1);
        });
        serialCol.setPrefWidth(60);
        serialCol.setStyle("-fx-alignment: CENTER;");

        // Date
        TableColumn<LedgerDAO.LedgerEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDate();
            return new javafx.beans.property.SimpleStringProperty(
                date != null ? date.format(DATE_FORMATTER) : "");
        });
        dateCol.setPrefWidth(110);
        dateCol.setStyle("-fx-alignment: CENTER;");

        // Transaction Type
        TableColumn<LedgerDAO.LedgerEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTransactionType()));
        typeCol.setPrefWidth(150);

        // Transaction No
        TableColumn<LedgerDAO.LedgerEntry, String> noCol = new TableColumn<>("Transaction No");
        noCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTransactionNo()));
        noCol.setPrefWidth(130);

        // Debit
        TableColumn<LedgerDAO.LedgerEntry, Double> debitCol = new TableColumn<>("Debit");
        debitCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDebit()));
        debitCol.setCellFactory(col -> formatCurrencyCell());
        debitCol.setPrefWidth(120);
        debitCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Credit
        TableColumn<LedgerDAO.LedgerEntry, Double> creditCol = new TableColumn<>("Credit");
        creditCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCredit()));
        creditCol.setCellFactory(col -> formatCurrencyCell());
        creditCol.setPrefWidth(120);
        creditCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Running Balance
        TableColumn<LedgerDAO.LedgerEntry, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(cellData -> {
            double bal = cellData.getValue().getRunningBalance();
            String suffix = bal >= 0 ? " Cr" : " Dr";
            return new javafx.beans.property.SimpleStringProperty(
                String.format("%,.2f%s", Math.abs(bal), suffix));
        });
        balanceCol.setPrefWidth(140);
        balanceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        balanceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                } else {
                    setText(item);
                    if (item.endsWith("Dr")) {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Remarks
        TableColumn<LedgerDAO.LedgerEntry, String> remarksCol = new TableColumn<>("Remarks");
        remarksCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRemarks()));
        remarksCol.setPrefWidth(180);

        ledgerTable.getColumns().addAll(serialCol, dateCol, typeCol, noCol, debitCol, creditCol, balanceCol, remarksCol);

        VBox.setVgrow(ledgerTable, Priority.ALWAYS);
        section.getChildren().add(ledgerTable);
        return section;
    }

    private TableCell<LedgerDAO.LedgerEntry, Double> formatCurrencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                } else {
                    setText(String.format("%,.2f", item));
                }
            }
        };
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<Party> parties = partyDAO.getAll();
                Platform.runLater(() -> {
                    partyComboBox.setItems(FXCollections.observableArrayList(parties));
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    AlertUtil.showError("Error", "Failed to load parties: " + e.getMessage()));
            }
        });
    }

    private void generateLedger() {
        Party selectedParty = partyComboBox.getSelectionModel().getSelectedItem();
        if (selectedParty == null) {
            AlertUtil.showWarning("Ledger", "Please select a party.");
            return;
        }
        if (fromDate.getValue() == null || toDate.getValue() == null) {
            AlertUtil.showWarning("Ledger", "Please select both From and To dates.");
            return;
        }
        if (fromDate.getValue().isAfter(toDate.getValue())) {
            AlertUtil.showWarning("Ledger", "From date cannot be after To date.");
            return;
        }

        log.info("Generating ledger for party: {} (ID: {}), from: {}, to: {}", 
            selectedParty.getName(), selectedParty.getId(), fromDate.getValue(), toDate.getValue());

        AppExecutor.submit(() -> {
            try {
                LedgerDAO.LedgerResult result = ledgerDAO.generateLedger(
                    selectedParty.getId(), fromDate.getValue(), toDate.getValue());

                log.info("Ledger generated. Entries: {}, Opening: {}, Closing: {}", 
                    result.getEntries().size(), result.getOpeningBalance(), result.getClosingBalance());

                Platform.runLater(() -> {
                    // Store current ledger data for PDF export
                    currentLedgerResult = result;
                    currentParty = selectedParty;
                    currentFromDate = fromDate.getValue();
                    currentToDate = toDate.getValue();
                    
                    ledgerEntries.setAll(result.getEntries());

                    // Update summary cards
                    double totalDebit = result.getEntries().stream().mapToDouble(LedgerDAO.LedgerEntry::getDebit).sum();
                    double totalCredit = result.getEntries().stream().mapToDouble(LedgerDAO.LedgerEntry::getCredit).sum();

                    openingBalanceLabel.setText(formatBalance(result.getOpeningBalance()));
                    closingBalanceLabel.setText(formatBalance(result.getClosingBalance()));
                    totalDebitLabel.setText(String.format("%,.2f", totalDebit));
                    totalCreditLabel.setText(String.format("%,.2f", totalCredit));

                    // Color the balance labels
                    styleBalanceLabel(openingBalanceLabel, result.getOpeningBalance());
                    styleBalanceLabel(closingBalanceLabel, result.getClosingBalance());
                });
            } catch (Exception e) {
                log.error("Failed to generate ledger", e);
                Platform.runLater(() ->
                    AlertUtil.showError("Error", "Failed to generate ledger: " + e.getMessage()));
            }
        });
    }

    private String formatBalance(double balance) {
        String suffix = balance >= 0 ? " Cr" : " Dr";
        return String.format("%,.2f%s", Math.abs(balance), suffix);
    }

    private void styleBalanceLabel(Label label, double balance) {
        String color = balance >= 0 ? "#16a34a" : "#dc2626";
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }
    
    private void exportToPDF() {
        if (currentLedgerResult == null || currentParty == null) {
            AlertUtil.showWarning("Export PDF", "Please generate a ledger first.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Ledger PDF");
        fileChooser.setInitialFileName("Ledger_" + currentParty.getName().replaceAll("[^a-zA-Z0-9]", "_") + 
            "_" + currentFromDate.format(DATE_FORMATTER).replace("-", "") + ".pdf");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            AppExecutor.submit(() -> {
                try {
                    LedgerPDFGenerator.generateLedgerPDF(
                        currentParty, currentLedgerResult, currentFromDate, currentToDate, 
                        file.getAbsolutePath());
                    
                    Platform.runLater(() -> 
                        AlertUtil.showInfo("Success", "Ledger PDF exported successfully!"));
                } catch (Exception e) {
                    log.error("Failed to export ledger PDF", e);
                    Platform.runLater(() -> 
                        AlertUtil.showError("Error", "Failed to export PDF: " + e.getMessage()));
                }
            });
        }
    }
}
