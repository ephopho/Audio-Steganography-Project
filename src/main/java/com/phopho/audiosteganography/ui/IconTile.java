package com.phopho.audiosteganography.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A glyph on a soft accent-tinted tile, used as a file's thumbnail. */
final class IconTile extends JComponent {

    private Icons.Glyph glyph;

    IconTile(Icons.Glyph glyph) {
        this.glyph = glyph;
        Dimension d = new Dimension(44, 44);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    void setGlyph(Icons.Glyph glyph) {
        this.glyph = glyph;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            g2.setColor(p.accentSubtle());
            g2.fill(new RoundRectangle2D.Double(0, 0, 44, 44, 12, 12));
        } finally {
            g2.dispose();
        }
        Icons.paint(g, glyph, 11, 11, 22, p.accent());
    }
}
