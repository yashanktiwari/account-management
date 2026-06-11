package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
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
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class PrintPreviewDialog {

    private static final Logger log = AppLogger.get(PrintPreviewDialog.class);
    private final String pdfFilePath;
    private final BiConsumer<String, String> pdfGenerator;
    private final boolean generateCopiesFromSpinner;
    private Runnable onClose;

    public PrintPreviewDialog(String pdfFilePath) {
        this(pdfFilePath, null);
    }

    public PrintPreviewDialog(String pdfFilePath, BiConsumer<String, String> pdfGenerator) {
        this(pdfFilePath, pdfGenerator, false);
    }

    public PrintPreviewDialog(String pdfFilePath, BiConsumer<String, String> pdfGenerator, boolean generateCopiesFromSpinner) {
        this.pdfFilePath = pdfFilePath;
        this.pdfGenerator = pdfGenerator;
        this.generateCopiesFromSpinner = generateCopiesFromSpinner;
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
        printButton.setOnAction(e -> showPrintDialog(copiesSpinner.getValue()));

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 8 24; -fx-background-radius: 6; -fx-cursor: hand;");
        closeButton.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        topBar.getChildren().addAll(titleLabel, spacer, copiesLabel, copiesSpinner, printButton, closeButton);
        root.setTop(topBar);

        // --- Center: PDF Preview (with loading indicator) ---
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #e5e7eb; -fx-background-color: #e5e7eb;");

        VBox pagesContainer = new VBox(15);
        pagesContainer.setAlignment(Pos.TOP_CENTER);
        pagesContainer.setPadding(new Insets(10));

        // Show loading message initially
        Label loadingLabel = new Label("Loading preview...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
        pagesContainer.getChildren().add(loadingLabel);

        scrollPane.setContent(pagesContainer);
        root.setCenter(scrollPane);

        // Render PDF in background to avoid UI freeze
        AppExecutor.submit(() -> {
            try {
                File pdfFile = new File(pdfFilePath);
                if (pdfFile.exists()) {
                    PDDocument document = Loader.loadPDF(pdfFile);
                    PDFRenderer renderer = new PDFRenderer(document);
                    int pageCount = document.getNumberOfPages();

                    javafx.scene.image.WritableImage[] fxImages = new javafx.scene.image.WritableImage[pageCount];
                    for (int i = 0; i < pageCount; i++) {
                        BufferedImage bufferedImage = renderer.renderImageWithDPI(i, 96); // Reduced DPI for faster rendering
                        fxImages[i] = SwingFXUtils.toFXImage(bufferedImage, null);
                    }
                    document.close();

                    // Update UI on FX thread
                    Platform.runLater(() -> {
                        pagesContainer.getChildren().clear();
                        for (int i = 0; i < pageCount; i++) {
                            ImageView imageView = new ImageView(fxImages[i]);
                            imageView.setPreserveRatio(true);
                            imageView.setFitWidth(560);
                            imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 2, 2);");

                            Label pageLabel = new Label("Page " + (i + 1) + " of " + pageCount);
                            pageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                            VBox pageBox = new VBox(4, imageView, pageLabel);
                            pageBox.setAlignment(Pos.CENTER);
                            pagesContainer.getChildren().add(pageBox);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        pagesContainer.getChildren().clear();
                        Label errorLabel = new Label("PDF file not found: " + pdfFilePath);
                        errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444;");
                        pagesContainer.getChildren().add(errorLabel);
                    });
                }
            } catch (Exception e) {
                log.error("Failed to render PDF preview", e);
                Platform.runLater(() -> {
                    pagesContainer.getChildren().clear();
                    Label errorLabel = new Label("Failed to load PDF preview: " + e.getMessage());
                    errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444;");
                    errorLabel.setWrapText(true);
                    pagesContainer.getChildren().add(errorLabel);
                });
            }
        });

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

    private void showPrintDialog(int numberOfCopies) {
        // Show file chooser to select output path
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File originalFile = new File(pdfFilePath);
        fileChooser.setInitialFileName(originalFile.getName());

        Window window = MainApp.getPrimaryStage();
        File selectedFile = fileChooser.showSaveDialog(window);

        if (selectedFile != null) {
            // Generate copies in background
            AppExecutor.submit(() -> {
                try {
                    if (generateCopiesFromSpinner && pdfGenerator != null) {
                        generateAndSaveCopies(selectedFile, numberOfCopies);
                        return;
                    }

                    // Copy original to selected location
                    java.nio.file.Files.copy(originalFile.toPath(), selectedFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    // Also copy any other files that were generated alongside (for LR 4-copy flow)
                    File originalDir = originalFile.getParentFile();
                    String originalName = originalFile.getName();
                    File selectedDir = selectedFile.getParentFile();

                    // Extract the base timestamp pattern (everything before the copy label)
                    // For LR: LR_123_20240611_103000_CONSIGNEE_COPY.pdf -> base = LR_123_20240611_103000
                    String tempPattern = originalName.replace(".pdf", "");
                    final String basePattern;
                    if (tempPattern.contains("_CONSIGNEE_COPY")) {
                        basePattern = tempPattern.substring(0, tempPattern.indexOf("_CONSIGNEE_COPY"));
                    } else if (tempPattern.contains("_CONSIGNOR_COPY")) {
                        basePattern = tempPattern.substring(0, tempPattern.indexOf("_CONSIGNOR_COPY"));
                    } else if (tempPattern.contains("_ACCOUNT_COPY")) {
                        basePattern = tempPattern.substring(0, tempPattern.indexOf("_ACCOUNT_COPY"));
                    } else if (tempPattern.contains("_DRIVER_COPY")) {
                        basePattern = tempPattern.substring(0, tempPattern.indexOf("_DRIVER_COPY"));
                    } else {
                        basePattern = tempPattern;
                    }

                    // Find all PDFs with the same base pattern and copy them with original names
                    File[] matchingFiles = originalDir.listFiles((dir, name) ->
                            name.startsWith(basePattern) && name.endsWith(".pdf"));

                    final int[] additionalCopies = {0};
                    if (matchingFiles != null) {
                        for (File sourceFile : matchingFiles) {
                            if (!sourceFile.equals(originalFile)) {
                                // Copy with original name (not renaming based on user selection)
                                File destFile = new File(selectedDir, sourceFile.getName());
                                java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                additionalCopies[0]++;
                            }
                        }
                    }

                    Platform.runLater(() -> {
                        String copyInfo = "Saved: " + selectedFile.getAbsolutePath();
                        if (additionalCopies[0] > 0) {
                            copyInfo += "\n" + additionalCopies[0] + " additional copy/copies saved in same directory.";
                        }
                        AlertUtil.showInfo("Success", "PDF saved successfully.\n" + copyInfo);
                    });
                } catch (Exception e) {
                    log.error("Failed to save PDF", e);
                    Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save PDF: " + e.getMessage()));
                }
            });
        }
    }

    private void generateAndSaveCopies(File selectedFile, int numberOfCopies) {
        int safeCopies = Math.max(1, numberOfCopies);
        String baseName = selectedFile.getName();
        String extension = ".pdf";
        if (baseName.toLowerCase().endsWith(".pdf")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        File parentDir = selectedFile.getParentFile();
        List<String> savedPaths = new ArrayList<>();

        for (int i = 1; i <= safeCopies; i++) {
            String copyLabel = getCopyLabel(i);
            String outputName = (i == 1)
                    ? selectedFile.getName()
                    : baseName + "_" + copyLabel + extension;
            File outputFile = new File(parentDir, outputName);
            pdfGenerator.accept(copyLabel, outputFile.getAbsolutePath());
            savedPaths.add(outputFile.getAbsolutePath());
        }

        Platform.runLater(() -> AlertUtil.showInfo(
                "Success",
                "Generated " + safeCopies + " copy/copies successfully.\n" + String.join("\n", savedPaths)
        ));
    }

    private String getCopyLabel(int copyIndex) {
        if (copyIndex == 1) return "Original";
        if (copyIndex == 2) return "Duplicate";
        if (copyIndex == 3) return "Triplicate";
        return copyIndex + "th Copy";
    }
}
