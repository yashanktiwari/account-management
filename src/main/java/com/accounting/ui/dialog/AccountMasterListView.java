package com.accounting.ui.dialog;

import com.accounting.MainApp;
import com.accounting.dao.AccountDAO;
import com.accounting.model.Account;
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

public class AccountMasterListView {

    private final String accountType;
    private final AccountDAO dao = new AccountDAO();
    private final ObservableList<Account> rows = FXCollections.observableArrayList();
    private TableView<Account> table;

    public AccountMasterListView(String accountType) {
        this.accountType = accountType;
    }

    public Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        String title = "CUSTOMER".equals(accountType) ? "Customers" : "Suppliers";
        Label heading = new Label(title + " List");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Button addBtn = new Button("Add New " + ("CUSTOMER".equals(accountType) ? "Customer" : "Supplier"));
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            AccountEntryDialog dialog = new AccountEntryDialog(accountType);
            dialog.show(MainApp.getPrimaryStage(), this::loadRows);
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadRows());

        HBox actions = new HBox(10, addBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Account, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));

        TableColumn<Account, String> mobileCol = new TableColumn<>("Mobile");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));

        TableColumn<Account, String> cityCol = new TableColumn<>("City");
        cityCol.setCellValueFactory(new PropertyValueFactory<>("cityName"));

        TableColumn<Account, String> areaCol = new TableColumn<>("Area");
        areaCol.setCellValueFactory(new PropertyValueFactory<>("rootAreaName"));

        TableColumn<Account, String> balanceTypeCol = new TableColumn<>("Bal Type");
        balanceTypeCol.setCellValueFactory(new PropertyValueFactory<>("balanceType"));

        table.getColumns().addAll(nameCol, mobileCol, cityCol, areaCol, balanceTypeCol);
        table.setItems(rows);

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(heading, actions, table);

        loadRows();
        return root;
    }

    private void loadRows() {
        AppExecutor.submit(() -> {
            List<Account> data = dao.getAll(accountType);
            Platform.runLater(() -> rows.setAll(data));
        });
    }
}
