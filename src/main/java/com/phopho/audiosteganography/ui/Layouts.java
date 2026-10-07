package com.phopho.audiosteganography.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Layout and wiring helpers shared by the Hide and Reveal screens. */
final class Layouts {

    static final int CARD_GAP = 18;

    private Layouts() {
    }

    /** Stack cards top to bottom; the card at index {@code grow} takes spare height (-1: none does). */
    static JPanel column(int grow, Component... cards) {
        JPanel col = Ui.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        for (int i = 0; i < cards.length; i++) {
            c.gridy = i;
            c.weighty = i == grow ? 1 : 0;
            c.insets = new Insets(i == 0 ? 0 : CARD_GAP, 0, 0, 0);
            col.add(cards[i], c);
        }
        if (grow < 0) {
            c.gridy = cards.length;
            c.weighty = 1;
            c.insets = new Insets(0, 0, 0, 0);
            col.add(Box.createGlue(), c);
        }
        return col;
    }

    /**
     * Wrap {@code component} with empty space above/below that appears and
     * disappears together with it (for notices that are often hidden).
     */
    static JComponent spaced(JComponent component, int above, int below) {
        JPanel wrap = Ui.clear(new BorderLayout());
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setBorder(BorderFactory.createEmptyBorder(above, 0, below, 0));
        wrap.add(component, BorderLayout.CENTER);
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                wrap.setVisible(true);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                wrap.setVisible(false);
            }
        });
        wrap.setVisible(component.isVisible());
        return wrap;
    }

    static JLabel fieldLabel(String text) {
        return left(Ui.label(text, Theme.semibold(12.5f), Theme.Palette::textSecondary));
    }

    static <T extends JComponent> T left(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    static DocumentListener onEdit(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
    }
}
