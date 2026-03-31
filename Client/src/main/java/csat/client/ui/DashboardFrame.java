package csat.client.ui;

import csat.client.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * DashboardFrame — Bảng quản lý nhân viên.
 *
 * Hiển thị dữ liệu User từ Server. Dữ liệu đã được:
 *   1. Mã hóa AES tại BE trước khi lưu DB
 *   2. Giải mã khi Query lên
 *   3. Mask bớt tùy Level (*** thay thế trường nhạy cảm)
 *   4. Mã hóa AES lần nữa khi truyền qua mạng
 *   5. Client nhận về Hex → giải mã AES → Parse JSON → Hiển thị JTable
 */
public class DashboardFrame extends JFrame {

    private final ApiClient apiClient;
    private final String    username;
    private final int       level;

    private JTable          table;
    private DefaultTableModel tableModel;
    private JLabel          lblInfo;
    private JTabbedPane     tabs;

    // Crypto playground controls
    private JTextArea       txtPlainIn;
    private JTextArea       txtHexOut;
    private JTextArea       txtHexIn;
    private JTextArea       txtPlainOut;
    private JLabel          lblCryptoStatus;
    private JLabel          lblSessionKey;

    private static final String[] COLUMNS = {
        "ID", "Họ tên", "CCCD", "SĐT", "Email", "Số TK Ngân hàng", "Lương", "Algo"
    };

    public DashboardFrame(ApiClient apiClient, String username, int level) {
        this.apiClient = apiClient;
        this.username  = username;
        this.level     = level;
        initUI();
        loadData();
    }

