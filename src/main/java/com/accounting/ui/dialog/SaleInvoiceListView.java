package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.SaleInvoiceDAO;
import com.accounting.model.SaleInvoice;
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
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SaleInvoiceListView {

    private final SaleInvoiceDAO dao = new SaleInvoiceDAO();
    private final ObservableList<SaleInvoice> rows = FXCollections.observableArrayList();
    private TableView<SaleInvoice> table;
    private TextField searchField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Label rowCountLabel;
    private Label totalAmountLabel;
    private final Set<String> searchTerms = new HashSet<>();
    private final ObservableList<String> searchTagsList = FXCollections.observableArrayList();
    private Timer debounceTimer;
    private static final int DEBOUNCE_DELAY = 500;
    private static final int MAX_SEARCH_TERMS = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Sale Invoice List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("Create New Invoice");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            new SaleInvoiceDialog().show(MainApp.getPrimaryStage(), this::loadRows);
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        searchField = new TextField();
        searchField.setPromptText("Type and press Enter to add search term...");
        searchField.setPrefWidth(250);
        searchField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                addSearchTerm();
                e.consume();
            }
        });
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (debounceTimer != null) {
                debounceTimer.cancel();
            }
            debounceTimer = new Timer();
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(SaleInvoiceListView.this::searchRows);
                }
            }, DEBOUNCE_DELAY);
        });

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchTerms.clear();
            searchTagsList.clear();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            loadRows();
        });

        // Date filter
        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefWidth(120);

        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefWidth(120);

        Button filterBtn = new Button("Filter");
        filterBtn.setOnAction(e -> loadRows());

        // Row count and total amount labels
        rowCountLabel = new Label("Total: 0");
        rowCountLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        totalAmountLabel = new Label("Total Amount: ₹0.00");
        totalAmountLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

        HBox tagsContainer = new HBox(8);
        tagsContainer.setAlignment(Pos.CENTER_LEFT);
        tagsContainer.setPrefHeight(32);
        searchTagsList.addListener((javafx.collections.ListChangeListener<String>) change -> {
            tagsContainer.getChildren().clear();
            for (String term : searchTagsList) {
                HBox tag = new HBox(5);
                tag.setStyle("-fx-padding: 4px 8px; -fx-background-color: #e3f2fd; -fx-border-color: #1976d2; -fx-border-radius: 4; -fx-alignment: CENTER;");
                Label label = new Label(term);
                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-padding: 0; -fx-font-size: 12px;");
                removeBtn.setOnAction(e -> removeSearchTerm(term));
                tag.getChildren().addAll(label, removeBtn);
                tagsContainer.getChildren().add(tag);
            }
        });

        HBox searchControls = new HBox(10, new Label("Search:"), searchField, clearBtn);
        searchControls.setAlignment(Pos.CENTER_LEFT);

        HBox dateControls = new HBox(10, new Label("Date:"), startDatePicker, new Label("to"), endDatePicker, filterBtn);
        dateControls.setAlignment(Pos.CENTER_LEFT);

        HBox statsControls = new HBox(15, rowCountLabel, totalAmountLabel);
        statsControls.setAlignment(Pos.CENTER_RIGHT);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getChildren().addAll(searchControls, tagsContainer);
        HBox.setHgrow(tagsContainer, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, searchRow, dateControls, statsControls, spacer, addBtn, refreshBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<SaleInvoice, Integer> serialCol = new TableColumn<>("S.No");
        serialCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleObjectProperty<>(index + 1);
        });
        serialCol.setPrefWidth(70);
        table.getColumns().add(serialCol);
        table.getColumns().add(col("Invoice No", "invoiceNo", 150));
        table.getColumns().add(col("Invoice Date", "invoiceDate", 120));
        table.getColumns().add(col("Party Name", "partyName", 200));
        table.getColumns().add(col("Voucher Type", "voucherType", 120));
        
        // Total Amount column with custom cell factory for 2 decimal places
        TableColumn<SaleInvoice, Double> totalAmountCol = new TableColumn<>("Total Amount");
        totalAmountCol.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        totalAmountCol.setPrefWidth(120);
        totalAmountCol.setCellFactory(col -> new TableCell<SaleInvoice, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        table.getColumns().add(totalAmountCol);
        table.getColumns().add(col("Remarks", "remarks", 200));

        // Created At column with custom date formatting
        TableColumn<SaleInvoice, Object> createdAtCol = new TableColumn<>("Created At");
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtCol.setPrefWidth(120);
        createdAtCol.setCellFactory(column -> new TableCell<SaleInvoice, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof java.time.LocalDateTime) {
                    setText(((java.time.LocalDateTime) item).format(DATE_FORMATTER));
                } else if (item instanceof java.time.LocalDate) {
                    setText(((java.time.LocalDate) item).format(DATE_FORMATTER));
                } else {
                    setText(item.toString());
                }
            }
        });
        table.getColumns().add(createdAtCol);

        table.setItems(rows);

        // Right-click context menu
        ContextMenu ctxMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            SaleInvoice selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new SaleInvoiceDialog(selected).show(MainApp.getPrimaryStage(), this::loadRows);
            }
        });

        MenuItem printItem = new MenuItem("Print");
        printItem.setOnAction(e -> {
            SaleInvoice selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                printInvoice(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            SaleInvoice selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean confirmed = AlertUtil.showConfirmation("Delete Invoice",
                    "Delete invoice '" + selected.getInvoiceNo() + "'? This cannot be undone.");
            if (!confirmed) return;
            AppExecutor.submit(() -> {
                try {
                    dao.delete(selected.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Deleted", "Invoice deleted.");
                        rows.remove(selected);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Error", "Failed to delete: " + ex.getMessage()));
                }
            });
        });

        ctxMenu.getItems().addAll(editItem, printItem, new SeparatorMenuItem(), deleteItem);

        // Show menu only on rows that have data
        table.setRowFactory(tv -> {
            TableRow<SaleInvoice> row = new TableRow<>();
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

        // Save column state when columns change
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<SaleInvoice, ?>>) change -> saveColumnState());
        
        // Load saved column state
        loadColumnState();

        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            try {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                List<SaleInvoice> data = dao.getAll(startDate, endDate);
                ObservableList<SaleInvoice> observableData = FXCollections.observableArrayList(data);
                Platform.runLater(() -> {
                    rows.setAll(observableData);
                    updateStats(rows);
                });
            } catch (Exception ignored) {
                Platform.runLater(() -> {
                    rows.clear();
                    updateStats(rows);
                });
            }
        });
    }

    private void updateStats(ObservableList<SaleInvoice> data) {
        rowCountLabel.setText("Total: " + data.size());
        double total = data.stream().mapToDouble(SaleInvoice::getNetAmount).sum();
        totalAmountLabel.setText("Total Amount: ₹" + String.format("%.2f", total));
    }

    private void searchRows() {
        String liveSearchText = searchField.getText().trim();
        
        if (searchTerms.isEmpty() && liveSearchText.isEmpty() && startDatePicker.getValue() == null && endDatePicker.getValue() == null) {
            loadRows();
            return;
        }
        
        AppExecutor.submit(() -> {
            try {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                List<SaleInvoice> data = dao.getAll(startDate, endDate);
                
                // If there are search terms (chips), use AND logic
                if (!searchTerms.isEmpty()) {
                    List<SaleInvoice> termResults = dao.searchAllColumns(searchTerms.iterator().next());
                    for (String term : searchTerms) {
                        List<SaleInvoice> results = dao.searchAllColumns(term);
                        termResults.retainAll(results);
                    }
                    data.retainAll(termResults);
                }
                
                // If there's live search text, apply it as additional filter
                if (!liveSearchText.isEmpty()) {
                    List<SaleInvoice> liveResults = dao.searchAllColumns(liveSearchText);
                    data.retainAll(liveResults);
                }
                
                final List<SaleInvoice> finalData = data;
                System.out.println("Search - Terms: " + searchTerms + ", Live text: '" + liveSearchText + "', Date: " + startDate + " to " + endDate + ", Results: " + finalData.size());
                Platform.runLater(() -> {
                    rows.setAll(finalData);
                    updateStats(rows);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    rows.clear();
                    updateStats(rows);
                });
            }
        });
    }

    private void addSearchTerm() {
        String term = searchField.getText().trim();
        if (term.isEmpty() || searchTerms.size() >= MAX_SEARCH_TERMS) {
            return;
        }
        if (!searchTerms.contains(term)) {
            searchTerms.add(term);
            searchTagsList.add(term);
            searchField.clear();
            searchRows();
        }
    }

    private void removeSearchTerm(String term) {
        searchTerms.remove(term);
        searchTagsList.remove(term);
        searchRows();
    }

    private void printInvoice(SaleInvoice invoice) {
        try {
            // Use system temp directory for initial preview (not saved to app folder)
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            String fileName = tempDir.getAbsolutePath() + "/Sale_Invoice_" + invoice.getInvoiceNo() + "_" +
                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            // Generate Original PDF
            com.accounting.util.InvoicePDFGenerator.generateSaleInvoicePDF(invoice, fileName, "Original");

            // Show print preview embedded in app; Close returns to this list
            new PrintPreviewDialog(fileName, (copyLabel, outputPath) ->
                    com.accounting.util.InvoicePDFGenerator.generateSaleInvoicePDF(invoice, outputPath, copyLabel)
            ).showInApp(() -> MainApp.showContentInApp(createContent()));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private TableColumn<SaleInvoice, Object> col(String title, String property, double width) {
        TableColumn<SaleInvoice, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private void saveColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(SaleInvoiceListView.class);
        StringBuilder columnOrder = new StringBuilder();
        StringBuilder columnWidths = new StringBuilder();
        
        for (int i = 0; i < table.getColumns().size(); i++) {
            TableColumn<SaleInvoice, ?> col = table.getColumns().get(i);
            if (i > 0) {
                columnOrder.append(",");
                columnWidths.append(",");
            }
            columnOrder.append(col.getText());
            columnWidths.append((int) col.getWidth());
        }
        
        prefs.put("saleInvoiceTable_columnOrder", columnOrder.toString());
        prefs.put("saleInvoiceTable_columnWidths", columnWidths.toString());
    }

    private void loadColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(SaleInvoiceListView.class);
        String columnOrderStr = prefs.get("saleInvoiceTable_columnOrder", "");
        String columnWidthsStr = prefs.get("saleInvoiceTable_columnWidths", "");
        
        try {
            // Restore column order
            if (!columnOrderStr.isEmpty()) {
                String[] columnNames = columnOrderStr.split(",");
                List<TableColumn<SaleInvoice, ?>> currentColumns = new java.util.ArrayList<>(table.getColumns());
                
                // Reorder columns based on saved order
                for (int i = 0; i < columnNames.length && i < currentColumns.size(); i++) {
                    String targetName = columnNames[i];
                    for (int j = i; j < currentColumns.size(); j++) {
                        if (currentColumns.get(j).getText().equals(targetName)) {
                            // Swap columns
                            TableColumn<SaleInvoice, ?> temp = currentColumns.get(i);
                            currentColumns.set(i, currentColumns.get(j));
                            currentColumns.set(j, temp);
                            break;
                        }
                    }
                }
                
                // Clear and re-add columns in correct order
                table.getColumns().clear();
                table.getColumns().addAll(currentColumns);
            }
            
            // Restore column widths
            if (!columnWidthsStr.isEmpty()) {
                String[] widths = columnWidthsStr.split(",");
                for (int i = 0; i < widths.length && i < table.getColumns().size(); i++) {
                    int width = Integer.parseInt(widths[i]);
                    if (width > 0) {
                        table.getColumns().get(i).setPrefWidth(width);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if preferences are corrupted
            e.printStackTrace();
        }
    }
}
