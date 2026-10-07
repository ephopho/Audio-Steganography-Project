package com.phopho.audiosteganography.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** A rounded surface with a (optionally numbered) title; content goes in {@link #body()}. */
final class Card extends JPanel {

    private final JPanel header;
    private final JPanel body;

    Card(String step, String title) {
        super(new BorderLayout(0, 14));
        setOpaque(false);
        Ui.pad(this, 18, 20, 20, 20);

        header = Ui.clear(null);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.add(Box.createRigidArea(new Dimension(0, 30))); // same height with or without a trailing control
        if (step != null) {
            header.add(new StepDot(step));
            header.add(Box.createHorizontalStrut(10));
        }
        header.add(Ui.label(title, Theme.semibold(15), Theme.Palette::text));
        header.add(Box.createHorizontalGlue());
        add(header, BorderLayout.NORTH);

        body = Ui.clear(new BorderLayout());
        add(body, BorderLayout.CENTER);
    }

    JPanel body() {
        return body;
    }

    /** Put a small control (e.g. a segmented switch) at the right of the title row. */
    void setTrailing(JComponent component) {
        header.add(component);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(p.surface());
            g2.fill(shape);
            g2.setColor(p.border());
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
    }

    /** The circled step number in the title row. */
    private static final class StepDot extends JComponent {
        private final String step;

        StepDot(String step) {
            this.step = step;
            setFont(Theme.semibold(12));
            Dimension d = new Dimension(24, 24);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme.Palette p = Theme.p();
            Graphics2D g2 = Ui.g2(g);
            try {
                g2.setColor(p.accentSubtle());
                g2.fill(new Ellipse2D.Double(0, 0, 24, 24));
                g2.setColor(p.accent());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(step, (24 - fm.stringWidth(step)) / 2f, (24 - fm.getHeight()) / 2f + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
