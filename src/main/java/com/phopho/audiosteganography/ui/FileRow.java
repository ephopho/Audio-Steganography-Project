package com.phopho.audiosteganography.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A file shown as a tile: thumbnail, name, detail line and action buttons. */
final class FileRow extends JPanel {

    private final JLabel name = Ui.label(" ", Theme.semibold(14), Theme.Palette::text);
    private final JLabel detail = Ui.label(" ", Theme.font(12.5f), Theme.Palette::textSecondary);
    private final JPanel actions = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 6, 0));

    FileRow() {
        super(new BorderLayout(12, 0));
        setOpaque(false);
        Ui.pad(this, 12, 12, 12, 12);
        add(new IconTile(Icons.Glyph.FILE), BorderLayout.WEST);
        JPanel text = Ui.clear(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalStrut(3));
        text.add(name);
        text.add(Box.createVerticalStrut(3));
        text.add(detail);
        add(text, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
    }

    void show(String fileName, String details) {
        name.setText(fileName);
        name.setToolTipText(fileName);
        detail.setText(details);
    }

    void addAction(JComponent action) {
        actions.add(action);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(p.surfaceAlt());
            g2.fill(shape);
            g2.setColor(p.border());
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
    }
}
