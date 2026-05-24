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
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        table.getColumns().add(col("ID", "id", 70));
        table.getColumns().add(col("Account Name", "accountName", 220));
        table.getColumns().add(col("Account Type", "accountType", 120));
        table.getColumns().add(col("A/c As", "acAs", 100));
        table.getColumns().add(col("A/c Type", "acType", 180));
        table.getColumns().add(col("Mailing Name", "mailingName", 180));
        table.getColumns().add(col("Address", "address", 260));
        table.getColumns().add(col("State", "stateName", 120));
        table.getColumns().add(col("State Code", "stateCode", 90));
        table.getColumns().add(col("City", "cityName", 120));
        table.getColumns().add(col("Fax", "fax", 110));
        table.getColumns().add(col("Pin Code", "pinCode", 100));
        table.getColumns().add(col("Email", "email", 200));
        table.getColumns().add(col("Mobile", "mobile", 130));
        table.getColumns().add(col("Root Area", "rootAreaName", 140));
        table.getColumns().add(col("GSTIN", "gstin", 150));
        table.getColumns().add(col("CST No", "cstNo", 130));
        table.getColumns().add(col("TAN No", "tanNo", 130));
        table.getColumns().add(col("PAN No", "panNo", 130));
        table.getColumns().add(col("TDS %", "tdsPercent", 90));
        table.getColumns().add(col("TDS Applicable", "tdsApplicable", 120));
        table.getColumns().add(col("Aadhar No", "aadharNo", 150));
        table.getColumns().add(col("Drugs Lic No", "drugsLicNo", 150));
        table.getColumns().add(col("Credit Period", "creditPeriod", 110));
        table.getColumns().add(col("Credit Limit", "creditAmtLimit", 120));
        table.getColumns().add(col("Opening Balance", "openingBalance", 130));
        table.getColumns().add(col("Balance Type", "balanceType", 110));
        table.getColumns().add(col("Nature of Payment", "natureOfPayment", 140));
        table.getColumns().add(col("Created At", "createdAt", 170));
        table.getColumns().add(col("Updated At", "updatedAt", 170));

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

    private TableColumn<Account, Object> col(String title, String property, double width) {
        TableColumn<Account, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }
}
