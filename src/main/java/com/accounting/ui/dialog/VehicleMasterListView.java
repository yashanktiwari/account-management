package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.VehicleDAO;
import com.accounting.model.Vehicle;
import com.accounting.util.AppExecutor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class VehicleMasterListView {

    private final VehicleDAO dao = new VehicleDAO();
    private final ObservableList<Vehicle> rows = FXCollections.observableArrayList();
    private TableView<Vehicle> table;

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Vehicle List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("Add New Vehicle");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            VehicleMasterDialog dialog = new VehicleMasterDialog();
            dialog.show(MainApp.getPrimaryStage(), this::loadRows);
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        HBox actions = new HBox(10, addBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Vehicle, String> noCol = new TableColumn<>("Vehicle No");
        noCol.setCellValueFactory(new PropertyValueFactory<>("vehicleNo"));

        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("vehicleModel"));

        TableColumn<Vehicle, String> accountCol = new TableColumn<>("Account Name");
        accountCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));

        table.getColumns().addAll(noCol, modelCol, accountCol);
        table.setItems(rows);

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(heading, actions, table);

        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            List<Vehicle> data = dao.getAll();
            Platform.runLater(() -> rows.setAll(data));
        });
    }
}
