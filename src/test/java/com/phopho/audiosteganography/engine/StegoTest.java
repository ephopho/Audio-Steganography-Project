package com.phopho.audiosteganography.engine;

import static com.phopho.audiosteganography.testing.Assert.bytesEqual;
import static com.phopho.audiosteganography.testing.Assert.check;
import static com.phopho.audiosteganography.testing.Assert.equal;
import static com.phopho.audiosteganography.testing.Assert.failsWith;

import com.phopho.audiosteganography.engine.StegoException.Code;
import com.phopho.audiosteganography.testing.Test;
import com.phopho.audiosteganography.testing.Wavs;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class StegoTest {

    private static final char[] PASSWORD = "correct horse battery staple".toCharArray();

    private static String revealText(byte[] wav, char[] password) throws StegoException {
        return ((Revealed.Text) Stego.reveal(wav, password)).message();
    }

    @Test
    public void roundTripsUnicodeText() throws Exception {
        String message = "Meet at the docks, 9pm. 🕵️ Δelta — ñ";
        byte[] stego = Stego.hide(Wavs.pcm16(20_000, 2), Secret.text(message), PASSWORD).wav();
        equal(message, revealText(stego, PASSWORD));
    }

    @Test
    public void handlesFilesLongerThanTheOldFixedBuffer() throws Exception {
        // The 2020 code read at most 154,600 frames and wrote back a scrambled, truncated file.
        byte[] cover = Wavs.pcm16(441_000, 2);
        byte[] stego = Stego.hide(cover, Secret.text("long file"), PASSWORD).wav();
        equal(cover.length, stego.length);
        equal(WavFile.parse(cover), WavFile.parse(stego));
        equal("long file", revealText(stego, PASSWORD));
    }

    @Test
    public void leavesTheCoverUntouched() throws Exception {
        byte[] cover = Wavs.pcm16(20_000, 1);
        byte[] snapshot = cover.clone();
        Stego.hide(cover, Secret.text("hello"), PASSWORD);
        bytesEqual(snapshot, cover);
    }

    @Test
    public void changesOnlyLowBitsOfTheSamplesItUses() throws Exception {
        for (int bits : new int[] {8, 16, 24, 32}) {
            byte[] cover = Wavs.builder().bits(bits).frames(30_000).channels(2).build();
            WavFile wav = WavFile.parse(cover);
            Stego.HideResult result = Stego.hide(cover, Secret.text("x".repeat(200)), PASSWORD);
            byte[] stego = result.wav();
            int width = bits / 8;

            for (int i = 0; i < stego.length; i++) {
                int sampleByte = i - wav.dataOffset();
                boolean lowByteOfUsedSample = sampleByte >= 0 && sampleByte % width == 0
                        && sampleByte / width < result.samplesUsed();
                int diff = (cover[i] ^ stego[i]) & 0xFF;
                if (lowByteOfUsedSample) {
                    check(diff <= 1, bits + "-bit: byte " + i + " changed beyond its LSB");
                } else {
                    check(diff == 0, bits + "-bit: byte " + i + " outside the payload changed");
                }
            }
            check(result.samplesChanged() > 0 && result.samplesChanged() <= result.samplesUsed(), "changed count");
        }
    }

    @Test
    public void everyDepthAndChannelLayoutRoundTrips() throws Exception {
        for (int bits : new int[] {8, 16, 24, 32}) {
            for (int channels : new int[] {1, 2, 6}) {
                byte[] cover = Wavs.builder().bits(bits).channels(channels).frames(8_000).build();
                String msg = bits + "-bit, " + channels + " ch";
                equal(msg, revealText(Stego.hide(cover, Secret.text(msg), PASSWORD).wav(), PASSWORD));
            }
        }
        byte[] extensible24 = Wavs.builder().bits(24).channels(2).extensible(24).build();
        equal("ext", revealText(Stego.hide(extensible24, Secret.text("ext"), PASSWORD).wav(), PASSWORD));
    }

    @Test
    public void wrongPasswordIsRejectedNotGarbled() throws Exception {
        byte[] stego = Stego.hide(Wavs.pcm16(20_000, 1), Secret.text("secret"), PASSWORD).wav();
        failsWith(Code.WRONG_PASSWORD, () -> Stego.reveal(stego, "Correct horse battery staple".toCharArray()));
    }

    @Test
    public void detectsTampering() throws Exception {
        byte[] stego = Stego.hide(Wavs.pcm16(20_000, 1), Secret.text("secret"), PASSWORD).wav();
        WavFile wav = WavFile.parse(stego);
        int sampleInsidePayload = (Stego.HEADER_BYTES + 40) * 8;
        stego[wav.dataOffset() + sampleInsidePayload * 2] ^= 1;
        failsWith(Code.WRONG_PASSWORD, () -> Stego.reveal(stego, PASSWORD));
    }

    @Test
    public void cleanAudioHasNoMessage() throws Exception {
        byte[] clean = Wavs.pcm16(20_000, 1);
        failsWith(Code.NO_MESSAGE, () -> Stego.reveal(clean, PASSWORD));
        Stego.Inspection inspection = Stego.inspect(clean);
        check(!inspection.hasSecret(), "no secret expected");
        equal(20_000L / 8, inspection.capacityBytes());
    }

    @Test
    public void tinyAudioHasNoMessageInsteadOfCrashing() throws Exception {
        // The 2020 code threw NegativeArraySizeException when extracting from a clean file.
        failsWith(Code.NO_MESSAGE, () -> Stego.reveal(Wavs.pcm16(40, 1), PASSWORD));
    }

    @Test
    public void absurdLengthHeaderIsCorruptNotOutOfMemory() throws Exception {
        byte[] stego = Stego.hide(Wavs.pcm16(20_000, 1), Secret.text("secret"), PASSWORD).wav();
        WavFile wav = WavFile.parse(stego);
        for (int bit = 40; bit < 48; bit++) { // top byte of the length field -> 0xFF......
            stego[wav.dataOffset() + bit * 2] |= 1;
        }
        failsWith(Code.CORRUPT, () -> Stego.reveal(stego, PASSWORD));
    }

    @Test
    public void tooLargeSecretIsRefusedUpFront() {
        byte[] cover = Wavs.pcm16(2_000, 1); // 250 bytes of capacity
        StegoException e = failsWith(Code.TOO_LARGE, () -> Stego.hide(cover, Secret.text("y".repeat(400)), PASSWORD));
        check(e.getMessage().contains("250 B"), e.getMessage());
    }

    @Test
    public void exactlyFullCapacityFits() throws Exception {
        byte[] cover = Wavs.pcm16(8_000, 1); // 1000 bytes
        int room = 1000 - Stego.HEADER_BYTES - Crypto.OVERHEAD;
        String msg = "z".repeat(room);
        equal(msg, revealText(Stego.hide(cover, Secret.text(msg), PASSWORD).wav(), PASSWORD));
        failsWith(Code.TOO_LARGE, () -> Stego.hide(cover, Secret.text(msg + "z"), PASSWORD));
    }

    @Test
    public void passwordIsRequired() {
        byte[] cover = Wavs.pcm16(20_000, 1);
        failsWith(Code.MISSING_PASSWORD, () -> Stego.hide(cover, Secret.text("x"), new char[0]));
        failsWith(Code.MISSING_PASSWORD, () -> Stego.reveal(cover, null));
    }

    @Test
    public void unsupportedAndInvalidInputs() {
        byte[] floatWav = Wavs.builder().bits(32).formatTag(WavFile.FORMAT_FLOAT).build();
        failsWith(Code.UNSUPPORTED_FORMAT, () -> Stego.hide(floatWav, Secret.text("x"), PASSWORD));
        failsWith(Code.BAD_WAV, () -> Stego.hide("RIFF....nope".getBytes(StandardCharsets.US_ASCII),
                Secret.text("x"), PASSWORD));
    }

    @Test
    public void hidesFilesWithTheirNameAndCompression() throws Exception {
        byte[] doc = ("Quarterly plan\n" + "line of very repetitive text\n".repeat(400)).getBytes(StandardCharsets.UTF_8);
        Secret secret = Secret.file("C:\\Users\\me\\Documents\\plan.txt", doc);
        check(secret.embeddedSize() < doc.length / 4, "repetitive text should compress well");
        equal("plan.txt", secret.name());

        byte[] stego = Stego.hide(Wavs.pcm16(60_000, 2), secret, PASSWORD).wav();
        equal(Stego.Kind.FILE, Stego.inspect(stego).hidden());
        Revealed.HiddenFile file = (Revealed.HiddenFile) Stego.reveal(stego, PASSWORD);
        equal("plan.txt", file.name());
        bytesEqual(doc, file.data());
    }

    @Test
    public void incompressibleFilesAreStoredAsIs() throws Exception {
        byte[] noise = new byte[3_000];
        new Random(7).nextBytes(noise);
        Secret secret = Secret.file("noise.bin", noise);
        equal((long) Stego.HEADER_BYTES + Crypto.OVERHEAD + 3 + "noise.bin".length() + noise.length,
                secret.embeddedSize());
        byte[] stego = Stego.hide(Wavs.pcm16(60_000, 2), secret, PASSWORD).wav();
        bytesEqual(noise, ((Revealed.HiddenFile) Stego.reveal(stego, PASSWORD)).data());
    }

    @Test
    public void storedFileNamesCannotEscapeTheTargetFolder() {
        equal("passwd", Secret.safeFileName("../../etc/passwd"));
        equal("evil.bat", Secret.safeFileName("..\\..\\Startup\\evil.bat"));
        equal("hidden-file", Secret.safeFileName(".."));
        equal("hidden-file", Secret.safeFileName(""));
        equal("hidden-file", Secret.safeFileName(null));
        equal("_CON.txt", Secret.safeFileName("CON.txt"));
        equal("ab.txt", Secret.safeFileName("a\u0000b?.txt"));
        equal("trailing", Secret.safeFileName("trailing. . "));
        check(Secret.safeFileName("é".repeat(300)).getBytes(StandardCharsets.UTF_8).length <= 255, "length cap");
    }

    @Test
    public void inspectReportsWhatIsHidden() throws Exception {
        byte[] stego = Stego.hide(Wavs.pcm16(20_000, 1), Secret.text("hi"), PASSWORD).wav();
        Stego.Inspection inspection = Stego.inspect(stego);
        equal(Stego.Kind.TEXT, inspection.hidden());
        check(inspection.hiddenReadable(), "readable");
        check(inspection.embeddable(), "embeddable");
    }

    @Test
    public void hidingAgainReplacesTheOldSecret() throws Exception {
        byte[] first = Stego.hide(Wavs.pcm16(20_000, 1), Secret.text("first secret, quite long"), PASSWORD).wav();
        byte[] second = Stego.hide(first, Secret.text("second"), "other".toCharArray()).wav();
        equal("second", revealText(second, "other".toCharArray()));
        failsWith(Code.WRONG_PASSWORD, () -> Stego.reveal(second, PASSWORD));
    }

    @Test
    public void emptyTextMessageIsAllowed() throws Exception {
        byte[] stego = Stego.hide(Wavs.pcm16(20_000, 1), Secret.text(""), PASSWORD).wav();
        equal("", revealText(stego, PASSWORD));
    }

    @Test
    public void freshSaltAndNonceEveryTime() throws Exception {
        byte[] cover = Wavs.pcm16(20_000, 1);
        byte[] a = Stego.hide(cover, Secret.text("same"), PASSWORD).wav();
        byte[] b = Stego.hide(cover, Secret.text("same"), PASSWORD).wav();
        check(!Arrays.equals(a, b), "identical inputs must not give identical output");
    }

    @Test
    public void formatsSizes() {
        equal("512 B", Stego.formatBytes(512));
        check(Stego.formatBytes(1536).matches("1[.,]5 KB"), Stego.formatBytes(1536));
        check(Stego.formatBytes(3 * 1024 * 1024).matches("3[.,]0 MB"), Stego.formatBytes(3 * 1024 * 1024));
    }
}
