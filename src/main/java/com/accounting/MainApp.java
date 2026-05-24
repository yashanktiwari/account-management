package com.accounting;

import atlantafx.base.theme.PrimerLight;
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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.util.Objects;

public class MainApp extends Application {

    private static final Logger log = AppLogger.get(MainApp.class);

    private static Stage primaryStage;

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

        // ── Center: Welcome / Dashboard area ──
        VBox center = new VBox(20);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        Label welcomeLabel = new Label("Account Management System");
        welcomeLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label subtitleLabel = new Label("Use the menu bar to navigate to different sections");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        // Quick action buttons
        Button customerBtn = createQuickAction("Customer Entry", "#16a34a",
                () -> new AccountEntryDialog("CUSTOMER").show(stage));
        Button supplierBtn = createQuickAction("Supplier Entry", "#dc2626",
                () -> new AccountEntryDialog("SUPPLIER").show(stage));
        Button vehicleBtn = createQuickAction("Vehicle Master", "#1e3a5f",
                () -> new VehicleMasterDialog().show(stage));
        Button invoiceBtn = createQuickAction("Invoice Register", "#7c3aed",
                () -> new InvoiceRegisterDialog().show(stage));
        Button outstandingBtn = createQuickAction("Outstanding Register", "#ea580c",
                () -> new OutstandingRegisterDialog().show(stage));
        Button statementBtn = createQuickAction("Account Statement", "#0891b2",
                () -> new AccountStatementDialog().show(stage));

        HBox row1 = new HBox(16, customerBtn, supplierBtn, vehicleBtn);
        row1.setAlignment(Pos.CENTER);
        HBox row2 = new HBox(16, invoiceBtn, outstandingBtn, statementBtn);
        row2.setAlignment(Pos.CENTER);

        center.getChildren().addAll(welcomeLabel, subtitleLabel, row1, row2);
        root.setCenter(center);

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

        // ── Registers Menu ──
        Menu registersMenu = new Menu("Registers");

        MenuItem invoiceRegItem = new MenuItem("Invoice Register");
        invoiceRegItem.setOnAction(e -> new InvoiceRegisterDialog().show(primaryStage));

        MenuItem outstandingItem = new MenuItem("Outstanding Register");
        outstandingItem.setOnAction(e -> new OutstandingRegisterDialog().show(primaryStage));

        registersMenu.getItems().addAll(invoiceRegItem, outstandingItem);

        // ── Reports Menu ──
        Menu reportsMenu = new Menu("Reports");

        MenuItem statementItem = new MenuItem("Account Statement");
        statementItem.setOnAction(e -> new AccountStatementDialog().show(primaryStage));

        MenuItem invoicePdfItem = new MenuItem("Invoice Register (PDF)");
        invoicePdfItem.setOnAction(e -> {
            // Open the register dialog and use its export
            new InvoiceRegisterDialog().show(primaryStage);
        });

        reportsMenu.getItems().addAll(statementItem, invoicePdfItem);

        // ── Settings Menu ──
        Menu settingsMenu = new Menu("Settings");

        MenuItem dbSetupItem = new MenuItem("Database Setup");
        dbSetupItem.setOnAction(e -> DatabaseSetupDialog.show(primaryStage, () -> {}));

        MenuItem companyItem = new MenuItem("Company Settings");
        companyItem.setOnAction(e -> showCompanySettings());

        settingsMenu.getItems().addAll(dbSetupItem, companyItem);

        menuBar.getMenus().addAll(mastersMenu, registersMenu, reportsMenu, settingsMenu);
        return menuBar;
    }

    private Button createQuickAction(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setPrefWidth(180);
        btn.setPrefHeight(60);
        btn.setStyle("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-cursor: hand;
                """.formatted(color));
        btn.setOnAction(e -> action.run());
        return btn;
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
