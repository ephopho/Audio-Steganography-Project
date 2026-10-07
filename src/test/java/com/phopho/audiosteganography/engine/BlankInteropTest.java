package com.phopho.audiosteganography.engine;

import static com.phopho.audiosteganography.testing.Assert.check;
import static com.phopho.audiosteganography.testing.Assert.equal;
import static com.phopho.audiosteganography.testing.Assert.failsWith;

import com.phopho.audiosteganography.engine.StegoException.Code;
import com.phopho.audiosteganography.testing.Test;
import com.phopho.audiosteganography.testing.Wavs;
import java.nio.charset.StandardCharsets;

/**
 * Cross-implementation checks against files written by the Blank iOS app's own
 * TypeScript engine ({@code @noble/hashes} scrypt + {@code @noble/ciphers}
 * AES-GCM). See {@code src/test/resources/golden/README.md}.
 */
public class BlankInteropTest {

    private static final char[] PASSWORD = "correct horse battery staple".toCharArray();

    @Test
    public void revealsAMessageHiddenByBlank() throws Exception {
        byte[] wav = Wavs.resource("/golden/blank-v2-text.wav");
        Revealed revealed = Stego.reveal(wav, PASSWORD);
        equal(new Revealed.Text("Meet at the docks, 9pm. 🕵️ Δelta"), revealed);
    }

    @Test
    public void revealsALegacyBlank100Message() throws Exception {
        byte[] wav = Wavs.resource("/golden/blank-v1-text.wav");
        equal(new Revealed.Text("legacy v1 hello"), Stego.reveal(wav, PASSWORD));
    }

    @Test
    public void wrongPasswordOnABlankFile() {
        byte[] wav = Wavs.resource("/golden/blank-v2-text.wav");
        failsWith(Code.WRONG_PASSWORD, () -> Stego.reveal(wav, "nope".toCharArray()));
    }

    @Test
    public void blankChangedOnlySampleLowBits() {
        byte[] cover = Wavs.resource("/golden/cover-8k-mono16.wav");
        byte[] stego = Wavs.resource("/golden/blank-v2-text.wav");
        equal(cover.length, stego.length);
        for (int i = 0; i < cover.length; i++) {
            int diff = (cover[i] ^ stego[i]) & 0xFF;
            check(diff == 0 || (diff == 1 && i >= 44 && (i - 44) % 2 == 0), "unexpected change at byte " + i);
        }
    }

    /** What Blank's parser reads: pin the exact header bytes we write for text. */
    @Test
    public void writesTheHeaderBlankExpects() throws Exception {
        byte[] cover = Wavs.resource("/golden/cover-8k-mono16.wav");
        byte[] stego = Stego.hide(cover, Secret.text("hi"), PASSWORD).wav();
        WavFile wav = WavFile.parse(stego);
        byte[] header = Lsb.read(stego, wav, 0, Stego.HEADER_BYTES + 1);
        equal("BLNK", new String(header, 0, 4, StandardCharsets.US_ASCII));
        equal(2, (int) header[4]);
        int length = ((header[5] & 0xFF) << 24) | ((header[6] & 0xFF) << 16) | ((header[7] & 0xFF) << 8) | (header[8] & 0xFF);
        equal(Crypto.OVERHEAD + 2, length);
        equal(14, (int) header[9]); // scrypt logN, first byte of the blob
    }
}
