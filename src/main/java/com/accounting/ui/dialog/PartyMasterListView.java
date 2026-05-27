package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
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

public class PartyMasterListView {

    private final PartyDAO dao = new PartyDAO();
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
            // Only trigger debounced search if there are search terms OR if the field has text
            if (!searchTerms.isEmpty() || (newVal != null && !newVal.trim().isEmpty())) {
                debounceTimer = new Timer();
                debounceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(PartyMasterListView.this::searchRows);
                    }
                }, DEBOUNCE_DELAY);
            }
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
        table.getColumns().add(col("Routes",       "routes",     200));
        table.getColumns().add(col("Created At",   "createdAt",  170));
        table.getColumns().add(col("Updated At",   "updatedAt",  170));

        table.setItems(rows);

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
}