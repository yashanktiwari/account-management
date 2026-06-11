package com.accounting;

import atlantafx.base.theme.PrimerLight;
import com.accounting.dao.DashboardDAO;
import com.accounting.dao.SettingsDAO;
import com.accounting.database.AppConfig;
import com.accounting.database.DBConnection;
import com.accounting.model.Payment;
import com.accounting.ui.dialog.*;
import com.accounting.ui.dialog.ReportView;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.NotificationUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class MainApp extends Application {

    private static final Logger log = AppLogger.get(MainApp.class);
    private static final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static Stage primaryStage;
    private static MainApp instance;

    // Dashboard KPI labels
    private Label totalOutstandingLabel;
    private Label overdueLabel;
    private Label dueTodayLabel;
    private Label collectedTodayLabel;
    private Label collectedMTDLabel;
    private Label pendingInvoicesLabel;

    // Dashboard tables
    private TableView<DashboardDAO.OutstandingCustomer> outstandingTable;
    private ObservableList<DashboardDAO.OutstandingCustomer> outstandingList = FXCollections.observableArrayList();
    private TableView<Payment> recentPaymentsTable;
    private ObservableList<Payment> recentPaymentsList = FXCollections.observableArrayList();
    private StackPane contentHost;
    private ScrollPane dashboardScroll;
    private final List<Button> sidebarNavButtons = new ArrayList<>();
    private Button dashBtn;
    private Button partyBtn;
    private Button purchaseInvoiceBtn;
    private Button saleInvoiceBtn;
    private Button purchaseReceiptBtn;
    private Button saleReceiptBtn;
    private Button loadingSlipBtn;
    private Button lorryReceiptBtn;
    private Button reportsBtn;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void showContentInApp(Parent content) {
        if (instance != null) {
            instance.showContent(content);
        }
    }

    public static void showContentNodeInApp(javafx.scene.Node node) {
        if (instance != null) {
            instance.contentHost.getChildren().setAll(node);
        }
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                log.error("Unhandled exception on thread {}", thread.getName(), throwable)
        );

        log.info("Max Heap: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));

        primaryStage = stage;
        instance = this;
        NotificationUtil.init(stage);

        // Apply AtlantaFX theme
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        AppExecutor.submit(() -> {
            boolean loaded = AppConfig.loadDatabaseConfig();

            if (loaded) {
                try {
                    DBConnection.createDatabaseIfNotExists();
                    DBConnection.initializeDatabase();
                } catch (Exception e) {
                    log.error("Database initialization failed during startup", e);
                    Platform.runLater(() -> AlertUtil.showError("Startup Warning",
                            "Failed to initialize database. Some features may be unavailable."));
                }
            } else {
                log.warn("Database config not loaded. App started without active DB connection.");
            }

            Platform.runLater(() -> {
                try {
                    buildMainUI(stage);
                } catch (Exception e) {
                    log.error("Failed to load main UI", e);
                }
            });
        });
    }

    private void buildMainUI(Stage stage) {
        BorderPane root = new BorderPane();

        // ── Sidebar Navigation ──
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // ── Center: Dashboard ──
        dashboardScroll = new ScrollPane(buildDashboard());
        dashboardScroll.setFitToWidth(true);
        dashboardScroll.setStyle("-fx-background-color: #f8fafc;");
        contentHost = new StackPane(dashboardScroll);
        root.setCenter(contentHost);

        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/notifications.css")).toExternalForm()
        );

        stage.setTitle("Account Management System");
        stage.setScene(scene);
        stage.setMaximized(true);

        stage.setOnCloseRequest(event -> {
            AppExecutor.shutdown();
            Platform.exit();
            System.exit(0);
        });

        stage.show();

        // Load dashboard data
        refreshDashboard();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(2);
        sidebar.setPrefWidth(200);
        sidebar.setPadding(new Insets(8));
        sidebar.getStyleClass().add("sidebar");

        Label navTitle = new Label("Navigation");
        navTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 8 0 4 8;");

        dashBtn = sidebarButton("Dashboard", this::showDashboard);

        Label mastersTitle = new Label("Masters");
        mastersTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        partyBtn = sidebarButton("Parties", this::showParties);

        Label invoicesTitle = new Label("Invoices");
        invoicesTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        purchaseInvoiceBtn = sidebarButton("Purchase Invoice", this::showPurchaseInvoice);
        saleInvoiceBtn = sidebarButton("Sale Invoice", this::showSaleInvoice);

        Label receiptsTitle = new Label("Receipts");
        receiptsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        purchaseReceiptBtn = sidebarButton("Purchase Receipt", this::showPurchaseReceipt);
        saleReceiptBtn = sidebarButton("Sale Receipt", this::showSaleReceipt);

        Label slipsTitle = new Label("Slips");
        slipsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        loadingSlipBtn = sidebarButton("Loading Slips", this::showLoadingSlips);
        lorryReceiptBtn = sidebarButton("Lorry Receipt (LR)", this::showLorryReceipts);

        Label reportsTitle = new Label("Reports");
        reportsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        reportsBtn = sidebarButton("Reports", this::showReports);

        Label settingsTitle = new Label("Settings");
        settingsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        Button dbBtn = sidebarButton("Database Setup", () -> DatabaseSetupDialog.show(primaryStage, () -> {}));
        Button companyBtn = sidebarButton("Company Settings", this::showCompanySettings);
        Button invoiceBtn = sidebarButton("Invoice Settings", this::showInvoiceSettings);

        sidebar.getChildren().addAll(
                navTitle, dashBtn,
            mastersTitle, partyBtn,
                invoicesTitle, purchaseInvoiceBtn, saleInvoiceBtn,
                receiptsTitle, purchaseReceiptBtn, saleReceiptBtn,
                slipsTitle, loadingSlipBtn, lorryReceiptBtn,
                reportsTitle, reportsBtn,
                settingsTitle, dbBtn, companyBtn, invoiceBtn
        );

        setActiveSidebarButton(dashBtn);

        return sidebar;
    }

    private Button sidebarButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("sidebar-btn");
        sidebarNavButtons.add(btn);
        btn.setOnAction(e -> {
            setActiveSidebarButton(btn);
            action.run();
        });
        return btn;
    }

    private void setActiveSidebarButton(Button active) {
        for (Button button : sidebarNavButtons) {
            button.getStyleClass().remove("sidebar-btn-active");
        }
        if (!active.getStyleClass().contains("sidebar-btn-active")) {
            active.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void showDashboard() {
        contentHost.getChildren().setAll(dashboardScroll);
        refreshDashboard();
    }

    private void showContent(Parent content) {
        ScrollPane wrapper = new ScrollPane(content);
        wrapper.setFitToWidth(true);
        wrapper.setFitToHeight(true);
        wrapper.setStyle("-fx-background-color: #f8fafc;");
        contentHost.getChildren().setAll(wrapper);
    }

    private void showParties() {
        showContent(new PartyMasterListView().createContent());
    }

    private void showPurchaseInvoice() {
        showContent(new PurchaseInvoiceListView().createContent());
    }

    private void showSaleInvoice() {
        showContent(new SaleInvoiceListView().createContent());
    }

    private void showPurchaseReceipt() {
        showContent(new PurchaseReceiptListView().createContent());
    }

    private void showSaleReceipt() {
        showContent(new SaleReceiptListView().createContent());
    }

    private void showLoadingSlips() {
        showContent(new LoadingSlipListView().createContent());
    }

    private void showLorryReceipts() {
        showContent(new LorryReceiptListView().createContent());
    }

    private void showReports() {
        showContent(new ReportView().createContent());
    }

    private VBox buildDashboard() {
        VBox dashboard = new VBox(16);
        dashboard.setPadding(new Insets(20));
        dashboard.setStyle("-fx-background-color: #f8fafc;");

        // Title
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label subtitle = new Label("Collections & Outstanding Overview");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // KPI Cards row
        HBox kpiRow = buildKPICards();

        // Quick Actions
        HBox quickActions = buildQuickActions();

        // Main content: Outstanding table + Recent Payments
        HBox mainContent = new HBox(16);
        mainContent.setMinHeight(350);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        VBox outstandingSection = buildOutstandingSection();
        HBox.setHgrow(outstandingSection, Priority.ALWAYS);

        VBox recentSection = buildRecentPaymentsSection();
        recentSection.setPrefWidth(380);
        recentSection.setMinWidth(350);

        mainContent.getChildren().addAll(outstandingSection, recentSection);

        dashboard.getChildren().addAll(title, subtitle, kpiRow, quickActions, mainContent);
        return dashboard;
    }

    private HBox buildKPICards() {
        totalOutstandingLabel = new Label("0");
        overdueLabel = new Label("0");
        dueTodayLabel = new Label("0");
        collectedTodayLabel = new Label("0");
        collectedMTDLabel = new Label("0");
        pendingInvoicesLabel = new Label("0");

        HBox row = new HBox(12);
        row.getChildren().addAll(
                kpiCard("Total Outstanding", totalOutstandingLabel, "#dc2626"),
                kpiCard("Overdue", overdueLabel, "#ea580c"),
                kpiCard("Due Today", dueTodayLabel, "#d97706"),
                kpiCard("Collected Today", collectedTodayLabel, "#16a34a"),
                kpiCard("Collected (Month)", collectedMTDLabel, "#0891b2"),
                kpiCard("Pending Invoices", pendingInvoicesLabel, "#7c3aed")
        );
        return row;
    }

    private VBox kpiCard(String title, Label valueLabel, String color) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        VBox card = new VBox(4, titleLbl, valueLabel);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);");
        card.setPrefWidth(160);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildQuickActions() {
        Button purchaseInvBtn = quickActionBtn("Purchase Invoice", "#16a34a",
                () -> {
                    setActiveSidebarButton(purchaseInvoiceBtn);
                    showPurchaseInvoice();
                });
        Button saleInvBtn = quickActionBtn("Sale Invoice", "#2563eb",
                () -> {
                    setActiveSidebarButton(saleInvoiceBtn);
                    showSaleInvoice();
                });
        Button purchaseRecBtn = quickActionBtn("Purchase Receipt", "#dc2626",
                () -> {
                    setActiveSidebarButton(purchaseReceiptBtn);
                    showPurchaseReceipt();
                });
        Button saleRecBtn = quickActionBtn("Sale Receipt", "#0891b2",
                () -> {
                    setActiveSidebarButton(saleReceiptBtn);
                    showSaleReceipt();
                });
        Button partyBtn = quickActionBtn("Manage Parties", "#7c3aed",
                () -> {
                    setActiveSidebarButton(this.partyBtn);
                    showParties();
                });

        Button refreshBtn = quickActionBtn("Refresh", "#475569", () -> {
            setActiveSidebarButton(dashBtn);
            showDashboard();
        });

        HBox actions = new HBox(10, purchaseInvBtn, saleInvBtn, purchaseRecBtn, saleRecBtn,
            partyBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private Button quickActionBtn(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-background-radius: 6;
                -fx-padding: 8 16 8 16;
                -fx-cursor: hand;
                """.formatted(color));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    @SuppressWarnings("unchecked")
    private VBox buildOutstandingSection() {
        Label sectionTitle = new Label("Top Outstanding Customers");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        outstandingTable = new TableView<>();
        outstandingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<DashboardDAO.OutstandingCustomer, String> nameCol = new TableColumn<>("Customer");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAccountName()));
        nameCol.setPrefWidth(180);

        TableColumn<DashboardDAO.OutstandingCustomer, String> areaCol = new TableColumn<>("Area");
        areaCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getArea() != null ? c.getValue().getArea() : ""));
        areaCol.setPrefWidth(100);

        TableColumn<DashboardDAO.OutstandingCustomer, String> mobileCol = new TableColumn<>("Mobile");
        mobileCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getMobile() != null ? c.getValue().getMobile() : ""));
        mobileCol.setPrefWidth(100);

        TableColumn<DashboardDAO.OutstandingCustomer, String> amtCol = new TableColumn<>("Outstanding");
        amtCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.2f", c.getValue().getOutstanding())));
        amtCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        amtCol.setPrefWidth(120);

        outstandingTable.getColumns().addAll(nameCol, areaCol, mobileCol, amtCol);
        outstandingTable.setItems(outstandingList);

        VBox.setVgrow(outstandingTable, Priority.ALWAYS);

        VBox section = new VBox(8, sectionTitle, outstandingTable);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return section;
    }

    @SuppressWarnings("unchecked")
    private VBox buildRecentPaymentsSection() {
        Label sectionTitle = new Label("Recent Payments");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        recentPaymentsTable = new TableView<>();
        recentPaymentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Payment, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFmt));
            }
        });
        dateCol.setPrefWidth(80);

        TableColumn<Payment, String> nameCol = new TableColumn<>("Customer");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        nameCol.setPrefWidth(140);

        TableColumn<Payment, Double> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        amtCol.setPrefWidth(90);

        recentPaymentsTable.getColumns().addAll(dateCol, nameCol, amtCol);
        recentPaymentsTable.setItems(recentPaymentsList);

        VBox.setVgrow(recentPaymentsTable, Priority.ALWAYS);

        VBox section = new VBox(8, sectionTitle, recentPaymentsTable);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return section;
    }

    private void refreshDashboard() {
        AppExecutor.submit(() -> {
            DashboardDAO dao = new DashboardDAO();
            double outstanding = dao.getTotalOutstanding();
            double overdue = dao.getOverdueAmount();
            double dueToday = dao.getDueToday();
            double collectedToday = dao.getCollectedToday();
            double collectedMTD = dao.getCollectedThisMonth();
            int pendingCount = dao.getPendingInvoiceCount();
            List<DashboardDAO.OutstandingCustomer> topCustomers = dao.getTopOutstandingCustomers(20);
            List<Payment> recentPayments = dao.getRecentPayments(15);

            Platform.runLater(() -> {
                totalOutstandingLabel.setText(formatAmount(outstanding));
                overdueLabel.setText(formatAmount(overdue));
                dueTodayLabel.setText(formatAmount(dueToday));
                collectedTodayLabel.setText(formatAmount(collectedToday));
                collectedMTDLabel.setText(formatAmount(collectedMTD));
                pendingInvoicesLabel.setText(String.valueOf(pendingCount));
                outstandingList.setAll(topCustomers);
                recentPaymentsList.setAll(recentPayments);
            });
        });
    }

    private String formatAmount(double amount) {
        if (amount >= 10_000_000) return String.format("%.2f Cr", amount / 10_000_000);
        if (amount >= 100_000) return String.format("%.2f L", amount / 100_000);
        if (amount >= 1_000) return String.format("%.1f K", amount / 1_000);
        return String.format("%.0f", amount);
    }

    private void showCompanySettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Company Settings");
        dialog.initOwner(primaryStage);

        TextField nameField = new TextField(AppConfig.getCompanyName());
        nameField.setPrefWidth(300);

        TextField fyField = new TextField(AppConfig.getFinancialYear());
        fyField.setPrefWidth(150);
        fyField.setPromptText("e.g. 2026-27");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.add(new Label("Company Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Financial Year:"), 0, 1);
        grid.add(fyField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                AppConfig.saveCompanyName(nameField.getText().trim());
                AppConfig.saveFinancialYear(fyField.getText().trim());
                NotificationUtil.showSuccess("Saved", "Company settings updated. Restart to see changes.");
            }
        });
    }

    private void showInvoiceSettings() {
        AppExecutor.submit(() -> {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                String currentStartingNumber = settingsDAO.getSetting("global_invoice_starting_number");
                if (currentStartingNumber == null) {
                    currentStartingNumber = "1";
                }

                String currentSlipNumber = settingsDAO.getSetting("loading_slip_starting_number");
                if (currentSlipNumber == null) {
                    currentSlipNumber = "1";
                }

                String currentLrNumber = settingsDAO.getSetting("lr_starting_number");
                if (currentLrNumber == null) {
                    currentLrNumber = "1";
                }

                String currentPurchaseReceiptNumber = settingsDAO.getSetting("purchase_receipt_starting_number");
                if (currentPurchaseReceiptNumber == null) {
                    currentPurchaseReceiptNumber = "1";
                }

                String currentSaleReceiptNumber = settingsDAO.getSetting("sale_receipt_starting_number");
                if (currentSaleReceiptNumber == null) {
                    currentSaleReceiptNumber = "1";
                }

                String finalCurrentStartingNumber = currentStartingNumber;
                String finalCurrentSlipNumber = currentSlipNumber;
                String finalCurrentLrNumber = currentLrNumber;
                String finalCurrentPurchaseReceiptNumber = currentPurchaseReceiptNumber;
                String finalCurrentSaleReceiptNumber = currentSaleReceiptNumber;
                Platform.runLater(() -> {
                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Invoice & Slip Settings");
                    dialog.initOwner(primaryStage);

                    TextField startingNumberField = new TextField(finalCurrentStartingNumber);
                    startingNumberField.setPrefWidth(200);

                    TextField slipStartingNumberField = new TextField(finalCurrentSlipNumber);
                    slipStartingNumberField.setPrefWidth(200);

                    TextField lrStartingNumberField = new TextField(finalCurrentLrNumber);
                    lrStartingNumberField.setPrefWidth(200);

                    TextField purchaseReceiptStartingNumberField = new TextField(finalCurrentPurchaseReceiptNumber);
                    purchaseReceiptStartingNumberField.setPrefWidth(200);

                    TextField saleReceiptStartingNumberField = new TextField(finalCurrentSaleReceiptNumber);
                    saleReceiptStartingNumberField.setPrefWidth(200);

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(15));
                    grid.add(new Label("Global Starting Invoice Number:"), 0, 0);
                    grid.add(startingNumberField, 1, 0);
                    grid.add(new Label("This number will be used for all invoices and receipts."), 0, 1);
                    GridPane.setColumnSpan(grid.getChildren().get(2), 2);

                    grid.add(new Label("Loading Slip Starting Number:"), 0, 2);
                    grid.add(slipStartingNumberField, 1, 2);
                    grid.add(new Label("This number will be used for loading slips."), 0, 3);
                    GridPane.setColumnSpan(grid.getChildren().get(5), 2);

                    grid.add(new Label("LR Starting Number:"), 0, 4);
                    grid.add(lrStartingNumberField, 1, 4);
                    grid.add(new Label("This number will be used for lorry receipts."), 0, 5);
                    GridPane.setColumnSpan(grid.getChildren().get(8), 2);

                    grid.add(new Label("Purchase Receipt Starting Number:"), 0, 6);
                    grid.add(purchaseReceiptStartingNumberField, 1, 6);
                    grid.add(new Label("This number will be used for purchase receipts."), 0, 7);
                    GridPane.setColumnSpan(grid.getChildren().get(11), 2);

                    grid.add(new Label("Sale Receipt Starting Number:"), 0, 8);
                    grid.add(saleReceiptStartingNumberField, 1, 8);
                    grid.add(new Label("This number will be used for sale receipts."), 0, 9);
                    GridPane.setColumnSpan(grid.getChildren().get(14), 2);

                    dialog.getDialogPane().setContent(grid);
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                    dialog.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            try {
                                int num = Integer.parseInt(startingNumberField.getText().trim());
                                int slipNum = Integer.parseInt(slipStartingNumberField.getText().trim());
                                int lrNum = Integer.parseInt(lrStartingNumberField.getText().trim());
                                int purchaseReceiptNum = Integer.parseInt(purchaseReceiptStartingNumberField.getText().trim());
                                int saleReceiptNum = Integer.parseInt(saleReceiptStartingNumberField.getText().trim());
                                if (num >= 0 && slipNum >= 0 && lrNum >= 0 && purchaseReceiptNum >= 0 && saleReceiptNum >= 0) {
                                    AppExecutor.submit(() -> {
                                        try {
                                            settingsDAO.saveSetting("global_invoice_starting_number", String.valueOf(num));
                                            settingsDAO.saveSetting("loading_slip_starting_number", String.valueOf(slipNum));
                                            settingsDAO.saveSetting("lr_starting_number", String.valueOf(lrNum));
                                            settingsDAO.saveSetting("purchase_receipt_starting_number", String.valueOf(purchaseReceiptNum));
                                            settingsDAO.saveSetting("sale_receipt_starting_number", String.valueOf(saleReceiptNum));
                                            Platform.runLater(() -> {
                                                AlertUtil.showInfo("Success", "Settings updated.\nInvoice: " + num + "  |  Loading Slip: " + slipNum + "  |  LR: " + lrNum + "  |  Purchase Receipt: " + purchaseReceiptNum + "  |  Sale Receipt: " + saleReceiptNum);
                                            });
                                        } catch (Exception e) {
                                            log.error("Failed to save setting", e);
                                            Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save setting"));
                                        }
                                    });
                                } else {
                                    AlertUtil.showWarning("Validation", "Please enter non-negative numbers");
                                }
                            } catch (NumberFormatException e) {
                                AlertUtil.showWarning("Validation", "Please enter valid numbers");
                            }
                        }
                    });
                });
            } catch (Exception e) {
                log.error("Failed to load settings", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load settings"));
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
