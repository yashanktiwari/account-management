package com.accounting.ui.dialog;

import com.accounting.dao.AccountDAO;
import com.accounting.model.BillDetail;
import com.accounting.model.OutstandingEntry;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.ExportUtil;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class OutstandingRegisterDialog {

    private final AccountDAO dao = new AccountDAO();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private ComboBox<String> acTypeCombo;
    private ComboBox<String> rootCityCombo;
    private DatePicker fromDatePicker;
    private ComboBox<String> refTypeCombo;
    private ComboBox<String> natureOfPaymentCombo;
    private TextField dueDaysField;
    private DatePicker toDatePicker;

    private TreeTableView<OutstandingRow> treeTable;
    private Label totalRowsLabel;
    private TextField totalOutstandingField;
    private Label drCrLabel;

    private Stage stage;

    public void show(Window owner) {
        stage = new Stage();
        stage.setTitle("Outstanding Register");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);

        Scene scene = new Scene(createContent(), 1000, 650);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public Parent createContent() {
        VBox root = new VBox(0);

        // Filters
        GridPane filters = new GridPane();
        filters.setHgap(10);
        filters.setVgap(8);
        filters.setPadding(new Insets(12));

        acTypeCombo = new ComboBox<>(FXCollections.observableArrayList("RECEIVABLE", "PAYABLE"));
        acTypeCombo.setValue("RECEIVABLE");
        acTypeCombo.setPrefWidth(120);

        rootCityCombo = new ComboBox<>();
        rootCityCombo.setEditable(true);
        rootCityCombo.setPrefWidth(160);

        fromDatePicker = new DatePicker(LocalDate.of(LocalDate.now().getYear(), 1, 1));
        fromDatePicker.setPrefWidth(130);

        refTypeCombo = new ComboBox<>(FXCollections.observableArrayList("Against Reference", "On Account", "All"));
        refTypeCombo.setValue("Against Reference");
        refTypeCombo.setPrefWidth(140);

        natureOfPaymentCombo = new ComboBox<>(FXCollections.observableArrayList(
                "", "Cash", "Cheque", "NEFT", "RTGS", "Online"));
        natureOfPaymentCombo.setPrefWidth(120);

        dueDaysField = new TextField();
        dueDaysField.setPrefWidth(50);

        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(130);

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> searchOutstanding());

        Button excelBtn = new Button("Excel Export");
        excelBtn.setOnAction(e -> exportExcel());

        // Row 0
        filters.add(new Label("A/c Type"), 0, 0);
        filters.add(acTypeCombo, 1, 0);
        filters.add(new Label("Root/City Name"), 2, 0);
        filters.add(rootCityCombo, 3, 0);
        filters.add(fromDatePicker, 4, 0);

        // Row 1
        filters.add(new Label("Ref. Type"), 0, 1);
        filters.add(refTypeCombo, 1, 1);
        filters.add(new Label("Nature of Payment"), 2, 1);
        filters.add(natureOfPaymentCombo, 3, 1);
        filters.add(new Label("Due Days"), 4, 1);
        filters.add(dueDaysField, 5, 1);
        filters.add(toDatePicker, 6, 1);

        HBox btnBox = new HBox(10, searchBtn, excelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        filters.add(btnBox, 7, 0, 1, 2);

        // TreeTable
        treeTable = buildTreeTable();
        VBox.setVgrow(treeTable, Priority.ALWAYS);

        // Footer
        totalRowsLabel = new Label("Total Rows: 0");
        totalOutstandingField = new TextField("0");
        totalOutstandingField.setEditable(false);
        totalOutstandingField.setPrefWidth(120);
        drCrLabel = new Label("Dr");
        drCrLabel.setStyle("-fx-font-weight: bold;");

        HBox footer = new HBox(30, totalRowsLabel,
                new HBox(5, new Label("Total Outstanding :"), totalOutstandingField, drCrLabel));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.getStyleClass().add("footer-bar");

        root.getChildren().addAll(filters, treeTable, footer);

        // Load root areas
        AppExecutor.submit(() -> {
            List<String> areas = dao.getAllRootAreas();
            Platform.runLater(() -> rootCityCombo.setItems(FXCollections.observableArrayList(areas)));
        });

        return root;
    }

    @SuppressWarnings("unchecked")
    private TreeTableView<OutstandingRow> buildTreeTable() {
        TreeTableView<OutstandingRow> ttv = new TreeTableView<>();
        ttv.setShowRoot(false);
        ttv.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TreeTableColumn<OutstandingRow, Boolean> selCol = new TreeTableColumn<>("Sel");
        selCol.setPrefWidth(35);
        selCol.setCellFactory(col -> new TreeTableCell<>() {
            private final CheckBox cb = new CheckBox();
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : cb);
            }
        });

        TreeTableColumn<OutstandingRow, String> partCol = new TreeTableColumn<>("Particulars");
        partCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(p.getValue().getValue().getParticulars()));
        partCol.setPrefWidth(350);

        TreeTableColumn<OutstandingRow, String> rootCol = new TreeTableColumn<>("Root/Area");
        rootCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(p.getValue().getValue().getRootArea()));
        rootCol.setPrefWidth(120);

        TreeTableColumn<OutstandingRow, String> mobileCol = new TreeTableColumn<>("Mobile");
        mobileCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(p.getValue().getValue().getMobile()));
        mobileCol.setPrefWidth(110);

        TreeTableColumn<OutstandingRow, String> amountCol = new TreeTableColumn<>("Amount");
        amountCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(p.getValue().getValue().getAmount()));
        amountCol.setPrefWidth(120);
        amountCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TreeTableColumn<OutstandingRow, String> drCrCol = new TreeTableColumn<>("Dr/Cr");
        drCrCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(p.getValue().getValue().getDrCr()));
        drCrCol.setPrefWidth(50);

        ttv.getColumns().addAll(selCol, partCol, rootCol, mobileCol, amountCol, drCrCol);

        return ttv;
    }

    private void searchOutstanding() {
        String acType = acTypeCombo.getValue();
        String rootCity = rootCityCombo.getValue();
        String nature = natureOfPaymentCombo.getValue();
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        int dueDays = 0;
        try {
            if (dueDaysField.getText() != null && !dueDaysField.getText().isBlank())
                dueDays = Integer.parseInt(dueDaysField.getText());
        } catch (NumberFormatException ignored) {}

        if (from == null || to == null) {
            AlertUtil.showWarning("Validation", "Please select both dates.");
            return;
        }

        final int dd = dueDays;
        AppExecutor.submit(() -> {
            List<OutstandingEntry> entries = dao.getOutstanding(acType, rootCity, nature, dd, from, to);
            Platform.runLater(() -> populateTreeTable(entries));
        });
    }

    private void populateTreeTable(List<OutstandingEntry> entries) {
        TreeItem<OutstandingRow> rootItem = new TreeItem<>(new OutstandingRow("", "", "", "", ""));

        double totalOutstanding = 0;
        int totalRows = 0;

        for (OutstandingEntry entry : entries) {
            OutstandingRow parentRow = new OutstandingRow(
                    entry.getAccountName(),
                    safe(entry.getRootArea()),
                    safe(entry.getMobile()),
                    String.format("%.2f", entry.getAmount()),
                    safe(entry.getDrCr())
            );

            TreeItem<OutstandingRow> parentItem = new TreeItem<>(parentRow);
            parentItem.setExpanded(true);

            for (BillDetail bd : entry.getBillDetails()) {
                String detail = String.format("    Bill No.: %s  |  Date: %s  |  Due Days: %d",
                        safe(bd.getBillNo()),
                        bd.getDate() != null ? bd.getDate().format(dateFmt) : "",
                        bd.getDueDays());

                OutstandingRow childRow = new OutstandingRow(
                        detail, "",
                        String.format("Bill Amt: %.0f", bd.getBillAmt()),
                        String.format("PndgAmt: %.0f", bd.getPendingAmt()),
                        ""
                );

                parentItem.getChildren().add(new TreeItem<>(childRow));
            }

            rootItem.getChildren().add(parentItem);
            totalOutstanding += entry.getAmount();
            totalRows++;
        }

        treeTable.setRoot(rootItem);
        totalRowsLabel.setText("Total Rows: " + totalRows);
        totalOutstandingField.setText(String.format("%.0f", totalOutstanding));
        drCrLabel.setText(totalOutstanding >= 0 ? "Dr" : "Cr");
    }

    private void exportExcel() {
        if (treeTable.getRoot() == null || treeTable.getRoot().getChildren().isEmpty()) {
            AlertUtil.showWarning("Export", "No data to export.");
            return;
        }

        // Rebuild the entry list from the current search for export
        String acType = acTypeCombo.getValue();
        String rootCity = rootCityCombo.getValue();
        String nature = natureOfPaymentCombo.getValue();
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        int dueDays = 0;
        try {
            if (dueDaysField.getText() != null && !dueDaysField.getText().isBlank())
                dueDays = Integer.parseInt(dueDaysField.getText());
        } catch (NumberFormatException ignored) {}

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Outstanding to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("outstanding_register.xlsx");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        final int dd = dueDays;
        AppExecutor.submit(() -> {
            try {
                List<OutstandingEntry> entries = dao.getOutstanding(acType, rootCity, nature, dd, from, to);
                ExportUtil.exportOutstandingToExcel(entries, file.getAbsolutePath());
                Platform.runLater(() ->
                        NotificationUtil.showSuccess("Export", "Outstanding register exported."));
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Export Failed", e.getMessage()));
            }
        });
    }

    private String safe(String s) { return s == null ? "" : s; }

    // Row model for TreeTableView
    public static class OutstandingRow {
        private final String particulars;
        private final String rootArea;
        private final String mobile;
        private final String amount;
        private final String drCr;

        public OutstandingRow(String particulars, String rootArea, String mobile, String amount, String drCr) {
            this.particulars = particulars;
            this.rootArea = rootArea;
            this.mobile = mobile;
            this.amount = amount;
            this.drCr = drCr;
        }

        public String getParticulars() { return particulars; }
        public String getRootArea() { return rootArea; }
        public String getMobile() { return mobile; }
        public String getAmount() { return amount; }
        public String getDrCr() { return drCr; }
    }
}
