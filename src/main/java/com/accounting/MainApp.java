package com.accounting;

import atlantafx.base.theme.PrimerLight;
import com.accounting.dao.SettingsDAO;
import com.accounting.database.AppConfig;
import com.accounting.database.DBConnection;
import com.accounting.ui.dialog.*;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.NotificationUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainApp extends Application {

    private static final Logger log = AppLogger.get(MainApp.class);

    private static Stage primaryStage;
    private static MainApp instance;

    private StackPane contentHost;
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
    private Button ledgerBtn;

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
        contentHost = new StackPane();
        root.setCenter(contentHost);
        showDashboard();

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
        ledgerBtn = sidebarButton("Party Ledger", this::showLedger);

        Label settingsTitle = new Label("Settings");
        settingsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 12 0 4 8;");

        Button dbBtn = sidebarButton("Database Setup", () -> DatabaseSetupDialog.show(primaryStage, () -> {}));
        Button invoiceBtn = sidebarButton("App Settings", this::showInvoiceSettings);

        sidebar.getChildren().addAll(
                navTitle, dashBtn,
            mastersTitle, partyBtn,
                invoicesTitle, purchaseInvoiceBtn, saleInvoiceBtn,
                receiptsTitle, purchaseReceiptBtn, saleReceiptBtn,
                slipsTitle, loadingSlipBtn, lorryReceiptBtn,
                reportsTitle, reportsBtn, ledgerBtn,
                settingsTitle, dbBtn, invoiceBtn
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
        showContent(new DashboardView().createContent());
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

    private void showLedger() {
        showContent(new LedgerView().createContent());
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

                String currentReceiptNumber = settingsDAO.getSetting("purchase_receipt_starting_number");
                if (currentReceiptNumber == null) {
                    currentReceiptNumber = "1";
                }

                String finalCurrentStartingNumber = currentStartingNumber;
                String finalCurrentSlipNumber = currentSlipNumber;
                String finalCurrentLrNumber = currentLrNumber;
                String finalCurrentReceiptNumber = currentReceiptNumber;
                Platform.runLater(() -> {
                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("App Settings");
                    dialog.initOwner(primaryStage);

                    TextField startingNumberField = new TextField(finalCurrentStartingNumber);
                    startingNumberField.setPrefWidth(200);

                    TextField slipStartingNumberField = new TextField(finalCurrentSlipNumber);
                    slipStartingNumberField.setPrefWidth(200);

                    TextField lrStartingNumberField = new TextField(finalCurrentLrNumber);
                    lrStartingNumberField.setPrefWidth(200);

                    TextField receiptStartingNumberField = new TextField(finalCurrentReceiptNumber);
                    receiptStartingNumberField.setPrefWidth(200);

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(15));
                    grid.add(new Label("Global Starting Invoice Number:"), 0, 0);
                    grid.add(startingNumberField, 1, 0);
                    grid.add(new Label("This number will be used for all invoices."), 0, 1);
                    GridPane.setColumnSpan(grid.getChildren().get(2), 2);

                    grid.add(new Label("Loading Slip Starting Number:"), 0, 2);
                    grid.add(slipStartingNumberField, 1, 2);
                    grid.add(new Label("This number will be used for loading slips."), 0, 3);
                    GridPane.setColumnSpan(grid.getChildren().get(5), 2);

                    grid.add(new Label("LR Starting Number:"), 0, 4);
                    grid.add(lrStartingNumberField, 1, 4);
                    grid.add(new Label("This number will be used for lorry receipts."), 0, 5);
                    GridPane.setColumnSpan(grid.getChildren().get(8), 2);

                    grid.add(new Label("Receipt Starting Number:"), 0, 6);
                    grid.add(receiptStartingNumberField, 1, 6);
                    grid.add(new Label("This number will be used for both purchase and sale receipts."), 0, 7);
                    GridPane.setColumnSpan(grid.getChildren().get(11), 2);

                    dialog.getDialogPane().setContent(grid);
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                    dialog.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            try {
                                int num = Integer.parseInt(startingNumberField.getText().trim());
                                int slipNum = Integer.parseInt(slipStartingNumberField.getText().trim());
                                int lrNum = Integer.parseInt(lrStartingNumberField.getText().trim());
                                int receiptNum = Integer.parseInt(receiptStartingNumberField.getText().trim());
                                if (num >= 0 && slipNum >= 0 && lrNum >= 0 && receiptNum >= 0) {
                                    AppExecutor.submit(() -> {
                                        try {
                                            settingsDAO.saveSetting("global_invoice_starting_number", String.valueOf(num));
                                            settingsDAO.saveSetting("loading_slip_starting_number", String.valueOf(slipNum));
                                            settingsDAO.saveSetting("lr_starting_number", String.valueOf(lrNum));
                                            settingsDAO.saveSetting("purchase_receipt_starting_number", String.valueOf(receiptNum));
                                            settingsDAO.saveSetting("sale_receipt_starting_number", String.valueOf(receiptNum));
                                            Platform.runLater(() -> {
                                                AlertUtil.showInfo("Success", "Settings updated.\nInvoice: " + num + "  |  Loading Slip: " + slipNum + "  |  LR: " + lrNum + "  |  Receipt: " + receiptNum);
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
