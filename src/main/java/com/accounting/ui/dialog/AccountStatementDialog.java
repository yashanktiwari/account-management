package com.accounting.ui.dialog;

import com.accounting.dao.AccountDAO;
import com.accounting.model.Account;
import com.accounting.model.AccountTransaction;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.ExportUtil;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.textfield.TextFields;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class AccountStatementDialog {

    private final AccountDAO dao = new AccountDAO();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TextField accountNameField;
    private Label acTypeLabel;
    private ComboBox<String> refTypeCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private TableView<AccountTransaction> table;
    private ObservableList<AccountTransaction> transactionList = FXCollections.observableArrayList();

    private Label totalDrLabel;
    private Label totalCrLabel;
    private Label closingBalanceLabel;

    private Account selectedAccount;
    private Stage stage;

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("A/c Statement");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(0);

        // Filter bar
        HBox filterBar = new HBox(12);
        filterBar.setPadding(new Insets(12));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        accountNameField = new TextField();
        accountNameField.setPrefWidth(250);
        accountNameField.setPromptText("Account Name");

        acTypeLabel = new Label("");
        acTypeLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-style: italic;");

        refTypeCombo = new ComboBox<>(FXCollections.observableArrayList("On Account", "Against Reference", "All"));
        refTypeCombo.setValue("On Account");
        refTypeCombo.setPrefWidth(140);

        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(6));
        fromDatePicker.setPrefWidth(130);

        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(130);

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> searchStatement());

        Button printBtn = new Button("Print");
        printBtn.setOnAction(e -> exportPDF());

        Button excelBtn = new Button("Excel Export");
        excelBtn.setOnAction(e -> exportExcel());

        filterBar.getChildren().addAll(
                new Label("Account Name"), accountNameField, acTypeLabel,
                new Label("Reference Type"), refTypeCombo,
                fromDatePicker, toDatePicker,
                searchBtn, printBtn, excelBtn
        );

        // Setup autocomplete
        AppExecutor.submit(() -> {
            List<Account> allAccounts = dao.getAllAccounts();
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
                            acTypeLabel.setText(safe(selectedAccount.getAcType()));
                        }
                    }
                });
            });
        });

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Footer
        totalDrLabel = new Label("0 Dr");
        totalDrLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #dc2626;");
        totalCrLabel = new Label("0 Cr");
        totalCrLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #16a34a;");

        closingBalanceLabel = new Label("Closing Balance : 0 Dr");
        closingBalanceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #fef9c3; -fx-padding: 4 12 4 12;");

        Label totalRowsLabel = new Label("Total Rows: 0");

        HBox summaryRow = new HBox(40, totalRowsLabel, totalDrLabel, totalCrLabel);
        summaryRow.setAlignment(Pos.CENTER);
        summaryRow.setPadding(new Insets(6));

        HBox closingRow = new HBox(closingBalanceLabel);
        closingRow.setAlignment(Pos.CENTER);
        closingRow.setPadding(new Insets(4));
        closingRow.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e5e7eb transparent transparent transparent;");

        VBox footer = new VBox(summaryRow, closingRow);
        footer.getStyleClass().add("footer-bar");

        root.getChildren().addAll(filterBar, table, footer);

        Scene scene = new Scene(root, 1050, 650);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @SuppressWarnings("unchecked")
    private TableView<AccountTransaction> buildTable() {
        TableView<AccountTransaction> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<AccountTransaction, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFmt));
            }
        });
        dateCol.setPrefWidth(90);

        TableColumn<AccountTransaction, String> partCol = new TableColumn<>("Particulars");
        partCol.setCellValueFactory(new PropertyValueFactory<>("particulars"));
        partCol.setPrefWidth(150);

        TableColumn<AccountTransaction, String> vTypeCol = new TableColumn<>("VoucherType");
        vTypeCol.setCellValueFactory(new PropertyValueFactory<>("voucherType"));
        vTypeCol.setPrefWidth(90);

        TableColumn<AccountTransaction, String> vNoCol = new TableColumn<>("VoucherNo");
        vNoCol.setCellValueFactory(new PropertyValueFactory<>("voucherNo"));
        vNoCol.setPrefWidth(80);

        TableColumn<AccountTransaction, Double> debitCol = new TableColumn<>("Debit");
        debitCol.setCellValueFactory(new PropertyValueFactory<>("debit"));
        debitCol.setCellFactory(col -> numericCell());
        debitCol.setPrefWidth(100);

        TableColumn<AccountTransaction, Double> creditCol = new TableColumn<>("Credit");
        creditCol.setCellValueFactory(new PropertyValueFactory<>("credit"));
        creditCol.setCellFactory(col -> numericCell());
        creditCol.setPrefWidth(100);

        TableColumn<AccountTransaction, Double> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
        balanceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    AccountTransaction t = getTableRow() != null ? getTableRow().getItem() : null;
                    String type = t != null ? safe(t.getBalanceType()) : "";
                    setText(String.format("%.0f %s", item, type));
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        balanceCol.setPrefWidth(100);

        TableColumn<AccountTransaction, Integer> dueDaysCol = new TableColumn<>("Due\nDays");
        dueDaysCol.setCellValueFactory(new PropertyValueFactory<>("dueDays"));
        dueDaysCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item == 0 ? null : String.valueOf(item));
                setAlignment(Pos.CENTER);
            }
        });
        dueDaysCol.setPrefWidth(55);

        TableColumn<AccountTransaction, String> remarksCol = new TableColumn<>("Remarks");
        remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        remarksCol.setPrefWidth(180);

        tv.getColumns().addAll(dateCol, partCol, vTypeCol, vNoCol, debitCol, creditCol,
                balanceCol, dueDaysCol, remarksCol);

        tv.setItems(transactionList);
        return tv;
    }

    private TableCell<AccountTransaction, Double> numericCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(empty ? null : "0.00");
                } else {
                    setText(String.format("%.2f", item));
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        };
    }

    private void searchStatement() {
        if (selectedAccount == null) {
            String typed = accountNameField.getText();
            if (typed != null && !typed.isBlank()) {
                List<Account> found = dao.findByName(typed, "CUSTOMER");
                if (found.isEmpty()) found = dao.findByName(typed, "SUPPLIER");
                if (!found.isEmpty()) selectedAccount = found.get(0);
            }
        }

        if (selectedAccount == null) {
            AlertUtil.showWarning("Validation", "Please select a valid account name.");
            return;
        }

        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        String refType = refTypeCombo.getValue();

        if (from == null || to == null) {
            AlertUtil.showWarning("Validation", "Please select both dates.");
            return;
        }

        acTypeLabel.setText(safe(selectedAccount.getAcType()));

        AppExecutor.submit(() -> {
            List<AccountTransaction> results = dao.getAccountStatement(
                    selectedAccount.getId(), selectedAccount.getAccountType(), refType, from, to);
            Platform.runLater(() -> {
                transactionList.setAll(results);
                updateTotals();
            });
        });
    }

    private void updateTotals() {
        double totalDr = transactionList.stream().mapToDouble(AccountTransaction::getDebit).sum();
        double totalCr = transactionList.stream().mapToDouble(AccountTransaction::getCredit).sum();
        double closing = totalDr - totalCr;

        totalDrLabel.setText(String.format("%.0f Dr", totalDr));
        totalCrLabel.setText(String.format("%.0f Cr", totalCr));
        closingBalanceLabel.setText(String.format("Closing Balance : %.0f %s",
                Math.abs(closing), closing >= 0 ? "Dr" : "Cr"));

        // Update total rows via parent
        VBox footer = (VBox) closingBalanceLabel.getParent().getParent();
        HBox summaryRow = (HBox) footer.getChildren().get(0);
        Label rowLabel = (Label) summaryRow.getChildren().get(0);
        rowLabel.setText("Total Rows: " + transactionList.size());
    }

    private void exportExcel() {
        if (transactionList.isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Statement to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("account_statement.xlsx");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        String accName = selectedAccount != null ? selectedAccount.getAccountName() : "";
        AppExecutor.submit(() -> {
            try {
                ExportUtil.exportStatementToExcel(transactionList, accName, file.getAbsolutePath());
                Platform.runLater(() ->
                        NotificationUtil.showSuccess("Export", "Account statement exported."));
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Export Failed", e.getMessage()));
            }
        });
    }

    private void exportPDF() {
        if (transactionList.isEmpty()) {
            AlertUtil.showWarning("Print", "No data to print.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Print Statement to PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("account_statement.pdf");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        String accName = selectedAccount != null ? selectedAccount.getAccountName() : "";
        AppExecutor.submit(() -> {
            ExportUtil.exportStatementToPDF(transactionList, accName, file.getAbsolutePath());
            Platform.runLater(() ->
                    NotificationUtil.showSuccess("Print", "Account statement PDF generated."));
        });
    }

    private String safe(String s) { return s == null ? "" : s; }
}
