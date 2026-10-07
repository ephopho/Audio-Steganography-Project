package com.phopho.audiosteganography.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.JTextComponent;

/** Text inputs styled as Fluent text boxes: rounded, with placeholders and a focus accent. */
final class Fields {

    private Fields() {
    }

    /** A multi-line text area with a placeholder. */
    static JTextArea textArea(String placeholder) {
        JTextArea area = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPlaceholder(this, g, placeholder);
            }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.content(14));
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return styled(area);
    }

    /** A password field with a placeholder, shown as dots. */
    static JPasswordField password(String placeholder) {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPlaceholder(this, g, placeholder);
            }
        };
        field.setEchoChar('•');
        field.setFont(Theme.content(14));
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));
        return styled(field);
    }

    /** An eye button that shows or hides the given password field's text. */
    static JButton revealToggle(JPasswordField field) {
        Button eye = new Button("", Icons.of(Icons.Glyph.EYE, 18, Theme.Palette::textSecondary), Button.Kind.GHOST);
        eye.setPreferredSize(new Dimension(32, 30));
        eye.setToolTipText("Show password");
        eye.getAccessibleContext().setAccessibleName("Show password");
        eye.addActionListener(e -> {
            boolean hidden = field.getEchoChar() != 0;
            field.setEchoChar(hidden ? 0 : '•');
            Icons.Glyph glyph = hidden ? Icons.Glyph.EYE_OFF : Icons.Glyph.EYE;
            eye.setIcon(Icons.of(glyph, 18, Theme.Palette::textSecondary));
            eye.setToolTipText(hidden ? "Hide password" : "Show password");
            field.requestFocusInWindow();
        });
        return eye;
    }

    /** A borderless, transparent scroll pane with thin rounded scrollbars. */
    static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        thin(scroll.getVerticalScrollBar());
        return scroll;
    }

    static void thin(JScrollBar bar) {
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 10));
        bar.setUnitIncrement(16);
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                // No track: just the thumb, like modern overlay scrollbars.
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) {
                    return;
                }
                Graphics2D g2 = Ui.g2(g);
                try {
                    g2.setColor(Theme.alpha(Theme.p().text(), isThumbRollover() ? 110 : 60));
                    g2.fill(new RoundRectangle2D.Double(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 4, 4));
                } finally {
                    g2.dispose();
                }
            }
        });
    }

    private static JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    private static <T extends JTextComponent> T styled(T field) {
        return Theme.bind(field, f -> {
            Theme.Palette p = Theme.p();
            f.setForeground(p.text());
            f.setCaretColor(p.text());
            f.setSelectionColor(p.selection());
            f.setSelectedTextColor(p.text());
            f.setDisabledTextColor(p.textTertiary());
        });
    }

    private static void paintPlaceholder(JTextComponent field, Graphics g, String placeholder) {
        if (placeholder == null || field.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = Ui.g2(g);
        try {
            Insets in = field.getInsets();
            g2.setFont(field.getFont());
            g2.setColor(Theme.p().textTertiary());
            FontMetrics fm = g2.getFontMetrics();
            int y = field instanceof JTextArea
                    ? in.top + fm.getAscent()
                    : (field.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, in.left, y);
        } finally {
            g2.dispose();
        }
    }

    /**
     * The rounded box drawn around a text input; turns accent-coloured while
     * anything inside it has focus.
     */
    static final class Box extends JPanel {
        private final Function<Theme.Palette, Color> fill;
        private boolean focused;

        Box(JComponent field, JComponent trailing, int height) {
            this(field, trailing, height, Theme.Palette::input);
        }

        Box(JComponent field, JComponent trailing, int height, Function<Theme.Palette, Color> fill) {
            super(new BorderLayout());
            this.fill = fill;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, trailing == null ? 2 : 4));
            add(field, BorderLayout.CENTER);
            if (trailing != null) {
                add(trailing, BorderLayout.EAST);
            }
            if (height > 0) {
                setPreferredSize(new Dimension(200, height));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            }
            JComponent focusTarget = field instanceof JScrollPane sp ? (JComponent) sp.getViewport().getView() : field;
            focusTarget.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme.Palette p = Theme.p();
            Graphics2D g2 = Ui.g2(g);
            try {
                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 10, 10);
                g2.setColor(fill.apply(p));
                g2.fill(shape);
                g2.setColor(focused ? p.accent() : p.borderStrong());
                g2.draw(shape);
                if (focused) {
                    // Fluent text boxes underline the focused field with a thicker accent stroke.
                    g2.setClip(0, h - 2, w, 2);
                    g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 10, 10));
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
