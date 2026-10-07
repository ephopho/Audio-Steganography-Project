package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.engine.WavFile;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.SwingUtilities;

/**
 * Plays a WAV held in memory. Samples are decoded with the engine's own parser
 * and sent to the sound card as 16-bit PCM, so every depth the engine supports
 * plays (javax.sound alone can't play 24/32-bit on most systems). One clip at a
 * time; starting another stops the first.
 */
final class AudioPlayer {

    private static final int CHUNK_FRAMES = 4096;

    /** One run of one clip. Each has its own stop flag so an old thread can't outlive its clip. */
    private static final class Playback {
        final Object owner;
        final SourceDataLine line;
        final long totalFrames;
        final Runnable onEnd;
        volatile boolean stopped;

        Playback(Object owner, SourceDataLine line, long totalFrames, Runnable onEnd) {
            this.owner = owner;
            this.line = line;
            this.totalFrames = totalFrames;
            this.onEnd = onEnd;
        }
    }

    private Playback current;

    /** Start playing; {@code onEnd} runs on the UI thread when playback ends or is stopped. */
    synchronized void play(Object owner, byte[] bytes, WavFile wav, Runnable onEnd, Runnable onError) {
        stop();
        int outChannels = Math.min(2, wav.channels());
        AudioFormat format = new AudioFormat(wav.sampleRate(), 16, outChannels, true, false);
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, (int) Math.min(Integer.MAX_VALUE, (long) format.getFrameSize() * wav.sampleRate() / 4));
        } catch (LineUnavailableException | IllegalArgumentException e) {
            SwingUtilities.invokeLater(onError);
            return;
        }
        Playback playback = new Playback(owner, line, wav.frameCount(), onEnd);
        current = playback;
        line.start();

        Thread thread = new Thread(() -> stream(playback, bytes, wav, outChannels), "audio-playback");
        thread.setDaemon(true);
        thread.start();
    }

    private void stream(Playback playback, byte[] bytes, WavFile wav, int outChannels) {
        int channels = wav.channels();
        long frames = wav.frameCount();
        byte[] chunk = new byte[CHUNK_FRAMES * outChannels * 2];
        try {
            for (long f = 0; f < frames && !playback.stopped; f += CHUNK_FRAMES) {
                int n = (int) Math.min(CHUNK_FRAMES, frames - f);
                int at = 0;
                for (int i = 0; i < n; i++) {
                    long base = (f + i) * channels;
                    for (int c = 0; c < outChannels; c++) {
                        float v;
                        if (channels > 2) {
                            // Fold surround layouts down to stereo: even channels left, odd right.
                            float sum = 0;
                            int count = 0;
                            for (int k = c; k < channels; k += 2) {
                                sum += wav.normalizedSample(bytes, base + k);
                                count++;
                            }
                            v = sum / count;
                        } else {
                            v = wav.normalizedSample(bytes, base + c);
                        }
                        int s = Math.max(-32768, Math.min(32767, Math.round(v * 32767)));
                        chunk[at++] = (byte) s;
                        chunk[at++] = (byte) (s >> 8);
                    }
                }
                playback.line.write(chunk, 0, at);
            }
            if (!playback.stopped) {
                playback.line.drain();
            }
        } finally {
            playback.line.close();
            ended(playback);
        }
    }

    private void ended(Playback playback) {
        synchronized (this) {
            if (current != playback) {
                return; // stopped explicitly; stop() already reported it
            }
            current = null;
        }
        if (playback.onEnd != null) {
            SwingUtilities.invokeLater(playback.onEnd);
        }
    }

    synchronized void stop() {
        Playback playback = current;
        if (playback == null) {
            return;
        }
        current = null;
        playback.stopped = true;
        playback.line.stop();
        playback.line.flush(); // unblocks a pending write; the playback thread then closes the line
        if (playback.onEnd != null) {
            SwingUtilities.invokeLater(playback.onEnd);
        }
    }

    synchronized boolean isPlaying(Object who) {
        return current != null && current.owner == who;
    }

    /** Fraction played (0..1), or -1 if {@code who} isn't playing. */
    synchronized double progress(Object who) {
        if (current == null || current.owner != who || current.totalFrames == 0) {
            return -1;
        }
        return Math.min(1, (double) current.line.getLongFramePosition() / current.totalFrames);
    }
}
