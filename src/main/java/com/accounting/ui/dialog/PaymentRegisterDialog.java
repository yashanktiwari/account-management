package com.accounting.ui.dialog;

import com.accounting.dao.PaymentDAO;
import com.accounting.model.Payment;
import com.accounting.util.AlertUtil;
import com.accounting.util.ScreenUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.ExportUtil;
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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class PaymentRegisterDialog {

    private final PaymentDAO dao = new PaymentDAO();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private ComboBox<String> voucherTypeCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private TableView<Payment> table;
    private ObservableList<Payment> paymentList = FXCollections.observableArrayList();

    private Label totalRecordsLabel;
    private TextField totalAmountField;

    private Stage stage;

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("Payment Register");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);

        ScreenUtil.DialogSize size = ScreenUtil.getResponsiveSize(900, 600);
        
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
        if (!ScreenUtil.isSmallScreen()) {
            stage.setMaximized(true);
        }
        stage.show();
    }

    public Parent createContent() {
        VBox root = new VBox(0);

        // Filters
        HBox filterBar = new HBox(12);
        filterBar.setPadding(new Insets(12));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList("RECEIPT", "PAYMENT", "All"));
        voucherTypeCombo.setValue("RECEIPT");
        voucherTypeCombo.setPrefWidth(120);

        fromDatePicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        fromDatePicker.setPrefWidth(130);

        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(130);

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> searchPayments());

        Button excelBtn = new Button("Excel Export");
        excelBtn.setOnAction(e -> exportExcel());

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> deleteSelected());

        filterBar.getChildren().addAll(
                new Label("Type"), voucherTypeCombo,
                new Label("From"), fromDatePicker,
                new Label("To"), toDatePicker,
                searchBtn, excelBtn, deleteBtn
        );

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Footer
        totalRecordsLabel = new Label("Total Records: 0");
        totalAmountField = new TextField("0.00");
        totalAmountField.setEditable(false);
        totalAmountField.setPrefWidth(120);
        totalAmountField.setStyle("-fx-font-weight: bold;");

        HBox footer = new HBox(30, totalRecordsLabel,
                new HBox(5, new Label("Total Amount:"), totalAmountField));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.getStyleClass().add("footer-bar");

        root.getChildren().addAll(filterBar, table, footer);

        searchPayments();
        return root;
    }

    private void searchPayments() {
        String type = voucherTypeCombo.getValue();
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            AlertUtil.showWarning("Validation", "Please select both dates.");
            return;
        }

        AppExecutor.submit(() -> {
            List<Payment> results = dao.search(type, from, to);
            Platform.runLater(() -> {
                paymentList.setAll(results);
                totalRecordsLabel.setText("Total Records: " + results.size());
                double total = results.stream().mapToDouble(Payment::getAmount).sum();
                totalAmountField.setText(String.format("%.2f", total));
            });
        });
    }

    private void deleteSelected() {
        Payment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Delete", "No payment selected.");
            return;
        }

        boolean confirm = AlertUtil.showConfirmation("Delete Payment",
                "Are you sure you want to delete this payment of " +
                        String.format("%.2f", selected.getAmount()) + " from " + selected.getAccountName() + "?");
        if (!confirm) return;

        AppExecutor.submit(() -> {
            boolean success = dao.delete(selected.getId());
            Platform.runLater(() -> {
                if (success) {
                    NotificationUtil.showSuccess("Deleted", "Payment deleted successfully.");
                    searchPayments();
                } else {
                    AlertUtil.showError("Delete Failed", "Could not delete payment.");
                }
            });
        });
    }

    private void exportExcel() {
        if (paymentList.isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Payments to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("payment_register.xlsx");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        AppExecutor.submit(() -> {
            try {
                ExportUtil.exportPaymentsToExcel(paymentList, file.getAbsolutePath());
                Platform.runLater(() ->
                        NotificationUtil.showSuccess("Export", "Payment register exported."));
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Export Failed", e.getMessage()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private TableView<Payment> buildTable() {
        TableView<Payment> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

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

        TableColumn<Payment, String> vTypeCol = new TableColumn<>("Type");
        vTypeCol.setCellValueFactory(new PropertyValueFactory<>("voucherType"));
        vTypeCol.setPrefWidth(80);

        TableColumn<Payment, String> vNoCol = new TableColumn<>("Voucher No");
        vNoCol.setCellValueFactory(new PropertyValueFactory<>("voucherNo"));
        vNoCol.setPrefWidth(90);

        TableColumn<Payment, String> nameCol = new TableColumn<>("Account Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        nameCol.setPrefWidth(200);

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
        amtCol.setPrefWidth(110);

        TableColumn<Payment, String> invCol = new TableColumn<>("Against Invoice");
        invCol.setCellValueFactory(new PropertyValueFactory<>("againstInvoiceNo"));
        invCol.setPrefWidth(100);

        TableColumn<Payment, String> remCol = new TableColumn<>("Remarks");
        remCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        remCol.setPrefWidth(150);

        tv.getColumns().addAll(dateCol, vTypeCol, vNoCol, nameCol, amtCol, invCol, remCol);
        tv.setItems(paymentList);
        return tv;
    }
}
