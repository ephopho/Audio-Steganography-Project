package com.phopho.audiosteganography.engine;

/**
 * Least-significant-bit codec over integer PCM samples.
 *
 * <p>One hidden bit per sample, bytes written MSB-first, starting at the first
 * sample: the same bit order as the 2020 project and the Blank iOS app. Only
 * the lowest byte of each little-endian sample is touched, so every sample moves
 * by at most one step. The 2020 code wrote into every byte, which also hit the
 * high byte of 16-bit samples and changed them by 256 steps (audible hiss).
 */
final class Lsb {

    private Lsb() {
    }

    /** Hideable capacity in whole bytes: one bit per sample. */
    static long capacityBytes(WavFile wav) {
        return wav.sampleCount() / 8;
    }

    /**
     * Write {@code payload} into the samples of {@code audio} (mutated in place).
     *
     * @return how many samples actually changed value (about half of those used)
     */
    static int write(byte[] audio, WavFile wav, byte[] payload) {
        int stride = wav.bytesPerSample();
        int pos = wav.dataOffset();
        int changed = 0;
        for (byte b : payload) {
            for (int bit = 7; bit >= 0; bit--) {
                int value = (b >> bit) & 1;
                if ((audio[pos] & 1) != value) {
                    audio[pos] ^= 1;
                    changed++;
                }
                pos += stride;
            }
        }
        return changed;
    }

    /** Read {@code count} hidden bytes, starting {@code byteOffset} bytes into the stream. */
    static byte[] read(byte[] audio, WavFile wav, int byteOffset, int count) {
        int stride = wav.bytesPerSample();
        int pos = wav.dataOffset() + byteOffset * 8 * stride;
        byte[] out = new byte[count];
        for (int i = 0; i < count; i++) {
            int b = 0;
            for (int bit = 0; bit < 8; bit++) {
                b = (b << 1) | (audio[pos] & 1);
                pos += stride;
            }
            out[i] = (byte) b;
        }
        return out;
    }
}
