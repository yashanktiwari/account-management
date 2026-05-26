package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
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

public class PartyMasterListView {

    private final PartyDAO dao = new PartyDAO();
    private final ObservableList<Party> rows = FXCollections.observableArrayList();
    private TableView<Party> table;

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label heading = new Label("Party List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("Add New Party");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            PartyMasterDialog dialog = new PartyMasterDialog();
            dialog.show(MainApp.getPrimaryStage(), this::loadRows);
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        HBox actions = new HBox(10, addBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        table.getColumns().add(col("ID", "id", 80));
        table.getColumns().add(col("Name", "name", 220));
        table.getColumns().add(col("Type", "type", 120));
        table.getColumns().add(col("Mobile", "mobile", 140));
        table.getColumns().add(col("Email", "email", 220));
        table.getColumns().add(col("City", "city", 130));
        table.getColumns().add(col("GSTIN", "gstin", 170));
        table.getColumns().add(col("Created At", "createdAt", 180));
        table.getColumns().add(col("Updated At", "updatedAt", 180));

        table.setItems(rows);

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(heading, actions, table);

        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            try {
                List<Party> data = dao.getAll();
                Platform.runLater(() -> rows.setAll(data));
            } catch (Exception ignored) {
                Platform.runLater(rows::clear);
            }
        });
    }

    private TableColumn<Party, Object> col(String title, String property, double width) {
        TableColumn<Party, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }
}