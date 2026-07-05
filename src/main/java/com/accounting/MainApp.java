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
    private Runnable currentViewRefresher;
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

    /**
     * Refreshes the currently active view. Called after database restore.
     */
    public static void refreshCurrentView() {
        if (instance != null && instance.currentViewRefresher != null) {
            Platform.runLater(() -> {
                instance.currentViewRefresher.run();
                NotificationUtil.showSuccess("Data Refreshed", "All data has been reloaded from the restored database.");
            });
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
            event.consume(); // Prevent immediate close
            performBackupAndExit(stage);
        });

        stage.show();
    }

    private void performBackupAndExit(Stage stage) {
        // Show a blocking dialog with progress while backup runs
        javafx.scene.control.Dialog<Void> backupDialog = new javafx.scene.control.Dialog<>();
        backupDialog.setTitle("Backup in Progress");
        backupDialog.setHeaderText(null);
        backupDialog.initOwner(stage);

        VBox dialogContent = new VBox(16);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setPadding(new Insets(30));

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        Label messageLabel = new Label("Taking backup before closing...\nPlease wait.");
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e3a5f; -fx-text-alignment: center;");
        messageLabel.setAlignment(Pos.CENTER);

        dialogContent.getChildren().addAll(progressIndicator, messageLabel);
        backupDialog.getDialogPane().setContent(dialogContent);
        backupDialog.getDialogPane().getButtonTypes().clear(); // No buttons - auto closes

        // Run backup in background
        AppExecutor.submit(() -> {
            try {
                SettingsDAO settingsDAO = new SettingsDAO();
                String backupPath = settingsDAO.getSetting("backup_path");
                if (backupPath == null || backupPath.isBlank()) {
                    backupPath = System.getProperty("user.home") + java.io.File.separator + "AccountManagement_Backups";
                }
                com.accounting.util.BackupService.backupDatabase(backupPath);
                com.accounting.util.BackupService.cleanupOldBackups(backupPath, 15);
                log.info("Auto-backup completed successfully before exit.");
            } catch (Exception e) {
                log.error("Auto-backup failed before exit", e);
            } finally {
                Platform.runLater(() -> {
                    // Close the dialog by adding a dummy button and closing
                    backupDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    backupDialog.close();
                    AppExecutor.shutdown();
                    Platform.exit();
                    System.exit(0);
                });
            }
        });

        backupDialog.showAndWait();
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

        purchaseReceiptBtn = sidebarButton("Money Received", this::showPurchaseReceipt);
        saleReceiptBtn = sidebarButton("Money Paid", this::showSaleReceipt);

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
        Button backupBtn = sidebarButton("Backup & Restore", this::showBackupRestore);

        sidebar.getChildren().addAll(
                navTitle, dashBtn,
            mastersTitle, partyBtn,
                invoicesTitle, purchaseInvoiceBtn, saleInvoiceBtn,
                receiptsTitle, purchaseReceiptBtn, saleReceiptBtn,
                slipsTitle, loadingSlipBtn, lorryReceiptBtn,
                reportsTitle, reportsBtn, ledgerBtn,
                settingsTitle, dbBtn, invoiceBtn, backupBtn
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
        currentViewRefresher = this::showDashboard;
        showContent(new DashboardView().createContent());
        setActiveSidebarButton(dashBtn);
    }

    private void showContent(Parent content) {
        ScrollPane wrapper = new ScrollPane(content);
        wrapper.setFitToWidth(true);
        wrapper.setFitToHeight(true);
        wrapper.setStyle("-fx-background-color: #f8fafc;");
        contentHost.getChildren().setAll(wrapper);
    }

    private void showParties() {
        currentViewRefresher = this::showParties;
        showContent(new PartyMasterListView().createContent());
        setActiveSidebarButton(partyBtn);
    }

    private void showPurchaseInvoice() {
        currentViewRefresher = this::showPurchaseInvoice;
        showContent(new PurchaseInvoiceListView().createContent());
        setActiveSidebarButton(purchaseInvoiceBtn);
    }

    private void showSaleInvoice() {
        currentViewRefresher = this::showSaleInvoice;
        showContent(new SaleInvoiceListView().createContent());
        setActiveSidebarButton(saleInvoiceBtn);
    }

    private void showPurchaseReceipt() {
        currentViewRefresher = this::showPurchaseReceipt;
        showContent(new PurchaseReceiptListView().createContent());
        setActiveSidebarButton(purchaseReceiptBtn);
    }

    private void showSaleReceipt() {
        currentViewRefresher = this::showSaleReceipt;
        showContent(new SaleReceiptListView().createContent());
        setActiveSidebarButton(saleReceiptBtn);
    }

    private void showLoadingSlips() {
        currentViewRefresher = this::showLoadingSlips;
        showContent(new LoadingSlipListView().createContent());
        setActiveSidebarButton(loadingSlipBtn);
    }

    private void showLorryReceipts() {
        currentViewRefresher = this::showLorryReceipts;
        showContent(new LorryReceiptListView().createContent());
        setActiveSidebarButton(lorryReceiptBtn);
    }

    private void showReports() {
        currentViewRefresher = this::showReports;
        showContent(new ReportView().createContent());
        setActiveSidebarButton(reportsBtn);
    }

    private void showLedger() {
        currentViewRefresher = this::showLedger;
        showContent(new LedgerView().createContent());
        setActiveSidebarButton(ledgerBtn);
    }

    private void showBackupRestore() {
        currentViewRefresher = this::showBackupRestore;
        showContent(new BackupRestoreView().createContent());
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

                String currentBackupPath = settingsDAO.getSetting("backup_path");
                if (currentBackupPath == null) {
                    currentBackupPath = System.getProperty("user.home") + java.io.File.separator + "AccountManagement_Backups";
                }

                String finalCurrentStartingNumber = currentStartingNumber;
                String finalCurrentSlipNumber = currentSlipNumber;
                String finalCurrentLrNumber = currentLrNumber;
                String finalCurrentReceiptNumber = currentReceiptNumber;
                String finalCurrentBackupPath = currentBackupPath;
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

                    TextField backupPathField = new TextField(finalCurrentBackupPath);
                    backupPathField.setPrefWidth(300);
                    Button browseBtn = new Button("Browse...");
                    browseBtn.setOnAction(ev -> {
                        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
                        dc.setTitle("Select Backup Directory");
                        if (backupPathField.getText() != null && !backupPathField.getText().isBlank()) {
                            java.io.File initialDir = new java.io.File(backupPathField.getText());
                            if (initialDir.exists()) dc.setInitialDirectory(initialDir);
                        }
                        java.io.File chosen = dc.showDialog(primaryStage);
                        if (chosen != null) backupPathField.setText(chosen.getAbsolutePath());
                    });
                    HBox backupPathBox = new HBox(8, backupPathField, browseBtn);
                    backupPathBox.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(backupPathField, Priority.ALWAYS);

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

                    grid.add(new Label("Backup Directory:"), 0, 8);
                    grid.add(backupPathBox, 1, 8);
                    grid.add(new Label("Auto-backup will be saved here when application closes."), 0, 9);
                    GridPane.setColumnSpan(grid.getChildren().get(14), 2);

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
                                            String bkPath = backupPathField.getText().trim();
                                            if (!bkPath.isBlank()) {
                                                settingsDAO.saveSetting("backup_path", bkPath);
                                            }
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
        // Check if this is the first launch
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MainApp.class);
        boolean isFirstLaunch = prefs.getBoolean("first_launch", true);
        
        if (isFirstLaunch) {
            // Password check only on first launch
            String password = "Yashank01$";
            javax.swing.JPasswordField pf = new javax.swing.JPasswordField();
            int result = javax.swing.JOptionPane.showConfirmDialog(
                    null, pf, "Enter Password to Launch (First Time Setup)", 
                    javax.swing.JOptionPane.OK_CANCEL_OPTION,
                    javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                String entered = new String(pf.getPassword());
                if (!entered.equals(password)) {
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "Incorrect password.", "Access Denied", javax.swing.JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                // Mark as no longer first launch
                prefs.putBoolean("first_launch", false);
            } else {
                System.exit(0);
            }
        }
        launch(args);
    }
}
