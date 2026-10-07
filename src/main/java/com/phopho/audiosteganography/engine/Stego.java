package com.phopho.audiosteganography.engine;

import com.phopho.audiosteganography.engine.StegoException.Code;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Steganography engine: hide an encrypted secret in a WAV file's sample bits,
 * and get it back with the password.
 *
 * <p>Pure functions over byte arrays, with no file system or UI code, so the
 * CLI, the desktop app and the tests all drive the same code.
 *
 * <p>Hidden bitstream (one bit per sample, MSB-first), shared with the Blank iOS app:
 * <pre>
 *   [ magic (4) | version (1) | payloadLength uint32 BE (4) | payload = encrypted blob ]
 *   "BLNK" v2 = text message (Blank writes this too; v1 = Blank 1.0.0, read-only)
 *   "BLNF" v1 = file with its name (desktop only; Blank reports "no message")
 * </pre>
 * The magic means "is there anything here?" has a definite answer: a secret's
 * <em>content</em> is protected, not the fact that one exists.
 */
public final class Stego {

    /** Size of the hidden-stream header. */
    public static final int HEADER_BYTES = 9;

    private static final byte[] TEXT_MAGIC = {'B', 'L', 'N', 'K'};
    private static final byte[] FILE_MAGIC = {'B', 'L', 'N', 'F'};
    private static final int TEXT_VERSION = 2;
    private static final int LEGACY_TEXT_VERSION = 1;
    private static final int FILE_VERSION = 1;

    public enum Kind { TEXT, FILE }

    /**
     * What a WAV file looks like before any password is involved.
     *
     * @param wav            the parsed layout
     * @param capacityBytes  total bytes the audio can carry, header included
     *                       (0 if the format can't carry data)
     * @param hidden         the kind of secret found, or {@code null} if none
     * @param hiddenReadable false when a secret was found but was written by a
     *                       newer version of the format
     */
    public record Inspection(WavFile wav, long capacityBytes, Kind hidden, boolean hiddenReadable) {

        public boolean embeddable() {
            return wav.unsupportedReason() == null;
        }

        public boolean hasSecret() {
            return hidden != null;
        }

        /** Longest text message (in UTF-8 bytes) this audio can carry. */
        public long textCapacityBytes() {
            return Math.max(0, capacityBytes - HEADER_BYTES - Crypto.OVERHEAD);
        }

        /** Throws {@code TOO_LARGE} (with sizes in the message) unless {@code secret} fits. */
        public void requireFits(Secret secret) throws StegoException {
            if (secret.embeddedSize() > capacityBytes) {
                throw new StegoException(Code.TOO_LARGE, "This secret needs " + formatBytes(secret.embeddedSize())
                        + " but this audio can hold " + formatBytes(capacityBytes)
                        + ". Choose a longer clip or a smaller secret.");
            }
        }
    }

    /**
     * Outcome of {@link #hide}.
     *
     * @param wav            the new WAV file
     * @param bytesHidden    bytes written into the audio, headers included
     * @param samplesUsed    samples that carry hidden bits
     * @param samplesChanged samples whose value actually changed (each by one step)
     */
    public record HideResult(byte[] wav, int bytesHidden, long samplesUsed, int samplesChanged) {
    }

    private record Header(Kind kind, int version, long payloadLength, boolean supported) {
    }

    private Stego() {
    }

    /** Parse a WAV file and report capacity and any hidden secret. No password needed. */
    public static Inspection inspect(byte[] bytes) throws StegoException {
        WavFile wav = WavFile.parse(bytes);
        if (wav.unsupportedReason() != null) {
            return new Inspection(wav, 0, null, false);
        }
        Header header = readHeader(bytes, wav);
        return new Inspection(wav, Lsb.capacityBytes(wav),
                header == null ? null : header.kind(), header != null && header.supported());
    }

