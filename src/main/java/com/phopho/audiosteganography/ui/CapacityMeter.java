package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.engine.Stego;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/** "Uses 1.2 KB of 54.3 KB" with a bar that turns amber near full and red when over. */
final class CapacityMeter extends JComponent {

    private long used;
    private long capacity = -1;

    CapacityMeter() {
        setPreferredSize(new Dimension(300, 38));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        setFont(Theme.font(12.5f));
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PROGRESS_BAR;
                }
            };
        }
        return accessibleContext;
    }

    /** {@code capacity < 0} means no cover audio yet. */
    void set(long used, long capacity) {
        this.used = used;
        this.capacity = capacity;
        getAccessibleContext().setAccessibleDescription(label());
        repaint();
    }

    boolean fits() {
        return capacity >= 0 && used <= capacity;
    }

    private String label() {
        if (capacity < 0) {
            return "Choose a cover audio file to see how much it can hold";
        }
        if (used > capacity) {
            return "Too large: needs " + Stego.formatBytes(used) + ", this audio holds " + Stego.formatBytes(capacity);
        }
        return "Uses " + Stego.formatBytes(used) + " of " + Stego.formatBytes(capacity);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            double ratio = capacity <= 0 ? 0 : (double) used / capacity;
            Color tone = ratio > 1 ? p.danger() : ratio > 0.85 ? p.warning() : p.accent();

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(ratio > 1 ? p.danger() : p.textSecondary());
            g2.drawString(label(), 0, fm.getAscent());
            if (capacity > 0) {
                String pct = String.format(Locale.ROOT, "%.0f%%", Math.min(999, ratio * 100));
                g2.drawString(pct, getWidth() - fm.stringWidth(pct), fm.getAscent());
            }

            double y = fm.getHeight() + 8;
            double w = getWidth();
            g2.setColor(p.dark() ? p.bg() : Theme.mix(p.surfaceAlt(), p.text(), 0.06));
            g2.fill(new RoundRectangle2D.Double(0, y, w, 6, 6, 6));
            if (ratio > 0) {
                g2.setColor(tone);
                g2.fill(new RoundRectangle2D.Double(0, y, Math.max(6, w * Math.min(1, ratio)), 6, 6, 6));
            }
        } finally {
            g2.dispose();
        }
    }
}
