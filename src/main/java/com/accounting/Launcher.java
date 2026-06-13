package com.accounting;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Launcher extends Application {

    private static final String CORRECT_PASSWORD = "Yashank01$";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Account Management - Security Check");
        primaryStage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f8fafc;");

        Label titleLabel = new Label("Account Management");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label instructionLabel = new Label("Enter password to launch the application:");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setPrefWidth(250);

        Button launchButton = new Button("Launch");
        launchButton.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30 10 30; -fx-background-radius: 6;");
        launchButton.setPrefWidth(150);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");

        launchButton.setOnAction(e -> {
            String enteredPassword = passwordField.getText();
            if (enteredPassword.equals(CORRECT_PASSWORD)) {
                // Password correct - launch MainApp
                try {
                    MainApp mainApp = new MainApp();
                    mainApp.start(new Stage());
                    primaryStage.close();
                } catch (Exception ex) {
                    errorLabel.setText("Failed to launch application: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                errorLabel.setText("Incorrect password. Please try again.");
                passwordField.clear();
                passwordField.requestFocus();
            }
        });

        passwordField.setOnAction(e -> launchButton.fire());

        root.getChildren().addAll(titleLabel, instructionLabel, passwordField, launchButton, errorLabel);

        Scene scene = new Scene(root, 400, 250);
        scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        passwordField.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
