package com.accounting.ui.dialog;

import com.accounting.dao.SettingsDAO;
import com.accounting.dao.WidgetDAO;
import com.accounting.dao.WidgetDAO.WidgetData;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class DashboardView {

    private static final Logger log = AppLogger.get(DashboardView.class);
    private static final String SETTINGS_KEY = "dashboard_active_widgets";
    private static final String[] CHART_COLORS = {
        "#4a7a94", "#7a5565", "#4a7a60", "#8a7a4a", "#6b5a8a", "#8a5a6a",
        "#4a8a7a", "#8a6a4a", "#5a5a8a", "#6a8a5a", "#4a7a8a", "#8a4a6a"
    };

    private final WidgetDAO widgetDAO = new WidgetDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private FlowPane widgetGrid;
    private final List<WidgetDef> allWidgets = new ArrayList<>();
    private final List<String> activeWidgetIds = new ArrayList<>();

    enum RenderType { TABLE, BAR_CHART, PIE_CHART, LINE_CHART }

    // ── Widget definition ───────────────────────────────────────────────────
    private static class WidgetDef {
        final String id, name, category, icon, color1, color2;
        final RenderType renderType;
        final int labelCol, valueCol;
        final Supplier<WidgetData> fetcher;

        WidgetDef(String id, String name, String category, String icon,
                  String color1, String color2, RenderType type,
                  int labelCol, int valueCol, Supplier<WidgetData> fetcher) {
            this.id = id; this.name = name; this.category = category;
            this.icon = icon; this.color1 = color1; this.color2 = color2;
            this.renderType = type; this.labelCol = labelCol; this.valueCol = valueCol;
            this.fetcher = fetcher;
        }

        static WidgetDef table(String id, String name, String category, String icon,
                               String c1, String c2, Supplier<WidgetData> fetcher) {
            return new WidgetDef(id, name, category, icon, c1, c2, RenderType.TABLE, 0, 0, fetcher);
        }

        static WidgetDef chart(String id, String name, String category, String icon,
                               String c1, String c2, RenderType type,
                               int labelCol, int valueCol, Supplier<WidgetData> fetcher) {
            return new WidgetDef(id, name, category, icon, c1, c2, type, labelCol, valueCol, fetcher);
        }
    }

    public DashboardView() {
        registerWidgets();
        loadActiveWidgets();
    }

    private void registerWidgets() {
        // ── Table Widgets ───────────────────────────────────────────────────
        // Recent Activity
        allWidgets.add(WidgetDef.table("latest_purchase_inv", "Latest Purchase Invoices", "Recent Activity", "\uD83D\uDCE6", "#476b8a", "#35546e", () -> widgetDAO.getLatestPurchaseInvoices(5)));
        allWidgets.add(WidgetDef.table("latest_sale_inv", "Latest Sale Invoices", "Recent Activity", "\uD83D\uDCB0", "#4a5680", "#384268", () -> widgetDAO.getLatestSaleInvoices(5)));
        allWidgets.add(WidgetDef.table("latest_purchase_rec", "Latest Purchase Receipts", "Recent Activity", "\uD83D\uDCB3", "#7a5565", "#5e414e", () -> widgetDAO.getLatestPurchaseReceipts(5)));
        allWidgets.add(WidgetDef.table("latest_sale_rec", "Latest Sale Receipts", "Recent Activity", "\uD83D\uDCB5", "#457b7b", "#335e5e", () -> widgetDAO.getLatestSaleReceipts(5)));
        allWidgets.add(WidgetDef.table("latest_lr", "Latest Lorry Receipts", "Recent Activity", "\uD83D\uDE9A", "#6b5080", "#533d66", () -> widgetDAO.getLatestLorryReceipts(5)));
        allWidgets.add(WidgetDef.table("latest_ls", "Latest Loading Slips", "Recent Activity", "\uD83D\uDCCB", "#6b6050", "#534a3c", () -> widgetDAO.getLatestLoadingSlips(5)));

        // Top Performers
        allWidgets.add(WidgetDef.table("top_purchase_parties", "Top Parties by Purchase", "Top Performers", "\uD83C\uDFC6", "#476b8a", "#35546e", () -> widgetDAO.getTopPartiesByPurchase(5)));
        allWidgets.add(WidgetDef.table("top_sale_parties", "Top Parties by Sale", "Top Performers", "\u2B50", "#4a5680", "#384268", () -> widgetDAO.getTopPartiesBySale(5)));
        allWidgets.add(WidgetDef.table("top_vehicles", "Top Vehicles by Revenue", "Top Performers", "\uD83D\uDE9B", "#6b5080", "#533d66", () -> widgetDAO.getTopVehiclesByRevenue(5)));

        // Party Insights
        allWidgets.add(WidgetDef.table("least_interacted", "Least Interacted Parties", "Party Insights", "\uD83D\uDCA4", "#7a6050", "#5e4a3c", () -> widgetDAO.getLeastInteractedParties(5)));
        allWidgets.add(WidgetDef.table("longest_inactive", "Longest Inactive Parties", "Party Insights", "\u23F0", "#7a5565", "#5e414e", () -> widgetDAO.getLongestInactiveParties(5)));
        allWidgets.add(WidgetDef.table("purchase_gst", "Purchase GST Summary", "Financial Summary", "\uD83D\uDCC4", "#4a7a60", "#385e4a", () -> widgetDAO.getPurchaseGSTSummary()));
        allWidgets.add(WidgetDef.table("sale_gst", "Sale GST Summary", "Financial Summary", "\uD83D\uDCC4", "#4a5680", "#384268", () -> widgetDAO.getSaleGSTSummary()));

        // ── Chart Widgets ───────────────────────────────────────────────────
        allWidgets.add(WidgetDef.chart("purchase_gst_chart", "Purchase GST Breakdown", "Charts", "\uD83E\uDD67", "#4a7a60", "#385e4a", RenderType.PIE_CHART, 0, 1, () -> widgetDAO.getPurchaseGSTSummary()));
        allWidgets.add(WidgetDef.chart("sale_gst_chart", "Sale GST Breakdown", "Charts", "\uD83E\uDD67", "#4a5680", "#384268", RenderType.PIE_CHART, 0, 1, () -> widgetDAO.getSaleGSTSummary()));
        allWidgets.add(WidgetDef.chart("party_type_chart", "Party Distribution", "Charts", "\uD83C\uDF69", "#6b5080", "#533d66", RenderType.PIE_CHART, 0, 1, () -> widgetDAO.getPartyCountByType()));
        allWidgets.add(WidgetDef.chart("top_vehicles_chart", "Vehicle Revenue Chart", "Charts", "\uD83D\uDCCA", "#457b7b", "#335e5e", RenderType.BAR_CHART, 0, 2, () -> widgetDAO.getTopVehiclesByRevenue(5)));
        allWidgets.add(WidgetDef.chart("top_purchase_chart", "Top Purchase Parties Chart", "Charts", "\uD83D\uDCCA", "#476b8a", "#35546e", RenderType.BAR_CHART, 0, 2, () -> widgetDAO.getTopPartiesByPurchase(5)));
        allWidgets.add(WidgetDef.chart("top_sale_chart", "Top Sale Parties Chart", "Charts", "\uD83D\uDCCA", "#4a5680", "#384268", RenderType.BAR_CHART, 0, 2, () -> widgetDAO.getTopPartiesBySale(5)));
        allWidgets.add(WidgetDef.chart("monthly_purchase_chart", "Monthly Purchase Trend", "Charts", "\uD83D\uDCC8", "#6b6050", "#534a3c", RenderType.LINE_CHART, 0, 2, () -> widgetDAO.getMonthlyPurchaseSummary()));
        allWidgets.add(WidgetDef.chart("monthly_sale_chart", "Monthly Sale Trend", "Charts", "\uD83D\uDCC8", "#457b7b", "#335e5e", RenderType.LINE_CHART, 0, 2, () -> widgetDAO.getMonthlySaleSummary()));
    }

    private void loadActiveWidgets() {
        try {
            String saved = settingsDAO.getSetting(SETTINGS_KEY);
            if (saved != null && !saved.isBlank()) {
                activeWidgetIds.addAll(Arrays.asList(saved.split(",")));
            } else {
                // Default widgets
                activeWidgetIds.addAll(List.of(
                    "latest_purchase_inv", "latest_sale_inv", "top_vehicles_chart",
                    "purchase_gst_chart", "sale_gst_chart", "monthly_purchase_chart"
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
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f0f4ff, #f8fafc);");

        // Header
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label subtitle = new Label("Drag widgets to reorder  |  Resize from the bottom-right corner");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        Button addWidgetBtn = new Button("+ Add Widget");
        addWidgetBtn.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #7c3aed); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                "-fx-padding: 10 24 10 24; -fx-background-radius: 8; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 8, 0, 0, 3);");
        addWidgetBtn.setOnAction(e -> showWidgetPicker());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-font-weight: bold; " +
                "-fx-font-size: 13px; -fx-padding: 10 20 10 20; -fx-background-radius: 8; -fx-cursor: hand; " +
                "-fx-border-color: #cbd5e1; -fx-border-radius: 8;");
        refreshBtn.setOnAction(e -> refreshAllWidgets());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, title, spacer, refreshBtn, addWidgetBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Widget grid
        widgetGrid = new FlowPane(18, 18);
        widgetGrid.setPadding(new Insets(8));

        ScrollPane scrollPane = new ScrollPane(widgetGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, subtitle, scrollPane);
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
            setupDragAndDrop(card);
            widgetGrid.getChildren().add(card);

            AppExecutor.submit(() -> {
                try {
                    WidgetData data = def.fetcher.get();
                    Platform.runLater(() -> {
                        int idx = widgetGrid.getChildren().indexOf(card);
                        if (idx >= 0) {
                            double w = card.getPrefWidth();
                            double h = card.getPrefHeight();
                            VBox loaded = buildWidgetCard(def, data, false);
                            loaded.setPrefWidth(w);
                            if (h > 0) loaded.setPrefHeight(h);
                            setupDragAndDrop(loaded);
                            widgetGrid.getChildren().set(idx, loaded);
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to load widget: {}", def.name, e);
                }
            });
        }

        if (activeWidgetIds.isEmpty()) {
            Label empty = new Label("No widgets added yet. Click '+ Add Widget' to get started!");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 60;");
            widgetGrid.getChildren().add(empty);
        }
    }

    // ── Build a single widget card ──────────────────────────────────────────
    private VBox buildWidgetCard(WidgetDef def, WidgetData data, boolean loading) {
        // Title bar with gradient
        Label iconLabel = new Label(def.icon);
        iconLabel.setStyle("-fx-font-size: 16px;");

        Label titleLabel = new Label(def.name);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button removeBtn = new Button("\u2715");
        removeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 7 2 7; " +
                "-fx-background-radius: 12; -fx-font-size: 11px;");
        removeBtn.setOnAction(e -> removeWidget(def.id));

        HBox titleBar = new HBox(8, iconLabel, titleLabel, removeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(12, 14, 12, 14));
        titleBar.setStyle("-fx-background-color: linear-gradient(to right, " + def.color1 + ", " + def.color2 + "); " +
                "-fx-background-radius: 10 10 0 0;");

        // Content area
        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(12));
        contentBox.setMinHeight(140);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        if (loading) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(36, 36);
            contentBox.getChildren().add(spinner);
            contentBox.setAlignment(Pos.CENTER);
        } else if (data != null && !data.getRows().isEmpty()) {
            Node content = switch (def.renderType) {
                case BAR_CHART -> buildBarChart(data, def);
                case PIE_CHART -> buildPieChart(data, def);
                case LINE_CHART -> buildLineChart(data, def);
                default -> buildDataGrid(data);
            };
            VBox.setVgrow(content, Priority.ALWAYS);
            contentBox.getChildren().add(content);
        } else {
            Label noData = new Label("No data available");
            noData.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            contentBox.getChildren().add(noData);
            contentBox.setAlignment(Pos.CENTER);
        }

        // Resize handle
        Region resizeHandle = new Region();
        resizeHandle.setPrefSize(14, 14);
        resizeHandle.setMaxSize(14, 14);
        resizeHandle.setCursor(Cursor.SE_RESIZE);
        resizeHandle.setStyle("-fx-background-color: linear-gradient(to bottom right, transparent 50%, #94a3b8 50%); " +
                "-fx-background-radius: 0 0 10 0;");

        StackPane resizeCorner = new StackPane(resizeHandle);
        resizeCorner.setAlignment(Pos.BOTTOM_RIGHT);
        resizeCorner.setPadding(new Insets(0, 2, 2, 0));

        // Card container
        VBox card = new VBox(0, titleBar, contentBox, resizeCorner);
        boolean isChart = def.renderType != RenderType.TABLE;
        card.setPrefWidth(isChart ? 440 : 420);
        card.setMinWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: " + def.color1 + "; -fx-border-radius: 10; -fx-border-width: 1.5; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);"));

        // Resize logic
        setupResize(resizeHandle, card);

        return card;
    }

    // ── Build data grid (table widget) ──────────────────────────────────────
    private GridPane buildDataGrid(WidgetData data) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(4);
        grid.setPadding(new Insets(4));

        List<String> headers = data.getHeaders();
        List<List<String>> rows = data.getRows();

        for (int c = 0; c < headers.size(); c++) {
            Label h = new Label(headers.get(c).toUpperCase());
            h.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8094a8; -fx-padding: 2 4 6 4; " +
                       "-fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");
            h.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(h, Priority.ALWAYS);
            grid.add(h, c, 0);
        }

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String rowBg = r % 2 == 0 ? "-fx-background-color: #f4f7fa; -fx-background-radius: 3;" : "";
            for (int c = 0; c < row.size() && c < headers.size(); c++) {
                String val = row.get(c);
                Label cell = new Label(val);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setWrapText(true);
                GridPane.setHgrow(cell, Priority.ALWAYS);
                cell.setPadding(new Insets(4, 4, 4, 4));
                if (val != null && !val.isEmpty()) cell.setTooltip(new Tooltip(val));

                if (val.matches("^[\\d,.-]+$")) {
                    cell.setStyle("-fx-font-size: 13px; -fx-text-fill: #0f172a; -fx-font-weight: bold; " + rowBg);
                    cell.setAlignment(Pos.CENTER_RIGHT);
                } else {
                    cell.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; " + rowBg);
                }
                grid.add(cell, c, r + 1);
            }
        }

        for (int c = 0; c < headers.size(); c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        return grid;
    }

    // ── Build Bar Chart ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Node buildBarChart(WidgetData data, WidgetDef def) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFill(Color.web("#64748b"));
        xAxis.setTickLabelFill(Color.web("#64748b"));
        xAxis.setTickLabelFont(javafx.scene.text.Font.font(10));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setCategoryGap(8);
        chart.setBarGap(2);
        chart.setMinHeight(180);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (List<String> row : data.getRows()) {
            String label = row.get(def.labelCol);
            double value = parseNumber(row.get(Math.min(def.valueCol, row.size() - 1)));
            if (label.length() > 12) label = label.substring(0, 12) + "..";
            series.getData().add(new XYChart.Data<>(label, value));
        }
        chart.getData().add(series);

        // Color bars after render
        Platform.runLater(() -> {
            int i = 0;
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-bar-fill: " + CHART_COLORS[i % CHART_COLORS.length] + "; -fx-background-radius: 4 4 0 0;");
                }
                i++;
            }
        });
        return chart;
    }

    // ── Build Pie Chart ─────────────────────────────────────────────────────
    private Node buildPieChart(WidgetData data, WidgetDef def) {
        PieChart chart = new PieChart();
        chart.setAnimated(true);
        chart.setLegendSide(Side.BOTTOM);
        chart.setLabelsVisible(true);
        chart.setMinHeight(200);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setLabelLineLength(8);

        for (List<String> row : data.getRows()) {
            String label = row.get(def.labelCol);
            double value = parseNumber(row.get(Math.min(def.valueCol, row.size() - 1)));
            if (value > 0) chart.getData().add(new PieChart.Data(label, value));
        }

        Platform.runLater(() -> {
            int i = 0;
            for (PieChart.Data d : chart.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-pie-color: " + CHART_COLORS[i % CHART_COLORS.length] + ";");
                }
                i++;
            }
        });
        return chart;
    }

    // ── Build Line Chart ────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Node buildLineChart(WidgetData data, WidgetDef def) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFill(Color.web("#64748b"));
        xAxis.setTickLabelFill(Color.web("#64748b"));
        xAxis.setTickLabelFont(javafx.scene.text.Font.font(10));

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setCreateSymbols(true);
        chart.setMinHeight(180);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (List<String> row : data.getRows()) {
            String label = row.get(def.labelCol);
            double value = parseNumber(row.get(Math.min(def.valueCol, row.size() - 1)));
            series.getData().add(new XYChart.Data<>(label, value));
        }
        chart.getData().add(series);

        Platform.runLater(() -> {
            Node line = series.getNode().lookup(".chart-series-line");
            if (line != null) line.setStyle("-fx-stroke: " + def.color1 + "; -fx-stroke-width: 3;");
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-background-color: " + def.color1 + ", white; " +
                            "-fx-background-radius: 6; -fx-padding: 4;");
                }
            }
        });
        return chart;
    }

    // ── Drag and Drop ───────────────────────────────────────────────────────
    private void setupDragAndDrop(VBox card) {
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            int idx = widgetGrid.getChildren().indexOf(card);
            content.putString(String.valueOf(idx));
            db.setContent(content);

            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            db.setDragView(card.snapshot(sp, null), e.getX(), e.getY());

            card.setOpacity(0.4);
            e.consume();
        });

        card.setOnDragOver(e -> {
            if (e.getGestureSource() != card && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        card.setOnDragEntered(e -> {
            if (e.getGestureSource() != card && e.getDragboard().hasString()) {
                card.setStyle(card.getStyle() + " -fx-border-color: #3b82f6; -fx-border-width: 2;");
            }
            e.consume();
        });

        card.setOnDragExited(e -> {
            card.setStyle(card.getStyle().replaceAll("-fx-border-color: #3b82f6; -fx-border-width: 2;", ""));
            e.consume();
        });

        card.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                int sourceIdx = Integer.parseInt(db.getString());
                int targetIdx = widgetGrid.getChildren().indexOf(card);
                if (sourceIdx != targetIdx && sourceIdx >= 0 && targetIdx >= 0
                        && sourceIdx < activeWidgetIds.size() && targetIdx < activeWidgetIds.size()) {
                    String movedId = activeWidgetIds.remove(sourceIdx);
                    activeWidgetIds.add(targetIdx, movedId);
                    saveActiveWidgets();
                    refreshAllWidgets();
                }
                e.setDropCompleted(true);
            }
            e.consume();
        });

        card.setOnDragDone(e -> {
            card.setOpacity(1.0);
            e.consume();
        });
    }

    // ── Resize Handle ───────────────────────────────────────────────────────
    private void setupResize(Region handle, VBox card) {
        final double[] dragStart = new double[4];

        // Prevent resize drag from triggering card's drag-and-drop
        handle.setOnDragDetected(e -> e.consume());

        handle.setOnMousePressed(e -> {
            dragStart[0] = e.getScreenX();
            dragStart[1] = e.getScreenY();
            dragStart[2] = card.getWidth();
            dragStart[3] = card.getHeight();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            double newW = dragStart[2] + (e.getScreenX() - dragStart[0]);
            double newH = dragStart[3] + (e.getScreenY() - dragStart[1]);
            card.setPrefWidth(Math.max(300, Math.min(800, newW)));
            card.setPrefHeight(Math.max(200, Math.min(650, newH)));
            card.setMaxWidth(Region.USE_PREF_SIZE);
            card.setMinWidth(Region.USE_PREF_SIZE);
            e.consume();
        });
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
        dialog.getDialogPane().setPrefWidth(520);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));

        Map<String, List<WidgetDef>> categories = new LinkedHashMap<>();
        for (WidgetDef w : allWidgets) {
            categories.computeIfAbsent(w.category, k -> new ArrayList<>()).add(w);
        }

        Map<String, CheckBox> checkBoxes = new LinkedHashMap<>();

        for (var entry : categories.entrySet()) {
            Label catLabel = new Label(entry.getKey());
            catLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

            VBox catBox = new VBox(6);
            catBox.setPadding(new Insets(4, 0, 8, 12));
            for (WidgetDef w : entry.getValue()) {
                CheckBox cb = new CheckBox(w.icon + "  " + w.name);
                cb.setSelected(activeWidgetIds.contains(w.id));
                cb.setStyle("-fx-font-size: 13px;");
                checkBoxes.put(w.id, cb);
                catBox.getChildren().add(cb);
            }
            content.getChildren().addAll(catLabel, catBox);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);
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

    // ── Helpers ──────────────────────────────────────────────────────────────
    private WidgetDef findWidget(String id) {
        return allWidgets.stream().filter(w -> w.id.equals(id)).findFirst().orElse(null);
    }

    private double parseNumber(String s) {
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
