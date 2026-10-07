package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.FileIO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/** What the app does, how safe it is, and where it came from. */
final class AboutDialog extends JDialog {

    AboutDialog(JFrame owner) {
        super(owner, "About Audio Steganography", true);
        JPanel root = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.p().surface());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        Ui.pad(root, 24, 28, 22, 28);

        JPanel head = Ui.clear(new BorderLayout(16, 0));
        head.add(new JLabel(AppIcon.icon(56)), BorderLayout.WEST);
        JPanel titles = Ui.clear(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Box.createVerticalStrut(6));
        titles.add(Ui.label("Audio Steganography", Theme.semibold(18), Theme.Palette::text));
        titles.add(Box.createVerticalStrut(2));
        titles.add(Ui.label("Version " + FileIO.appVersion(), Theme.font(12.5f), Theme.Palette::textSecondary));
        head.add(titles, BorderLayout.CENTER);
        root.add(head, BorderLayout.NORTH);

        JPanel body = Ui.clear(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        section(body, "How it works",
                "Your secret is encrypted, then written into the lowest bit of the audio samples, one bit per sample. "
                + "Each sample changes by at most one step, which is far below what anyone can hear.");
        section(body, "Security",
                "Passwords are stretched with scrypt (N = 2^14, r = 8) and the secret is sealed with AES-256-GCM, so a "
                + "wrong password or a modified file is detected rather than producing garbage. The content is "
                + "protected; the fact that something is hidden can be detected by tools that know the format.");
        section(body, "Keep the WAV as it is",
                "Converting to MP3/AAC, trimming, normalising or sending through apps that re-encode audio destroys "
                + "the hidden data. Share the file as a document, zip it, or use cloud storage.");
        section(body, "Works with Blank",
                "Text messages in 16-bit WAVs use the same format as Blank for iPhone, so a message hidden in either "
                + "app opens in the other.");
        section(body, "Credits",
                "Based on the 2020 BSc Computer Engineering project “Design and Implementation of an Audio "
                + "Steganographic System for Data Transmission” by Kudjordji Emmanuel and Agyare Samuel Marrion "
                + "(Ghana Technology University College).");
        root.add(body, BorderLayout.CENTER);

        Button close = new Button("Close", Button.Kind.SECONDARY);
        close.addActionListener(e -> setVisible(false));
        JPanel actions = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        root.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setContentPane(root);
        getRootPane().setDefaultButton(close);
        setSize(new Dimension(560, 640));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static void section(JPanel body, String title, String text) {
        body.add(Layouts.left(Ui.label(title, Theme.semibold(13.5f), Theme.Palette::text)));
        body.add(Box.createVerticalStrut(4));
        WrapText paragraph = new WrapText(13);
        paragraph.setText(text);
        Theme.bind(paragraph, p -> p.setForeground(Theme.p().textSecondary()));
        body.add(Layouts.left(paragraph));
        body.add(Box.createVerticalStrut(14));
    }
}
