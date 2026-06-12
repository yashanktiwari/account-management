package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.LorryReceiptDAO;
import com.accounting.model.LorryReceipt;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.LorryReceiptPDFGenerator;
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
import java.time.format.DateTimeFormatter;

public class LorryReceiptListView {

    private final LorryReceiptDAO dao = new LorryReceiptDAO();
    private final ObservableList<LorryReceipt> rows = FXCollections.observableArrayList();
    private TableView<LorryReceipt> table;
    private TextField searchField;
    private final Set<String> searchTerms = new HashSet<>();
    private final ObservableList<String> searchTagsList = FXCollections.observableArrayList();
    private Timer debounceTimer;
    private static final int DEBOUNCE_DELAY = 500;
    private static final int MAX_SEARCH_TERMS = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Lorry Receipt (LR) List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("New LR");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            new LorryReceiptDialog().show(MainApp.getPrimaryStage(), this::loadRows);
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
            if (debounceTimer != null) debounceTimer.cancel();
            debounceTimer = new Timer();
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(LorryReceiptListView.this::searchRows);
                }
            }, DEBOUNCE_DELAY);
        });

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchTerms.clear();
            searchTagsList.clear();
            loadRows();
        });

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

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getChildren().addAll(searchControls, tagsContainer);
        HBox.setHgrow(tagsContainer, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, searchRow, spacer, addBtn, refreshBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<LorryReceipt, Integer> serialCol = new TableColumn<>("S.No");
        serialCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleObjectProperty<>(index + 1);
        });
        serialCol.setPrefWidth(60);
        table.getColumns().add(serialCol);
        table.getColumns().add(col("LR No", "lrNo", 90));
        table.getColumns().add(col("LR Date", "lrDate", 100));
        table.getColumns().add(col("Vehicle No", "vehicleNo", 110));
        table.getColumns().add(col("From", "fromLocation", 110));
        table.getColumns().add(col("To", "toLocation", 110));
        table.getColumns().add(col("Consignor", "consignorName", 150));
        table.getColumns().add(col("Consignee", "consigneeName", 150));
        table.getColumns().add(col("Freight", "freight", 90));
        table.getColumns().add(col("Total", "total", 90));
        table.getColumns().add(col("Remarks", "remarks", 200));

        TableColumn<LorryReceipt, Object> createdAtCol = new TableColumn<>("Created At");
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtCol.setPrefWidth(100);
        createdAtCol.setCellFactory(column -> new TableCell<LorryReceipt, Object>() {
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
            LorryReceipt selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new LorryReceiptDialog(selected).show(MainApp.getPrimaryStage(), this::loadRows);
            }
        });

        MenuItem printItem = new MenuItem("Print");
        printItem.setOnAction(e -> {
            LorryReceipt selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) printLR(selected);
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            LorryReceipt selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean confirmed = AlertUtil.showConfirmation("Delete LR",
                    "Delete LR '" + selected.getLrNo() + "'? This cannot be undone.");
            if (!confirmed) return;
            AppExecutor.submit(() -> {
                try {
                    dao.delete(selected.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Deleted", "LR deleted.");
                        rows.remove(selected);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Error", "Failed to delete: " + ex.getMessage()));
                }
            });
        });

        ctxMenu.getItems().addAll(editItem, printItem, new SeparatorMenuItem(), deleteItem);

        table.setRowFactory(tv -> {
            TableRow<LorryReceipt> row = new TableRow<>();
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

        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<LorryReceipt, ?>>) change -> saveColumnState());
        loadColumnState();
        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            try {
                List<LorryReceipt> data = dao.getAll();
                Platform.runLater(() -> rows.setAll(data));
            } catch (Exception ignored) {
                Platform.runLater(rows::clear);
            }
        });
    }

    private void searchRows() {
        String liveSearchText = searchField.getText().trim();
        if (searchTerms.isEmpty() && liveSearchText.isEmpty()) {
            loadRows();
            return;
        }
        AppExecutor.submit(() -> {
            try {
                List<LorryReceipt> data = null;
                if (!searchTerms.isEmpty()) {
                    data = dao.searchAllColumns(searchTerms.iterator().next());
                    for (String term : searchTerms) {
                        List<LorryReceipt> termResults = dao.searchAllColumns(term);
                        data.retainAll(termResults);
                    }
                }
                if (!liveSearchText.isEmpty()) {
                    List<LorryReceipt> liveResults = dao.searchAllColumns(liveSearchText);
                    if (data == null) data = liveResults;
                    else data.retainAll(liveResults);
                }
                if (data == null) data = new java.util.ArrayList<>();
                final List<LorryReceipt> finalData = data;
                Platform.runLater(() -> rows.setAll(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(rows::clear);
            }
        });
    }

    private void addSearchTerm() {
        String term = searchField.getText().trim();
        if (term.isEmpty() || searchTerms.size() >= MAX_SEARCH_TERMS) return;
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

    private void printLR(LorryReceipt lr) {
        try {
            // Use system temp directory for initial preview (not saved to app folder)
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            String baseFileName = tempDir.getAbsolutePath() + "/LR_" + lr.getLrNo() + "_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Generate only first copy (Consignee Copy) for preview
            String previewFile = baseFileName + "_CONSIGNEE_COPY.pdf";
            LorryReceiptPDFGenerator.generateLorryReceiptPDF(lr, previewFile, "CONSIGNEE COPY");

            // Show preview - user can save to their preferred location
            new PrintPreviewDialog(previewFile, (copyLabel, outputPath) ->
                    LorryReceiptPDFGenerator.generateLorryReceiptPDF(lr, outputPath, copyLabel)
            ).showInApp(() -> MainApp.showContentInApp(createContent()));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private TableColumn<LorryReceipt, Object> col(String title, String property, double width) {
        TableColumn<LorryReceipt, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private void saveColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(LorryReceiptListView.class);
        StringBuilder columnOrder = new StringBuilder();
        StringBuilder columnWidths = new StringBuilder();
        for (int i = 0; i < table.getColumns().size(); i++) {
            TableColumn<LorryReceipt, ?> col = table.getColumns().get(i);
            if (i > 0) { columnOrder.append(","); columnWidths.append(","); }
            columnOrder.append(col.getText());
            columnWidths.append((int) col.getWidth());
        }
        prefs.put("lrTable_columnOrder", columnOrder.toString());
        prefs.put("lrTable_columnWidths", columnWidths.toString());
    }

    private void loadColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(LorryReceiptListView.class);
        String columnOrderStr = prefs.get("lrTable_columnOrder", "");
        String columnWidthsStr = prefs.get("lrTable_columnWidths", "");
        try {
            if (!columnOrderStr.isEmpty()) {
                String[] columnNames = columnOrderStr.split(",");
                java.util.List<TableColumn<LorryReceipt, ?>> currentColumns = new java.util.ArrayList<>(table.getColumns());
                for (int i = 0; i < columnNames.length && i < currentColumns.size(); i++) {
                    String targetName = columnNames[i];
                    for (int j = i; j < currentColumns.size(); j++) {
                        if (currentColumns.get(j).getText().equals(targetName)) {
                            TableColumn<LorryReceipt, ?> temp = currentColumns.get(i);
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
                    if (width > 0) table.getColumns().get(i).setPrefWidth(width);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
