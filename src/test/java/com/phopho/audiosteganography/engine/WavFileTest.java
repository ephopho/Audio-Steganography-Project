package com.phopho.audiosteganography.engine;

import static com.phopho.audiosteganography.testing.Assert.check;
import static com.phopho.audiosteganography.testing.Assert.equal;
import static com.phopho.audiosteganography.testing.Assert.failsWith;

import com.phopho.audiosteganography.engine.StegoException.Code;
import com.phopho.audiosteganography.testing.Test;
import com.phopho.audiosteganography.testing.Wavs;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WavFileTest {

    @Test
    public void parsesPlainPcm16Stereo() throws Exception {
        WavFile wav = WavFile.parse(Wavs.pcm16(44_100, 2));
        equal(WavFile.FORMAT_PCM, wav.format());
        equal(2, wav.channels());
        equal(44_100, wav.sampleRate());
        equal(16, wav.bitsPerSample());
        equal(44, wav.dataOffset());
        equal(44_100L * 2, wav.sampleCount());
        equal(1.0, wav.durationSeconds());
        check(wav.blankCompatible(), "plain 16-bit PCM is Blank-compatible");
        equal(null, wav.unsupportedReason());
    }

    @Test
    public void walksPastChunksBeforeTheAudio() throws Exception {
        WavFile wav = WavFile.parse(Wavs.builder().frames(1000).listChunkFirst().build());
        // 12 RIFF header + LIST (8 + 17 + 1 pad) + fmt (8 + 16) + data header 8
        equal(12 + 26 + 24 + 8, wav.dataOffset());
        equal(2000, wav.dataLength());
    }

    @Test
    public void clampsDataThatTheHeaderOverstates() throws Exception {
        // Same shape as the 2020 repo's hakunaMata.wav: header claims more audio than the file holds.
        WavFile wav = WavFile.parse(Wavs.builder().frames(1000).truncateDataBy(500).build());
        equal(1500, wav.dataLength());
        equal(750L, wav.sampleCount());
    }

    @Test
    public void readsExtensibleHeaders() throws Exception {
        WavFile wav = WavFile.parse(Wavs.builder().bits(24).channels(2).extensible(24).build());
        check(wav.extensible(), "extensible flag");
        equal(WavFile.FORMAT_PCM, wav.format());
        equal(null, wav.unsupportedReason());
        check(!wav.blankCompatible(), "Blank only reads plain 16-bit headers");
    }

    @Test
    public void explainsUnsupportedFormats() throws Exception {
        WavFile flt = WavFile.parse(Wavs.builder().bits(32).formatTag(WavFile.FORMAT_FLOAT).build());
        check(flt.unsupportedReason().contains("float"), flt.unsupportedReason());
        failsWith(Code.UNSUPPORTED_FORMAT, flt::requireEmbeddable);

        WavFile padded = WavFile.parse(Wavs.builder().bits(24).extensible(20).build());
        failsWith(Code.UNSUPPORTED_FORMAT, padded::requireEmbeddable);

        WavFile adpcm = WavFile.parse(Wavs.builder().formatTag(2).build());
        check(adpcm.unsupportedReason().contains("compressed"), adpcm.unsupportedReason());
    }

    @Test
    public void rejectsNonWavInput() {
        failsWith(Code.BAD_WAV, () -> WavFile.parse("hello, not audio".getBytes(StandardCharsets.US_ASCII)));
        failsWith(Code.BAD_WAV, () -> WavFile.parse(new byte[0]));

        byte[] noData = Arrays.copyOf(Wavs.pcm16(10, 1), 36); // RIFF + fmt only
        failsWith(Code.BAD_WAV, () -> WavFile.parse(noData));
    }

    @Test
    public void decodesSamplesAtEveryDepth() throws Exception {
        for (int bits : new int[] {8, 16, 24, 32}) {
            byte[] bytes = Wavs.builder().bits(bits).frames(100).build();
            WavFile wav = WavFile.parse(bytes);
            float expected = (float) (Math.sin(10 / 9.0) * 0.4);
            float actual = wav.normalizedSample(bytes, 10);
            check(Math.abs(expected - actual) < 0.01, bits + "-bit sample decoded as " + actual + ", expected " + expected);
        }
    }
}
