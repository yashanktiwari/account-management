package com.accounting.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationUtil {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void showSuccess(String title, String message) {
        showToast("success-toast", title + "\n" + message, 4);
    }

    public static void showError(String title, String message) {
        showToast("error-toast", title + "\n" + message, 6);
    }

    public static void showWarning(String title, String message) {
        showToast("warning-toast", title + "\n" + message, 5);
    }

    private static void showToast(String styleClass, String message, int seconds) {
        Platform.runLater(() -> {
            if (primaryStage == null || !primaryStage.isShowing()) return;

            Popup popup = new Popup();

            Label icon = new Label("\u26A0\uFE0F");
            icon.setStyle("-fx-text-fill: white; -fx-font-size:22px;");
            icon.getStyleClass().add("toast-icon");

            String[] parts = message.split("\n", 2);

            Label title = new Label(parts[0]);
            title.getStyleClass().add("toast-title");

            Label body = new Label(parts.length > 1 ? parts[1] : "");
            body.getStyleClass().add("toast-body");
            body.setWrapText(true);

            VBox textBox = new VBox(8, title, body);

            HBox content = new HBox(16, icon, textBox);
            content.setAlignment(Pos.CENTER_LEFT);

            StackPane toast = new StackPane(content);
            toast.getStyleClass().addAll("toast", styleClass);
            toast.setPrefWidth(360);

            popup.getContent().add(toast);
            popup.show(primaryStage);

            double x = primaryStage.getX() + primaryStage.getWidth() - 380;
            double y = primaryStage.getY() + primaryStage.getHeight() - 130;

            popup.setX(x);
            popup.setY(y);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            PauseTransition wait = new PauseTransition(Duration.seconds(seconds));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            fadeOut.setOnFinished(e -> popup.hide());

            new SequentialTransition(fadeIn, wait, fadeOut).play();
        });
    }
}
