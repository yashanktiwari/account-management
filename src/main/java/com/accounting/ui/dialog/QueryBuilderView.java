package com.accounting.ui.dialog;

import com.accounting.dao.QueryBuilderDAO;
import com.accounting.dao.QueryBuilderDAO.*;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;

import java.util.*;

public class QueryBuilderView {

    private static final Logger log = AppLogger.get(QueryBuilderView.class);

    private final QueryBuilderDAO dao = new QueryBuilderDAO();
    private final List<DataSourceDef> dataSources = QueryBuilderDAO.getDataSources();

    private ComboBox<DataSourceDef> dataSourceCombo;
    private VBox filterRowsBox;
    private RadioButton detailRadio, summaryRadio;
    private HBox summaryOptionsBox;
    private ComboBox<FieldDef> groupByCombo;
    private ComboBox<String> aggFunctionCombo;
    private FlowPane aggFieldsPane;
    private TableView<Map<String, Object>> resultsTable;
    private Label statusLabel;

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label heading = new Label("Custom Query Builder");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        HBox toolbar = buildToolbar();
        VBox filterSection = buildFilterSection();
        HBox modeSection = buildModeSection();
        VBox resultsSection = buildResultsSection();

        root.getChildren().addAll(heading, toolbar, filterSection, modeSection, resultsSection);
        VBox.setVgrow(resultsSection, Priority.ALWAYS);

