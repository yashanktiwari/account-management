package com.accounting.ui.dialog;

import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public class PartyMasterDialog {

    private static final Logger log = AppLogger.get(PartyMasterDialog.class);
    private Stage stage;
    private Party currentParty;
    private ObservableList<Party> partiesList = FXCollections.observableArrayList();
    private TableView<Party> partiesTable;
    private ComboBox<String> typeCombo;
    private TextField nameField;
    private TextField mailingNameField;
    private TextField addressField;
    private TextField cityField;
    private TextField stateField;
    private TextField pincodeField;
    private TextField mobileField;
    private TextField emailField;
    private TextField panField;
    private TextField gstinField;
    private TextField creditLimitField;
    private TextField openingBalanceField;
    private ComboBox<String> balanceTypeCombo;
    private TextField natureOfPaymentField;
    private TextField bankNameField;
    private TextField bankAccountField;
    private TextField ifscCodeField;
    private TextField remarksField;
    private Runnable onClose;

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Party Master");
        stage.initModality(Modality.NONE);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Scene scene = new Scene(createContent(), 1000, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();
    }

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label title = new Label("PARTY MASTER");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        HBox searchBar = createSearchBar();
        VBox tableSection = createTableSection();
        VBox formSection = createFormSection();
        HBox buttonsBox = createButtonsBox();

        root.getChildren().addAll(title, searchBar, tableSection, formSection, buttonsBox);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        loadParties();
        return root;
    }

    private HBox createSearchBar() {
        typeCombo = new ComboBox<>(FXCollections.observableArrayList("ALL", "CUSTOMER", "SUPPLIER"));
        typeCombo.setValue("ALL");
        typeCombo.setPrefWidth(120);
        typeCombo.setOnAction(e -> filterParties());

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name, mobile, email...");
        searchField.setOnKeyReleased(e -> filterParties());

        HBox box = new HBox(10, new Label("Type:"), typeCombo, new Label("Search:"), searchField);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return box;
    }

    private VBox createTableSection() {
        partiesTable = new TableView<>(partiesList);
        partiesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Party, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);

        TableColumn<Party, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);

        TableColumn<Party, String> mobileCol = new TableColumn<>("Mobile");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        mobileCol.setPrefWidth(120);

        TableColumn<Party, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(150);

        TableColumn<Party, String> cityCol = new TableColumn<>("City");
        cityCol.setCellValueFactory(new PropertyValueFactory<>("city"));
        cityCol.setPrefWidth(100);

        TableColumn<Party, String> gstinCol = new TableColumn<>("GSTIN");
        gstinCol.setCellValueFactory(new PropertyValueFactory<>("gstin"));
        gstinCol.setPrefWidth(120);

        partiesTable.getColumns().addAll(nameCol, typeCol, mobileCol, emailCol, cityCol, gstinCol);
        partiesTable.setOnMouseClicked(e -> {
            if (partiesTable.getSelectionModel().getSelectedItem() != null) {
                loadPartyDetails(partiesTable.getSelectionModel().getSelectedItem());
            }
        });

        VBox section = new VBox(8, new Label("Parties List"), partiesTable);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(partiesTable, Priority.ALWAYS);
        return section;
    }

    private VBox createFormSection() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));

        typeCombo = new ComboBox<>(FXCollections.observableArrayList("CUSTOMER", "SUPPLIER"));
        typeCombo.setValue("CUSTOMER");
        grid.add(label("Type"), 0, 0);
        grid.add(typeCombo, 1, 0);

        nameField = new TextField();
        grid.add(label("Name"), 2, 0);
        grid.add(nameField, 3, 0);

        mailingNameField = new TextField();
        grid.add(label("Mailing Name"), 0, 1);
        grid.add(mailingNameField, 1, 1);

        mobileField = new TextField();
        grid.add(label("Mobile"), 2, 1);
        grid.add(mobileField, 3, 1);

        emailField = new TextField();
        grid.add(label("Email"), 0, 2);
        grid.add(emailField, 1, 2);

        addressField = new TextField();
        grid.add(label("Address"), 2, 2);
        grid.add(addressField, 3, 2);

        cityField = new TextField();
        grid.add(label("City"), 0, 3);
        grid.add(cityField, 1, 3);

        stateField = new TextField();
        grid.add(label("State"), 2, 3);
        grid.add(stateField, 3, 3);

        pincodeField = new TextField();
        grid.add(label("Pincode"), 0, 4);
        grid.add(pincodeField, 1, 4);

        panField = new TextField();
        grid.add(label("PAN"), 2, 4);
        grid.add(panField, 3, 4);

        gstinField = new TextField();
        grid.add(label("GSTIN"), 0, 5);
        grid.add(gstinField, 1, 5);

        creditLimitField = new TextField();
        grid.add(label("Credit Limit"), 2, 5);
        grid.add(creditLimitField, 3, 5);

        openingBalanceField = new TextField();
        grid.add(label("Opening Balance"), 0, 6);
        grid.add(openingBalanceField, 1, 6);

        balanceTypeCombo = new ComboBox<>(FXCollections.observableArrayList("DEBIT", "CREDIT"));
        balanceTypeCombo.setValue("DEBIT");
        grid.add(label("Balance Type"), 2, 6);
        grid.add(balanceTypeCombo, 3, 6);

        natureOfPaymentField = new TextField();
        grid.add(label("Nature of Payment"), 0, 7);
        grid.add(natureOfPaymentField, 1, 7);

        bankNameField = new TextField();
        grid.add(label("Bank Name"), 2, 7);
        grid.add(bankNameField, 3, 7);

        bankAccountField = new TextField();
        grid.add(label("Bank Account"), 0, 8);
        grid.add(bankAccountField, 1, 8);

        ifscCodeField = new TextField();
        grid.add(label("IFSC Code"), 2, 8);
        grid.add(ifscCodeField, 3, 8);

        remarksField = new TextField();
        grid.add(label("Remarks"), 0, 9);
        grid.add(remarksField, 1, 9);

        VBox section = new VBox(8, new Label("Party Details"), grid);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return section;
    }

    private HBox createButtonsBox() {
        Button newBtn = new Button("New");
        newBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #2563eb; -fx-text-fill: white;");
        newBtn.setOnAction(e -> clearForm());

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveParty());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px; -fx-background-color: #dc2626; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteParty());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox box = new HBox(10, newBtn, saveBtn, deleteBtn, closeBtn);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return box;
    }

    private void loadParties() {
        AppExecutor.submit(() -> {
            try {
                List<Party> parties = new PartyDAO().getAll();
                Platform.runLater(() -> partiesList.setAll(parties));
            } catch (Exception e) {
                log.error("Failed to load parties", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load parties"));
            }
        });
    }

    private void filterParties() {
        // TODO: Implement filtering by type and search
    }

    private void loadPartyDetails(Party party) {
        currentParty = party;
        typeCombo.setValue(party.getType());
        nameField.setText(party.getName());
        mailingNameField.setText(party.getMailingName());
        mobileField.setText(party.getMobile());
        emailField.setText(party.getEmail());
        addressField.setText(party.getAddress());
        cityField.setText(party.getCity());
        stateField.setText(party.getState());
        pincodeField.setText(party.getPincode());
        panField.setText(party.getPan());
        gstinField.setText(party.getGstin());
        creditLimitField.setText(party.getCreditLimit());
        openingBalanceField.setText(party.getOpeningBalance());
        balanceTypeCombo.setValue(party.getBalanceType());
        natureOfPaymentField.setText(party.getNatureOfPayment());
        bankNameField.setText(party.getBankName());
        bankAccountField.setText(party.getBankAccount());
        ifscCodeField.setText(party.getIfscCode());
        remarksField.setText(party.getRemarks());
    }

    private void clearForm() {
        currentParty = null;
        typeCombo.setValue("CUSTOMER");
        nameField.clear();
        mailingNameField.clear();
        mobileField.clear();
        emailField.clear();
        addressField.clear();
        cityField.clear();
        stateField.clear();
        pincodeField.clear();
        panField.clear();
        gstinField.clear();
        creditLimitField.clear();
        openingBalanceField.clear();
        balanceTypeCombo.setValue("DEBIT");
        natureOfPaymentField.clear();
        bankNameField.clear();
        bankAccountField.clear();
        ifscCodeField.clear();
        remarksField.clear();
    }

    private void saveParty() {
        if (nameField.getText().isEmpty()) {
            AlertUtil.showWarning("Validation", "Please enter party name");
            return;
        }

        Party party = currentParty != null ? currentParty : new Party();
        party.setType(typeCombo.getValue());
        party.setName(nameField.getText());
        party.setMailingName(mailingNameField.getText());
        party.setMobile(mobileField.getText());
        party.setEmail(emailField.getText());
        party.setAddress(addressField.getText());
        party.setCity(cityField.getText());
        party.setState(stateField.getText());
        party.setPincode(pincodeField.getText());
        party.setPan(panField.getText());
        party.setGstin(gstinField.getText());
        party.setCreditLimit(creditLimitField.getText());
        party.setOpeningBalance(openingBalanceField.getText());
        party.setBalanceType(balanceTypeCombo.getValue());
        party.setNatureOfPayment(natureOfPaymentField.getText());
        party.setBankName(bankNameField.getText());
        party.setBankAccount(bankAccountField.getText());
        party.setIfscCode(ifscCodeField.getText());
        party.setRemarks(remarksField.getText());

        AppExecutor.submit(() -> {
            try {
                PartyDAO dao = new PartyDAO();
                if (currentParty != null) {
                    dao.update(party);
                } else {
                    dao.save(party);
                }
                Platform.runLater(() -> {
                    NotificationUtil.showSuccess("Success", "Party saved successfully");
                    loadParties();
                    clearForm();
                });
            } catch (Exception e) {
                log.error("Failed to save party", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save party: " + e.getMessage()));
            }
        });
    }

    private void deleteParty() {
        if (currentParty == null) {
            AlertUtil.showWarning("Validation", "Please select a party to delete");
            return;
        }

        if (AlertUtil.showConfirmation("Confirm", "Are you sure you want to delete this party?")) {
            AppExecutor.submit(() -> {
                try {
                    new PartyDAO().delete(currentParty.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Success", "Party deleted successfully");
                        loadParties();
                        clearForm();
                    });
                } catch (Exception e) {
                    log.error("Failed to delete party", e);
                    Platform.runLater(() -> AlertUtil.showError("Error", "Failed to delete party: " + e.getMessage()));
                }
            });
        }
    }

    private Label label(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        return lbl;
    }
}
