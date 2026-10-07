package com.phopho.audiosteganography;

import com.phopho.audiosteganography.engine.WavFile;
import java.util.Locale;

/** Plain-language descriptions of audio, shared by the CLI and the desktop app. */
public final class Describe {

    private Describe() {
    }

    /** "44.1 kHz, 16-bit, stereo, 0:10" (separator chosen by the caller). */
    public static String audio(WavFile wav, String separator) {
        return String.join(separator, hz(wav.sampleRate()), wav.bitsPerSample() + "-bit",
                channels(wav.channels()), duration(wav.durationSeconds()));
    }

    public static String hz(int rate) {
        return rate % 1000 == 0 ? rate / 1000 + " kHz" : String.format(Locale.ROOT, "%.1f kHz", rate / 1000.0);
    }

    public static String channels(int n) {
        return switch (n) {
            case 1 -> "mono";
            case 2 -> "stereo";
            default -> n + " channels";
        };
    }

    public static String duration(double seconds) {
        long s = Math.round(seconds);
        return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }
}
