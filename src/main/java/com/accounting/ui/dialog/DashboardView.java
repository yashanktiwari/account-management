package com.accounting.ui.dialog;

import com.accounting.dao.SettingsDAO;
import com.accounting.dao.WidgetDAO;
import com.accounting.dao.WidgetDAO.WidgetData;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class DashboardView {

    private static final Logger log = AppLogger.get(DashboardView.class);
    private static final String SETTINGS_KEY = "dashboard_active_widgets";

    private final WidgetDAO widgetDAO = new WidgetDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private FlowPane widgetGrid;
    private final List<WidgetDef> allWidgets = new ArrayList<>();
    private final List<String> activeWidgetIds = new ArrayList<>();

    // ── Widget definition ───────────────────────────────────────────────────
    private static class WidgetDef {
        final String id, name, category, color;
        final Supplier<WidgetData> fetcher;

        WidgetDef(String id, String name, String category, String color, Supplier<WidgetData> fetcher) {
            this.id = id; this.name = name; this.category = category;
            this.color = color; this.fetcher = fetcher;
        }
    }

    public DashboardView() {
        registerWidgets();
        loadActiveWidgets();
    }

    private void registerWidgets() {
        // Recent Activity
        allWidgets.add(new WidgetDef("latest_purchase_inv", "Latest Purchase Invoices", "Recent Activity", "#16a34a", () -> widgetDAO.getLatestPurchaseInvoices(5)));
        allWidgets.add(new WidgetDef("latest_sale_inv", "Latest Sale Invoices", "Recent Activity", "#2563eb", () -> widgetDAO.getLatestSaleInvoices(5)));
        allWidgets.add(new WidgetDef("latest_purchase_rec", "Latest Purchase Receipts", "Recent Activity", "#dc2626", () -> widgetDAO.getLatestPurchaseReceipts(5)));
        allWidgets.add(new WidgetDef("latest_sale_rec", "Latest Sale Receipts", "Recent Activity", "#0891b2", () -> widgetDAO.getLatestSaleReceipts(5)));
        allWidgets.add(new WidgetDef("latest_lr", "Latest Lorry Receipts", "Recent Activity", "#7c3aed", () -> widgetDAO.getLatestLorryReceipts(5)));
        allWidgets.add(new WidgetDef("latest_ls", "Latest Loading Slips", "Recent Activity", "#d97706", () -> widgetDAO.getLatestLoadingSlips(5)));

        // Top Performers
        allWidgets.add(new WidgetDef("top_purchase_parties", "Top Parties by Purchase", "Top Performers", "#16a34a", () -> widgetDAO.getTopPartiesByPurchase(5)));
        allWidgets.add(new WidgetDef("top_sale_parties", "Top Parties by Sale", "Top Performers", "#2563eb", () -> widgetDAO.getTopPartiesBySale(5)));
        allWidgets.add(new WidgetDef("top_vehicles", "Top Vehicles by Revenue", "Top Performers", "#7c3aed", () -> widgetDAO.getTopVehiclesByRevenue(5)));

        // Party Insights
        allWidgets.add(new WidgetDef("least_interacted", "Least Interacted Parties", "Party Insights", "#ea580c", () -> widgetDAO.getLeastInteractedParties(5)));
        allWidgets.add(new WidgetDef("longest_inactive", "Longest Inactive Parties", "Party Insights", "#dc2626", () -> widgetDAO.getLongestInactiveParties(5)));
        allWidgets.add(new WidgetDef("party_count", "Party Count by Type", "Party Insights", "#0891b2", () -> widgetDAO.getPartyCountByType()));

        // Financial Summary
        allWidgets.add(new WidgetDef("purchase_gst", "Purchase GST Summary (This Year)", "Financial Summary", "#16a34a", () -> widgetDAO.getPurchaseGSTSummary()));
        allWidgets.add(new WidgetDef("sale_gst", "Sale GST Summary (This Year)", "Financial Summary", "#2563eb", () -> widgetDAO.getSaleGSTSummary()));
        allWidgets.add(new WidgetDef("monthly_purchase", "Monthly Purchase Summary", "Financial Summary", "#d97706", () -> widgetDAO.getMonthlyPurchaseSummary()));
        allWidgets.add(new WidgetDef("monthly_sale", "Monthly Sale Summary", "Financial Summary", "#0891b2", () -> widgetDAO.getMonthlySaleSummary()));
    }

    private void loadActiveWidgets() {
        try {
            String saved = settingsDAO.getSetting(SETTINGS_KEY);
            if (saved != null && !saved.isBlank()) {
                activeWidgetIds.addAll(Arrays.asList(saved.split(",")));
            } else {
                // Default widgets
                activeWidgetIds.addAll(List.of(
                    "latest_purchase_inv", "latest_sale_inv", "top_vehicles",
                    "longest_inactive", "purchase_gst", "sale_gst"
                ));
            }
        } catch (Exception e) {
            log.error("Failed to load widget settings", e);
            activeWidgetIds.addAll(List.of("latest_purchase_inv", "latest_sale_inv"));
        }
    }

    private void saveActiveWidgets() {
        AppExecutor.submit(() -> {
            try {
                settingsDAO.saveSetting(SETTINGS_KEY, String.join(",", activeWidgetIds));
            } catch (Exception e) {
                log.error("Failed to save widget settings", e);
            }
        });
    }

    // ── Build UI ────────────────────────────────────────────────────────────
    public Parent createContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        // Header
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label subtitle = new Label("Your customizable business overview");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Button addWidgetBtn = new Button("+ Add Widget");
        addWidgetBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 20 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        addWidgetBtn.setOnAction(e -> showWidgetPicker());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: bold; " +
                "-fx-padding: 8 16 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshAllWidgets());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, title, spacer, refreshBtn, addWidgetBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Widget grid
        widgetGrid = new FlowPane(16, 16);
        widgetGrid.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(widgetGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, subtitle, scrollPane);

        // Load widgets asynchronously
        refreshAllWidgets();

        return root;
    }

    // ── Refresh all widgets ─────────────────────────────────────────────────
    private void refreshAllWidgets() {
        widgetGrid.getChildren().clear();

        for (String widgetId : activeWidgetIds) {
            WidgetDef def = findWidget(widgetId);
            if (def == null) continue;

            VBox card = buildWidgetCard(def, null, true);
            widgetGrid.getChildren().add(card);

            // Load data async
            AppExecutor.submit(() -> {
                try {
                    WidgetData data = def.fetcher.get();
                    Platform.runLater(() -> {
                        int idx = widgetGrid.getChildren().indexOf(card);
                        if (idx >= 0) {
                            VBox loaded = buildWidgetCard(def, data, false);
                            widgetGrid.getChildren().set(idx, loaded);
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to load widget: {}", def.name, e);
                }
            });
        }

        if (activeWidgetIds.isEmpty()) {
            Label empty = new Label("No widgets added. Click '+ Add Widget' to get started.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-padding: 40;");
            widgetGrid.getChildren().add(empty);
        }
    }

    // ── Build a single widget card ──────────────────────────────────────────
    private VBox buildWidgetCard(WidgetDef def, WidgetData data, boolean loading) {
        // Title bar
        Label titleLabel = new Label(def.name);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button removeBtn = new Button("x");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-font-size: 12px;");
        removeBtn.setOnAction(e -> removeWidget(def.id));

        HBox titleBar = new HBox(8, titleLabel, removeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 12, 10, 12));
        titleBar.setStyle("-fx-background-color: " + def.color + "; -fx-background-radius: 8 8 0 0;");

        // Content
        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(8));
        contentBox.setMinHeight(120);

        if (loading) {
            Label loadLabel = new Label("Loading...");
            loadLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            contentBox.getChildren().add(loadLabel);
            contentBox.setAlignment(Pos.CENTER);
        } else if (data != null && !data.getRows().isEmpty()) {
            contentBox.getChildren().add(buildDataGrid(data));
        } else {
            Label noData = new Label("No data available");
            noData.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            contentBox.getChildren().add(noData);
            contentBox.setAlignment(Pos.CENTER);
        }

        VBox card = new VBox(0, titleBar, contentBox);
        card.setPrefWidth(370);
        card.setMinWidth(340);
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        return card;
    }

    // ── Build data grid inside widget ───────────────────────────────────────
    private GridPane buildDataGrid(WidgetData data) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(4);
        grid.setPadding(new Insets(4));

        List<String> headers = data.getHeaders();
        List<List<String>> rows = data.getRows();

        // Header row
        for (int c = 0; c < headers.size(); c++) {
            Label h = new Label(headers.get(c));
            h.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            h.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(h, Priority.ALWAYS);
            grid.add(h, c, 0);
        }

        // Separator
        Separator sep = new Separator();
        grid.add(sep, 0, 1, headers.size(), 1);

        // Data rows
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size() && c < headers.size(); c++) {
                Label cell = new Label(row.get(c));
                cell.setStyle("-fx-font-size: 11px; -fx-text-fill: #334155;");
                cell.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(cell, Priority.ALWAYS);

                // Right-align numeric-looking values
                String val = row.get(c);
                if (val.matches("^[\\d,.]+$")) {
                    cell.setStyle("-fx-font-size: 11px; -fx-text-fill: #1e3a5f; -fx-font-weight: bold;");
                    cell.setAlignment(Pos.CENTER_RIGHT);
                }
                grid.add(cell, c, r + 2);
            }
        }

        // Set column constraints to distribute space
        for (int c = 0; c < headers.size(); c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            if (c == 0) cc.setPercentWidth(40);
            grid.getColumnConstraints().add(cc);
        }

        return grid;
    }

    // ── Remove widget ───────────────────────────────────────────────────────
    private void removeWidget(String widgetId) {
        activeWidgetIds.remove(widgetId);
        saveActiveWidgets();
        refreshAllWidgets();
    }

    // ── Widget picker dialog ────────────────────────────────────────────────
    private void showWidgetPicker() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Add Widgets");
        dialog.setHeaderText("Select widgets to display on your dashboard");
        dialog.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));

        Map<String, List<WidgetDef>> categories = new LinkedHashMap<>();
        for (WidgetDef w : allWidgets) {
            categories.computeIfAbsent(w.category, k -> new ArrayList<>()).add(w);
        }

        Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();

        for (var entry : categories.entrySet()) {
            Label catLabel = new Label(entry.getKey());
            catLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

            VBox catBox = new VBox(4);
            for (WidgetDef w : entry.getValue()) {
                CheckBox cb = new CheckBox(w.name);
                cb.setSelected(activeWidgetIds.contains(w.id));
                cb.setStyle("-fx-font-size: 12px;");
                checkBoxes.put(w.id, cb);
                catBox.getChildren().add(cb);
            }

            content.getChildren().addAll(catLabel, catBox);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                List<String> selected = new ArrayList<>();
                for (var e : checkBoxes.entrySet()) {
                    if (e.getValue().isSelected()) selected.add(e.getKey());
                }
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selected -> {
            activeWidgetIds.clear();
            activeWidgetIds.addAll(selected);
            saveActiveWidgets();
            refreshAllWidgets();
        });
    }

    // ── Find widget by ID ───────────────────────────────────────────────────
    private WidgetDef findWidget(String id) {
        return allWidgets.stream().filter(w -> w.id.equals(id)).findFirst().orElse(null);
    }
}
