package csat.client.ui;

import csat.client.api.ApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

public class AddEmployeeDialog extends JDialog {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{9,11}$");
    private static final Pattern CCCD_PATTERN  = Pattern.compile("^[0-9]{9,12}$");

    private final ApiClient apiClient;
    private final int userLevel;
    private final Runnable onSuccess;

    private JTextField txtFullName;
    private JTextField txtCccd;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtBank;
    private JTextField txtSalary;
    private JLabel lblStatus;
    private JButton btnSave;

    public AddEmployeeDialog(Frame owner, ApiClient apiClient, int userLevel, Runnable onSuccess) {
        super(owner, "Add Employee", true);
        this.apiClient = apiClient;
        this.userLevel = userLevel;
        this.onSuccess = onSuccess;
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        setContentPane(root);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        txtFullName = addField(form, gbc, "Full name");
        txtCccd = addField(form, gbc, "CCCD");
        txtPhone = addField(form, gbc, "Phone");
        txtEmail = addField(form, gbc, "Email");
        txtBank = addField(form, gbc, "Bank account");
        txtSalary = addField(form, gbc, "Salary");

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
        gbc.gridy++;
        form.add(lblStatus, gbc);

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> dispose());

        btnSave = new JButton("Add employee");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(new Color(0, 120, 215));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> submit());

        actions.add(btnCancel);
        actions.add(btnSave);
        root.add(actions, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnSave);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(430, 520);
        setLocationRelativeTo(getOwner());
    }

    private JTextField addField(JPanel form, GridBagConstraints gbc, String label) {
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        gbc.gridy++;
        form.add(l, gbc);

        JTextField field = new JTextField();
        field.putClientProperty("JComponent.roundRect", true);
        field.setPreferredSize(new Dimension(0, 36));
        gbc.gridy++;
        form.add(field, gbc);
        return field;
    }

    private void submit() {
        System.out.println("[CLIENT] Add employee submit clicked");

        if (userLevel != 3) {
            showValidationError("Only Director can add employee.");
            return;
        }

        String fullName = txtFullName.getText().trim();
        String cccd = normalizeDigits(txtCccd.getText());
        String phone = normalizePhone(txtPhone.getText());
        String email = txtEmail.getText().trim();
        String bank = normalizeDigits(txtBank.getText());
        String salary = normalizeSalary(txtSalary.getText());

        if (fullName.isEmpty() || cccd.isEmpty() || phone.isEmpty() || email.isEmpty()
                || bank.isEmpty() || salary.isEmpty()) {
            showValidationError("Please fill all fields.");
            return;
        }

        String validationError = validateInputs(fullName, cccd, phone, email, bank, salary);
        if (validationError != null) {
            showValidationError(validationError);
            return;
        }

        long salaryValue = Long.parseLong(salary);

        btnSave.setEnabled(false);
        lblStatus.setForeground(UiTheme.color("App.status.info", new Color(0, 99, 194)));
        lblStatus.setText("Submitting to server...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                System.out.println("[CLIENT] Add employee submit: " + fullName + " / " + cccd);
                apiClient.addEmployee(fullName, cccd, phone, email, bank, salaryValue);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lblStatus.setForeground(UiTheme.color("App.status.success", new Color(21, 128, 61)));
                    lblStatus.setText("Employee added successfully.");
                    JOptionPane.showMessageDialog(AddEmployeeDialog.this,
                            "Employee added successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    if (onSuccess != null) onSuccess.run();
                    dispose();
                } catch (Exception ex) {
                    lblStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
                    lblStatus.setText("Error: " + getRootMessage(ex));
                    JOptionPane.showMessageDialog(AddEmployeeDialog.this,
                            "Add employee failed:\n" + getRootMessage(ex),
                            "Request failed",
                            JOptionPane.ERROR_MESSAGE);
                    btnSave.setEnabled(true);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void showValidationError(String message) {
        lblStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
        lblStatus.setText(message);
        JOptionPane.showMessageDialog(this, message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    private String normalizeDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalizePhone(String value) {
        String digits = normalizeDigits(value);
        if (digits.startsWith("84") && digits.length() >= 10) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    private String normalizeSalary(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\s,._]", "");
    }

    private String validateInputs(String fullName, String cccd, String phone,
                                  String email, String bank, String salary) {
        if (fullName.length() < 2) return "Full name must have at least 2 characters.";
        if (!CCCD_PATTERN.matcher(cccd).matches()) return "CCCD must be 9-12 digits.";
        if (!PHONE_PATTERN.matcher(phone).matches()) return "Phone must be 9-11 digits.";
        if (!EMAIL_PATTERN.matcher(email).matches()) return "Email format is invalid.";
        if (bank.length() < 6 || bank.length() > 20) return "Bank account must be 6-20 digits.";
        try {
            long salaryVal = Long.parseLong(salary);
            if (salaryVal <= 0) return "Salary must be greater than 0.";
        } catch (NumberFormatException ex) {
            return "Salary must be a number.";
        }
        return null;
    }

    private String getRootMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }
}





