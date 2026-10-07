package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.Describe;
import com.phopho.audiosteganography.engine.Stego;
import com.phopho.audiosteganography.engine.WavFile;
import java.awt.BorderLayout;
import java.nio.file.Path;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A loaded WAV: name, format, waveform, play button and a caption line. */
final class AudioSummary extends JPanel {

    private final JLabel name = Ui.label(" ", Theme.semibold(14.5f), Theme.Palette::text);
    private final JLabel meta = Ui.label(" ", Theme.font(12.5f), Theme.Palette::textSecondary);
    private final JLabel caption = Ui.label(" ", Theme.font(12.5f), Theme.Palette::textSecondary);
    private final WaveformView waveform = new WaveformView(76);
    private final PlayControl play;
    private int generation;

    AudioSummary(AudioPlayer player, String playLabel, Runnable change, Runnable playbackFailed) {
        super(new BorderLayout(0, 14));
        setOpaque(false);
        play = new PlayControl(player, playLabel, playbackFailed);
        play.setWaveform(waveform);

        JPanel top = Ui.clear(new BorderLayout(12, 0));
        top.add(new IconTile(Icons.Glyph.MUSIC), BorderLayout.WEST);
        JPanel text = Ui.clear(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalStrut(3));
        text.add(name);
        text.add(Box.createVerticalStrut(3));
        text.add(meta);
        top.add(text, BorderLayout.CENTER);
        Button changeButton = new Button("Change", Button.Kind.GHOST);
        changeButton.getAccessibleContext().setAccessibleName("Choose a different audio file");
        changeButton.addActionListener(e -> change.run());
        top.add(changeButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        add(waveform, BorderLayout.CENTER);

        JPanel bottom = Ui.clear(new BorderLayout(12, 0));
        bottom.add(play.button(), BorderLayout.WEST);
        caption.setHorizontalAlignment(JLabel.RIGHT);
        bottom.add(caption, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    void show(Path path, byte[] bytes, Stego.Inspection inspection) {
        WavFile wav = inspection.wav();
        name.setText(path.getFileName().toString());
        name.setToolTipText(path.toAbsolutePath().toString());
        meta.setText(Describe.audio(wav, "  ·  ") + "  ·  " + Stego.formatBytes(bytes.length));
        play.setSource(bytes, wav);
        waveform.setPeaks(null);
        if (inspection.embeddable()) {
            int ticket = ++generation;
            Ui.background(() -> WaveformView.peaks(bytes, wav), peaks -> {
                if (ticket == generation) {
                    waveform.setPeaks(peaks);
                }
            }, error -> { });
        }
    }

    void setCaption(String text) {
        caption.setText(text == null || text.isEmpty() ? " " : text);
    }
}
