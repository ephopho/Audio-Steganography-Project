package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.engine.WavFile;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A bar waveform of the audio, with the played part highlighted while it plays. */
final class WaveformView extends JComponent {

    private static final int BUCKETS = 720;
    private float[] peaks;
    private double progress = -1;

    WaveformView(int height) {
        setPreferredSize(new Dimension(300, height));
        setMinimumSize(new Dimension(100, height));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    /**
     * Loudest sample per slice of the file, scaled so the loudest slice is 1.
     * Long files are sampled sparsely to stay fast; this is a picture, not a meter.
     */
    static float[] peaks(byte[] bytes, WavFile wav) {
        float[] out = new float[BUCKETS];
        long frames = wav.frameCount();
        if (frames == 0) {
            return out;
        }
        int channels = wav.channels();
        long perBucket = Math.max(1, frames / BUCKETS);
        long step = Math.max(1, perBucket / 2048);
        float loudest = 0;
        for (int b = 0; b < BUCKETS; b++) {
            long start = frames * b / BUCKETS;
            long end = Math.min(frames, start + perBucket);
            float peak = 0;
            for (long f = start; f < end; f += step) {
                for (int c = 0; c < channels; c++) {
                    peak = Math.max(peak, Math.abs(wav.normalizedSample(bytes, f * channels + c)));
                }
            }
            out[b] = peak;
            loudest = Math.max(loudest, peak);
        }
        if (loudest > 0) {
            for (int b = 0; b < BUCKETS; b++) {
                out[b] /= loudest;
            }
        }
        return out;
    }

    void setPeaks(float[] peaks) {
        this.peaks = peaks;
        repaint();
    }

    /** 0..1 while playing, negative when stopped. */
    void setProgress(double progress) {
        this.progress = progress;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.Palette p = Theme.p();
        Graphics2D g2 = Ui.g2(g);
        try {
            int w = getWidth();
            int h = getHeight();
            double mid = h / 2.0;
            if (peaks == null) {
                g2.setColor(p.border());
                g2.fill(new RoundRectangle2D.Double(0, mid - 1, w, 2, 2, 2));
                return;
            }
            double barWidth = 2.5;
            double pitch = 4;
            int bars = Math.max(1, (int) (w / pitch));
            for (int i = 0; i < bars; i++) {
                int from = i * peaks.length / bars;
                int to = Math.max(from + 1, (i + 1) * peaks.length / bars);
                float peak = 0;
                for (int k = from; k < to && k < peaks.length; k++) {
                    peak = Math.max(peak, peaks[k]);
                }
                double barHeight = Math.max(2.5, peak * (h - 4));
                boolean played = progress >= 0 && (double) i / bars <= progress;
                g2.setColor(played ? p.accent()
                        : progress >= 0 ? Theme.alpha(p.textSecondary(), 80) : Theme.alpha(p.accent(), p.dark() ? 150 : 120));
                g2.fill(new RoundRectangle2D.Double(i * pitch, mid - barHeight / 2, barWidth, barHeight, barWidth, barWidth));
            }
        } finally {
            g2.dispose();
        }
    }
}
