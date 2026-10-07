package com.phopho.audiosteganography.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/** A segmented switch (like the iOS/Fluent pivot): click or arrow keys to change. */
final class Segmented extends JComponent {

    private final String[] options;
    private final boolean large;
    private final List<IntConsumer> listeners = new ArrayList<>();
    private int selected;
    private int hover = -1;

    Segmented(boolean large, String... options) {
        this.options = options;
        this.large = large;
        setFont(large ? Theme.semibold(13.5f) : Theme.semibold(12.5f));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(String.join(" / ", options));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                select(indexAt(e.getX()));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i != hover) {
                    hover = i;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    select(Math.max(0, selected - 1));
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    select(Math.min(options.length - 1, selected + 1));
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PAGE_TAB_LIST;
                }
            };
        }
        return accessibleContext;
    }

    void onChange(IntConsumer listener) {
        listeners.add(listener);
    }

    int selected() {
        return selected;
    }

    void select(int index) {
        if (index < 0 || index == selected) {
            return;
        }
        selected = index;
        repaint();
        listeners.forEach(l -> l.accept(index));
    }

    private int segmentWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        int widest = 0;
        for (String o : options) {
            widest = Math.max(widest, fm.stringWidth(o));
        }
        return widest + (large ? 44 : 28);
    }

    private int indexAt(int x) {
        int i = (x - 3) / segmentWidth();
        return i >= 0 && i < options.length ? i : -1;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(segmentWidth() * options.length + 6, large ? 38 : 30);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            int h = getPreferredSize().height;
            int y0 = (getHeight() - h) / 2;
            int seg = segmentWidth();
            double r = h - 4;
            RoundRectangle2D track = new RoundRectangle2D.Double(0.5, y0 + 0.5, seg * options.length + 5, h - 1, r, r);
            g2.setColor(p.dark() ? p.bg() : Theme.mix(p.surfaceAlt(), p.text(), 0.03));
            g2.fill(track);
            g2.setColor(isFocusOwner() ? p.accent() : p.border());
            g2.draw(track);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < options.length; i++) {
                double x = 3 + i * seg;
                RoundRectangle2D pill = new RoundRectangle2D.Double(x, y0 + 3, seg, h - 6, r - 6, r - 6);
                if (i == selected) {
                    g2.setColor(p.accent());
                    g2.fill(pill);
                } else if (i == hover) {
                    g2.setColor(Theme.alpha(p.text(), 14));
                    g2.fill(pill);
                }
                g2.setColor(i == selected ? p.accentText() : p.textSecondary());
                String text = options[i];
                g2.drawString(text, (float) (x + (seg - fm.stringWidth(text)) / 2.0),
                        y0 + (h - fm.getHeight()) / 2f + fm.getAscent());
            }
        } finally {
            g2.dispose();
        }
    }
}
