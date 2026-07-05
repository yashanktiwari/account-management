package com.accounting.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.util.Callback;

/**
 * Provides custom editable cell factories that commit on focus loss (not just Enter).
 * Fixes the standard TextFieldTableCell which cancels edits when focus is lost.
 */
public class EditableCellFactory {

    /**
     * Returns a cell factory for String columns that commits on both Enter and focus loss.
     */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forStringColumn() {
        return col -> new TableCell<>() {
            private TextField textField;

            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    textField = new TextField();
                    textField.setOnAction(e -> commitEdit(textField.getText()));
                    textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (!isFocused && isEditing()) {
                            commitEdit(textField.getText());
                        }
                    });
                }
                textField.setText(getItem() != null ? getItem() : "");
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() != null ? getItem() : "");
                setGraphic(null);
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    if (textField != null) {
                        textField.setText(item != null ? item : "");
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(item != null ? item : "");
                    setGraphic(null);
                }
            }
        };
    }

    /**
     * Returns a cell factory for Double columns that commits on both Enter and focus loss.
     */
    public static <S> Callback<TableColumn<S, Double>, TableCell<S, Double>> forDoubleColumn() {
        return col -> new TableCell<>() {
            private TextField textField;

            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    textField = new TextField();
                    textField.setOnAction(e -> commitValue());
                    textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (!isFocused && isEditing()) {
                            commitValue();
                        }
                    });
                    // Only allow numeric input
                    textField.textProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null && !newVal.isEmpty() && !newVal.matches("-?\\d*\\.?\\d*")) {
                            textField.setText(oldVal);
                        }
                    });
                }
                textField.setText(getItem() != null ? getItem().toString() : "0");
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() != null ? String.valueOf(getItem()) : "0");
                setGraphic(null);
            }

            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    if (textField != null) {
                        textField.setText(item != null ? item.toString() : "0");
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(item != null ? String.valueOf(item) : "0");
                    setGraphic(null);
                }
            }

            private void commitValue() {
                try {
                    String text = textField.getText().trim();
                    commitEdit(text.isEmpty() ? 0.0 : Double.parseDouble(text));
                } catch (NumberFormatException e) {
                    cancelEdit();
                }
            }
        };
    }
}
