package com.accounting;

import javax.swing.*;
import java.awt.*;

public class Launcher {

    private static final String CORRECT_PASSWORD = "Yashank01$";

    public static void main(String[] args) {
        // Set system property for JavaFX to work with Swing
        System.setProperty("javafx.embed.singleThread", "true");

        // Create password dialog
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(350, 150));
        panel.setBackground(new Color(248, 250, 252));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Account Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 58, 95));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel instructionLabel = new JLabel("Enter password to launch:");
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        instructionLabel.setForeground(new Color(100, 116, 139));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(instructionLabel, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(passwordLabel, gbc);

        JPasswordField passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(passwordField, gbc);

        JButton launchButton = new JButton("Launch");
        launchButton.setBackground(new Color(22, 163, 74));
        launchButton.setForeground(Color.WHITE);
        launchButton.setFont(new Font("Arial", Font.BOLD, 12));
        launchButton.setPreferredSize(new Dimension(100, 30));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(launchButton, gbc);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(errorLabel, gbc);

        // Create dialog
        JDialog dialog = new JDialog();
        dialog.setTitle("Security Check");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        // Button action
        launchButton.addActionListener(e -> {
            String enteredPassword = new String(passwordField.getPassword());
            if (enteredPassword.equals(CORRECT_PASSWORD)) {
                dialog.dispose();
                // Launch MainApp
                try {
                    MainApp.main(args);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to launch application: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else {
                errorLabel.setText("Incorrect password. Please try again.");
                passwordField.setText("");
                passwordField.requestFocus();
            }
        });

        // Enter key action
        passwordField.addActionListener(e -> launchButton.doClick());

        // Show dialog
        dialog.setVisible(true);
    }
}
