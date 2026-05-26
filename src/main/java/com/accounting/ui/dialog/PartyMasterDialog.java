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

    // Form fields
    private TextField companyNameField;
    private TextField ownerNameField;
    private TextArea addressField;
    private ComboBox<String> stateCombo;
    private ComboBox<String> cityCombo;
    private TextField pinCodeField;
    private TextField mobileField;
    private TextField emailField;
    private TextField gstField;
    private TextField panField;
    private TextField cstNoField;
    private TextField tanNoField;
    private TextField tdsField;
    private TextField aadharNoField;

    // Routes
    private final ObservableList<String> routesList = FXCollections.observableArrayList();
    private TextField routeInputField;

    private Runnable onClose;

    // ── State / City data ────────────────────────────────────────────────────
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

    // ── Public API ───────────────────────────────────────────────────────────

    /** Open dialog for adding a new party */
    public void show(Window owner, Runnable onClose) {
        show(owner, null, onClose);
    }

    /** Open dialog pre-filled for editing an existing party */
    public void show(Window owner, Party partyToEdit, Runnable onClose) {
        this.onClose = onClose;
        stage = new Stage();
        stage.setTitle(partyToEdit == null ? "Add Party" : "Edit Party");
        stage.initModality(Modality.NONE);
        stage.setResizable(true);
        stage.setMinWidth(880);
        stage.setMinHeight(680);
        if (owner != null) stage.initOwner(owner);
        if (onClose != null) {
            stage.setOnHidden(e -> onClose.run());
        }

        Scene scene = new Scene(createContent(), 920, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        if (partyToEdit != null) {
            loadPartyIntoForm(partyToEdit);
        }
    }

    // ── UI Build ─────────────────────────────────────────────────────────────

    public Parent createContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label title = new Label("PARTY MASTER");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        root.getChildren().addAll(title, createFormSection(), createButtonsBox());
        return root;
    }

    private VBox createFormSection() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints lc = new ColumnConstraints();
        lc.setMinWidth(130);
        lc.setPrefWidth(140);

        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        fc.setFillWidth(true);

        grid.getColumnConstraints().addAll(lc, fc, lc, fc);

        // Row 0: Company Name | Owner Name
        companyNameField = field();
        ownerNameField   = field();
        grid.add(lbl("Company Name"), 0, 0); grid.add(companyNameField, 1, 0);
        grid.add(lbl("Owner Name"),   2, 0); grid.add(ownerNameField,   3, 0);

        // Row 1: Mobile | Email
        mobileField = field();
        emailField  = field();
        grid.add(lbl("Mobile"), 0, 1); grid.add(mobileField, 1, 1);
        grid.add(lbl("Email"),  2, 1); grid.add(emailField,  3, 1);

        // Row 2-3: Address (rowspan 2, left side) | State / City (right side)
        addressField = new TextArea();
        addressField.setPrefRowCount(3);
        addressField.setWrapText(true);
        addressField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setRowSpan(addressField, 2);
        grid.add(lbl("Address"), 0, 2);
        grid.add(addressField,   1, 2);

        stateCombo = new ComboBox<>(FXCollections.observableArrayList(STATES));
        stateCombo.setPromptText("Select state");
        stateCombo.setMaxWidth(Double.MAX_VALUE);
        stateCombo.setOnAction(e -> updateCitiesForState());
        grid.add(lbl("State"), 2, 2); grid.add(stateCombo, 3, 2);

        cityCombo = new ComboBox<>();
        cityCombo.setPromptText("Select or type city");
        cityCombo.setEditable(true);
        cityCombo.setDisable(true);
        cityCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(lbl("City"), 2, 3); grid.add(cityCombo, 3, 3);

        // Row 4: Pin Code | GST
        pinCodeField = field();
        gstField     = field();
        grid.add(lbl("Pin Code"), 0, 4); grid.add(pinCodeField, 1, 4);
        grid.add(lbl("GST"),      2, 4); grid.add(gstField,     3, 4);

        // Row 5: PAN | CST No.
        panField   = field();
        cstNoField = field();
        grid.add(lbl("PAN Card"), 0, 5); grid.add(panField,   1, 5);
        grid.add(lbl("CST No."),  2, 5); grid.add(cstNoField, 3, 5);

        // Row 6: TAN No. | TDS
        tanNoField = field();
        tdsField   = field();
        grid.add(lbl("TAN No."), 0, 6); grid.add(tanNoField, 1, 6);
        grid.add(lbl("TDS"),     2, 6); grid.add(tdsField,   3, 6);

        // Row 7: Aadhar No.
        aadharNoField = field();
        grid.add(lbl("Aadhar No."), 0, 7); grid.add(aadharNoField, 1, 7);

        // ── Routes section ───────────────────────────────────────────────────
        Label routesTitle = new Label("Routes");
        routesTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        routeInputField = new TextField();
        routeInputField.setPromptText("Type a route and press Add or Enter");
        routeInputField.setMaxWidth(Double.MAX_VALUE);
        routeInputField.setOnAction(e -> addRoute());

        Button addRouteBtn = new Button("Add");
        addRouteBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 6 14 6 14;");
        addRouteBtn.setOnAction(e -> addRoute());

        HBox routeInputRow = new HBox(8, routeInputField, addRouteBtn);
        routeInputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(routeInputField, Priority.ALWAYS);

        ListView<String> routesListView = new ListView<>(routesList);
        routesListView.setPrefHeight(90);
        routesListView.setMaxHeight(120);

        ContextMenu routeCtx = new ContextMenu();
        MenuItem removeRoute = new MenuItem("Remove");
        removeRoute.setOnAction(e -> {
            String sel = routesListView.getSelectionModel().getSelectedItem();
            if (sel != null) routesList.remove(sel);
        });
        routeCtx.getItems().add(removeRoute);
        routesListView.setContextMenu(routeCtx);

        VBox routesBox = new VBox(6, routesTitle, routeInputRow, routesListView);

        VBox section = new VBox(12, new Label("Party Details"), grid, routesBox);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return section;
    }

    private void addRoute() {
        String val = routeInputField.getText();
        if (val != null && !val.isBlank() && !routesList.contains(val.trim())) {
            routesList.add(val.trim());
        }
        routeInputField.clear();
    }

    private void updateCitiesForState() {
        String state = stateCombo.getValue();
        cityCombo.getItems().clear();
        if (state == null || state.isBlank()) {
            cityCombo.setDisable(true);
            cityCombo.getEditor().clear();
            return;
        }
        cityCombo.setDisable(false);
        List<String> cities = CITIES_BY_STATE.get(state);
        if (cities != null) cityCombo.getItems().addAll(cities);
        cityCombo.getSelectionModel().clearSelection();
        cityCombo.getEditor().clear();
    }

    private HBox createButtonsBox() {
        Button newBtn = new Button("New");
        newBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #2563eb; -fx-text-fill: white;");
        newBtn.setOnAction(e -> clearForm());

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #16a34a; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveParty());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-background-color: #dc2626; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteParty());

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 8 20; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> stage.close());

        HBox box = new HBox(10, newBtn, saveBtn, deleteBtn, closeBtn);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return box;
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadPartyIntoForm(Party p) {
        currentParty = p;
        companyNameField.setText(nvl(p.getName()));
        ownerNameField.setText(nvl(p.getOwnerName()));
        mobileField.setText(nvl(p.getMobile()));
        emailField.setText(nvl(p.getEmail()));
        addressField.setText(nvl(p.getAddress()));

        if (p.getState() != null && !p.getState().isBlank()) {
            stateCombo.setValue(p.getState());
            updateCitiesForState();
        }
        if (p.getCity() != null && !p.getCity().isBlank()) {
            cityCombo.setValue(p.getCity());
        }

        pinCodeField.setText(nvl(p.getPincode()));
        gstField.setText(nvl(p.getGstin()));
        panField.setText(nvl(p.getPan()));
        cstNoField.setText(nvl(p.getCstNo()));
        tanNoField.setText(nvl(p.getTanNo()));
        tdsField.setText(nvl(p.getTds()));
        aadharNoField.setText(nvl(p.getAadharNo()));

        routesList.clear();
        if (p.getRoutes() != null && !p.getRoutes().isBlank()) {
            for (String r : p.getRoutes().split("\\|")) {
                String t = r.trim();
                if (!t.isBlank()) routesList.add(t);
            }
        }
    }

    private void clearForm() {
        currentParty = null;
        companyNameField.clear();
        ownerNameField.clear();
        mobileField.clear();
        emailField.clear();
        addressField.clear();
        stateCombo.getSelectionModel().clearSelection();
        cityCombo.getItems().clear();
        cityCombo.getSelectionModel().clearSelection();
        cityCombo.getEditor().clear();
        cityCombo.setDisable(true);
        pinCodeField.clear();
        gstField.clear();
        panField.clear();
        cstNoField.clear();
        tanNoField.clear();
        tdsField.clear();
        aadharNoField.clear();
        routesList.clear();
        routeInputField.clear();
    }

    private void saveParty() {
        if (companyNameField.getText().isBlank()) {
            AlertUtil.showWarning("Validation", "Company Name is required.");
            return;
        }

        Party party = currentParty != null ? currentParty : new Party();
        party.setName(companyNameField.getText().trim());
        party.setOwnerName(ownerNameField.getText());
        party.setMobile(mobileField.getText());
        party.setEmail(emailField.getText());
        party.setAddress(addressField.getText());
        party.setState(stateCombo.getValue());
        String selCity = cityCombo.getValue();
        party.setCity((selCity != null && !selCity.isBlank()) ? selCity : cityCombo.getEditor().getText());
        party.setPincode(pinCodeField.getText());
        party.setGstin(gstField.getText());
        party.setPan(panField.getText());
        party.setCstNo(cstNoField.getText());
        party.setTanNo(tanNoField.getText());
        party.setTds(tdsField.getText());
        party.setAadharNo(aadharNoField.getText());
        party.setRoutes(String.join("|", routesList));

        boolean isEdit = currentParty != null;
        AppExecutor.submit(() -> {
            try {
                PartyDAO dao = new PartyDAO();
                if (isEdit) {
                    dao.update(party);
                } else {
                    dao.save(party);
                }
                Platform.runLater(() -> {
                    NotificationUtil.showSuccess("Success", "Party saved successfully.");
                    if (isEdit) stage.close(); else clearForm();
                });
            } catch (Exception e) {
                log.error("Failed to save party", e);
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to save party: " + e.getMessage()));
            }
        });
    }

    private void deleteParty() {
        if (currentParty == null) {
            AlertUtil.showWarning("Validation", "Select an existing party to delete.");
            return;
        }
        if (AlertUtil.showConfirmation("Delete Party",
                "Delete '" + currentParty.getName() + "'? This cannot be undone.")) {
            AppExecutor.submit(() -> {
                try {
                    new PartyDAO().delete(currentParty.getId());
                    Platform.runLater(() -> {
                        NotificationUtil.showSuccess("Deleted", "Party deleted.");
                        stage.close();
                    });
                } catch (Exception e) {
                    log.error("Failed to delete party", e);
                    Platform.runLater(() -> AlertUtil.showError("Error", "Failed to delete: " + e.getMessage()));
                }
            });
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TextField field() {
        TextField tf = new TextField();
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        return l;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}