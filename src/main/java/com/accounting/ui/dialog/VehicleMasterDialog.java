package com.accounting.ui.dialog;

import com.accounting.dao.VehicleDAO;
import com.accounting.model.Vehicle;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class VehicleMasterDialog {

    private final VehicleDAO dao = new VehicleDAO();
    private Vehicle currentVehicle;

    private TextField vehicleNoField;
    private TextField vehicleModelField;
    private TextField accountNameField;
    private TextField searchField;
    private TableView<Vehicle> table;
    private ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();

    private Button saveBtn;
    private Stage stage;

    public void show(Window owner) {
        show(owner, null);
    }

    public void show(Window owner, Runnable onClose) {
        stage = new Stage();
        stage.setTitle("Vehicle Master");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Scene scene = new Scene(createContent(), 450, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    public Parent createContent() {
        VBox root = new VBox(0);

        // Form
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(16));

        vehicleNoField = new TextField();
        vehicleNoField.setPrefWidth(250);
        vehicleNoField.setStyle("-fx-background-color: #e0f2fe;");

        vehicleModelField = new TextField();
        vehicleModelField.setPrefWidth(250);

        accountNameField = new TextField();
        accountNameField.setPrefWidth(250);

        form.add(label("Vehicle No.:"), 0, 0);
        form.add(vehicleNoField, 1, 0);
        form.add(label("Vehicle Model :"), 0, 1);
        form.add(vehicleModelField, 1, 1);
        form.add(label("Account Name :"), 0, 2);
        form.add(accountNameField, 1, 2);

        // Action buttons
        Button newBtn = new Button("New");
        newBtn.setPrefWidth(70);
        newBtn.setOnAction(e -> clearForm());

        saveBtn = new Button("Save");
        saveBtn.setPrefWidth(70);
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> saveVehicle());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setPrefWidth(70);
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> deleteVehicle());

        Button excelBtn = new Button("Excel");
        excelBtn.setPrefWidth(70);
        excelBtn.setOnAction(e -> exportToExcel());

        HBox actionBar = new HBox(10, newBtn, saveBtn, deleteBtn, excelBtn);
        actionBar.setAlignment(Pos.CENTER);
        actionBar.setPadding(new Insets(10, 0, 10, 0));

        // Search
        searchField = new TextField();
        searchField.setPrefWidth(200);
        searchField.setPromptText("Search...");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> searchVehicles());

        searchField.setOnAction(e -> searchVehicles());

        HBox searchBar = new HBox(10, new Label("Search"), searchField, searchBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 16, 10, 16));

        // Table
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Vehicle, String> colNo = new TableColumn<>("Vehicle No.");
        colNo.setCellValueFactory(new PropertyValueFactory<>("vehicleNo"));

        TableColumn<Vehicle, String> colModel = new TableColumn<>("Vehicle Model");
        colModel.setCellValueFactory(new PropertyValueFactory<>("vehicleModel"));

        TableColumn<Vehicle, String> colAccount = new TableColumn<>("Account Name");
        colAccount.setCellValueFactory(new PropertyValueFactory<>("accountName"));

        table.getColumns().addAll(colNo, colModel, colAccount);
        table.setItems(vehicleList);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) loadVehicle(selected);
        });

        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(form, actionBar, searchBar, table);

        loadAllVehicles();
        return root;
    }

    private void loadAllVehicles() {
        AppExecutor.submit(() -> {
            List<Vehicle> all = dao.getAll();
            Platform.runLater(() -> vehicleList.setAll(all));
        });
    }

    private void searchVehicles() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isBlank()) {
            loadAllVehicles();
            return;
        }
        AppExecutor.submit(() -> {
            List<Vehicle> results = dao.search(keyword);
            Platform.runLater(() -> vehicleList.setAll(results));
        });
    }

    private void saveVehicle() {
        String vNo = vehicleNoField.getText();
        if (vNo == null || vNo.isBlank()) {
            AlertUtil.showWarning("Validation", "Vehicle No. is required.");
            return;
        }

        Vehicle v = currentVehicle != null ? currentVehicle : new Vehicle();
        v.setVehicleNo(vNo.trim().toUpperCase());
        v.setVehicleModel(vehicleModelField.getText());
        v.setAccountName(accountNameField.getText());

        AppExecutor.submit(() -> {
            boolean success;
            if (v.getId() > 0) {
                success = dao.update(v);
            } else {
                int id = dao.save(v);
                success = id > 0;
                if (success) v.setId(id);
            }

            Platform.runLater(() -> {
                if (success) {
                    currentVehicle = v;
                    saveBtn.setText("Update");
                    NotificationUtil.showSuccess("Saved", v.getVehicleNo() + " saved.");
                    loadAllVehicles();
                } else {
                    AlertUtil.showError("Save Failed", "Could not save vehicle. Check for duplicate vehicle no.");
                }
            });
        });
    }

    private void deleteVehicle() {
        if (currentVehicle == null || currentVehicle.getId() <= 0) {
            AlertUtil.showWarning("Delete", "No vehicle selected.");
            return;
        }
        boolean confirm = AlertUtil.showConfirmation("Delete Vehicle",
                "Delete vehicle " + currentVehicle.getVehicleNo() + "?");
        if (!confirm) return;

        AppExecutor.submit(() -> {
            boolean success = dao.delete(currentVehicle.getId());
            Platform.runLater(() -> {
                if (success) {
                    NotificationUtil.showSuccess("Deleted", currentVehicle.getVehicleNo() + " deleted.");
                    clearForm();
                    loadAllVehicles();
                } else {
                    AlertUtil.showError("Delete Failed", "Could not delete vehicle.");
                }
            });
        });
    }

    private void exportToExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Vehicles to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("vehicles.xlsx");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        AppExecutor.submit(() -> {
            try {
                ExportUtil.exportVehiclesToExcel(vehicleList, file.getAbsolutePath());
                Platform.runLater(() ->
                        NotificationUtil.showSuccess("Export", "Vehicles exported to Excel."));
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Export Failed", e.getMessage()));
            }
        });
    }

    private void loadVehicle(Vehicle v) {
        currentVehicle = v;
        vehicleNoField.setText(v.getVehicleNo());
        vehicleModelField.setText(v.getVehicleModel());
        accountNameField.setText(v.getAccountName());
        saveBtn.setText("Update");
    }

    private void clearForm() {
        currentVehicle = null;
        vehicleNoField.clear();
        vehicleModelField.clear();
        accountNameField.clear();
        saveBtn.setText("Save");
        vehicleNoField.requestFocus();
        table.getSelectionModel().clearSelection();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }
}
