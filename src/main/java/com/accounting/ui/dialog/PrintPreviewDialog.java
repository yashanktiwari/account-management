package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppLogger;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class PrintPreviewDialog {

    private static final Logger log = AppLogger.get(PrintPreviewDialog.class);
    private final String pdfFilePath;
    private final BiConsumer<String, String> pdfGenerator;
    private Runnable onClose;

    public PrintPreviewDialog(String pdfFilePath) {
        this(pdfFilePath, null);
    }

    public PrintPreviewDialog(String pdfFilePath, BiConsumer<String, String> pdfGenerator) {
        this.pdfFilePath = pdfFilePath;
        this.pdfGenerator = pdfGenerator;
    }

    public void show(Window owner) {
        showInApp(null);
    }

    public void showInApp(Runnable onCloseCallback) {
        this.onClose = onCloseCallback;
        Parent previewPane = createPreviewPane();
        MainApp.showContentInApp(previewPane);
    }

    private Parent createPreviewPane() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f2f5;");

        // --- Top bar ---
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: #1e3a5f;");

        Label titleLabel = new Label("Print Preview");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label copiesLabel = new Label("Copies:");
        copiesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");

        Spinner<Integer> copiesSpinner = new Spinner<>();
        copiesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        copiesSpinner.setPrefWidth(75);
        copiesSpinner.setEditable(true);
        copiesSpinner.setStyle("-fx-font-size: 13px;");

        Button printButton = new Button("Print");
        printButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 8 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        printButton.setOnAction(e -> printCopies(copiesSpinner.getValue()));

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 8 24; -fx-background-radius: 6; -fx-cursor: hand;");
        closeButton.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        topBar.getChildren().addAll(titleLabel, spacer, copiesLabel, copiesSpinner, printButton, closeButton);
        root.setTop(topBar);

        // --- Center: PDF Preview ---
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #e5e7eb; -fx-background-color: #e5e7eb;");

        VBox pagesContainer = new VBox(15);
        pagesContainer.setAlignment(Pos.TOP_CENTER);
        pagesContainer.setPadding(new Insets(10));

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
        bottomBar.setPadding(new Insets(6, 16, 6, 16));
        bottomBar.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        Label infoLabel = new Label("Copy 1: Original  |  Copy 2: Duplicate  |  Copies 3+: Triplicate");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label pathLabel = new Label(pdfFilePath);
        pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        pathLabel.setWrapText(true);

        bottomBar.getChildren().addAll(infoLabel, spacer2, pathLabel);
        root.setBottom(bottomBar);

        return root;
    }

    private void printCopies(int numberOfCopies) {
        try {
            // Collect all PDF files: Original + Duplicates
            List<File> filesToPrint = new ArrayList<>();
            filesToPrint.add(new File(pdfFilePath));

            if (numberOfCopies > 1 && pdfGenerator != null) {
                String basePath = pdfFilePath.replace(".pdf", "");
                for (int i = 2; i <= numberOfCopies; i++) {
                    String copyLabel = (i == 2) ? "Duplicate" : "Triplicate";
                    String duplicatePath = basePath + "_" + copyLabel + "_" + i + ".pdf";
                    try {
                        pdfGenerator.accept(copyLabel, duplicatePath);
                        filesToPrint.add(new File(duplicatePath));
                    } catch (Exception e) {
                        log.error("Failed to generate " + copyLabel + " copy " + i, e);
                    }
                }
            } else if (numberOfCopies > 1) {
                for (int i = 2; i <= numberOfCopies; i++) {
                    filesToPrint.add(new File(pdfFilePath));
                }
            }

            // Merge all PDFs into one document for a single print job
            PDDocument mergedDoc = new PDDocument();
            for (File file : filesToPrint) {
                if (file.exists()) {
                    PDDocument doc = Loader.loadPDF(file);
                    for (int p = 0; p < doc.getNumberOfPages(); p++) {
                        mergedDoc.addPage(doc.getPage(p));
                    }
                    // Note: don't close doc yet, pages are referenced
                }
            }

            if (mergedDoc.getNumberOfPages() > 0) {
                PrinterJob printerJob = PrinterJob.getPrinterJob();
                printerJob.setPageable(new PDFPageable(mergedDoc));
                printerJob.setJobName("Invoice - " + numberOfCopies + " copies");

                // Show single native print dialog
                if (printerJob.printDialog()) {
                    printerJob.print();
                    String copyInfo = "Copy 1: Original";
                    if (numberOfCopies >= 2) {
                        copyInfo += "\nCopy 2: Duplicate";
                    }
                    if (numberOfCopies >= 3) {
                        copyInfo += "\nCopies 3-" + numberOfCopies + ": Triplicate";
                    }
                    AlertUtil.showInfo("Print", "Sent " + numberOfCopies + " copy(ies) to printer.\n" + copyInfo);
                }
            }

            mergedDoc.close();
        } catch (Exception e) {
            log.error("Failed to print", e);
            AlertUtil.showError("Error", "Failed to print: " + e.getMessage());
        }
    }
}
