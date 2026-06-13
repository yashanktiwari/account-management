package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.LedgerDAO;
import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.NotificationUtil;
import com.accounting.util.PartyExcelImporter;
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
import javafx.stage.FileChooser;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.util.Arrays;

public class PartyMasterListView {

    private final PartyDAO dao = new PartyDAO();
    private final LedgerDAO ledgerDAO = new LedgerDAO();
    private final ObservableList<Party> rows = FXCollections.observableArrayList();
    private TableView<Party> table;
    private TextField searchField;
    private final Set<String> searchTerms = new HashSet<>();
    private final ObservableList<String> searchTagsList = FXCollections.observableArrayList();
    private Timer debounceTimer;
    private static final int DEBOUNCE_DELAY = 500;
    private static final int MAX_SEARCH_TERMS = 5;

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Party List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("Add New Party");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            new PartyMasterDialog().show(MainApp.getPrimaryStage(), this::loadRows);
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        Button downloadTemplateBtn = new Button("Download Template");
        downloadTemplateBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        downloadTemplateBtn.setOnAction(e -> downloadTemplate());
 
        Button importBtn = new Button("Import from Excel");
        importBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6;");
        importBtn.setOnAction(e -> importFromExcel());

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
                    Platform.runLater(PartyMasterListView.this::searchRows);
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

