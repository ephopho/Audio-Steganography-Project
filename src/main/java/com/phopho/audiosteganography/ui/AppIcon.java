package com.phopho.audiosteganography.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Icon;

/**
 * The app icon, drawn in code: a blue tile with a waveform whose one highlighted
 * bar stands for the hidden bit. Build.java renders it into the native
 * launcher's .ico, so there are no image files to keep in sync.
 */
public final class AppIcon {

    private static final double[] BARS = {0.30, 0.55, 0.85, 0.62, 1.0, 0.70, 0.42, 0.26};
    private static final int HIDDEN_BAR = 4;

    private AppIcon() {
    }

    public static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double s = size;
            double inset = s * 0.04;
            double tile = s - inset * 2;
            g.setPaint(new GradientPaint(0, 0, new Color(0x2A8CE6), 0, (float) s, new Color(0x0052A3)));
            g.fill(new RoundRectangle2D.Double(inset, inset, tile, tile, tile * 0.44, tile * 0.44));

            double barWidth = tile * 0.075;
            double gap = (tile * 0.70 - barWidth * BARS.length) / (BARS.length - 1);
            double x = inset + tile * 0.15;
            double mid = s / 2;
            for (int i = 0; i < BARS.length; i++) {
                double h = Math.max(barWidth, tile * 0.56 * BARS[i]);
                g.setColor(i == HIDDEN_BAR ? new Color(0x9BE7FF) : Color.WHITE);
                g.fill(new RoundRectangle2D.Double(x, mid - h / 2, barWidth, h, barWidth, barWidth));
                x += barWidth + gap;
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /** An Icon that renders at the screen's real pixel density, so it stays sharp when scaled. */
    static Icon icon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                double scale = g instanceof Graphics2D gd ? gd.getTransform().getScaleX() : 1;
                int pixels = (int) Math.ceil(size * Math.max(1, scale));
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(render(pixels), x, y, size, size, null);
                } finally {
                    g2.dispose();
                }
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

    static List<Image> windowIcons() {
        return List.of(render(16), render(20), render(24), render(32), render(48), render(64), render(128), render(256));
    }
}
