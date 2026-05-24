package com.accounting.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.util.Optional;

public class AlertUtil {

    private static Alert createAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(380);
        messageLabel.setMaxWidth(380);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(messageLabel);
        content.setFillWidth(true);

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);
        pane.setPrefWidth(450);
        pane.setMinHeight(Region.USE_PREF_SIZE);
        pane.setPadding(new Insets(14));

        pane.setStyle("""
            -fx-background-color: white;
            -fx-font-size: 14px;
        """);

        return alert;
    }

    private static void styleButton(Button button, String color) {
        if (button == null) return;
        button.setStyle("""
                -fx-background-color:%s;
                -fx-text-fill:white;
                -fx-font-weight:bold;
                -fx-padding:6 18 6 18;
                -fx-background-radius:6;
                """.formatted(color));
    }

    public static void showInfo(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.INFORMATION, title, message);
        alert.getButtonTypes().setAll(ButtonType.OK);
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary-button");
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.ERROR, title, message);
        alert.getButtonTypes().setAll(ButtonType.OK);
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        styleButton(okButton, "#ef4444");
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.WARNING, title, message);
        alert.getButtonTypes().setAll(ButtonType.OK);
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        styleButton(okButton, "#f59e0b");
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Confirm");
        styleButton(okButton, "#2563eb");

        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setText("Cancel");
            cancelButton.setStyle("""
                    -fx-background-color:#e5e7eb;
                    -fx-text-fill:#111827;
                    -fx-font-weight:bold;
                    -fx-padding:6 18 6 18;
                    -fx-background-radius:6;
                    """);
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
