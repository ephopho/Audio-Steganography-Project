package com.phopho.audiosteganography.engine;

import com.phopho.audiosteganography.engine.StegoException.Code;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Something to hide, already turned into the plaintext that gets encrypted.
 *
 * <p>Text is stored as plain UTF-8, exactly what Blank expects, so text secrets
 * open on both apps. A file is wrapped in a small envelope that keeps its name:
 * <pre>
 *   [ flags (1) | nameLength uint16 BE (2) | name UTF-8 | data ]   flags bit 0 = data is zlib-compressed
 * </pre>
 * The name sits inside the encrypted plaintext, so it stays secret too.
 */
public final class Secret {

    private static final int FLAG_DEFLATED = 1;
    private static final int MAX_NAME_BYTES = 255;
    // Cap on a decompressed hidden file so a damaged stream can't exhaust memory.
    private static final int MAX_FILE_BYTES = 1 << 30;

    private final Stego.Kind kind;
    private final byte[] plaintext;
    private final String name;
    private final long originalSize;

    private Secret(Stego.Kind kind, byte[] plaintext, String name, long originalSize) {
        this.kind = kind;
        this.plaintext = plaintext;
        this.name = name;
        this.originalSize = originalSize;
    }

    /** A text message (opens in Blank too). */
    public static Secret text(String message) {
        byte[] utf8 = message.getBytes(StandardCharsets.UTF_8);
        return new Secret(Stego.Kind.TEXT, utf8, null, utf8.length);
    }

    /** A file, kept with its name; compressed when that makes it smaller. */
    public static Secret file(String fileName, byte[] data) {
        String name = safeFileName(fileName);
        byte[] packed = deflate(data);
        boolean deflated = packed.length < data.length;
        byte[] body = deflated ? packed : data;

        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] plain = new byte[3 + nameBytes.length + body.length];
        plain[0] = (byte) (deflated ? FLAG_DEFLATED : 0);
        plain[1] = (byte) (nameBytes.length >>> 8);
        plain[2] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, plain, 3, nameBytes.length);
        System.arraycopy(body, 0, plain, 3 + nameBytes.length, body.length);
        return new Secret(Stego.Kind.FILE, plain, name, data.length);
    }

    public Stego.Kind kind() {
        return kind;
    }

    /** The stored file name, or {@code null} for a text secret. */
    public String name() {
        return name;
    }

    /** Size before compression: the text's UTF-8 length or the file's length. */
    public long originalSize() {
        return originalSize;
    }

    /** Bytes this secret occupies inside the audio, headers and encryption included. */
    public long embeddedSize() {
        return Stego.HEADER_BYTES + Crypto.OVERHEAD + plaintext.length;
    }

    byte[] plaintext() {
        return plaintext;
    }

    /** Reverse of {@link #file}: the envelope's name and original bytes. */
    static Revealed.HiddenFile unpackFile(byte[] plain) throws StegoException {
        if (plain.length < 3) {
            throw corrupt();
        }
        int flags = plain[0] & 0xFF;
        int nameLength = ((plain[1] & 0xFF) << 8) | (plain[2] & 0xFF);
        if (3 + nameLength > plain.length) {
            throw corrupt();
        }
        String name = safeFileName(new String(plain, 3, nameLength, StandardCharsets.UTF_8));
        int bodyAt = 3 + nameLength;
        byte[] data = (flags & FLAG_DEFLATED) != 0
                ? inflate(plain, bodyAt, plain.length - bodyAt)
                : Arrays.copyOfRange(plain, bodyAt, plain.length);
        return new Revealed.HiddenFile(name, data);
    }

    /**
     * Reduce any name to a plain file name that is safe to create on Windows,
     * macOS and Linux: last path component only, no control or reserved
     * characters, no reserved device names. A crafted file can't use its stored
     * name to write outside the folder the user picks.
     */
    public static String safeFileName(String raw) {
        String name = raw == null ? "" : raw;
        int cut = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        name = name.substring(cut + 1);

        StringBuilder clean = new StringBuilder();
        name.codePoints()
                .filter(cp -> cp >= 0x20 && cp != 0x7F && "<>:\"|?*".indexOf(cp) < 0)
                .forEach(clean::appendCodePoint);
        name = clean.toString().strip();
        while (name.endsWith(".") || name.endsWith(" ")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty()) {
            name = "hidden-file";
        }
        String stem = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
        if (stem.toUpperCase(Locale.ROOT).matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            name = "_" + name;
        }
        while (name.getBytes(StandardCharsets.UTF_8).length > MAX_NAME_BYTES) {
            name = name.substring(0, name.offsetByCodePoints(name.length(), -1));
        }
        return name;
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 2));
            byte[] buffer = new byte[64 * 1024];
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(byte[] src, int offset, int length) throws StegoException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(src, offset, length);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, length * 2));
            byte[] buffer = new byte[64 * 1024];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw corrupt();
                }
                if (out.size() + n > MAX_FILE_BYTES) {
                    throw new StegoException(Code.CORRUPT, "The hidden file is too large to extract.");
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            throw corrupt();
        } finally {
            inflater.end();
        }
    }

    private static StegoException corrupt() {
        return new StegoException(Code.CORRUPT, "The hidden file is incomplete or damaged.");
    }
}
