package csat.client;

import csat.client.api.ApiClient;
import csat.client.ui.LoginFrame;
import csat.client.ui.UiTheme;
import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;

/**
 * MainApp — Entry point của Java Swing Desktop Client.
 *
 * Chạy: java -jar CSAT-BMTT-Client-jar-with-dependencies.jar
 * Hoặc chạy trực tiếp trong IntelliJ bằng cách chạy hàm main() này.
 *
 * ĐẢMBẢO Server Spring Boot (CSAT&BMTT) đang chạy ở port 8081 trước khi chạy Client.
 */
public class MainApp {

    public static void main(String[] args) {
        // Font anti-aliasing for crisper text rendering.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Swing must be started on Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> {
            try {
                FlatIntelliJLaf.setup();
                UiTheme.applyWin11Defaults();
            } catch (Exception ex) {
                System.err.println("Cannot initialize look and feel: " + ex.getMessage());
            }

            ApiClient apiClient = new ApiClient();
            LoginFrame loginFrame = new LoginFrame(apiClient);
            loginFrame.setVisible(true);
        });
    }
}
