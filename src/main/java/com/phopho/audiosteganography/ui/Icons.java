package com.phopho.audiosteganography.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;
import javax.swing.Icon;

/**
 * Line icons drawn with Java2D on a 24-unit grid (no image files, crisp at any
 * scale). Colour is looked up at paint time so icons follow the theme.
 */
final class Icons {

    enum Glyph {
        UPLOAD, DOWNLOAD, PLAY, STOP, EYE, EYE_OFF, SUN, MOON, LOCK, UNLOCK, FILE, CHECK, ALERT, INFO, COPY,
        FOLDER, X, MUSIC, MESSAGE
    }

    private Icons() {
    }

    static Icon of(Glyph glyph, int size, Function<Theme.Palette, Color> color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Color tint = color.apply(Theme.p());
                if (c != null && !c.isEnabled()) {
                    tint = Theme.p().textTertiary();
                }
                paint(g, glyph, x, y, size, tint);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    static void paint(Graphics g, Glyph glyph, int x, int y, int size, Color color) {
        Graphics2D g2 = Ui.g2(g);
        try {
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2, glyph);
        } finally {
            g2.dispose();
        }
    }

    private static void draw(Graphics2D g, Glyph glyph) {
        switch (glyph) {
            case UPLOAD -> {
                tray(g);
                g.draw(new Line2D.Double(12, 15, 12, 3));
                g.draw(poly(7, 8, 12, 3, 17, 8));
            }
            case DOWNLOAD -> {
                tray(g);
                g.draw(new Line2D.Double(12, 3, 12, 15));
                g.draw(poly(7, 10, 12, 15, 17, 10));
            }
            case PLAY -> {
                Path2D p = poly(7, 4.5, 19, 12, 7, 19.5);
                p.closePath();
                g.fill(p);
                g.draw(p);
            }
            case STOP -> g.fill(new RoundRectangle2D.Double(6, 6, 12, 12, 3, 3));
            case EYE, EYE_OFF -> {
                Path2D eye = new Path2D.Double();
                eye.moveTo(2.5, 12);
                eye.curveTo(5.5, 6, 18.5, 6, 21.5, 12);
                eye.curveTo(18.5, 18, 5.5, 18, 2.5, 12);
                g.draw(eye);
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                if (glyph == Glyph.EYE_OFF) {
                    g.draw(new Line2D.Double(4, 4, 20, 20));
                }
            }
            case SUN -> {
                g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    g.draw(new Line2D.Double(12 + Math.cos(a) * 7, 12 + Math.sin(a) * 7,
                            12 + Math.cos(a) * 9.5, 12 + Math.sin(a) * 9.5));
                }
            }
            case MOON -> {
                Area moon = new Area(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                moon.subtract(new Area(new Ellipse2D.Double(9.5, -1.5, 15, 15)));
                g.draw(moon);
            }
            case LOCK, UNLOCK -> {
                g.draw(new RoundRectangle2D.Double(4.5, 11, 15, 10, 4, 4));
                if (glyph == Glyph.LOCK) {
                    g.draw(new Arc2D.Double(8, 3.5, 8, 9, 0, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Double(8, 8, 8, 11));
                    g.draw(new Line2D.Double(16, 8, 16, 11));
                } else {
                    g.draw(new Arc2D.Double(8, 3.5, 8, 9, 20, 160, Arc2D.OPEN));
                    g.draw(new Line2D.Double(8, 8, 8, 11));
                }
            }
            case FILE -> {
                Path2D f = new Path2D.Double();
                f.moveTo(14, 3);
                f.lineTo(7, 3);
                f.quadTo(5, 3, 5, 5);
                f.lineTo(5, 19);
                f.quadTo(5, 21, 7, 21);
                f.lineTo(17, 21);
                f.quadTo(19, 21, 19, 19);
                f.lineTo(19, 8);
                f.closePath();
                g.draw(f);
                g.draw(poly(14, 3, 14, 8, 19, 8));
            }
            case CHECK -> g.draw(poly(5, 12.5, 10, 17.5, 19.5, 7));
            case ALERT -> {
                Path2D t = poly(12, 3.5, 21.5, 20, 2.5, 20);
                t.closePath();
                g.draw(t);
                g.draw(new Line2D.Double(12, 9.5, 12, 13.5));
                g.fill(new Ellipse2D.Double(10.9, 15.9, 2.2, 2.2));
            }
            case INFO -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.draw(new Line2D.Double(12, 11, 12, 16.5));
                g.fill(new Ellipse2D.Double(10.9, 6.9, 2.2, 2.2));
            }
            case COPY -> {
                g.draw(new RoundRectangle2D.Double(8.5, 8.5, 12.5, 12.5, 4, 4));
                Path2D back = new Path2D.Double();
                back.moveTo(5, 15.5);
                back.quadTo(3, 15.5, 3, 13.5);
                back.lineTo(3, 5);
                back.quadTo(3, 3, 5, 3);
                back.lineTo(13.5, 3);
                back.quadTo(15.5, 3, 15.5, 5);
                g.draw(back);
            }
            case FOLDER -> {
                Path2D f = new Path2D.Double();
                f.moveTo(3, 7);
                f.quadTo(3, 5, 5, 5);
                f.lineTo(9.5, 5);
                f.lineTo(11.5, 7.5);
                f.lineTo(19, 7.5);
                f.quadTo(21, 7.5, 21, 9.5);
                f.lineTo(21, 17.5);
                f.quadTo(21, 19.5, 19, 19.5);
                f.lineTo(5, 19.5);
                f.quadTo(3, 19.5, 3, 17.5);
                f.closePath();
                g.draw(f);
            }
            case X -> {
                g.draw(new Line2D.Double(6, 6, 18, 18));
                g.draw(new Line2D.Double(18, 6, 6, 18));
            }
            case MUSIC -> {
                g.draw(new Ellipse2D.Double(3.5, 15, 5.5, 5));
                g.draw(new Ellipse2D.Double(14.5, 13, 5.5, 5));
                g.draw(poly(9, 17.5, 9, 5, 20, 3, 20, 15.5));
            }
            case MESSAGE -> {
                Path2D m = new Path2D.Double();
                m.moveTo(21, 15);
                m.quadTo(21, 17, 19, 17);
                m.lineTo(8, 17);
                m.lineTo(4, 21);
                m.lineTo(4, 6);
                m.quadTo(4, 4, 6, 4);
                m.lineTo(19, 4);
                m.quadTo(21, 4, 21, 6);
                m.closePath();
                g.draw(m);
                g.draw(new Line2D.Double(8.5, 8.5, 16.5, 8.5));
                g.draw(new Line2D.Double(8.5, 12.5, 13.5, 12.5));
            }
            default -> throw new IllegalStateException("no drawing for " + glyph);
        }
    }

    private static void tray(Graphics2D g) {
        Path2D tray = new Path2D.Double();
        tray.moveTo(3, 15);
        tray.lineTo(3, 19);
        tray.quadTo(3, 21, 5, 21);
        tray.lineTo(19, 21);
        tray.quadTo(21, 21, 21, 19);
        tray.lineTo(21, 15);
        g.draw(tray);
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }
}
