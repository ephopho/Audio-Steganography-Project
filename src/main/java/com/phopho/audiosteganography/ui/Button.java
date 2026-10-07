package com.phopho.audiosteganography.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JButton;

/** A Fluent-style button: real JButton (keyboard, focus, accessibility), custom painting. */
final class Button extends JButton {

    enum Kind { PRIMARY, SECONDARY, GHOST }

    private final Kind kind;
    private boolean hover;

    Button(String text, Kind kind) {
        this(text, null, kind);
    }

    Button(String text, Icon icon, Kind kind) {
        super(text, icon);
        this.kind = kind;
        setFont(Theme.semibold(13.5f));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setIconTextGap(8);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        FontMetrics fm = getFontMetrics(getFont());
        String text = getText();
        int textWidth = text == null || text.isEmpty() ? 0 : fm.stringWidth(text);
        Icon icon = getIcon();
        int iconWidth = icon == null ? 0 : icon.getIconWidth() + (textWidth > 0 ? getIconTextGap() : 0);
        boolean iconOnly = textWidth == 0 && icon != null;
        int height = kind == Kind.PRIMARY ? 40 : 34;
        return new Dimension(iconOnly ? height : textWidth + iconWidth + 32, height);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            boolean pressed = getModel().isArmed() && getModel().isPressed();
            boolean enabled = isEnabled();
            Color fill = null;
            Color stroke = null;
            Color fg;
            switch (kind) {
                case PRIMARY -> {
                    fill = !enabled ? p.borderStrong() : pressed ? p.accentPressed() : hover ? p.accentHover() : p.accent();
                    fg = enabled ? p.accentText() : p.textTertiary();
                }
                case SECONDARY -> {
                    fill = pressed ? Theme.mix(p.surfaceAlt(), p.text(), 0.08)
                            : hover ? Theme.mix(p.surfaceAlt(), p.text(), 0.04) : p.surfaceAlt();
                    stroke = p.border();
                    fg = enabled ? p.text() : p.textTertiary();
                }
                default -> {
                    if (hover || pressed) {
                        fill = Theme.alpha(p.text(), pressed ? 26 : 14);
                    }
                    fg = enabled ? p.text() : p.textTertiary();
                }
            }
            double w = getWidth();
            double h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 9, 9);
            if (fill != null) {
                g2.setColor(fill);
                g2.fill(shape);
            }
            if (stroke != null) {
                g2.setColor(stroke);
                g2.draw(shape);
            }
            if (isFocusOwner()) {
                g2.setColor(p.accent());
                g2.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 9, 9));
            }

            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = getText() == null ? "" : getText();
            Icon icon = getIcon();
            int textWidth = fm.stringWidth(text);
            int iconWidth = icon == null ? 0 : icon.getIconWidth();
            int gap = icon != null && !text.isEmpty() ? getIconTextGap() : 0;
            int x = (int) Math.round((w - textWidth - iconWidth - gap) / 2);
            if (icon != null) {
                icon.paintIcon(this, g2, x, (int) Math.round((h - icon.getIconHeight()) / 2));
                x += iconWidth + gap;
            }
            g2.setFont(getFont());
            g2.setColor(fg);
            g2.drawString(text, x, (int) Math.round((h - fm.getHeight()) / 2 + fm.getAscent()));
        } finally {
            g2.dispose();
        }
    }
}