    /**
     * Encrypt {@code secret} with {@code password} and hide it in {@code cover}.
     * Returns a new WAV; {@code cover} is not modified. Any secret already in the
     * cover is overwritten.
     */
    public static HideResult hide(byte[] cover, Secret secret, char[] password) throws StegoException {
        byte[] key = passwordBytes(password);
        try {
            WavFile wav = WavFile.parse(cover);
            wav.requireEmbeddable();
            new Inspection(wav, Lsb.capacityBytes(wav), null, false).requireFits(secret);

            byte[] blob = Crypto.encrypt(secret.plaintext(), key);
            byte[] stream = new byte[HEADER_BYTES + blob.length];
            writeHeader(stream, secret.kind(), blob.length);
            System.arraycopy(blob, 0, stream, HEADER_BYTES, blob.length);

            byte[] out = cover.clone();
            int changed = Lsb.write(out, wav, stream);
            return new HideResult(out, stream.length, stream.length * 8L, changed);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    /**
     * Recover a secret hidden by {@link #hide} (or by the Blank app).
     *
     * @throws StegoException {@code NO_MESSAGE} if nothing is hidden,
     *                        {@code WRONG_PASSWORD} if the password is wrong or the
     *                        audio was altered, {@code CORRUPT} if the data is damaged
     */
    public static Revealed reveal(byte[] stego, char[] password) throws StegoException {
        byte[] key = passwordBytes(password);
        try {
            WavFile wav = WavFile.parse(stego);
            wav.requireEmbeddable();

            Header header = readHeader(stego, wav);
            if (header == null) {
                throw new StegoException(Code.NO_MESSAGE, "No hidden secret was found in this audio.");
            }
            if (!header.supported()) {
                throw new StegoException(Code.CORRUPT,
                        "This secret was made with a newer version of the format. Update the app to open it.");
            }
            long available = Lsb.capacityBytes(wav) - HEADER_BYTES;
            if (header.payloadLength() <= 0 || header.payloadLength() > available) {
                throw new StegoException(Code.CORRUPT, "The hidden data is incomplete or damaged.");
            }

            byte[] blob = Lsb.read(stego, wav, HEADER_BYTES, (int) header.payloadLength());
            boolean legacy = header.kind() == Kind.TEXT && header.version() == LEGACY_TEXT_VERSION;
            byte[] plain = Crypto.decrypt(blob, key, legacy);
            return header.kind() == Kind.TEXT
                    ? new Revealed.Text(new String(plain, StandardCharsets.UTF_8))
                    : Secret.unpackFile(plain);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    /** Human-readable size: "512 B", "3.4 KB", "1.2 MB". */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static Header readHeader(byte[] bytes, WavFile wav) {
        if (Lsb.capacityBytes(wav) < HEADER_BYTES) {
            return null;
        }
        byte[] h = Lsb.read(bytes, wav, 0, HEADER_BYTES);
        long length = ((h[5] & 0xFFL) << 24) | ((h[6] & 0xFFL) << 16) | ((h[7] & 0xFFL) << 8) | (h[8] & 0xFFL);
        int version = h[4] & 0xFF;
        if (startsWith(h, TEXT_MAGIC)) {
            return new Header(Kind.TEXT, version, length, version == TEXT_VERSION || version == LEGACY_TEXT_VERSION);
        }
        if (startsWith(h, FILE_MAGIC)) {
            return new Header(Kind.FILE, version, length, version == FILE_VERSION);
        }
        return null;
    }

    private static void writeHeader(byte[] stream, Kind kind, int payloadLength) {
        System.arraycopy(kind == Kind.TEXT ? TEXT_MAGIC : FILE_MAGIC, 0, stream, 0, 4);
        stream[4] = (byte) (kind == Kind.TEXT ? TEXT_VERSION : FILE_VERSION);
        stream[5] = (byte) (payloadLength >>> 24);
        stream[6] = (byte) (payloadLength >>> 16);
        stream[7] = (byte) (payloadLength >>> 8);
        stream[8] = (byte) payloadLength;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }

    /** UTF-8 bytes of the password, as Blank derives keys from them. */
    private static byte[] passwordBytes(char[] password) throws StegoException {
        if (password == null || password.length == 0) {
            throw new StegoException(Code.MISSING_PASSWORD, "Enter a password.");
        }
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] out = new byte[encoded.remaining()];
        encoded.get(out);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }
        return out;
    }
}
