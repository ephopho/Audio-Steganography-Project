package com.phopho.audiosteganography.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.View;

/**
 * Read-only text that wraps to whatever width the layout gives it and reports
 * the matching height. A plain JTextArea reports its height for a width it
 * doesn't know yet on the first layout pass, which stretches the surrounding
 * cards; this one measures for its real (or best-guess) width and re-lays out
 * when the width changes.
 */
final class WrapText extends JTextArea {

    private int laidOutWidth = -1;

    WrapText(float fontSize) {
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setHighlighter(null);
        setFont(Theme.font(fontSize));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, heightFor(currentWidth()));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(60, heightFor(currentWidth()));
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, heightFor(currentWidth()));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width != laidOutWidth) {
            laidOutWidth = width;
            SwingUtilities.invokeLater(this::revalidate);
        }
    }

    private int currentWidth() {
        if (getWidth() > 0) {
            return getWidth();
        }
        Container parent = getParent();
        if (parent != null && parent.getWidth() > 0) {
            Insets in = parent.getInsets();
            return Math.max(60, parent.getWidth() - in.left - in.right - 40);
        }
        return 320;
    }

    private int heightFor(int width) {
        Insets in = getInsets();
        View root = getUI().getRootView(this);
        root.setSize(Math.max(1, width - in.left - in.right), Integer.MAX_VALUE);
        return (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)) + in.top + in.bottom;
    }
}
