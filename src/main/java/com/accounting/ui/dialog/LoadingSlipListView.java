package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.LoadingSlipDAO;
import com.accounting.model.LoadingSlip;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.LoadingSlipPDFGenerator;
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
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

public class LoadingSlipListView {

    private final LoadingSlipDAO dao = new LoadingSlipDAO();
    private final ObservableList<LoadingSlip> rows = FXCollections.observableArrayList();
    private List<LoadingSlip> allData = new java.util.ArrayList<>();
    private TableView<LoadingSlip> table;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Loading Slip List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("New Loading Slip");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            new LoadingSlipDialog().show(MainApp.getPrimaryStage(), this::loadRows);
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
                    Platform.runLater(LoadingSlipListView.this::searchRows);
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

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox row1 = new HBox(8, searchControls, tagsContainer, spacer1, addBtn, refreshBtn);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tagsContainer, Priority.ALWAYS);

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

        TableColumn<LoadingSlip, Integer> serialCol = new TableColumn<>("S.No");
        serialCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleObjectProperty<>(index + 1);
        });
        serialCol.setPrefWidth(70);
        table.getColumns().add(serialCol);
        table.getColumns().add(col("Slip No", "slipNo", 100));

        // Slip Date column with custom date formatting
        TableColumn<LoadingSlip, Object> slipDateCol = new TableColumn<>("Slip Date");
        slipDateCol.setCellValueFactory(new PropertyValueFactory<>("slipDate"));
        slipDateCol.setPrefWidth(120);
        slipDateCol.setCellFactory(column -> new TableCell<LoadingSlip, Object>() {
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
        table.getColumns().add(slipDateCol);

        table.getColumns().add(col("Party Name", "partyName", 200));
        table.getColumns().add(col("Vehicle No", "vehicleNo", 120));
        table.getColumns().add(col("Station", "station", 120));
        table.getColumns().add(col("To", "toLocation", 120));
        table.getColumns().add(amountCol("Freight", "freightAmount", 120));
        table.getColumns().add(amountCol("Advance", "advanceAmount", 120));
        table.getColumns().add(amountCol("Balance", "balanceAmount", 120));
        table.getColumns().add(col("Remarks", "remarks", 200));

        // Created At column with custom date formatting
        TableColumn<LoadingSlip, Object> createdAtCol = new TableColumn<>("Created At");
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtCol.setPrefWidth(120);
        createdAtCol.setCellFactory(column -> new TableCell<LoadingSlip, Object>() {
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
            LoadingSlip selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new LoadingSlipDialog(selected).show(MainApp.getPrimaryStage(), this::loadRows);
            }
        });

        MenuItem printItem = new MenuItem("Print");
        printItem.setOnAction(e -> {
            LoadingSlip selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                printSlip(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            LoadingSlip selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean confirmed = AlertUtil.showConfirmation("Delete Loading Slip",
                    "Delete loading slip '" + selected.getSlipNo() + "'? This cannot be undone.");
            if (!confirmed) return;
            AppExecutor.submit(() -> {
                try {
                    dao.delete(selected.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Deleted", "Loading slip deleted.");
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
            TableRow<LoadingSlip> row = new TableRow<>();
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
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<LoadingSlip, ?>>) change -> saveColumnState());

        // Load saved column state
        loadColumnState();

        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            try {
                List<LoadingSlip> data = dao.getAll();
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
        List<LoadingSlip> filtered = new java.util.ArrayList<>(allData);

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null) {
            filtered = filtered.stream()
                .filter(r -> r.getSlipDate() != null && !r.getSlipDate().isBefore(start) && !r.getSlipDate().isAfter(end))
                .collect(Collectors.toList());
        } else if (start != null) {
            filtered = filtered.stream()
                .filter(r -> r.getSlipDate() != null && !r.getSlipDate().isBefore(start))
                .collect(Collectors.toList());
        } else if (end != null) {
            filtered = filtered.stream()
                .filter(r -> r.getSlipDate() != null && !r.getSlipDate().isAfter(end))
                .collect(Collectors.toList());
        }

        String liveSearchText = searchField.getText().trim().toLowerCase();
        if (!searchTerms.isEmpty() || !liveSearchText.isEmpty()) {
            filtered = filtered.stream().filter(slip -> {
                String combined = ((slip.getSlipNo() != null ? slip.getSlipNo() : "") + " "
                    + (slip.getPartyName() != null ? slip.getPartyName() : "") + " "
                    + (slip.getVehicleNo() != null ? slip.getVehicleNo() : "") + " "
                    + (slip.getStation() != null ? slip.getStation() : "") + " "
                    + (slip.getToLocation() != null ? slip.getToLocation() : "") + " "
                    + (slip.getRemarks() != null ? slip.getRemarks() : "")).toLowerCase();
                for (String term : searchTerms) {
                    if (!combined.contains(term.toLowerCase())) return false;
                }
                if (!liveSearchText.isEmpty() && !combined.contains(liveSearchText)) return false;
                return true;
            }).collect(Collectors.toList());
        }

        rows.setAll(filtered);
        updateStats();
    }

    private void searchRows() {
        applyFilters();
    }

    private void updateStats() {
        rowCountLabel.setText("Total: " + rows.size());
        double total = rows.stream().mapToDouble(LoadingSlip::getFreightAmount).sum();
        totalAmountLabel.setText("Total Amount: \u20B9" + String.format("%.2f", total));
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

    private void printSlip(LoadingSlip slip) {
        try {
            // Use system temp directory for initial preview (not saved to app folder)
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            String fileName = tempDir.getAbsolutePath() + "/Loading_Slip_" + slip.getSlipNo() + "_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

            LoadingSlipPDFGenerator.generateLoadingSlipPDF(slip, fileName, "Original");

            new PrintPreviewDialog(fileName, (copyLabel, outputPath) ->
                    LoadingSlipPDFGenerator.generateLoadingSlipPDF(slip, outputPath, copyLabel)
            ).showInApp(() -> MainApp.showContentInApp(createContent()));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private TableColumn<LoadingSlip, Object> col(String title, String property, double width) {
        TableColumn<LoadingSlip, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<LoadingSlip, Object> amountCol(String title, String property, double width) {
        TableColumn<LoadingSlip, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        column.setCellFactory(c -> new TableCell<LoadingSlip, Object>() {
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
        return column;
    }

    private void saveColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(LoadingSlipListView.class);
        StringBuilder columnOrder = new StringBuilder();
        StringBuilder columnWidths = new StringBuilder();

        for (int i = 0; i < table.getColumns().size(); i++) {
            TableColumn<LoadingSlip, ?> col = table.getColumns().get(i);
            if (i > 0) {
                columnOrder.append(",");
                columnWidths.append(",");
            }
            columnOrder.append(col.getText());
            columnWidths.append((int) col.getWidth());
        }

        prefs.put("loadingSlipTable_columnOrder", columnOrder.toString());
        prefs.put("loadingSlipTable_columnWidths", columnWidths.toString());
    }

    private void loadColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(LoadingSlipListView.class);
        String columnOrderStr = prefs.get("loadingSlipTable_columnOrder", "");
        String columnWidthsStr = prefs.get("loadingSlipTable_columnWidths", "");

        try {
            if (!columnOrderStr.isEmpty()) {
                String[] columnNames = columnOrderStr.split(",");
                java.util.List<TableColumn<LoadingSlip, ?>> currentColumns = new java.util.ArrayList<>(table.getColumns());

                for (int i = 0; i < columnNames.length && i < currentColumns.size(); i++) {
                    String targetName = columnNames[i];
                    for (int j = i; j < currentColumns.size(); j++) {
                        if (currentColumns.get(j).getText().equals(targetName)) {
                            TableColumn<LoadingSlip, ?> temp = currentColumns.get(i);
                            currentColumns.set(i, currentColumns.get(j));
                            currentColumns.set(j, temp);
                            break;
                        }
                    }
                }

                table.getColumns().clear();
                table.getColumns().addAll(currentColumns);
            }

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
            e.printStackTrace();
        }
    }
}
