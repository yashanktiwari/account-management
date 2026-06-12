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

import java.util.List;
import java.time.format.DateTimeFormatter;

public class SaleReceiptListView {

    private final SaleReceiptDAO dao = new SaleReceiptDAO();
    private final ObservableList<SaleReceipt> rows = FXCollections.observableArrayList();
    private TableView<SaleReceipt> table;
    private TextField searchField;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Sale Receipt List");
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
            loadRows();
        });

        HBox searchControls = new HBox(10, new Label("Search:"), searchField, clearBtn);
        searchControls.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, searchControls, spacer, addBtn, refreshBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

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
        table.getColumns().add(col("Receipt Date", "receiptDate", 120));
        table.getColumns().add(col("Party", "partyName", 200));
        table.getColumns().add(col("Amount", "amount", 120));
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
                Platform.runLater(() -> rows.setAll(data));
            } catch (Exception ignored) {
                Platform.runLater(rows::clear);
            }
        });
    }

    private void searchRows() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadRows();
            return;
        }
        AppExecutor.submit(() -> {
            try {
                List<SaleReceipt> data = dao.searchAllColumns(searchText);
                Platform.runLater(() -> rows.setAll(data));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(rows::clear);
            }
        });
    }

    private <T, U> TableColumn<T, U> col(String title, String property, double width) {
        TableColumn<T, U> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }
}
