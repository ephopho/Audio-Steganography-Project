package com.phopho.audiosteganography.ui;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/** Starts the desktop app. */
public final class App {

    private App() {
    }

    public static void launch() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            Theme.init();
            styleTooltips();
            Theme.onChange(App::styleTooltips);
            ToolTipManager.sharedInstance().setInitialDelay(400);
            MainWindow window = new MainWindow();
            window.setVisible(true);
            window.initialFocus().requestFocusInWindow();
        });
    }

    private static void styleTooltips() {
        Theme.Palette p = Theme.p();
        Color bg = p.dark() ? Theme.hex(0x2C2C2C) : Theme.hex(0xF9F9F9);
        UIManager.put("ToolTip.background", bg);
        UIManager.put("ToolTip.foreground", p.text());
        UIManager.put("ToolTip.font", Theme.font(12.5f));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(p.borderStrong()), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
}
