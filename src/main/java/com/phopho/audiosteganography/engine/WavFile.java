package com.phopho.audiosteganography.engine;

import com.phopho.audiosteganography.engine.StegoException.Code;

/**
 * Layout of a RIFF/WAVE file, read straight from its bytes.
 *
 * <p>The 2020 version went through {@code javax.sound}, read into a fixed
 * 154,600-frame buffer and rewrote the header, which truncated and scrambled any
 * longer file. Here the chunk list is walked by hand (files can carry LIST, fact
 * or bext chunks before the audio) and every byte outside the samples' low bits
 * is left exactly as it was.
 *
 * @param format        effective format code: 1 = integer PCM, 3 = float, etc.
 *                      For WAVE_FORMAT_EXTENSIBLE files this is the sub-format.
 * @param extensible    whether the header used WAVE_FORMAT_EXTENSIBLE (0xFFFE)
 * @param channels      channel count
 * @param sampleRate    frames per second
 * @param bitsPerSample container bits per sample (8, 16, 24 or 32 for PCM)
 * @param validBits     significant bits per sample (equals bitsPerSample unless
 *                      an extensible header says otherwise)
 * @param dataOffset    byte offset of the first sample within the file
 * @param dataLength    byte length of the sample data, clamped to what the file
 *                      actually holds (some files claim more than they contain)
 */
public record WavFile(
        int format,
        boolean extensible,
        int channels,
        int sampleRate,
        int bitsPerSample,
        int validBits,
        int dataOffset,
        int dataLength) {

    public static final int FORMAT_PCM = 1;
    public static final int FORMAT_FLOAT = 3;
    private static final int FORMAT_EXTENSIBLE = 0xFFFE;

    /** Parse the header of a WAV file held in memory. */
    public static WavFile parse(byte[] bytes) throws StegoException {
        if (bytes.length < 12 || !fourCC(bytes, 0, "RIFF") || !fourCC(bytes, 8, "WAVE")) {
            throw new StegoException(Code.BAD_WAV, "This file is not a WAV audio file.");
        }

        int format = 0;
        boolean extensible = false;
        int channels = 0;
        int sampleRate = 0;
        int bits = 0;
        int validBits = 0;
        boolean haveFmt = false;
        int dataOffset = -1;
        int dataLength = 0;

        long offset = 12;
        while (offset + 8 <= bytes.length) {
            int at = (int) offset;
            long size = u32(bytes, at + 4);
            int body = at + 8;

            if (fourCC(bytes, at, "fmt ") && !haveFmt) {
                if (size < 16 || body + 16 > bytes.length) {
                    throw new StegoException(Code.BAD_WAV, "This WAV file has a damaged format header.");
                }
                format = u16(bytes, body);
                channels = u16(bytes, body + 2);
                sampleRate = (int) Math.min(Integer.MAX_VALUE, u32(bytes, body + 4));
                bits = u16(bytes, body + 14);
                validBits = bits;
                if (format == FORMAT_EXTENSIBLE && size >= 40 && body + 40 <= bytes.length) {
                    extensible = true;
                    int declaredValid = u16(bytes, body + 18);
                    if (declaredValid != 0) {
                        validBits = declaredValid;
                    }
                    // The first two bytes of the SubFormat GUID are the real format code.
                    format = u16(bytes, body + 24);
                }
                haveFmt = true;
            } else if (fourCC(bytes, at, "data") && dataOffset < 0) {
                dataOffset = body;
                dataLength = (int) Math.min(size, Math.max(0, bytes.length - body));
            }

            // RIFF chunks are word-aligned: an odd-sized chunk is followed by one pad byte.
            offset = body + size + (size & 1);
        }

        if (!haveFmt) {
            throw new StegoException(Code.BAD_WAV, "This WAV file is missing its format header.");
        }
        if (dataOffset < 0 || dataLength <= 0) {
            throw new StegoException(Code.BAD_WAV, "This WAV file contains no audio.");
        }
        return new WavFile(format, extensible, channels, sampleRate, bits, validBits, dataOffset, dataLength);
    }

    /**
     * Throws {@link Code#UNSUPPORTED_FORMAT} unless secrets can be carried in
     * this audio: uncompressed integer PCM, 8/16/24/32-bit, every bit significant.
     */
    public void requireEmbeddable() throws StegoException {
        String reason = unsupportedReason();
        if (reason != null) {
            throw new StegoException(Code.UNSUPPORTED_FORMAT, reason);
        }
    }

    /** Why this audio can't carry a secret, or {@code null} if it can. */
    public String unsupportedReason() {
        if (format == FORMAT_FLOAT) {
            return "This is 32-bit float audio. Export it as 16-bit or 24-bit PCM WAV and try again.";
        }
        if (format != FORMAT_PCM) {
            return "This WAV is compressed. Choose an uncompressed (PCM) WAV file.";
        }
        if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32) {
            return bitsPerSample + "-bit audio isn't supported. Use 8, 16, 24 or 32-bit PCM.";
        }
        if (validBits != bitsPerSample) {
            return "This WAV pads " + validBits + "-bit samples into " + bitsPerSample
                    + "-bit slots, which isn't supported.";
        }
        if (channels < 1) {
            return "This WAV file declares no audio channels.";
        }
        return null;
    }

    /**
     * Whether the Blank iOS app can open a text secret hidden in this file. Blank
     * reads plain 16-bit PCM headers only.
     */
    public boolean blankCompatible() {
        return format == FORMAT_PCM && !extensible && bitsPerSample == 16;
    }

    public int bytesPerSample() {
        return bitsPerSample / 8;
    }

    /** Samples across all channels; each one carries one hidden bit. */
    public long sampleCount() {
        int width = bytesPerSample();
        return width == 0 ? 0 : dataLength / width;
    }

    public long frameCount() {
        return channels == 0 ? 0 : sampleCount() / channels;
    }

    public double durationSeconds() {
        return sampleRate == 0 ? 0 : (double) frameCount() / sampleRate;
    }

    /**
     * Sample {@code index} (across all channels) scaled to -1..1. Used for the
     * waveform and playback; never for hiding data.
     */
    public float normalizedSample(byte[] bytes, long index) {
        int at = dataOffset + (int) (index * bytesPerSample());
        return switch (bitsPerSample) {
            case 8 -> ((bytes[at] & 0xFF) - 128) / 128f;
            case 16 -> (short) ((bytes[at] & 0xFF) | (bytes[at + 1] << 8)) / 32768f;
            case 24 -> ((bytes[at] & 0xFF) | ((bytes[at + 1] & 0xFF) << 8) | (bytes[at + 2] << 16)) / 8388608f;
            case 32 -> ((bytes[at] & 0xFF) | ((bytes[at + 1] & 0xFF) << 8)
                    | ((bytes[at + 2] & 0xFF) << 16) | (bytes[at + 3] << 24)) / 2147483648f;
            default -> 0f;
        };
    }

    private static boolean fourCC(byte[] bytes, int offset, String cc) {
        if (offset + 4 > bytes.length) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (bytes[offset + i] != cc.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | ((b[at + 1] & 0xFF) << 8);
    }

    private static long u32(byte[] b, int at) {
        return (b[at] & 0xFFL) | ((b[at + 1] & 0xFFL) << 8) | ((b[at + 2] & 0xFFL) << 16) | ((b[at + 3] & 0xFFL) << 24);
    }
}