    private void initUI() {
        setTitle("CSAT-BMTT Dashboard - " + username + " (Level " + level + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        setContentPane(main);

        // ── Header ───────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JLabel lblTitle = new JLabel("Employee Directory");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));

        lblInfo = new JLabel(getLevelBadge());
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD, 12f));
        lblInfo.setForeground(getLevelColor());

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);

        JButton btnRefresh = new JButton("Refresh");
        styleActionButton(btnRefresh, new Color(0, 120, 215));
        btnRefresh.addActionListener(e -> loadData());

        JButton btnAddEmployee = new JButton("Add employee");
        styleActionButton(btnAddEmployee, new Color(21, 128, 61));
        btnAddEmployee.addActionListener(e -> new AddEmployeeDialog(this, apiClient, level, this::loadData).setVisible(true));

        JButton btnLogout = new JButton("Sign out");
        styleActionButton(btnLogout, new Color(185, 28, 28));
        btnLogout.addActionListener(e -> {
            new LoginFrame(apiClient).setVisible(true);
            dispose();
        });

        headerRight.add(lblInfo);
        headerRight.add(btnRefresh);
        if (level == 3) headerRight.add(btnAddEmployee);
        headerRight.add(btnLogout);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        main.add(header, BorderLayout.NORTH);

        // ── Tabs ─────────────────────────────────────────────────────
        tabs = new JTabbedPane();
        tabs.addTab("Employees", buildEmployeeTab());
        tabs.addTab("Encrypt / Decrypt", buildCryptoTab());
        main.add(tabs, BorderLayout.CENTER);

        // ── Footer / Status bar ──────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));

        JLabel lblEncNote = new JLabel("Network transport and stored data are protected by AES-128");
        lblEncNote.setFont(lblEncNote.getFont().deriveFont(Font.ITALIC, 11f));
        lblEncNote.setForeground(UIManager.getColor("Label.disabledForeground"));
        footer.add(lblEncNote, BorderLayout.WEST);

        main.add(footer, BorderLayout.SOUTH);

        setSize(1100, 600);
        setLocationRelativeTo(null);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        lblInfo.setText("Loading and decrypting data...");
        lblInfo.setForeground(UiTheme.color("App.status.info", new Color(0, 99, 194)));

        SwingWorker<JsonArray, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonArray doInBackground() throws Exception {
                return apiClient.getUsers();
            }

            @Override
            protected void done() {
                try {
                    JsonArray users = get();
                    for (JsonElement el : users) {
                        JsonObject u = el.getAsJsonObject();
                        tableModel.addRow(new Object[]{
                            u.get("id").getAsLong(),
                            u.get("fullName").getAsString(),
                            u.get("cccd").getAsString(),
                            u.get("phone").getAsString(),
                            u.get("email").getAsString(),
                            u.get("bankAccount").getAsString(),
                            formatSalary(u.get("salary").getAsString()),
                            u.get("algo").getAsString()
                        });
                    }
                    lblInfo.setText(getLevelBadge() + "  (" + users.size() + " employees)");
                    lblInfo.setForeground(getLevelColor());
                } catch (Exception ex) {
                    lblInfo.setText("Error: " + ex.getMessage());
                    lblInfo.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
                }
            }
        };
        worker.execute();
    }

    private JPanel buildEmployeeTab() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(table.getFont().deriveFont(Font.PLAIN, 13f));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(50, 80, 140));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader th = table.getTableHeader();
        th.setFont(th.getFont().deriveFont(Font.BOLD, 12f));
        th.setBorder(BorderFactory.createEmptyBorder());

        int[] widths = {40, 160, 130, 110, 180, 140, 100, 110};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.setDefaultRenderer(Object.class, new AlternatingRowRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCryptoTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel grids = new JPanel(new GridLayout(1, 2, 12, 0));

        // Encrypt block
        JPanel encryptBox = new JPanel(new BorderLayout(8, 8));
        encryptBox.setBorder(BorderFactory.createTitledBorder("Encrypt to hex"));
        txtPlainIn = new JTextArea(8, 30);
        txtPlainIn.setLineWrap(true);
        txtPlainIn.setWrapStyleWord(true);
        JScrollPane spPlain = new JScrollPane(txtPlainIn);
        encryptBox.add(spPlain, BorderLayout.CENTER);

        JButton btnEncrypt = new JButton("Encrypt ➜ Hex");
        btnEncrypt.addActionListener(e -> doEncrypt());
        encryptBox.add(btnEncrypt, BorderLayout.SOUTH);

        txtHexOut = new JTextArea(6, 30);
        txtHexOut.setEditable(false);
        txtHexOut.setLineWrap(true);
        txtHexOut.setWrapStyleWord(true);
        JScrollPane spHexOut = new JScrollPane(txtHexOut);
        JPanel encBottom = new JPanel(new BorderLayout(4, 4));
        encBottom.add(new JLabel("Encrypted hex:"), BorderLayout.NORTH);
        encBottom.add(spHexOut, BorderLayout.CENTER);
        encryptBox.add(encBottom, BorderLayout.EAST);

        // Decrypt block
        JPanel decryptBox = new JPanel(new BorderLayout(8, 8));
        decryptBox.setBorder(BorderFactory.createTitledBorder("Decrypt from hex"));
        txtHexIn = new JTextArea(8, 30);
        txtHexIn.setLineWrap(true);
        txtHexIn.setWrapStyleWord(true);
        JScrollPane spHexIn = new JScrollPane(txtHexIn);
        decryptBox.add(spHexIn, BorderLayout.CENTER);

        JButton btnDecrypt = new JButton("Decrypt ➜ Text");
        btnDecrypt.addActionListener(e -> doDecrypt());
        decryptBox.add(btnDecrypt, BorderLayout.SOUTH);

        txtPlainOut = new JTextArea(6, 30);
        txtPlainOut.setEditable(false);
        txtPlainOut.setLineWrap(true);
        txtPlainOut.setWrapStyleWord(true);
        JScrollPane spPlainOut = new JScrollPane(txtPlainOut);
        JPanel decBottom = new JPanel(new BorderLayout(4, 4));
        decBottom.add(new JLabel("Decrypted text:"), BorderLayout.NORTH);
        decBottom.add(spPlainOut, BorderLayout.CENTER);
        decryptBox.add(decBottom, BorderLayout.EAST);

        grids.add(encryptBox);
        grids.add(decryptBox);
        panel.add(grids, BorderLayout.CENTER);

        lblSessionKey = new JLabel(sessionKeyLabelText());
        lblSessionKey.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lblSessionKey.setForeground(UiTheme.color("App.status.info", new Color(0, 99, 194)));

        lblCryptoStatus = new JLabel("Use the established session key to test payloads.");
        lblCryptoStatus.setForeground(UiTheme.color("App.status.info", new Color(0, 99, 194)));

        JPanel south = new JPanel(new GridLayout(0, 1, 0, 4));
        south.setOpaque(false);
        south.add(lblSessionKey);
        south.add(lblCryptoStatus);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    private void doEncrypt() {
        try {
            String plain = txtPlainIn.getText();
            if (plain == null || plain.isBlank()) {
                lblCryptoStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
                lblCryptoStatus.setText("Please enter text to encrypt.");
                return;
            }
            String hex = apiClient.encryptLocal(plain);
            txtHexOut.setText(hex);
            lblCryptoStatus.setForeground(UiTheme.color("App.status.success", new Color(21, 128, 61)));
            lblCryptoStatus.setText("Encrypted successfully.");
        } catch (Exception ex) {
            lblCryptoStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
            lblCryptoStatus.setText("Encrypt error: " + ex.getMessage());
        }
    }

    private void doDecrypt() {
        try {
            String hex = txtHexIn.getText();
            if (hex == null || hex.isBlank()) {
                lblCryptoStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
                lblCryptoStatus.setText("Please enter hex to decrypt.");
                return;
            }
            String plain = apiClient.decryptLocal(hex.trim());
            txtPlainOut.setText(plain);
            lblCryptoStatus.setForeground(UiTheme.color("App.status.success", new Color(21, 128, 61)));
            lblCryptoStatus.setText("Decrypted successfully.");
        } catch (Exception ex) {
            lblCryptoStatus.setForeground(UiTheme.color("App.status.error", new Color(185, 28, 28)));
            lblCryptoStatus.setText("Decrypt error: " + ex.getMessage());
        }
    }

    private String sessionKeyLabelText() {
        try {
            String hex = apiClient.getSessionKeyHex();
            return "Session AES-128 key (hex): " + hex;
        } catch (Exception ex) {
            return "Session AES-128 key: (not established yet)";
        }
    }

    private String formatSalary(String raw) {
        if (raw.contains("*")) return raw; // Đã bị mask
        try {
            long val = Long.parseLong(raw);
            return String.format("%,d ₫", val);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private String getLevelBadge() {
        return switch (level) {
            case 3 -> "Director (Level 3) - Full access";
            case 2 -> "Manager (Level 2) - Salary masked";
            default -> "Staff (Level 1) - Sensitive fields masked";
        };
    }

    private Color getLevelColor() {
        return switch (level) {
            case 3 -> UiTheme.color("App.level.3", new Color(146, 64, 14));
            case 2 -> UiTheme.color("App.level.2", new Color(6, 95, 70));
            default -> UiTheme.color("App.level.1", new Color(3, 105, 161));
        };
    }

    private void styleActionButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 32));
    }

    // ── Alternating row colors ────────────────────────────────────

    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        private static final Color ROW_ODD  = new Color(250, 252, 255);
        private static final Color ROW_EVEN = new Color(242, 246, 252);
        private static final Color MASKED   = new Color(183, 53, 53);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            if (selected) {
                setBackground(new Color(50, 80, 140));
                setForeground(Color.WHITE);
            } else {
                setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                String s = value != null ? value.toString() : "";
                // Highlight màu đỏ nhạt nếu là masked data (***)
                setForeground(s.contains("*") ? MASKED : new Color(30, 41, 59));
            }
            return this;
        }
    }
}
