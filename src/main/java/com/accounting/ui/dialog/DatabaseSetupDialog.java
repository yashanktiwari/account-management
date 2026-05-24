package com.accounting.ui.dialog;

import com.accounting.database.AppConfig;
import com.accounting.database.DBConnection;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Window;

public class DatabaseSetupDialog {

    public static void show(Window owner, Runnable onSuccess) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(owner, onSuccess));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Setup Database");
        dialog.setHeaderText("Enter MySQL Server Details");
        dialog.getDialogPane().setPrefWidth(450);
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.NONE);

        ButtonType connectBtn = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectBtn, ButtonType.CANCEL);

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("3306");
        TextField dbNameField = new TextField("account_management");
        TextField userField = new TextField("root");
        PasswordField passField = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Host:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("Database Name:"), 0, 2);
        grid.add(dbNameField, 1, 2);
        grid.add(new Label("Username:"), 0, 3);
        grid.add(userField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passField, 1, 4);

        VBox container = new VBox(15, grid);
        container.setPadding(new Insets(15));

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.setStyle("-fx-text-fill: #b91c1c;");
        container.getChildren().add(statusLabel);

        dialog.getDialogPane().setContent(container);

        Button connectButton = (Button) dialog.getDialogPane().lookupButton(connectBtn);
        connectButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            final String host = hostField.getText();
            final String port = portField.getText();
            final String dbName = dbNameField.getText();
            final String user = userField.getText();
            final String pass = passField.getText();

            statusLabel.setText("");
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
            connectButton.setDisable(true);
            connectButton.setText("Connecting...");

            AppExecutor.submit(() -> {
                try {
                    DBConnection.setDatabaseConfig(host, port, dbName, user, pass);
                    DBConnection.createDatabaseIfNotExists();
                    DBConnection.initializeDatabase();
                    AppConfig.saveDatabaseConfig(host, port, dbName, user, pass);

                    Platform.runLater(() -> {
                        dialog.setResult(connectBtn);
                        dialog.close();
                        if (onSuccess != null) onSuccess.run();
                        Platform.runLater(() ->
                                AlertUtil.showInfo("Database Connected", "Database connected successfully."));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        connectButton.setText("Connect");
                        String message = e.getMessage();
                        if (message == null || message.isBlank()) message = e.getClass().getSimpleName();
                        statusLabel.setText("Could not connect to MySQL.\n" + message);
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                    });
                }
            });
        });

        dialog.showAndWait();
    }
}
