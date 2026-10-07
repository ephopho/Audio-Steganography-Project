package com.phopho.audiosteganography.cli;

import static com.phopho.audiosteganography.testing.Assert.bytesEqual;
import static com.phopho.audiosteganography.testing.Assert.check;
import static com.phopho.audiosteganography.testing.Assert.equal;

import com.phopho.audiosteganography.testing.Test;
import com.phopho.audiosteganography.testing.Wavs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class CliTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private int prompts;

    private int run(String password, String stdin, String... args) {
        out.reset();
        err.reset();
        Cli cli = new Cli(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8),
                (label, confirm) -> {
                    prompts++;
                    return password.toCharArray();
                });
        return cli.run(args);
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private static Path tempDir() throws IOException {
        return Files.createTempDirectory("stego-cli-test");
    }

    private static void deleteTree(Path dir) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    @Test
    public void hideThenRevealText() throws Exception {
        Path dir = tempDir();
        try {
            Path cover = dir.resolve("cover.wav");
            Files.write(cover, Wavs.pcm16(20_000, 2));
            Path stego = dir.resolve("stego.wav");

            equal(Cli.OK, run("pw", "", "hide", cover.toString(), stego.toString(), "--text", "hello from the CLI"));
            check(out().contains("also opens in the Blank iOS app"), out());

            equal(Cli.OK, run("pw", "", "reveal", stego.toString()));
            equal("hello from the CLI", out().strip());

            equal(Cli.FAILED, run("wrong", "", "reveal", stego.toString()));
            check(err().contains("Wrong password"), err());
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    public void messageFromStdinDropsTrailingNewline() throws Exception {
        Path dir = tempDir();
        try {
            Path cover = dir.resolve("cover.wav");
            Files.write(cover, Wavs.pcm16(20_000, 1));
            Path stego = dir.resolve("out.wav");
            equal(Cli.OK, run("pw", "line one\nline two\n", "hide", cover.toString(), stego.toString(), "--text", "-"));
            equal(Cli.OK, run("pw", "", "reveal", stego.toString()));
            equal("line one\nline two" + System.lineSeparator(), out());
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    public void hideThenRevealFileKeepsItsName() throws Exception {
        Path dir = tempDir();
        try {
            Path cover = dir.resolve("cover.wav");
            Files.write(cover, Wavs.pcm16(40_000, 2));
            Path secret = dir.resolve("notes.md");
            byte[] content = "# Notes\nsecret plan\n".getBytes(StandardCharsets.UTF_8);
            Files.write(secret, content);
            Path stego = dir.resolve("stego.wav");

            equal(Cli.OK, run("pw", "", "hide", cover.toString(), stego.toString(), "--file", secret.toString()));
            Path outDir = Files.createDirectory(dir.resolve("extracted"));
            equal(Cli.OK, run("pw", "", "reveal", stego.toString(), "--out", outDir.toString()));
            bytesEqual(content, Files.readAllBytes(outDir.resolve("notes.md")));

            // A second reveal must not silently overwrite the extracted file.
            equal(Cli.FAILED, run("pw", "", "reveal", stego.toString(), "--out", outDir.toString()));
            check(err().contains("--force"), err());
            equal(Cli.OK, run("pw", "", "reveal", stego.toString(), "--out", outDir.toString(), "--force"));
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    public void refusesToOverwriteWithoutForce() throws Exception {
        Path dir = tempDir();
        try {
            Path cover = dir.resolve("cover.wav");
            Files.write(cover, Wavs.pcm16(20_000, 1));
            Path existing = dir.resolve("existing.wav");
            Files.write(existing, new byte[] {1, 2, 3});
            equal(Cli.FAILED, run("pw", "", "hide", cover.toString(), existing.toString(), "--text", "x"));
            bytesEqual(new byte[] {1, 2, 3}, Files.readAllBytes(existing));
            equal(Cli.OK, run("pw", "", "hide", cover.toString(), existing.toString(), "--text", "x", "--force"));
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    public void noPasswordPromptWhenThereIsNothingToDo() throws Exception {
        Path dir = tempDir();
        try {
            Path clean = dir.resolve("clean.wav");
            Files.write(clean, Wavs.pcm16(20_000, 1));
            prompts = 0;
            equal(Cli.FAILED, run("pw", "", "reveal", clean.toString()));
            check(err().contains("No hidden secret"), err());

            Path tiny = dir.resolve("tiny.wav");
            Files.write(tiny, Wavs.pcm16(400, 1)); // 50 bytes: less than the 54 bytes of headers alone
            equal(Cli.FAILED, run("pw", "", "hide", tiny.toString(), dir.resolve("o.wav").toString(),
                    "--text", "too long"));
            check(err().contains("can hold"), err());
            equal(0, prompts);
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    public void infoDescribesTheFile() throws Exception {
        Path dir = tempDir();
        try {
            Path cover = dir.resolve("cover.wav");
            Files.write(cover, Wavs.pcm16(44_100 * 3, 2));
            equal(Cli.OK, run("pw", "", "info", cover.toString()));
            check(out().contains("44.1 kHz, 16-bit, stereo, 0:03"), out());
            check(out().contains("nothing found"), out());
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    public void usageErrors() {
        equal(Cli.USAGE, run("pw", ""));
        equal(Cli.USAGE, run("pw", "", "frobnicate"));
        equal(Cli.USAGE, run("pw", "", "hide", "a.wav", "b.wav"));
        check(err().contains("--text or --file"), err());
        equal(Cli.USAGE, run("pw", "", "hide", "a.wav", "b.wav", "--text", "x", "--file", "y"));
        equal(Cli.USAGE, run("pw", "", "reveal", "a.wav", "--bogus"));
        equal(Cli.OK, run("pw", "", "--help"));
        check(out().contains("STEGO_PASSWORD"), out());
    }

    @Test
    public void formatting() {
        equal("44.1 kHz", Cli.hz(44_100));
        equal("48 kHz", Cli.hz(48_000));
        equal("1:05", Cli.duration(65));
        equal("1:01:01", Cli.duration(3661));
        equal("6 channels", Cli.channels(6));
    }
}
