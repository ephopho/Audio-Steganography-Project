package com.phopho.audiosteganography.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** A tinted, rounded message line: info, success, warning or error. Hidden while empty. */
final class Notice extends JPanel {

    enum Tone { NEUTRAL, INFO, SUCCESS, WARNING, DANGER }

    private final JLabel icon = new JLabel();
    private final WrapText text = new WrapText(13);
    private Tone tone = Tone.NEUTRAL;

    Notice() {
        super(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        icon.setVerticalAlignment(SwingConstants.TOP);
        add(icon, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        setVisible(false);
        Theme.onChange(this::restyle);
    }

    void show(Tone tone, String message) {
        this.tone = tone;
        Icons.Glyph glyph = switch (tone) {
            case SUCCESS -> Icons.Glyph.CHECK;
            case WARNING, DANGER -> Icons.Glyph.ALERT;
            default -> Icons.Glyph.INFO;
        };
        icon.setIcon(Icons.of(glyph, 18, p -> foreground(this.tone, p)));
        text.setText(message);
        restyle();
        setVisible(true);
        revalidate();
        repaint();
    }

    void clear() {
        setVisible(false);
        text.setText("");
    }

    private void restyle() {
        text.setForeground(foreground(tone, Theme.p()));
    }

    private static Color foreground(Tone tone, Theme.Palette p) {
        return switch (tone) {
            case INFO -> p.accent();
            case SUCCESS -> p.success();
            case WARNING -> p.warning();
            case DANGER -> p.danger();
            default -> p.textSecondary();
        };
    }

    private static Color background(Tone tone, Theme.Palette p) {
        return switch (tone) {
            case INFO -> p.accentSubtle();
            case SUCCESS -> p.successSubtle();
            case WARNING -> p.warningSubtle();
            case DANGER -> p.dangerSubtle();
            default -> p.surfaceAlt();
        };
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.g2(g);
        try {
            g2.setColor(background(tone, Theme.p()));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
        } finally {
            g2.dispose();
        }
    }
}
