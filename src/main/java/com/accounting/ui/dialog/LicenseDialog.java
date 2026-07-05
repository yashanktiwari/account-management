package com.accounting.ui.dialog;

import com.accounting.util.AlertUtil;
import com.accounting.util.LicenseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class LicenseDialog {

    public static void showActivationDialog(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Activate License");
        dialog.setHeaderText("Enter your license key to activate the full version");
        dialog.initOwner(owner);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "Please enter the license key provided to you.\n" +
            "Contact support if you need assistance."
        );
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        infoLabel.setWrapText(true);

        TextArea licenseKeyField = new TextArea();
        licenseKeyField.setPromptText("Paste your license key here...");
        licenseKeyField.setPrefRowCount(4);
        licenseKeyField.setWrapText(true);

        content.getChildren().addAll(infoLabel, licenseKeyField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String key = licenseKeyField.getText().trim();
                if (key.isEmpty()) {
                    AlertUtil.showWarning("Invalid Input", "Please enter a license key.");
                    return;
                }

                if (LicenseManager.activateLicense(key)) {
                    AlertUtil.showInfo("Success", 
                        "License activated successfully!\n\n" +
                        "You now have full access to all features.");
                } else {
                    AlertUtil.showError("Invalid License", 
                        "The license key you entered is invalid.\n\n" +
                        "Please check the key and try again, or contact support.");
                }
            }
        });
    }

    public static void showLicenseInfo(Window owner) {
        LicenseManager.LicenseStatus status = LicenseManager.checkLicense();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("License Information");
        alert.setHeaderText("Current License Status");
        alert.initOwner(owner);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));

        Label typeLabel = new Label("License Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");
        Label typeValue = new Label(status.type.toString());

        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-weight: bold;");
        Label statusValue = new Label(status.message);

        grid.add(typeLabel, 0, 0);
        grid.add(typeValue, 1, 0);
        grid.add(statusLabel, 0, 1);
        grid.add(statusValue, 1, 1);

        if (status.type == LicenseManager.LicenseType.DEMO && status.daysRemaining > 0) {
            Label daysLabel = new Label("Days Remaining:");
            daysLabel.setStyle("-fx-font-weight: bold;");
            Label daysValue = new Label(String.valueOf(status.daysRemaining));
            grid.add(daysLabel, 0, 2);
            grid.add(daysValue, 1, 2);
        }

        alert.getDialogPane().setContent(grid);
        alert.showAndWait();
    }
}
