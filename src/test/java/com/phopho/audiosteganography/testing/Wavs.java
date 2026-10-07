package com.phopho.audiosteganography.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Builds WAV files in memory so tests don't depend on audio on disk. */
public final class Wavs {

    private Wavs() {
    }

    /** A 44.1 kHz 16-bit PCM WAV holding a gentle sine. */
    public static byte[] pcm16(int frames, int channels) {
        return builder().frames(frames).channels(channels).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Load a test resource (e.g. a golden fixture) as bytes. */
    public static byte[] resource(String path) {
        try (InputStream in = Wavs.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("missing test resource " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static final class Builder {
        private int frames = 20_000;
        private int channels = 1;
        private int bits = 16;
        private int sampleRate = 44_100;
        private int formatTag = 1;
        private boolean extensible;
        private int validBits;
        private boolean listChunkFirst;
        private int truncateDataBy;

        public Builder frames(int v) {
            frames = v;
            return this;
        }

        public Builder channels(int v) {
            channels = v;
            return this;
        }

        public Builder bits(int v) {
            bits = v;
            return this;
        }

        public Builder sampleRate(int v) {
            sampleRate = v;
            return this;
        }

        public Builder formatTag(int v) {
            formatTag = v;
            return this;
        }

        /** Write a WAVE_FORMAT_EXTENSIBLE header with {@code validBits} significant bits. */
        public Builder extensible(int valid) {
            extensible = true;
            validBits = valid;
            return this;
        }

        /** Put an odd-sized LIST chunk before fmt/data, as tagging tools do. */
        public Builder listChunkFirst() {
            listChunkFirst = true;
            return this;
        }

        /** Cut this many bytes off the end while the header keeps claiming them. */
        public Builder truncateDataBy(int bytes) {
            truncateDataBy = bytes;
            return this;
        }

        public byte[] build() {
            int width = bits / 8;
            int dataLength = frames * channels * width;
            ByteArrayOutputStream body = new ByteArrayOutputStream();

            if (listChunkFirst) {
                byte[] info = "INFOISFT\5\0\0\0Test\0".getBytes(StandardCharsets.ISO_8859_1); // 17 bytes, odd
                chunk(body, "LIST", info);
            }

            ByteBuffer fmt = ByteBuffer.allocate(extensible ? 40 : 16).order(ByteOrder.LITTLE_ENDIAN);
            fmt.putShort((short) (extensible ? 0xFFFE : formatTag));
            fmt.putShort((short) channels);
            fmt.putInt(sampleRate);
            fmt.putInt(sampleRate * channels * width);
            fmt.putShort((short) (channels * width));
            fmt.putShort((short) bits);
            if (extensible) {
                fmt.putShort((short) 22);
                fmt.putShort((short) validBits);
                fmt.putInt(0);
                fmt.putShort((short) formatTag); // SubFormat GUID, first two bytes = format code
                fmt.put(new byte[] {0, 0, 0, 0, 0x10, 0, (byte) 0x80, 0, 0, (byte) 0xAA, 0, 0x38, (byte) 0x9B, 0x71});
            }
            chunk(body, "fmt ", fmt.array());

            ByteBuffer data = ByteBuffer.allocate(dataLength).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < frames * channels; i++) {
                double s = Math.sin(i / 9.0) * 0.4;
                switch (bits) {
                    case 8 -> data.put((byte) (128 + (int) (s * 127)));
                    case 16 -> data.putShort((short) (s * 32767));
                    case 24 -> {
                        int v = (int) (s * 8_388_607);
                        data.put((byte) v).put((byte) (v >> 8)).put((byte) (v >> 16));
                    }
                    case 32 -> {
                        if (formatTag == 3) {
                            data.putFloat((float) s);
                        } else {
                            data.putInt((int) (s * Integer.MAX_VALUE));
                        }
                    }
                    default -> throw new IllegalArgumentException("bits " + bits);
                }
            }
            chunk(body, "data", data.array());

            byte[] chunks = body.toByteArray();
            ByteBuffer wav = ByteBuffer.allocate(12 + chunks.length).order(ByteOrder.LITTLE_ENDIAN);
            wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(4 + chunks.length);
            wav.put("WAVE".getBytes(StandardCharsets.US_ASCII)).put(chunks);
            byte[] out = wav.array();
            return truncateDataBy == 0 ? out : Arrays.copyOf(out, out.length - truncateDataBy);
        }

        private static void chunk(ByteArrayOutputStream out, String id, byte[] payload) {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.put(id.getBytes(StandardCharsets.US_ASCII)).putInt(payload.length);
            out.writeBytes(header.array());
            out.writeBytes(payload);
            if ((payload.length & 1) == 1) {
                out.write(0);
            }
        }
    }
}
