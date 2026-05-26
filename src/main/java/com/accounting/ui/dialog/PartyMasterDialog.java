package com.accounting.ui.dialog;

import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
import com.accounting.util.AlertUtil;
import com.accounting.util.AppExecutor;
import com.accounting.util.AppLogger;
import com.accounting.util.NotificationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PartyMasterDialog {

    private static final Logger log = AppLogger.get(PartyMasterDialog.class);
    private Stage stage;
    private Party currentParty;
    private ComboBox<String> typeCombo;
    private TextField nameField;
    private TextField mailingNameField;
    private TextArea addressField;
    private ComboBox<String> cityCombo;
    private ComboBox<String> stateCombo;
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

    private static final List<String> STATES = Arrays.asList(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa",
            "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
            "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
            "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana",
            "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
            "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
            "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    );

    private static final Map<String, List<String>> CITIES_BY_STATE = new HashMap<>();

    static {
        CITIES_BY_STATE.put("Andhra Pradesh", Arrays.asList("Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool"));
        CITIES_BY_STATE.put("Assam", Arrays.asList("Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Tezpur"));
        CITIES_BY_STATE.put("Bihar", Arrays.asList("Patna", "Gaya", "Muzaffarpur", "Bhagalpur", "Darbhanga"));
        CITIES_BY_STATE.put("Chhattisgarh", Arrays.asList("Raipur", "Bhilai", "Bilaspur", "Korba", "Durg"));
        CITIES_BY_STATE.put("Delhi", Arrays.asList("New Delhi", "North Delhi", "South Delhi", "East Delhi", "West Delhi"));
        CITIES_BY_STATE.put("Gujarat", Arrays.asList("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar"));
        CITIES_BY_STATE.put("Haryana", Arrays.asList("Gurugram", "Faridabad", "Panipat", "Ambala", "Hisar"));
        CITIES_BY_STATE.put("Jharkhand", Arrays.asList("Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Hazaribagh"));
        CITIES_BY_STATE.put("Karnataka", Arrays.asList("Bengaluru", "Mysuru", "Hubballi", "Mangaluru", "Belagavi"));
        CITIES_BY_STATE.put("Kerala", Arrays.asList("Kochi", "Thiruvananthapuram", "Kozhikode", "Thrissur", "Kollam"));
        CITIES_BY_STATE.put("Madhya Pradesh", Arrays.asList("Indore", "Bhopal", "Jabalpur", "Gwalior", "Ujjain"));
        CITIES_BY_STATE.put("Maharashtra", Arrays.asList("Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad"));
        CITIES_BY_STATE.put("Odisha", Arrays.asList("Bhubaneswar", "Cuttack", "Rourkela", "Sambalpur", "Puri"));
        CITIES_BY_STATE.put("Punjab", Arrays.asList("Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda"));
        CITIES_BY_STATE.put("Rajasthan", Arrays.asList("Jaipur", "Jodhpur", "Kota", "Udaipur", "Ajmer"));
        CITIES_BY_STATE.put("Tamil Nadu", Arrays.asList("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem"));
        CITIES_BY_STATE.put("Telangana", Arrays.asList("Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam"));
        CITIES_BY_STATE.put("Uttar Pradesh", Arrays.asList("Lucknow", "Kanpur", "Noida", "Varanasi", "Agra"));
        CITIES_BY_STATE.put("West Bengal", Arrays.asList("Kolkata", "Howrah", "Durgapur", "Siliguri", "Asansol"));
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle("Party Master");
        stage.initModality(Modality.NONE);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(680);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Scene scene = new Scene(createContent(), 940, 720);
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

        VBox formSection = createFormSection();
        HBox buttonsBox = createButtonsBox();

        root.getChildren().addAll(title, formSection, buttonsBox);
        return root;
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

        addressField = new TextArea();
        addressField.setPrefRowCount(3);
        addressField.setWrapText(true);
        grid.add(label("Address"), 2, 2);
        grid.add(addressField, 3, 2);

        stateCombo = new ComboBox<>(FXCollections.observableArrayList(STATES));
        stateCombo.setPromptText("Select state");
        stateCombo.setOnAction(e -> updateCitiesForState());
        grid.add(label("State"), 0, 3);
        grid.add(stateCombo, 1, 3);

        cityCombo = new ComboBox<>();
        cityCombo.setPromptText("Select or type city");
        cityCombo.setEditable(true);
        cityCombo.setDisable(true);
        grid.add(label("City"), 2, 3);
        grid.add(cityCombo, 3, 3);

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

    private void updateCitiesForState() {
        String selectedState = stateCombo.getValue();
        cityCombo.getItems().clear();

        if (selectedState == null || selectedState.isBlank()) {
            cityCombo.setDisable(true);
            cityCombo.getEditor().clear();
            return;
        }

        cityCombo.setDisable(false);
        List<String> cities = CITIES_BY_STATE.get(selectedState);
        if (cities != null) {
            cityCombo.getItems().addAll(cities);
        }
        cityCombo.getSelectionModel().clearSelection();
        cityCombo.getEditor().clear();
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

    private void clearForm() {
        currentParty = null;
        typeCombo.setValue("CUSTOMER");
        nameField.clear();
        mailingNameField.clear();
        mobileField.clear();
        emailField.clear();
        addressField.clear();
        stateCombo.getSelectionModel().clearSelection();
        cityCombo.getItems().clear();
        cityCombo.getSelectionModel().clearSelection();
        cityCombo.getEditor().clear();
        cityCombo.setDisable(true);
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
        String selectedCity = cityCombo.getValue();
        String typedCity = cityCombo.getEditor().getText();
        party.setCity((selectedCity != null && !selectedCity.isBlank()) ? selectedCity : typedCity);
        party.setState(stateCombo.getValue());
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
            AlertUtil.showWarning("Validation", "Delete is available while editing an existing party");
            return;
        }

        if (AlertUtil.showConfirmation("Confirm", "Are you sure you want to delete this party?")) {
            AppExecutor.submit(() -> {
                try {
                    new PartyDAO().delete(currentParty.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Success", "Party deleted successfully");
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
