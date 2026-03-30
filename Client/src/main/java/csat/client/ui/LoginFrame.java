package csat.client.ui;

import csat.client.api.ApiClient;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;

/**
 * LoginFrame — Màn hình đăng nhập Java Swing.
 *
 * Khi nhấn "Đăng nhập":
 *   1. Đóng gói JSON {username, password}
 *   2. Mã hóa bằng AES-128 (AESCipher.java tự viết) thành Hex
 *   3. POST /api/auth/login với Hex payload
 *   4. Nhận Hex response, giải mã AES lấy {token, level}
 *   5. Mở DashboardFrame
 */
public class LoginFrame extends JFrame {

    private final ApiClient apiClient;

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JButton        btnLogin;
    private JLabel         lblStatus;

    public LoginFrame(ApiClient apiClient) {
        this.apiClient = apiClient;
        initUI();
    }

    private void initUI() {
        setTitle("CSAT-BMTT - Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        setContentPane(main);

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.color("Component.borderColor", new Color(222, 226, 230))),
            BorderFactory.createEmptyBorder(14, 14, 12, 14)
        ));
        card.putClientProperty("JComponent.roundRect", true);
        main.add(card, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 16, 8));

        JLabel lblTitle = new JLabel("Welcome back");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 24f));

        JLabel lblSub = new JLabel("Sign in to continue to CSAT-BMTT Desktop Client");
        lblSub.setFont(lblSub.getFont().deriveFont(Font.PLAIN, 13f));
        lblSub.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 4));
        headerText.setOpaque(false);
        headerText.add(lblTitle);
        headerText.add(lblSub);
        header.add(headerText, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        form.add(makeLabel("Username"), gbc);

        gbc.gridy = 1;
        txtUsername = makeTextField();
        form.add(txtUsername, gbc);

        // Password
        gbc.gridy = 2;
        form.add(makeLabel("Password"), gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField();
        styleTextField(txtPassword);
        form.add(txtPassword, gbc);

        // Status
        gbc.gridy = 4;
        lblStatus = new JLabel(" ");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.PLAIN, 12f));
        lblStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(lblStatus, gbc);

        gbc.gridy = 5;
        btnLogin = new JButton("Dang nhap");
        btnLogin.setFont(btnLogin.getFont().deriveFont(Font.BOLD, 14f));
        btnLogin.setBackground(new Color(52, 120, 246));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.putClientProperty("JButton.minimumWidth", 180);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setMinimumSize(new Dimension(180, 42));
        btnLogin.setMargin(new Insets(10, 16, 10, 16));
        form.add(btnLogin, gbc);

        card.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel lblNote = new JLabel("Transport uses AES-128 encrypted payloads");
        lblNote.setFont(lblNote.getFont().deriveFont(Font.ITALIC, 11f));
        lblNote.setForeground(UIManager.getColor("Label.disabledForeground"));
        footer.add(lblNote);
        card.add(footer, BorderLayout.SOUTH);

        // ── Events ───────────────────────────────────────────────────
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin()); // Enter in password

        pack();
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null); // Căn giữa màn hình
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please enter username and password.");
            return;
        }

        btnLogin.setEnabled(false);
        lblStatus.setForeground(UiTheme.color("App.status.info", new Color(0, 99, 194)));
        lblStatus.setText("Encrypting payload and contacting server...");

        // Chạy trên background thread để không block UI
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                return apiClient.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    JsonObject result = get();
                    String token    = result.get("token").getAsString();
                    int    level    = result.get("level").getAsInt();
                    String uname    = result.get("username").getAsString();
                    apiClient.setAccessToken(token);
                    lblStatus.setForeground(UiTheme.color("App.status.success", new Color(21, 128, 61)));
                    lblStatus.setText("Login successful. Opening dashboard...");
                    // Mở Dashboard sau 500ms
                    Timer t = new Timer(500, ev -> {
                        new DashboardFrame(apiClient, uname, level).setVisible(true);
                        dispose();
                    });
                    t.setRepeats(false);
                    t.start();
                } catch (Exception ex) {
                    lblStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
                    lblStatus.setText("Error: " + getRootMessage(ex));
                    btnLogin.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private String getRootMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    }

    // ── UI Helpers ───────────────────────────────────────────────

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        return l;
    }

    private JTextField makeTextField() {
        JTextField f = new JTextField();
        styleTextField(f);
        return f;
    }

    private void styleTextField(JTextField f) {
        f.setFont(f.getFont().deriveFont(Font.PLAIN, 14f));
        f.putClientProperty("JComponent.roundRect", true);
        f.setPreferredSize(new Dimension(0, 40));
    }
}
