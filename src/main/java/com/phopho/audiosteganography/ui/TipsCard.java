package com.phopho.audiosteganography.ui;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** A short "Good to know" list: icon + one or two sentences per tip. */
final class TipsCard {

    record Tip(Icons.Glyph glyph, String text) {
    }

    private TipsCard() {
    }

    static Card of(Tip... tips) {
        Card card = new Card(null, "Good to know");
        JPanel list = Ui.clear(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (int i = 0; i < tips.length; i++) {
            JPanel row = Ui.clear(new BorderLayout(12, 0));
            JLabel icon = new JLabel(Icons.of(tips[i].glyph(), 18, Theme.Palette::accent));
            icon.setVerticalAlignment(SwingConstants.TOP);
            row.add(icon, BorderLayout.WEST);
            WrapText text = new WrapText(13);
            text.setText(tips[i].text());
            Theme.bind(text, t -> t.setForeground(Theme.p().textSecondary()));
            row.add(text, BorderLayout.CENTER);
            list.add(Layouts.left(row));
            if (i < tips.length - 1) {
                list.add(Box.createVerticalStrut(12));
            }
        }
        card.body().add(list, BorderLayout.CENTER);
        return card;
    }
}
