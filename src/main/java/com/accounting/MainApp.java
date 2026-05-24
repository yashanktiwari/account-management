package com.accounting;

import atlantafx.base.theme.PrimerLight;
import com.accounting.dao.DashboardDAO;
import com.accounting.database.AppConfig;
import com.accounting.database.DBConnection;
import com.accounting.model.Payment;
import com.accounting.ui.dialog.*;
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

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class MainApp extends Application {

    private static final Logger log = AppLogger.get(MainApp.class);
    private static final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static Stage primaryStage;

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

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                log.error("Unhandled exception on thread {}", thread.getName(), throwable)
        );

        log.info("Max Heap: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));

        primaryStage = stage;
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

        // ── Menu Bar ──
        MenuBar menuBar = buildMenuBar();

        // ── Company Header ──
        String companyName = AppConfig.getCompanyName();
        String financialYear = AppConfig.getFinancialYear();

        Label companyLabel = new Label(companyName);
        companyLabel.getStyleClass().add("company-name");

        Label fyLabel = new Label(financialYear.isBlank() ? "" : "(" + financialYear + ")");
        fyLabel.getStyleClass().add("financial-year");

        HBox companyHeader = new HBox(12, companyLabel, fyLabel);
        companyHeader.setAlignment(Pos.CENTER_RIGHT);
        companyHeader.getStyleClass().add("company-header");

        VBox topSection = new VBox(menuBar, companyHeader);
        root.setTop(topSection);

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

        Button dashBtn = sidebarButton("Dashboard", this::showDashboard);
        dashBtn.getStyleClass().add("sidebar-btn-active");

        Label mastersTitle = new Label("Masters");
        mastersTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        Button customerBtn = sidebarButton("Customers", () -> {
            AccountEntryDialog dialog = new AccountEntryDialog("CUSTOMER");
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });
        Button supplierBtn = sidebarButton("Suppliers", () -> {
            AccountEntryDialog dialog = new AccountEntryDialog("SUPPLIER");
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });
        Button vehicleBtn = sidebarButton("Vehicles", () -> {
            VehicleMasterDialog dialog = new VehicleMasterDialog();
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });

        Label billingTitle = new Label("Billing");
        billingTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        Button invoiceBtn = sidebarButton("Invoice Register", () -> {
            InvoiceRegisterDialog dialog = new InvoiceRegisterDialog();
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });
        Button paymentEntryBtn = sidebarButton("Payment Entry", () -> {
            PaymentEntryDialog dialog = new PaymentEntryDialog(this::refreshDashboard);
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });
        Button paymentRegBtn = sidebarButton("Payment Register", () -> {
            PaymentRegisterDialog dialog = new PaymentRegisterDialog();
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });

        Label reportsTitle = new Label("Reports");
        reportsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        Button outstandingBtn = sidebarButton("Outstanding", () -> {
            OutstandingRegisterDialog dialog = new OutstandingRegisterDialog();
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });
        Button statementBtn = sidebarButton("Account Statement", () -> {
            AccountStatementDialog dialog = new AccountStatementDialog();
            showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
        });

        Label settingsTitle = new Label("Settings");
        settingsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        Button dbBtn = sidebarButton("Database Setup", () -> DatabaseSetupDialog.show(primaryStage, () -> {}));
        Button companyBtn = sidebarButton("Company Settings", this::showCompanySettings);

        sidebar.getChildren().addAll(
                navTitle, dashBtn,
                mastersTitle, customerBtn, supplierBtn, vehicleBtn,
                billingTitle, invoiceBtn, paymentEntryBtn, paymentRegBtn,
                reportsTitle, outstandingBtn, statementBtn,
                settingsTitle, dbBtn, companyBtn
        );

        return sidebar;
    }

    private Button sidebarButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("sidebar-btn");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void showDashboard() {
        contentHost.getChildren().setAll(dashboardScroll);
        refreshDashboard();
    }

    private void showEmbeddedDialog(Object dialogInstance, Runnable showAction) {
        showAction.run();
        try {
            Field stageField = dialogInstance.getClass().getDeclaredField("stage");
            stageField.setAccessible(true);
            Stage dialogStage = (Stage) stageField.get(dialogInstance);
            if (dialogStage == null || dialogStage.getScene() == null) {
                return;
            }
            Parent dialogRoot = dialogStage.getScene().getRoot();
            dialogStage.hide();
            dialogStage.close();
            contentHost.getChildren().setAll(dialogRoot);
        } catch (Exception ex) {
            log.error("Failed to embed dialog content in main view", ex);
        }
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
        Button addPaymentBtn = quickActionBtn("Add Payment", "#16a34a",
                () -> {
                    PaymentEntryDialog dialog = new PaymentEntryDialog(this::refreshDashboard);
                    showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
                });
        Button addCustomerBtn = quickActionBtn("Add Customer", "#2563eb",
                () -> {
                    AccountEntryDialog dialog = new AccountEntryDialog("CUSTOMER");
                    showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
                });
        Button invoiceBtn = quickActionBtn("Invoice Register", "#7c3aed",
                () -> {
                    InvoiceRegisterDialog dialog = new InvoiceRegisterDialog();
                    showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
                });
        Button outstandingBtn = quickActionBtn("Outstanding", "#ea580c",
                () -> {
                    OutstandingRegisterDialog dialog = new OutstandingRegisterDialog();
                    showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
                });
        Button statementBtn = quickActionBtn("Statement", "#0891b2",
                () -> {
                    AccountStatementDialog dialog = new AccountStatementDialog();
                    showEmbeddedDialog(dialog, () -> dialog.show(primaryStage));
                });

        Button refreshBtn = quickActionBtn("Refresh", "#475569", this::refreshDashboard);

        HBox actions = new HBox(10, addPaymentBtn, addCustomerBtn, invoiceBtn, outstandingBtn, statementBtn, refreshBtn);
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

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        // ── Masters Menu ──
        Menu mastersMenu = new Menu("Masters");

        MenuItem customerItem = new MenuItem("Customer Entry");
        customerItem.setOnAction(e -> new AccountEntryDialog("CUSTOMER").show(primaryStage));

        MenuItem supplierItem = new MenuItem("Supplier Entry");
        supplierItem.setOnAction(e -> new AccountEntryDialog("SUPPLIER").show(primaryStage));

        MenuItem vehicleItem = new MenuItem("Vehicle Master");
        vehicleItem.setOnAction(e -> new VehicleMasterDialog().show(primaryStage));

        mastersMenu.getItems().addAll(customerItem, supplierItem, new SeparatorMenuItem(), vehicleItem);

        // ── Billing Menu ──
        Menu billingMenu = new Menu("Billing");

        MenuItem invoiceRegItem = new MenuItem("Invoice Register");
        invoiceRegItem.setOnAction(e -> new InvoiceRegisterDialog().show(primaryStage));

        MenuItem paymentEntryItem = new MenuItem("Payment Entry");
        paymentEntryItem.setOnAction(e -> new PaymentEntryDialog(this::refreshDashboard).show(primaryStage));

        MenuItem paymentRegItem = new MenuItem("Payment Register");
        paymentRegItem.setOnAction(e -> new PaymentRegisterDialog().show(primaryStage));

        billingMenu.getItems().addAll(invoiceRegItem, new SeparatorMenuItem(), paymentEntryItem, paymentRegItem);

        // ── Reports Menu ──
        Menu reportsMenu = new Menu("Reports");

        MenuItem outstandingItem = new MenuItem("Outstanding Register");
        outstandingItem.setOnAction(e -> new OutstandingRegisterDialog().show(primaryStage));

        MenuItem statementItem = new MenuItem("Account Statement");
        statementItem.setOnAction(e -> new AccountStatementDialog().show(primaryStage));

        reportsMenu.getItems().addAll(outstandingItem, statementItem);

        // ── Settings Menu ──
        Menu settingsMenu = new Menu("Settings");

        MenuItem dbSetupItem = new MenuItem("Database Setup");
        dbSetupItem.setOnAction(e -> DatabaseSetupDialog.show(primaryStage, () -> {}));

        MenuItem companyItem = new MenuItem("Company Settings");
        companyItem.setOnAction(e -> showCompanySettings());

        settingsMenu.getItems().addAll(dbSetupItem, companyItem);

        menuBar.getMenus().addAll(mastersMenu, billingMenu, reportsMenu, settingsMenu);
        return menuBar;
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

    public static void main(String[] args) {
        launch(args);
    }
}
