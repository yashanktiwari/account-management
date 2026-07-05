package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.SaleReceiptDAO;
import com.accounting.model.SaleReceipt;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

public class SaleReceiptListView {

    private final SaleReceiptDAO dao = new SaleReceiptDAO();
    private final ObservableList<SaleReceipt> rows = FXCollections.observableArrayList();
    private List<SaleReceipt> allData = new java.util.ArrayList<>();
    private TableView<SaleReceipt> table;
    private TextField searchField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Label rowCountLabel;
    private Label totalAmountLabel;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Money Paid Receipts");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("New Sale Receipt");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            new SaleReceiptDialog().show(MainApp.getPrimaryStage(), this::loadRows);
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchRows());

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            loadRows();
        });

        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefWidth(120);

        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefWidth(120);

        Button filterBtn = new Button("Filter");
        filterBtn.setOnAction(e -> applyFilters());

        rowCountLabel = new Label("Total: 0");
        rowCountLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        totalAmountLabel = new Label("Total Amount: \u20B90.00");
        totalAmountLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

        HBox searchControls = new HBox(8, new Label("Search:"), searchField, clearBtn);
        searchControls.setAlignment(Pos.CENTER_LEFT);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox row1 = new HBox(8, searchControls, spacer1, addBtn, refreshBtn);
        row1.setAlignment(Pos.CENTER_LEFT);

        HBox dateControls = new HBox(8, new Label("Date:"), startDatePicker, new Label("to"), endDatePicker, filterBtn);
        dateControls.setAlignment(Pos.CENTER_LEFT);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox statsControls = new HBox(15, rowCountLabel, totalAmountLabel);
        statsControls.setAlignment(Pos.CENTER_RIGHT);

        HBox row2 = new HBox(8, dateControls, spacer2, statsControls);
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox topBar = new VBox(6, row1, row2);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<SaleReceipt, Integer> serialCol = new TableColumn<>("S.No");
        serialCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleObjectProperty<>(index + 1);
        });
        serialCol.setPrefWidth(60);
        table.getColumns().add(serialCol);
        table.getColumns().add(col("Receipt No", "receiptNo", 120));

        // Receipt Date column with custom date formatting
        TableColumn<SaleReceipt, Object> receiptDateCol = new TableColumn<>("Receipt Date");
        receiptDateCol.setCellValueFactory(new PropertyValueFactory<>("receiptDate"));
        receiptDateCol.setPrefWidth(120);
        receiptDateCol.setCellFactory(column -> new TableCell<SaleReceipt, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof java.time.LocalDate) {
                    setText(((java.time.LocalDate) item).format(DATE_FORMATTER));
                } else {
                    setText(item.toString());
                }
            }
        });
        table.getColumns().add(receiptDateCol);

        table.getColumns().add(col("Party", "partyName", 200));
        TableColumn<SaleReceipt, Object> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(120);
        amountCol.setCellFactory(c -> new TableCell<SaleReceipt, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof Number) {
                    setText(String.format("%.2f", ((Number) item).doubleValue()));
                } else {
                    setText(item.toString());
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        table.getColumns().add(amountCol);
        table.getColumns().add(col("Payment Mode", "paymentMode", 120));
        table.getColumns().add(col("Remarks", "remarks", 200));

        table.setItems(rows);

        // Right-click context menu
        ContextMenu ctxMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            SaleReceipt selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new SaleReceiptDialog(selected).show(MainApp.getPrimaryStage(), this::loadRows);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            SaleReceipt selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean confirmed = AlertUtil.showConfirmation("Delete Sale Receipt",
                    "Delete Sale Receipt '" + selected.getReceiptNo() + "'? This cannot be undone.");
            if (!confirmed) return;
            AppExecutor.submit(() -> {
                try {
                    dao.delete(selected.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Deleted", "Sale Receipt deleted.");
                        rows.remove(selected);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Error", "Failed to delete: " + ex.getMessage()));
                }
            });
        });

        ctxMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);

        table.setRowFactory(tv -> {
            TableRow<SaleReceipt> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    table.getSelectionModel().select(row.getItem());
                    ctxMenu.show(row, e.getScreenX(), e.getScreenY());
                }
                e.consume();
            });
            return row;
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(heading, topBar, table);

        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            try {
                List<SaleReceipt> data = dao.getAll();
                Platform.runLater(() -> {
                    allData = data;
                    applyFilters();
                });
            } catch (Exception ignored) {
                Platform.runLater(() -> {
                    allData = new java.util.ArrayList<>();
                    rows.clear();
                    updateStats();
                });
            }
        });
    }

    private void applyFilters() {
        List<SaleReceipt> filtered = new java.util.ArrayList<>(allData);

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null) {
            filtered = filtered.stream()
                .filter(r -> r.getReceiptDate() != null
                    && !r.getReceiptDate().isBefore(start)
                    && !r.getReceiptDate().isAfter(end))
                .collect(Collectors.toList());
        } else if (start != null) {
            filtered = filtered.stream()
                .filter(r -> r.getReceiptDate() != null && !r.getReceiptDate().isBefore(start))
                .collect(Collectors.toList());
        } else if (end != null) {
            filtered = filtered.stream()
                .filter(r -> r.getReceiptDate() != null && !r.getReceiptDate().isAfter(end))
                .collect(Collectors.toList());
        }

        String searchText = searchField.getText().trim().toLowerCase();
        if (!searchText.isEmpty()) {
            filtered = filtered.stream()
                .filter(r -> (r.getReceiptNo() != null && r.getReceiptNo().toLowerCase().contains(searchText))
                    || (r.getPartyName() != null && r.getPartyName().toLowerCase().contains(searchText))
                    || (r.getRemarks() != null && r.getRemarks().toLowerCase().contains(searchText))
                    || (r.getPaymentMode() != null && r.getPaymentMode().toLowerCase().contains(searchText)))
                .collect(Collectors.toList());
        }

        rows.setAll(filtered);
        updateStats();
    }

    private void searchRows() {
        applyFilters();
    }

    private void updateStats() {
        rowCountLabel.setText("Total: " + rows.size());
        double total = rows.stream().mapToDouble(SaleReceipt::getAmount).sum();
        totalAmountLabel.setText("Total Amount: \u20B9" + String.format("%.2f", total));
    }

    private <T, U> TableColumn<T, U> col(String title, String property, double width) {
        TableColumn<T, U> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }
}
