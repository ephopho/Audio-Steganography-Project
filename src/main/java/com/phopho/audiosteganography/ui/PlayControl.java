package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.engine.WavFile;
import javax.swing.Timer;

/** A Play/Stop button bound to one piece of audio, driving its waveform's playhead. */
final class PlayControl {

    private final AudioPlayer player;
    private final String label;
    private final Button button;
    private final Timer timer;
    private final Runnable onError;
    private WaveformView waveform;
    private byte[] bytes;
    private WavFile wav;

    PlayControl(AudioPlayer player, String label, Runnable onError) {
        this.player = player;
        this.label = label;
        this.onError = onError;
        this.button = new Button(label, Icons.of(Icons.Glyph.PLAY, 14, Theme.Palette::text), Button.Kind.SECONDARY);
        this.timer = new Timer(33, e -> tick());
        button.setEnabled(false);
        button.addActionListener(e -> toggle());
    }

    Button button() {
        return button;
    }

    void setWaveform(WaveformView waveform) {
        this.waveform = waveform;
    }

    /** Point at new audio (stopping the old one); {@code null} disables the button. */
    void setSource(byte[] bytes, WavFile wav) {
        if (player.isPlaying(this)) {
            player.stop();
        }
        this.bytes = bytes;
        this.wav = wav;
        button.setEnabled(bytes != null && wav != null && wav.unsupportedReason() == null);
    }

    private void toggle() {
        if (player.isPlaying(this)) {
            player.stop();
            return;
        }
        player.play(this, bytes, wav, this::ended, () -> {
            ended();
            onError.run();
        });
        if (player.isPlaying(this)) {
            button.setText("Stop");
            button.setIcon(Icons.of(Icons.Glyph.STOP, 14, Theme.Palette::text));
            timer.start();
        }
    }

    private void tick() {
        double progress = player.progress(this);
        if (progress < 0) {
            ended();
        } else if (waveform != null) {
            waveform.setProgress(progress);
        }
    }

    private void ended() {
        timer.stop();
        if (waveform != null) {
            waveform.setProgress(-1);
        }
        button.setText(label);
        button.setIcon(Icons.of(Icons.Glyph.PLAY, 14, Theme.Palette::text));
    }
}
