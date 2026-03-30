package csat.client.ui;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

public final class UiTheme {

    private UiTheme() {
    }

    public static void applyWin11Defaults() {
        UIManager.put("defaultFont", new FontUIResource("Segoe UI", Font.PLAIN, 14));

        // Win11-like rounded controls and compact focus rings.
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("CheckBox.arc", 6);

        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.innerFocusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Button.margin", new Insets(8, 14, 8, 14));
        UIManager.put("Button.minimumWidth", 120);

        // Accent palette close to modern Windows apps.
        UIManager.put("Component.accentColor", new Color(0, 120, 215));
        UIManager.put("Button.default.background", new Color(52, 120, 246));
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);

        // Semantic app colors.
        UIManager.put("App.status.info", new Color(0, 99, 194));
        UIManager.put("App.status.success", new Color(21, 128, 61));
        UIManager.put("App.status.error", new Color(185, 28, 28));
        UIManager.put("App.level.1", new Color(3, 105, 161));
        UIManager.put("App.level.2", new Color(6, 95, 70));
        UIManager.put("App.level.3", new Color(146, 64, 14));
    }

    public static Color color(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }
}

