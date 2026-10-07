package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.FileIO;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/** The single app window: header (brand, Hide/Reveal switch, theme, about), the two screens, footer. */
final class MainWindow extends JFrame {

    private final AudioPlayer player = new AudioPlayer();
    private final HidePanel hide = new HidePanel(player);
    private final RevealPanel reveal = new RevealPanel(player);
    private final Segmented tabs = new Segmented(true, "Hide", "Reveal");
    private final CardLayout pages = new CardLayout();
    private AboutDialog about;

    MainWindow() {
        super("Audio Steganography");
        setIconImages(AppIcon.windowIcons());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                player.stop();
            }
        });

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.p().bg());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.add(header(), BorderLayout.NORTH);

        JPanel content = Ui.clear(pages);
        Ui.pad(content, 6, 24, 0, 24);
        content.add(hide, "hide");
        content.add(reveal, "reveal");
        root.add(content, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);

        tabs.onChange(i -> {
            player.stop();
            pages.show(content, i == 0 ? "hide" : "reveal");
        });

        // Dropping a WAV anywhere that isn't a text box loads it into the current screen.
        root.setTransferHandler(DropZone.fileDropHandler(this::loadIntoCurrent, active -> { }));
        bindKeys(root);

        setContentPane(root);
        setMinimumSize(new Dimension(960, 720));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1120, screen.width - 80), Math.min(800, screen.height - 80));
        setLocationRelativeTo(null);
        Theme.onChange(root::repaint);
    }

    private JComponent header() {
        JPanel brand = Ui.clear(null);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new JLabel(AppIcon.icon(34)));
        brand.add(Box.createHorizontalStrut(12));
        JPanel titles = Ui.clear(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.label("Audio Steganography", Theme.semibold(16), Theme.Palette::text));
        titles.add(Ui.label("Hide encrypted messages and files inside WAV audio", Theme.font(12.5f),
                Theme.Palette::textSecondary));
        brand.add(titles);

        Button themeButton = new Button("", themeIcon(), Button.Kind.GHOST);
        themeButton.setPreferredSize(new Dimension(38, 38));
        themeButton.setToolTipText("Switch light / dark");
        themeButton.getAccessibleContext().setAccessibleName("Switch between light and dark theme");
        themeButton.addActionListener(e -> {
            Theme.toggle();
            themeButton.setIcon(themeIcon());
        });
        Button aboutButton = new Button("", Icons.of(Icons.Glyph.INFO, 20, Theme.Palette::textSecondary),
                Button.Kind.GHOST);
        aboutButton.setPreferredSize(new Dimension(38, 38));
        aboutButton.setToolTipText("About");
        aboutButton.getAccessibleContext().setAccessibleName("About this app");
        aboutButton.addActionListener(e -> {
            if (about == null) {
                about = new AboutDialog(this); // built once: its labels stay bound to the theme
            }
            about.setLocationRelativeTo(this);
            about.setVisible(true);
        });
        JPanel actions = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.add(themeButton);
        actions.add(aboutButton);

        // Brand left, actions right, the Hide/Reveal switch centred on the window.
        JPanel header = new JPanel(null) {
            @Override
            public void doLayout() {
                int h = getHeight();
                Dimension b = brand.getPreferredSize();
                Dimension a = actions.getPreferredSize();
                Dimension t = tabs.getPreferredSize();
                brand.setBounds(24, (h - b.height) / 2, b.width, b.height);
                actions.setBounds(getWidth() - 24 - a.width, (h - a.height) / 2, a.width, a.height);
                tabs.setBounds((getWidth() - t.width) / 2, (h - t.height) / 2, t.width, t.height);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(900, 84);
            }
        };
        header.setOpaque(false);
        header.add(brand);
        header.add(tabs);
        header.add(actions);
        return header;
    }

    private static Icon themeIcon() {
        return Icons.of(Theme.p().dark() ? Icons.Glyph.SUN : Icons.Glyph.MOON, 20, Theme.Palette::textSecondary);
    }

    private JComponent footer() {
        JPanel footer = Ui.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
        Ui.pad(footer, 14, 24, 16, 24);
        footer.add(Ui.label("Messages in 16-bit WAVs also open in Blank for iPhone  ·  scrypt + AES-256-GCM  ·  v"
                + FileIO.appVersion(), Theme.font(12), Theme.Palette::textTertiary));
        return footer;
    }

    private void bindKeys(JComponent root) {
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_O, menu), "open", () -> {
            if (tabs.selected() == 0) {
                hide.browse();
            } else {
                reveal.browse();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_1, menu), "tab-hide", () -> tabs.select(0));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_2, menu), "tab-reveal", () -> tabs.select(1));
    }

    private static void bind(JComponent root, KeyStroke key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void loadIntoCurrent(Path path) {
        if (tabs.selected() == 0) {
            hide.load(path);
        } else {
            reveal.load(path);
        }
    }

    /** Lets App focus something sensible once the window is up. */
    Component initialFocus() {
        return tabs;
    }
}
