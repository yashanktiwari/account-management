package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.NotificationUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class PartyMasterListView {

    private static final int MAX_TAGS = 5;

    private final PartyDAO dao = new PartyDAO();
    private final ObservableList<Party> rows = FXCollections.observableArrayList();
    private final ObservableList<String> searchTags = FXCollections.observableArrayList();

    private TableView<Party> table;
    private TextField searchField;
    private FlowPane tagsPane;

    public Parent createContent() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(16));

        Label heading = new Label("Party List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("Add New Party");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> new PartyMasterDialog().show(MainApp.getPrimaryStage(), this::loadRows));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> {
            searchTags.clear();
            searchField.clear();
            tagsPane.getChildren().clear();
            loadRows();
        });

        // ── Search field with clear button ────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("Type and press Enter to add search tag…");

        Button clearBtn = new Button("✕");
        clearBtn.setTooltip(new Tooltip("Clear search text"));
        clearBtn.setStyle("-fx-cursor: hand;");
        clearBtn.setOnAction(e -> searchField.clear());

        HBox searchBox = new HBox(0, searchField, clearBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(10, addBtn, refreshBtn, new Label("Search:"), searchBox);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ── Tags chip strip ───────────────────────────────────────────────────
        tagsPane = new FlowPane(6, 4);
        tagsPane.setAlignment(Pos.CENTER_LEFT);

        // ── Debounce: auto-search 400 ms after typing stops ───────────────────
        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> triggerSearch());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());

        // ── Enter key: add tag ────────────────────────────────────────────────
        searchField.setOnAction(e -> {
            String text = searchField.getText().trim();
            if (!text.isEmpty() && !searchTags.contains(text)) {
                if (searchTags.size() >= MAX_TAGS) {
                    NotificationUtil.showWarning("Limit reached", "Maximum " + MAX_TAGS + " search tags allowed.");
                    return;
                }
                searchTags.add(text);
                tagsPane.getChildren().add(buildTagChip(text));
                searchField.clear();
                triggerSearch();
            }
        });

        // ── Table ─────────────────────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Serial number column
        TableColumn<Party, Void> seqCol = new TableColumn<>("#");
        seqCol.setPrefWidth(50);
        seqCol.setSortable(false);
        seqCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        table.getColumns().add(seqCol);
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
        root.getChildren().addAll(heading, actions, tagsPane, table);

        loadRows();
        return root;
    }

    /** Build a chip label for a search tag with a remove (×) button. */
    private HBox buildTagChip(String tag) {
        Label tagLabel = new Label(tag);
        Button removeBtn = new Button("×");
        removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #666; " +
                "-fx-padding: 0 2 0 4; -fx-cursor: hand; -fx-font-size: 12px;");
        removeBtn.setOnAction(e -> {
            searchTags.remove(tag);
            tagsPane.getChildren().removeIf(node -> node instanceof HBox chip &&
                    chip.getChildren().stream()
                        .anyMatch(c -> c instanceof Label l && l.getText().equals(tag)));
            triggerSearch();
        });

        HBox chip = new HBox(4, tagLabel, removeBtn);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(
                "-fx-background-color: #dbe8f8; -fx-background-radius: 12; " +
                "-fx-padding: 3 8 3 8; -fx-border-radius: 12;");
        return chip;
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

    /** Collect all active search terms (tags + current field text) and run search. */
    private void triggerSearch() {
        List<String> allTerms = new ArrayList<>(searchTags);
        String fieldText = searchField.getText().trim();
        if (!fieldText.isEmpty()) allTerms.add(fieldText);

        if (allTerms.isEmpty()) {
            loadRows();
            return;
        }

        AppExecutor.submit(() -> {
            try {
                List<Party> data = dao.searchAllColumns(allTerms);
                Platform.runLater(() -> rows.setAll(data));
            } catch (Exception ignored) {
                Platform.runLater(rows::clear);
            }
        });
    }

    private TableColumn<Party, Object> col(String title, String property, double width) {
        TableColumn<Party, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }
}