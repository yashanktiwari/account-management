package com.accounting.ui.dialog;

import com.accounting.dao.SettingsDAO;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.BackupService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BackupRestoreView {

    private static final Logger log = AppLogger.get(BackupRestoreView.class);
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final SettingsDAO settingsDAO = new SettingsDAO();
    private VBox backupListContainer;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private String backupPath;

    public Parent createContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("Backup & Restore");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        // Load backup path
        try {
            backupPath = settingsDAO.getSetting("backup_path");
        } catch (Exception e) {
            log.error("Failed to load backup path", e);
        }
        if (backupPath == null || backupPath.isBlank()) {
            backupPath = System.getProperty("user.home") + File.separator + "AccountManagement_Backups";
        }

        HBox actionCards = buildActionCards();
        VBox backupHistory = buildBackupHistory();

        root.getChildren().addAll(title, actionCards, backupHistory);
        VBox.setVgrow(backupHistory, Priority.ALWAYS);

        refreshBackupList();
        return root;
    }

    private HBox buildActionCards() {
        HBox container = new HBox(16);
        container.setAlignment(Pos.CENTER);

        // Backup Card
        VBox backupCard = new VBox(12);
        backupCard.setPadding(new Insets(24));
        backupCard.setAlignment(Pos.CENTER);
        backupCard.setMaxWidth(Double.MAX_VALUE);
        backupCard.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; " +
            "-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        Label backupIcon = new Label("💾");
        backupIcon.setStyle("-fx-font-size: 36px;");

        Label backupTitle = new Label("Create Backup");
        backupTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label backupDesc = new Label("Export your database to a SQL file.\nBackups are saved to:\n" + backupPath);
        backupDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        backupDesc.setWrapText(true);
        backupDesc.setAlignment(Pos.CENTER);

        Button backupBtn = new Button("Take Backup Now");
        backupBtn.setStyle(
            "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 10 24 10 24; -fx-background-radius: 6; -fx-font-size: 13px;"
        );
        backupBtn.setOnAction(e -> takeBackup());

        backupCard.getChildren().addAll(backupIcon, backupTitle, backupDesc, backupBtn);

        // Restore Card
        VBox restoreCard = new VBox(12);
        restoreCard.setPadding(new Insets(24));
        restoreCard.setAlignment(Pos.CENTER);
        restoreCard.setMaxWidth(Double.MAX_VALUE);
        restoreCard.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; " +
            "-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        Label restoreIcon = new Label("📂");
        restoreIcon.setStyle("-fx-font-size: 36px;");

        Label restoreTitle = new Label("Restore from Backup");
        restoreTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label restoreDesc = new Label("Import a previously exported SQL backup file.\nThis will replace all current data.");
        restoreDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        restoreDesc.setWrapText(true);
        restoreDesc.setAlignment(Pos.CENTER);

        Button restoreBtn = new Button("Import from File");
        restoreBtn.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 10 24 10 24; -fx-background-radius: 6; -fx-font-size: 13px;"
        );
        restoreBtn.setOnAction(e -> restoreFromFile());

        restoreCard.getChildren().addAll(restoreIcon, restoreTitle, restoreDesc, restoreBtn);

        HBox.setHgrow(backupCard, Priority.ALWAYS);
        HBox.setHgrow(restoreCard, Priority.ALWAYS);

        // Status area
        VBox statusCard = new VBox(12);
        statusCard.setPadding(new Insets(24));
        statusCard.setAlignment(Pos.CENTER);
        statusCard.setMaxWidth(Double.MAX_VALUE);
        statusCard.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; " +
            "-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        Label statusIcon = new Label("📊");
        statusIcon.setStyle("-fx-font-size: 36px;");

        Label statusTitle = new Label("Status");
        statusTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(40, 40);
        progressIndicator.setVisible(false);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        statusCard.getChildren().addAll(statusIcon, statusTitle, progressIndicator, statusLabel);
        HBox.setHgrow(statusCard, Priority.ALWAYS);

        container.getChildren().addAll(backupCard, restoreCard, statusCard);
        return container;
    }

    private VBox buildBackupHistory() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle(
            "-fx-background-color: white; -fx-background-radius: 8; " +
            "-fx-border-color: #e2e8f0; -fx-border-radius: 8;"
        );
        VBox.setVgrow(section, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("Backup History");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-padding: 4 12 4 12; -fx-background-radius: 4;");
        refreshBtn.setOnAction(e -> refreshBackupList());

        header.getChildren().addAll(sectionTitle, spacer, refreshBtn);

        backupListContainer = new VBox(4);

        ScrollPane scrollPane = new ScrollPane(backupListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        section.getChildren().addAll(header, scrollPane);
        return section;
    }

    private void refreshBackupList() {
        backupListContainer.getChildren().clear();

        List<File> backups = BackupService.listBackups(backupPath);

        if (backups.isEmpty()) {
            Label empty = new Label("No backups found in: " + backupPath);
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20;");
            backupListContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < backups.size(); i++) {
            File backup = backups.get(i);
            HBox row = createBackupRow(backup, i);
            backupListContainer.getChildren().add(row);
        }
    }

    private HBox createBackupRow(File backup, int index) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        String bgColor = index % 2 == 0 ? "#f8fafc" : "white";
        row.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 6;");

        Label nameLabel = new Label(backup.getName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        LocalDateTime modified = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(backup.lastModified()), ZoneId.systemDefault()
        );
        Label dateLabel = new Label(modified.format(DISPLAY_FMT));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        long sizeKB = backup.length() / 1024;
        String sizeStr = sizeKB > 1024 ? String.format("%.1f MB", sizeKB / 1024.0) : sizeKB + " KB";
        Label sizeLabel = new Label(sizeStr);
        sizeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        sizeLabel.setMinWidth(70);

        Button restoreBtn = new Button("Restore");
        restoreBtn.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 11px; " +
            "-fx-padding: 4 12 4 12; -fx-background-radius: 4;"
        );
        restoreBtn.setOnAction(e -> restoreFromBackup(backup));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
            "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; " +
            "-fx-padding: 4 12 4 12; -fx-background-radius: 4;"
        );
        deleteBtn.setOnAction(e -> deleteBackup(backup));

        row.getChildren().addAll(nameLabel, dateLabel, sizeLabel, restoreBtn, deleteBtn);
        return row;
    }

    private void takeBackup() {
        setStatus("Taking backup...", true);

        AppExecutor.submit(() -> {
            try {
                String filePath = BackupService.backupDatabase(backupPath);
                BackupService.cleanupOldBackups(backupPath, 15);
                Platform.runLater(() -> {
                    setStatus("Backup completed!\n" + new File(filePath).getName(), false);
                    refreshBackupList();
                });
            } catch (Exception e) {
                log.error("Backup failed", e);
                Platform.runLater(() -> {
                    setStatus("Backup failed: " + e.getMessage(), false);
                    AlertUtil.showError("Backup Error", "Failed to create backup: " + e.getMessage());
                });
            }
        });
    }

    private void restoreFromFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Backup File to Restore");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Backup Files", "*.sql"));

        File initialDir = new File(backupPath);
        if (initialDir.exists()) {
            fc.setInitialDirectory(initialDir);
        }

        Window window = statusLabel.getScene() != null ? statusLabel.getScene().getWindow() : null;
        File file = fc.showOpenDialog(window);

        if (file != null) {
            confirmAndRestore(file);
        }
    }

    private void restoreFromBackup(File backup) {
        confirmAndRestore(backup);
    }

    private void confirmAndRestore(File backupFile) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Restore");
        confirm.setHeaderText("Restore from backup?");
        confirm.setContentText(
            "This will replace ALL current data with the backup:\n\n" +
            backupFile.getName() + "\n\n" +
            "This action cannot be undone. Are you sure?"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                setStatus("Restoring database...", true);

                AppExecutor.submit(() -> {
                    try {
                        BackupService.restoreDatabase(backupFile.getAbsolutePath());
                        Platform.runLater(() -> {
                            setStatus("Restore completed!\nPlease restart the application.", false);
                            AlertUtil.showInfo("Restore Complete",
                                "Database restored successfully from:\n" + backupFile.getName() +
                                "\n\nPlease restart the application for changes to take effect.");
                        });
                    } catch (Exception e) {
                        log.error("Restore failed", e);
                        Platform.runLater(() -> {
                            setStatus("Restore failed: " + e.getMessage(), false);
                            AlertUtil.showError("Restore Error", "Failed to restore: " + e.getMessage());
                        });
                    }
                });
            }
        });
    }

    private void deleteBackup(File backup) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Backup");
        confirm.setHeaderText("Delete this backup?");
        confirm.setContentText("File: " + backup.getName() + "\n\nThis cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (backup.delete()) {
                    refreshBackupList();
                } else {
                    AlertUtil.showError("Error", "Could not delete the backup file.");
                }
            }
        });
    }

    private void setStatus(String message, boolean showProgress) {
        statusLabel.setText(message);
        progressIndicator.setVisible(showProgress);
        if (showProgress) {
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563eb; -fx-font-weight: bold;");
        } else if (message.toLowerCase().contains("failed") || message.toLowerCase().contains("error")) {
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
        }
    }
}
