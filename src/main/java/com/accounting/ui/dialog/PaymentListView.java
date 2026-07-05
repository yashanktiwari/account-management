package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.PaymentDAO;
import com.accounting.model.Payment;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentListView {

    private final PaymentDAO dao = new PaymentDAO();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObservableList<Payment> rows = FXCollections.observableArrayList();

    private ComboBox<String> voucherTypeCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private TableView<Payment> table;
    private Label totalRecordsLabel;
    private Label totalAmountLabel;

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Payments");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button newBtn = new Button("New Payment");
        newBtn.getStyleClass().add("primary-button");
        newBtn.setOnAction(e -> new PaymentEntryDialog(this::loadRows).show(MainApp.getPrimaryStage()));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        voucherTypeCombo = new ComboBox<>(FXCollections.observableArrayList("RECEIPT", "PAYMENT", "All"));
        voucherTypeCombo.setValue("All");
        voucherTypeCombo.setPrefWidth(120);

        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(3));
        toDatePicker = new DatePicker(LocalDate.now());

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> loadRows());

        HBox actions = new HBox(10,
                newBtn,
                refreshBtn,
                new Label("Type"), voucherTypeCombo,
                new Label("From"), fromDatePicker,
                new Label("To"), toDatePicker,
                searchBtn
        );
        actions.setAlignment(Pos.CENTER_LEFT);

        table = buildTable();
        table.setItems(rows);
        VBox.setVgrow(table, Priority.ALWAYS);

        totalRecordsLabel = new Label("Total Records: 0");
        totalAmountLabel = new Label("Total Amount: 0.00");
        totalAmountLabel.setStyle("-fx-font-weight: bold;");

        HBox footer = new HBox(24, totalRecordsLabel, totalAmountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(heading, actions, table, footer);

        loadRows();
        return root;
    }

    private void loadRows() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        if (from == null || to == null) {
            AlertUtil.showWarning("Validation", "Please select both dates.");
            return;
        }

        String type = voucherTypeCombo.getValue();
        AppExecutor.submit(() -> {
            List<Payment> data = dao.search(type, from, to);
            Platform.runLater(() -> {
                rows.setAll(data);
                totalRecordsLabel.setText("Total Records: " + data.size());
                double total = data.stream().mapToDouble(Payment::getAmount).sum();
                totalAmountLabel.setText(String.format("Total Amount: %.2f", total));
            });
        });
    }

    @SuppressWarnings("unchecked")
    private TableView<Payment> buildTable() {
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

        TableColumn<Payment, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("voucherType"));

        TableColumn<Payment, String> vNoCol = new TableColumn<>("Voucher No");
        vNoCol.setCellValueFactory(new PropertyValueFactory<>("voucherNo"));

        TableColumn<Payment, String> accountCol = new TableColumn<>("Account Name");
        accountCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));

        TableColumn<Payment, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        TableColumn<Payment, String> invoiceCol = new TableColumn<>("Against Invoice");
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("againstInvoiceNo"));

        TableColumn<Payment, String> remarksCol = new TableColumn<>("Remarks");
        remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        tv.getColumns().addAll(dateCol, typeCol, vNoCol, accountCol, amountCol, invoiceCol, remarksCol);
        return tv;
    }
}