        return root;
    }

    // ── Toolbar: data source + run button ───────────────────────────────────
    private HBox buildToolbar() {
        Label dsLabel = new Label("Data Source:");
        dsLabel.setStyle("-fx-font-weight: bold;");

        dataSourceCombo = new ComboBox<>(FXCollections.observableArrayList(dataSources));
        dataSourceCombo.setPrefWidth(200);
        dataSourceCombo.setOnAction(e -> onDataSourceChanged());

        Button runBtn = new Button("▶  Run Query");
        runBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 24 8 24; -fx-background-radius: 6; -fx-font-size: 13px;");
        runBtn.setOnAction(e -> runQuery());

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: bold; " +
                "-fx-padding: 8 16 8 16; -fx-background-radius: 6;");
        clearBtn.setOnAction(e -> clearAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, dsLabel, dataSourceCombo, spacer, clearBtn, runBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    // ── Filter section ──────────────────────────────────────────────────────
    private VBox buildFilterSection() {
        Label title = new Label("Filters");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        filterRowsBox = new VBox(6);
        filterRowsBox.setPadding(new Insets(8));

        Button addFilterBtn = new Button("+ Add Filter");
        addFilterBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        addFilterBtn.setOnAction(e -> addFilterRow());

        VBox section = new VBox(6, title, filterRowsBox, addFilterBtn);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 8; -fx-background-radius: 8;");
        return section;
    }

    private void addFilterRow() {
        DataSourceDef source = dataSourceCombo.getValue();
        if (source == null) {
            AlertUtil.showWarning("Query Builder", "Please select a data source first.");
            return;
        }

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Connector (AND/OR) — hidden for first row
        ComboBox<String> connectorCombo = new ComboBox<>(FXCollections.observableArrayList("AND", "OR"));
        connectorCombo.setValue("AND");
        connectorCombo.setPrefWidth(70);
        connectorCombo.setVisible(filterRowsBox.getChildren().size() > 0);
        connectorCombo.setManaged(filterRowsBox.getChildren().size() > 0);

        // Field
        ComboBox<FieldDef> fieldCombo = new ComboBox<>(FXCollections.observableArrayList(source.getFields()));
        fieldCombo.setPromptText("Select Field");
        fieldCombo.setPrefWidth(160);

        // Operator
        ComboBox<String> operatorCombo = new ComboBox<>();
        operatorCombo.setPromptText("Operator");
        operatorCombo.setPrefWidth(120);

        // Value
        TextField valueField = new TextField();
        valueField.setPromptText("Value");
        valueField.setPrefWidth(180);

        // When field changes, update operators
        fieldCombo.setOnAction(e -> {
            FieldDef selected = fieldCombo.getValue();
            if (selected != null) {
                operatorCombo.setItems(FXCollections.observableArrayList(QueryBuilderDAO.getOperators(selected.getType())));
                operatorCombo.setValue(null);
                valueField.clear();
                if (selected.getType() == FieldType.DATE) {
                    valueField.setPromptText("dd-MM-yyyy");
                } else if (selected.getType() == FieldType.NUMBER || selected.getType() == FieldType.CURRENCY) {
                    valueField.setPromptText("Number");
                } else {
                    valueField.setPromptText("Value");
                }
            }
        });

        // When operator changes, show/hide value field
        operatorCombo.setOnAction(e -> {
            String op = operatorCombo.getValue();
            boolean needsValue = op != null && QueryBuilderDAO.operatorNeedsValue(op);
            valueField.setVisible(needsValue);
            valueField.setManaged(needsValue);
        });

        // Remove button
        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                "-fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> {
            filterRowsBox.getChildren().remove(row);
            // Fix visibility of first connector
            if (!filterRowsBox.getChildren().isEmpty()) {
                HBox firstRow = (HBox) filterRowsBox.getChildren().get(0);
                firstRow.getChildren().get(0).setVisible(false);
                firstRow.getChildren().get(0).setManaged(false);
            }
        });

        row.getChildren().addAll(connectorCombo, fieldCombo, operatorCombo, valueField, removeBtn);
        filterRowsBox.getChildren().add(row);
    }

    // ── Mode section (Detail / Summary) ─────────────────────────────────────
    private HBox buildModeSection() {
        Label modeLabel = new Label("Mode:");
        modeLabel.setStyle("-fx-font-weight: bold;");

        ToggleGroup modeGroup = new ToggleGroup();
        detailRadio = new RadioButton("Detail");
        detailRadio.setToggleGroup(modeGroup);
        detailRadio.setSelected(true);

        summaryRadio = new RadioButton("Summary");
        summaryRadio.setToggleGroup(modeGroup);

        // Summary options
        Label groupLabel = new Label("Group By:");
        groupByCombo = new ComboBox<>();
        groupByCombo.setPromptText("(Optional)");
        groupByCombo.setPrefWidth(150);

        Label aggLabel = new Label("Function:");
        aggFunctionCombo = new ComboBox<>(FXCollections.observableArrayList("SUM", "COUNT", "AVG", "MIN", "MAX"));
        aggFunctionCombo.setValue("SUM");
        aggFunctionCombo.setPrefWidth(90);

        Label aggFieldLabel = new Label("Fields:");
        aggFieldsPane = new FlowPane(8, 4);
        aggFieldsPane.setPrefWidth(300);

        summaryOptionsBox = new HBox(8, groupLabel, groupByCombo, aggLabel, aggFunctionCombo, aggFieldLabel, aggFieldsPane);
        summaryOptionsBox.setAlignment(Pos.CENTER_LEFT);
        summaryOptionsBox.setVisible(false);
        summaryOptionsBox.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            boolean isSummary = newVal == summaryRadio;
            summaryOptionsBox.setVisible(isSummary);
            summaryOptionsBox.setManaged(isSummary);
        });

        HBox modeRow = new HBox(12, modeLabel, detailRadio, summaryRadio);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        HBox section = new HBox(20, modeRow, summaryOptionsBox);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(8, 10, 8, 10));
        section.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 6; -fx-background-radius: 6;");
        return section;
    }

    // ── Results section ─────────────────────────────────────────────────────
    private VBox buildResultsSection() {
        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("Select a data source, add filters, and click 'Run Query'"));
        resultsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        VBox section = new VBox(6, resultsTable, statusLabel);
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }

    // ── Data source changed ─────────────────────────────────────────────────
    private void onDataSourceChanged() {
        filterRowsBox.getChildren().clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        statusLabel.setText("Ready");

        DataSourceDef source = dataSourceCombo.getValue();
        if (source == null) return;

        // Update group by combo
        groupByCombo.setItems(FXCollections.observableArrayList(source.getFields()));
        groupByCombo.setValue(null);

        // Update aggregate fields checkboxes
        aggFieldsPane.getChildren().clear();
        for (FieldDef f : source.getNumericFields()) {
            CheckBox cb = new CheckBox(f.getDisplay());
            cb.setUserData(f);
            cb.setStyle("-fx-font-size: 11px;");
            aggFieldsPane.getChildren().add(cb);
        }

        // Auto-add one filter row
        addFilterRow();
    }

    // ── Clear all ───────────────────────────────────────────────────────────
    private void clearAll() {
        filterRowsBox.getChildren().clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        statusLabel.setText("Ready");
        detailRadio.setSelected(true);
    }

    // ── Run query ───────────────────────────────────────────────────────────
    private void runQuery() {
        DataSourceDef source = dataSourceCombo.getValue();
        if (source == null) {
            AlertUtil.showWarning("Query Builder", "Please select a data source.");
            return;
        }

        // Collect filters
        List<FilterDef> filters = collectFilters();

        boolean isSummary = summaryRadio.isSelected();

        statusLabel.setText("Running query...");
        resultsTable.getItems().clear();

        AppExecutor.submit(() -> {
            try {
                List<Map<String, Object>> results;

                if (isSummary) {
                    FieldDef groupBy = groupByCombo.getValue();
                    String aggFunc = aggFunctionCombo.getValue();
                    List<FieldDef> aggFields = getSelectedAggFields();

                    if (aggFields.isEmpty()) {
                        Platform.runLater(() -> {
                            AlertUtil.showWarning("Query Builder", "Please select at least one numeric field to aggregate.");
                            statusLabel.setText("Ready");
                        });
                        return;
                    }

                    results = dao.executeSummaryQuery(source, filters, groupBy, aggFunc, aggFields);
                } else {
                    results = dao.executeDetailQuery(source, filters);
                }

                final List<Map<String, Object>> finalResults = results;
                Platform.runLater(() -> displayResults(finalResults));

            } catch (Exception e) {
                log.error("Query execution failed", e);
                Platform.runLater(() -> {
                    AlertUtil.showError("Query Error", "Failed to execute query: " + e.getMessage());
                    statusLabel.setText("Error");
                });
            }
        });
    }

    // ── Collect filters from UI ─────────────────────────────────────────────
    private List<FilterDef> collectFilters() {
        List<FilterDef> filters = new ArrayList<>();
        for (var node : filterRowsBox.getChildren()) {
            if (!(node instanceof HBox row)) continue;

            ComboBox<String> connCombo = (ComboBox<String>) row.getChildren().get(0);
            ComboBox<FieldDef> fieldCombo = (ComboBox<FieldDef>) row.getChildren().get(1);
            ComboBox<String> opCombo = (ComboBox<String>) row.getChildren().get(2);
            TextField valueField = (TextField) row.getChildren().get(3);

            FieldDef field = fieldCombo.getValue();
            String operator = opCombo.getValue();
            if (field == null || operator == null) continue;

            String value = valueField.getText().trim();
            if (QueryBuilderDAO.operatorNeedsValue(operator) && value.isEmpty()) continue;

            String connector = filters.isEmpty() ? "AND" : connCombo.getValue();
            filters.add(new FilterDef(connector, field, operator, value));
        }
        return filters;
    }

    // ── Get selected aggregate fields ───────────────────────────────────────
    private List<FieldDef> getSelectedAggFields() {
        List<FieldDef> fields = new ArrayList<>();
        for (var node : aggFieldsPane.getChildren()) {
            if (node instanceof CheckBox cb && cb.isSelected()) {
                fields.add((FieldDef) cb.getUserData());
            }
        }
        return fields;
    }

    // ── Display results in dynamic table ────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void displayResults(List<Map<String, Object>> results) {
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();

        if (results.isEmpty()) {
            statusLabel.setText("No results found.");
            return;
        }

        // Create columns from first row's keys
        Map<String, Object> firstRow = results.get(0);
        int colIndex = 0;
        for (String key : firstRow.keySet()) {
            final String columnKey = key;
            TableColumn<Map<String, Object>, Object> col = new TableColumn<>(formatColumnName(key));
            col.setCellValueFactory(param ->
                new javafx.beans.property.SimpleObjectProperty<>(param.getValue().get(columnKey)));

            // Format currency/numeric cells
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else if (item instanceof Number num) {
                        setText(String.format("%,.2f", num.doubleValue()));
                        setStyle("-fx-alignment: CENTER-RIGHT;");
                    } else {
                        setText(item.toString());
                        setStyle("");
                    }
                }
            });

            col.setPrefWidth(colIndex == 0 ? 140 : 120);
            resultsTable.getColumns().add(col);
            colIndex++;
        }

        resultsTable.getItems().addAll(results);

        // Calculate totals for numeric columns
        StringBuilder summary = new StringBuilder();
        summary.append(results.size()).append(" rows");

        for (String key : firstRow.keySet()) {
            Object sample = firstRow.get(key);
            if (sample instanceof Number) {
                double total = results.stream()
                    .mapToDouble(r -> r.get(key) instanceof Number n ? n.doubleValue() : 0)
                    .sum();
                summary.append("  |  ").append(formatColumnName(key)).append(": ").append(String.format("%,.2f", total));
            }
        }

        statusLabel.setText(summary.toString());
        statusLabel.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    // ── Format column name for display ──────────────────────────────────────
    private String formatColumnName(String sqlName) {
        if (sqlName.contains("(")) return sqlName; // Already formatted (aggregate)
        return sqlName.replace("_", " ").substring(0, 1).toUpperCase() +
               sqlName.replace("_", " ").substring(1);
    }
}
