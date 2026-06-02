package com.accounting.ui.dialog;

import com.accounting.util.AlertUtil;
import com.accounting.util.AppLogger;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiConsumer;

public class PrintPreviewDialog {

    private static final Logger log = AppLogger.get(PrintPreviewDialog.class);
    private Stage stage;
    private final String pdfFilePath;
    private final BiConsumer<String, String> pdfGenerator; // (copyLabel, outputPath) -> generates PDF

    public PrintPreviewDialog(String pdfFilePath) {
        this(pdfFilePath, null);
    }

    public PrintPreviewDialog(String pdfFilePath, BiConsumer<String, String> pdfGenerator) {
        this.pdfFilePath = pdfFilePath;
        this.pdfGenerator = pdfGenerator;
    }

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("Print Preview");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f2f5;");

        // --- Top bar ---
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.setStyle("-fx-background-color: #1e3a5f;");

        Label titleLabel = new Label("Print Preview");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label copiesLabel = new Label("Number of Copies:");
        copiesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");

        Spinner<Integer> copiesSpinner = new Spinner<>();
        copiesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        copiesSpinner.setPrefWidth(80);
        copiesSpinner.setEditable(true);
        copiesSpinner.setStyle("-fx-font-size: 13px;");

        Button printButton = new Button("Print");
        printButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 8 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        printButton.setOnAction(e -> printCopies(copiesSpinner.getValue()));

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 8 24; -fx-background-radius: 6; -fx-cursor: hand;");
        closeButton.setOnAction(e -> stage.close());

        topBar.getChildren().addAll(titleLabel, spacer, copiesLabel, copiesSpinner, printButton, closeButton);
        root.setTop(topBar);

        // --- Center: PDF Preview ---
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #e5e7eb; -fx-background-color: #e5e7eb;");
        scrollPane.setPadding(new Insets(10));

        VBox pagesContainer = new VBox(15);
        pagesContainer.setAlignment(Pos.TOP_CENTER);
        pagesContainer.setPadding(new Insets(10));

        // Render PDF pages as images
        try {
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                PDDocument document = Loader.loadPDF(pdfFile);
                PDFRenderer renderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();

                for (int i = 0; i < pageCount; i++) {
                    BufferedImage bufferedImage = renderer.renderImageWithDPI(i, 150);
                    WritableImage fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    ImageView imageView = new ImageView(fxImage);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(560);
                    imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 2, 2);");

                    // Page number label
                    Label pageLabel = new Label("Page " + (i + 1) + " of " + pageCount);
                    pageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                    VBox pageBox = new VBox(4, imageView, pageLabel);
                    pageBox.setAlignment(Pos.CENTER);
                    pagesContainer.getChildren().add(pageBox);
                }

                document.close();
            } else {
                Label errorLabel = new Label("PDF file not found: " + pdfFilePath);
                errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444;");
                pagesContainer.getChildren().add(errorLabel);
            }
        } catch (Exception e) {
            log.error("Failed to render PDF preview", e);
            Label errorLabel = new Label("Failed to load PDF preview: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444;");
            errorLabel.setWrapText(true);
            pagesContainer.getChildren().add(errorLabel);
        }

        scrollPane.setContent(pagesContainer);
        root.setCenter(scrollPane);

        // --- Bottom info bar ---
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 20, 8, 20));
        bottomBar.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        Label infoLabel = new Label("First copy: Original | Additional copies: Duplicate");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Label pathLabel = new Label("File: " + pdfFilePath);
        pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        pathLabel.setWrapText(true);

        javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);

        bottomBar.getChildren().addAll(infoLabel, spacer2, pathLabel);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 700, 850);
        stage.setScene(scene);
        stage.show();
    }

    private void printCopies(int numberOfCopies) {
        try {
            java.util.List<File> filesToPrint = new java.util.ArrayList<>();

            // First copy is the Original (already generated)
            filesToPrint.add(new File(pdfFilePath));

            // Generate Duplicate copies if needed
            if (numberOfCopies > 1 && pdfGenerator != null) {
                String basePath = pdfFilePath.replace(".pdf", "");
                for (int i = 2; i <= numberOfCopies; i++) {
                    String duplicatePath = basePath + "_Duplicate_" + i + ".pdf";
                    try {
                        pdfGenerator.accept("Duplicate", duplicatePath);
                        filesToPrint.add(new File(duplicatePath));
                    } catch (Exception e) {
                        log.error("Failed to generate duplicate copy " + i, e);
                    }
                }
            } else if (numberOfCopies > 1) {
                // No generator available, print same file multiple times
                for (int i = 2; i <= numberOfCopies; i++) {
                    filesToPrint.add(new File(pdfFilePath));
                }
            }

            // Print all files
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                for (File file : filesToPrint) {
                    if (file.exists()) {
                        if (desktop.isSupported(Desktop.Action.PRINT)) {
                            desktop.print(file);
                        } else {
                            desktop.open(file);
                        }
                    }
                }
                AlertUtil.showInfo("Print", "Sent " + numberOfCopies + " copy(ies) to printer.\n" +
                        "Copy 1: Original" + (numberOfCopies > 1 ? "\nCopies 2-" + numberOfCopies + ": Duplicate" : ""));
            } else {
                AlertUtil.showError("Error", "Desktop operations not supported on this system");
            }
        } catch (Exception e) {
            log.error("Failed to print", e);
            AlertUtil.showError("Error", "Failed to print: " + e.getMessage());
        }
    }
}
