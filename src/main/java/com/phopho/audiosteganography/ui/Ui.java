package com.phopho.audiosteganography.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Small helpers shared by the UI classes. */
final class Ui {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Ui.class);
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    private Ui() {
    }

    /** A Graphics2D copy with antialiasing and the desktop's text rendering. Dispose it after use. */
    static Graphics2D g2(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hints instanceof Map<?, ?> map) {
            map.forEach((k, v) -> g2.setRenderingHint((RenderingHints.Key) k, v));
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        return g2;
    }

    /** A themed label. */
    static JLabel label(String text, Font font, Function<Theme.Palette, Color> color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return Theme.bind(label, l -> l.setForeground(color.apply(Theme.p())));
    }

    static JPanel clear(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static <T extends JComponent> T pad(T component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return component;
    }

    /** Run {@code work} off the UI thread, then hand its result or failure back on it. */
    static <T> void background(Callable<T> work, Consumer<T> done, Consumer<Throwable> failed) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    done.accept(get());
                } catch (ExecutionException e) {
                    failed.accept(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed.accept(e);
                }
            }
        }.execute();
    }

    /** Native open dialog; returns {@code null} if cancelled. */
    static Path chooseOpen(Component parent, String title, String filter) {
        FileDialog dialog = new FileDialog(frameOf(parent), title, FileDialog.LOAD);
        dialog.setDirectory(lastDir());
        if (filter != null) {
            if (WINDOWS) {
                dialog.setFile(filter); // Windows ignores FilenameFilter but treats "*.wav" as a filter
            }
            dialog.setFilenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(filter.substring(1)));
        }
        dialog.setVisible(true);
        return result(dialog);
    }

    /** Native save dialog (it asks before replacing a file); returns {@code null} if cancelled. */
    static Path chooseSave(Component parent, String title, Path suggested) {
        FileDialog dialog = new FileDialog(frameOf(parent), title, FileDialog.SAVE);
        Path dir = suggested.getParent();
        dialog.setDirectory(dir != null ? dir.toString() : lastDir());
        dialog.setFile(suggested.getFileName().toString());
        dialog.setVisible(true);
        return result(dialog);
    }

    private static Path result(FileDialog dialog) {
        if (dialog.getFile() == null || dialog.getDirectory() == null) {
            return null;
        }
        PREFS.put("lastDir", dialog.getDirectory());
        return Path.of(dialog.getDirectory(), dialog.getFile());
    }

    static String lastDir() {
        String dir = PREFS.get("lastDir", System.getProperty("user.home"));
        return Files.isDirectory(Path.of(dir)) ? dir : System.getProperty("user.home");
    }

    static void rememberDir(Path file) {
        Path dir = file.toAbsolutePath().getParent();
        if (dir != null) {
            PREFS.put("lastDir", dir.toString());
        }
    }

    private static Frame frameOf(Component c) {
        Window w = c == null ? null : SwingUtilities.getWindowAncestor(c);
        return w instanceof Frame f ? f : null;
    }

    static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /** Open the file's folder with the file selected (Windows), or just the folder elsewhere. */
    static void showInFolder(Path file) {
        try {
            if (WINDOWS) {
                new ProcessBuilder("explorer.exe", "/select," + file.toAbsolutePath()).start();
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.toAbsolutePath().getParent().toFile());
            }
        } catch (IOException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
