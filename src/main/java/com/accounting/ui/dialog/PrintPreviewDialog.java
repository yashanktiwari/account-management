package com.accounting.ui.dialog;

import com.accounting.util.AlertUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import com.accounting.util.AppLogger;

import java.awt.Desktop;
import java.io.File;

public class PrintPreviewDialog {

    private static final Logger log = AppLogger.get(PrintPreviewDialog.class);
    private Stage stage;
    private String pdfFilePath;

    public PrintPreviewDialog(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("Print Preview");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        // Center content
        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(40));

        Label titleLabel = new Label("Invoice PDF Preview");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label infoLabel = new Label("PDF has been generated successfully!");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Label pathLabel = new Label("Location: " + pdfFilePath);
        pathLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(500);

        // Icon or placeholder
        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 48px;");

        centerContent.getChildren().addAll(iconLabel, titleLabel, infoLabel, pathLabel);
        root.setCenter(centerContent);

        // Bottom buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));

        Button viewButton = new Button("View PDF");
        viewButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        viewButton.setOnAction(e -> openPDF());

        Button printButton = new Button("Print");
        printButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        printButton.setOnAction(e -> printPDF());

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        closeButton.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(viewButton, printButton, closeButton);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void openPDF() {
        try {
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                } else {
                    AlertUtil.showError("Error", "Desktop operations not supported on this system");
                }
            } else {
                AlertUtil.showError("Error", "PDF file not found: " + pdfFilePath);
            }
        } catch (Exception e) {
            log.error("Failed to open PDF", e);
            AlertUtil.showError("Error", "Failed to open PDF: " + e.getMessage());
        }
    }

    private void printPDF() {
        try {
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.PRINT)) {
                        desktop.print(pdfFile);
                        AlertUtil.showInfo("Success", "Print dialog opened. Please select your printer.");
                    } else {
                        // Fallback: just open the PDF and user can print from there
                        desktop.open(pdfFile);
                        AlertUtil.showInfo("Info", "Please use the print option in your PDF viewer");
                    }
                } else {
                    AlertUtil.showError("Error", "Desktop operations not supported on this system");
                }
            } else {
                AlertUtil.showError("Error", "PDF file not found: " + pdfFilePath);
            }
        } catch (Exception e) {
            log.error("Failed to print PDF", e);
            AlertUtil.showError("Error", "Failed to print PDF: " + e.getMessage());
        }
    }
}