        HBox topBar = new HBox(10, searchRow, spacer, addBtn, refreshBtn, downloadTemplateBtn, importBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<Party, Integer> serialCol = new TableColumn<>("S.No");
        serialCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleObjectProperty<>(index + 1);
        });
        serialCol.setPrefWidth(70);
        table.getColumns().add(serialCol);
        table.getColumns().add(col("Company Name", "name",       200));
        table.getColumns().add(col("Owner Name",   "ownerName",  160));
        table.getColumns().add(col("Mobile",       "mobile",     130));
        table.getColumns().add(col("Email",        "email",      200));
        table.getColumns().add(col("Address",      "address",    220));
        table.getColumns().add(col("State",        "state",      130));
        table.getColumns().add(col("City",         "city",       120));
        table.getColumns().add(col("Pin Code",     "pincode",     90));
        table.getColumns().add(col("GST",          "gstin",      160));
        table.getColumns().add(col("PAN Card",     "pan",        130));
        table.getColumns().add(col("CST No.",      "cstNo",      110));
        table.getColumns().add(col("TAN No.",      "tanNo",      110));
        table.getColumns().add(col("TDS",          "tds",         80));
        table.getColumns().add(col("Aadhar No.",   "aadharNo",   150));
        
        // Routes column with custom formatting
        TableColumn<Party, String> routesCol = new TableColumn<>("Routes");
        routesCol.setCellValueFactory(new PropertyValueFactory<>("routes"));
        routesCol.setPrefWidth(200);
        routesCol.setCellFactory(col -> new TableCell<Party, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setText("");
                } else {
                    String[] routes = item.split("\\|");
                    String formatted = String.join(", ", routes);
                    setText(formatted);
                    setWrapText(true);
                }
            }
        });
        table.getColumns().add(routesCol);
        
        // Balance column with custom formatting (Cr/Dr with color)
        TableColumn<Party, Double> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCurrentBalance()));
        balanceCol.setPrefWidth(120);
        balanceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        balanceCol.setCellFactory(col -> new TableCell<Party, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    double balance = item;
                    String suffix = balance >= 0 ? " Cr" : " Dr";
                    setText(String.format("%,.2f%s", Math.abs(balance), suffix));
                    if (balance >= 0) {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });
        table.getColumns().add(balanceCol);
        
        // Created At column with custom date formatting
        TableColumn<Party, java.time.LocalDateTime> createdAtCol = new TableColumn<>("Created At");
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtCol.setPrefWidth(170);
        createdAtCol.setCellFactory(col -> new TableCell<Party, java.time.LocalDateTime>() {
            @Override
            protected void updateItem(java.time.LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
                    setText(item.format(formatter));
                }
            }
        });
        table.getColumns().add(createdAtCol);
        
        // Updated At column with custom date formatting
        TableColumn<Party, java.time.LocalDateTime> updatedAtCol = new TableColumn<>("Updated At");
        updatedAtCol.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        updatedAtCol.setPrefWidth(170);
        updatedAtCol.setCellFactory(col -> new TableCell<Party, java.time.LocalDateTime>() {
            @Override
            protected void updateItem(java.time.LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
                    setText(item.format(formatter));
                }
            }
        });
        table.getColumns().add(updatedAtCol);

        table.setItems(rows);
        
        // Save column state when columns change
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<Party, ?>>) change -> saveColumnState());
        
        // Load saved column state
        loadColumnState();

        // ── Right-click context menu ──────────────────────────────────────────
        ContextMenu ctxMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            Party selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new PartyMasterDialog().show(MainApp.getPrimaryStage(), selected, this::loadRows);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            Party selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean confirmed = AlertUtil.showConfirmation("Delete Party",
                    "Delete '" + selected.getName() + "'? This cannot be undone.");
            if (!confirmed) return;
            AppExecutor.submit(() -> {
                try {
                    dao.delete(selected.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Deleted", "Party deleted.");
                        rows.remove(selected);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Error", "Failed to delete: " + ex.getMessage()));
                }
            });
        });

        ctxMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);

        // Show menu only on rows that have data
        table.setRowFactory(tv -> {
            TableRow<Party> row = new TableRow<>();
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
                List<Party> data = dao.getAll();
                // Calculate balance for each party
                for (Party party : data) {
                    try {
                        double balance = ledgerDAO.getCurrentBalance(party.getId());
                        party.setCurrentBalance(balance);
                    } catch (Exception e) {
                        party.setCurrentBalance(0);
                    }
                }
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
                List<Party> data = null;
                
                // If there are search terms (chips), use AND logic
                if (!searchTerms.isEmpty()) {
                    data = dao.searchAllColumns(searchTerms.iterator().next());
                    for (String term : searchTerms) {
                        List<Party> termResults = dao.searchAllColumns(term);
                        data.retainAll(termResults);
                    }
                }
                
                // If there's live search text, apply it as additional filter
                if (!liveSearchText.isEmpty()) {
                    List<Party> liveResults = dao.searchAllColumns(liveSearchText);
                    if (data == null) {
                        data = liveResults;
                    } else {
                        data.retainAll(liveResults);
                    }
                }
                
                if (data == null) {
                    data = new java.util.ArrayList<>();
                }
                
                final List<Party> finalData = data;
                System.out.println("Search - Terms: " + searchTerms + ", Live text: '" + liveSearchText + "', Results: " + finalData.size());
                Platform.runLater(() -> rows.setAll(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(rows::clear);
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

    private TableColumn<Party, Object> col(String title, String property, double width) {
        TableColumn<Party, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private void saveColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(PartyMasterListView.class);
        StringBuilder columnOrder = new StringBuilder();
        StringBuilder columnWidths = new StringBuilder();
        
        for (int i = 0; i < table.getColumns().size(); i++) {
            TableColumn<Party, ?> col = table.getColumns().get(i);
            if (i > 0) {
                columnOrder.append(",");
                columnWidths.append(",");
            }
            columnOrder.append(col.getText());
            columnWidths.append((int) col.getWidth());
        }
        
        prefs.put("partyTable_columnOrder", columnOrder.toString());
        prefs.put("partyTable_columnWidths", columnWidths.toString());
    }

    private void loadColumnState() {
        Preferences prefs = Preferences.userNodeForPackage(PartyMasterListView.class);
        String columnOrderStr = prefs.get("partyTable_columnOrder", "");
        String columnWidthsStr = prefs.get("partyTable_columnWidths", "");
        
        try {
            // Restore column order
            if (!columnOrderStr.isEmpty()) {
                String[] columnNames = columnOrderStr.split(",");
                List<TableColumn<Party, ?>> currentColumns = new java.util.ArrayList<>(table.getColumns());
                
                // Reorder columns based on saved order
                for (int i = 0; i < columnNames.length && i < currentColumns.size(); i++) {
                    String targetName = columnNames[i];
                    for (int j = i; j < currentColumns.size(); j++) {
                        if (currentColumns.get(j).getText().equals(targetName)) {
                            // Swap columns
                            TableColumn<Party, ?> temp = currentColumns.get(i);
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

    private void downloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Party Import Template");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Party_Import_Template.xlsx");
        
        File file = fileChooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        
        AppExecutor.submit(() -> {
            try {
                PartyExcelImporter.downloadTemplate(file);
                Platform.runLater(() -> AlertUtil.showInfo("Template Downloaded", 
                    "Template saved to: " + file.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to download template: " + e.getMessage()));
            }
        });
    }

    private void importFromExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Parties from Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        
        File file = fileChooser.showOpenDialog(table.getScene().getWindow());
        if (file == null) return;
        
        AppExecutor.submit(() -> {
            PartyExcelImporter.ImportResult result = PartyExcelImporter.importFromExcel(file);
            Platform.runLater(() -> {
                if (result.hasErrors()) {
                    String errorDetails = String.join("\n", result.errorMessages);
                    AlertUtil.showError("Import Completed with Errors", 
                        result.getSummary() + "\n\nErrors:\n" + errorDetails);
                } else {
                    AlertUtil.showInfo("Import Successful", result.getSummary());
                }
                loadRows(); // Refresh the table
            });
        });
    }
}
