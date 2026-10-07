package com.phopho.audiosteganography.ui;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.function.Consumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/** A dashed target that takes a dropped file, or opens a file dialog when clicked. */
final class DropZone extends JComponent {

    private final Icons.Glyph glyph;
    private final String title;
    private final String subtitle;
    private boolean hover;
    private boolean dragging;

    DropZone(Icons.Glyph glyph, String title, String subtitle, int height, Runnable browse, Consumer<Path> dropped) {
        this.glyph = glyph;
        this.title = title;
        this.subtitle = subtitle;
        setPreferredSize(new Dimension(320, height));
        setMinimumSize(new Dimension(200, height));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(title + ", " + subtitle);
        setTransferHandler(fileDropHandler(dropped, active -> {
            dragging = active;
            repaint();
        }));
        try {
            getDropTarget().addDropTargetListener(new DropTargetAdapter() {
                @Override
                public void dragExit(DropTargetEvent e) {
                    dragEnded();
                }

                @Override
                public void drop(DropTargetDropEvent e) {
                    dragEnded();
                }
            });
        } catch (TooManyListenersException e) {
            // Only the highlight is lost; dropping still works.
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                browse.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    browse.run();
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
                    return AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }

    /**
     * A handler accepting one dropped file, usable on any component.
     *
     * @param highlight told {@code true} while a file is dragged over, {@code false} after
     */
    static TransferHandler fileDropHandler(Consumer<Path> dropped, Consumer<Boolean> highlight) {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                boolean ok = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                if (ok && support.isDrop()) {
                    support.setDropAction(COPY);
                    highlight.accept(true);
                }
                return ok;
            }

            @Override
            public boolean importData(TransferSupport support) {
                highlight.accept(false);
                try {
                    Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (data instanceof List<?> files && !files.isEmpty() && files.get(0) instanceof File file) {
                        dropped.accept(file.toPath());
                        return true;
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    return false;
                }
                return false;
            }
        };
    }

    private void dragEnded() {
        dragging = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            boolean active = dragging || hover || isFocusOwner();
            RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            g2.setColor(dragging ? p.accentSubtle() : active ? Theme.mix(p.surfaceAlt(), p.accent(), 0.04) : p.surfaceAlt());
            g2.fill(shape);
            g2.setColor(active ? p.accent() : p.borderStrong());
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[] {6, 5}, 0));
            g2.draw(shape);

            int iconSize = 28;
            g2.setFont(Theme.semibold(14));
            FontMetrics titleFm = g2.getFontMetrics();
            FontMetrics subFm = g2.getFontMetrics(Theme.font(12.5f));
            int block = iconSize + 10 + titleFm.getHeight() + 2 + subFm.getHeight();
            int y = (getHeight() - block) / 2;
            Icons.paint(g2, glyph, (getWidth() - iconSize) / 2, y, iconSize, active ? p.accent() : p.textSecondary());
            y += iconSize + 10;
            g2.setColor(p.text());
            g2.drawString(title, (getWidth() - titleFm.stringWidth(title)) / 2f, y + titleFm.getAscent());
            y += titleFm.getHeight() + 2;
            g2.setFont(Theme.font(12.5f));
            g2.setColor(p.textSecondary());
            g2.drawString(subtitle, (getWidth() - subFm.stringWidth(subtitle)) / 2f, y + subFm.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
