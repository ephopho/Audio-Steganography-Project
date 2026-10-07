package com.phopho.audiosteganography.cli;

import com.phopho.audiosteganography.FileIO;
import com.phopho.audiosteganography.engine.Revealed;
import com.phopho.audiosteganography.engine.Secret;
import com.phopho.audiosteganography.engine.Stego;
import com.phopho.audiosteganography.engine.StegoException;
import com.phopho.audiosteganography.engine.WavFile;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Command-line interface, for scripting and for machines without a display.
 * Passwords are never taken as arguments (they would land in shell history):
 * they are prompted for, or read from the {@code STEGO_PASSWORD} environment variable.
 */
public final class Cli {

    public static final int OK = 0;
    public static final int FAILED = 1;
    public static final int USAGE = 2;

    static final String PASSWORD_ENV = "STEGO_PASSWORD";

    /** Supplies a password; {@code confirm} asks twice when hiding. */
    @FunctionalInterface
    public interface PasswordPrompt {
        char[] read(String label, boolean confirm) throws IOException;
    }

    private static final String USAGE_TEXT = """
            Audio Steganography %s: hide encrypted messages and files inside WAV audio.

            Usage:
              audio-steganography                                   open the desktop app
              audio-steganography hide <cover.wav> <out.wav> --text "message"
              audio-steganography hide <cover.wav> <out.wav> --text -       (message from stdin)
              audio-steganography hide <cover.wav> <out.wav> --file <secret.pdf>
              audio-steganography reveal <stego.wav> [--out <file-or-folder>]
              audio-steganography info <file.wav>

            Options:
              --force      overwrite an existing output file
              -h, --help   show this help
              --version    print the version

            The password is asked for at the prompt, or read from the %s
            environment variable when set. Text secrets in 16-bit WAVs also open in the
            Blank iOS app.
            """;

    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private final PasswordPrompt prompt;

    public Cli(InputStream in, PrintStream out, PrintStream err, PasswordPrompt prompt) {
        this.in = in;
        this.out = out;
        this.err = err;
        this.prompt = prompt;
    }

    /** A CLI wired to the real console and environment. */
    public static Cli system() {
        return new Cli(System.in, System.out, System.err, Cli::consolePassword);
    }

    public int run(String[] argv) {
        List<String> args = new ArrayList<>(Arrays.asList(argv));
        if (args.isEmpty() || args.contains("-h") || args.contains("--help") || args.get(0).equals("help")) {
            out.print(usage());
            return args.isEmpty() ? USAGE : OK;
        }
        if (args.contains("--version") || args.get(0).equals("version")) {
            out.println(FileIO.appVersion());
            return OK;
        }

        String command = args.remove(0);
        try {
            return switch (command) {
                case "hide" -> hide(args);
                case "reveal" -> reveal(args);
                case "info" -> info(args);
                default -> usageError("Unknown command '" + command + "'.");
            };
        } catch (UsageException e) {
            return usageError(e.getMessage());
        } catch (StegoException e) {
            err.println("Error: " + e.getMessage());
            return FAILED;
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
            return FAILED;
        }
    }

    private int hide(List<String> args) throws IOException, StegoException, UsageException {
        boolean force = args.remove("--force");
        String text = option(args, "--text");
        String file = option(args, "--file");
        List<String> paths = positional(args, 2, "hide <cover.wav> <out.wav> (--text ... | --file ...)");
        if ((text == null) == (file == null)) {
            throw new UsageException("Give exactly one of --text or --file.");
        }
        Path coverPath = Path.of(paths.get(0));
        Path outPath = Path.of(paths.get(1));
        if (Files.exists(outPath) && !force && !sameFile(coverPath, outPath)) {
            throw new IOException(outPath + " already exists. Use --force to overwrite it.");
        }

        byte[] cover = FileIO.read(coverPath);
        Stego.Inspection inspection = Stego.inspect(cover);
        inspection.wav().requireEmbeddable();

        Secret secret;
        if (text != null) {
            secret = Secret.text(text.equals("-") ? readStdinText() : text);
        } else {
            Path filePath = Path.of(file);
            secret = Secret.file(filePath.getFileName().toString(), FileIO.read(filePath));
        }
        inspection.requireFits(secret); // before prompting for a password that would be wasted

        char[] password = prompt.read("Password: ", true);
        Stego.HideResult result;
        try {
            result = Stego.hide(cover, secret, password);
        } finally {
            Arrays.fill(password, '\0');
        }
        FileIO.writeAtomically(outPath, result.wav());

        WavFile wav = inspection.wav();
        out.printf(Locale.ROOT, "Hid %s in %s (%.1f%% of its capacity).%n",
                secret.name() == null ? "a " + Stego.formatBytes(secret.originalSize()) + " message"
                        : secret.name() + " (" + Stego.formatBytes(secret.originalSize()) + ")",
                outPath, 100.0 * result.bytesHidden() / inspection.capacityBytes());
        out.printf(Locale.ROOT, "%,d of %,d samples changed, each by one step.%n",
                result.samplesChanged(), wav.sampleCount());
        if (secret.kind() == Stego.Kind.TEXT) {
            out.println(wav.blankCompatible()
                    ? "This message also opens in the Blank iOS app."
                    : "Note: Blank (iOS) only reads 16-bit WAVs, so it can't open this one.");
        }
        return OK;
    }

    private int reveal(List<String> args) throws IOException, StegoException, UsageException {
        boolean force = args.remove("--force");
        String outOption = option(args, "--out");
        Path stegoPath = Path.of(positional(args, 1, "reveal <stego.wav> [--out <file-or-folder>]").get(0));

        byte[] stego = FileIO.read(stegoPath);
        Stego.Inspection inspection = Stego.inspect(stego);
        inspection.wav().requireEmbeddable();
        if (!inspection.hasSecret()) {
            throw new StegoException(StegoException.Code.NO_MESSAGE, "No hidden secret was found in " + stegoPath + ".");
        }

        char[] password = prompt.read("Password: ", false);
        Revealed revealed;
        try {
            revealed = Stego.reveal(stego, password);
        } finally {
            Arrays.fill(password, '\0');
        }

        switch (revealed) {
            case Revealed.Text text -> {
                if (outOption == null) {
                    out.println(text.message());
                } else {
                    Path target = Path.of(outOption);
                    if (Files.isDirectory(target)) {
                        target = target.resolve("hidden-message.txt");
                    }
                    writeNew(target, text.message().getBytes(StandardCharsets.UTF_8), force);
                    err.println("Saved the hidden message to " + target);
                }
            }
            case Revealed.HiddenFile hidden -> {
                Path target = outOption == null ? Path.of(hidden.name()) : Path.of(outOption);
                if (Files.isDirectory(target)) {
                    target = target.resolve(hidden.name());
                }
                writeNew(target, hidden.data(), force);
                err.println("Saved hidden file " + hidden.name() + " (" + Stego.formatBytes(hidden.data().length)
                        + ") to " + target);
            }
        }
        return OK;
    }

    private int info(List<String> args) throws IOException, StegoException, UsageException {
        Path path = Path.of(positional(args, 1, "info <file.wav>").get(0));
        Stego.Inspection inspection = Stego.inspect(FileIO.read(path));
        WavFile wav = inspection.wav();

        out.println(path.getFileName());
        out.printf(Locale.ROOT, "  Audio     %s, %d-bit, %s, %s%n", hz(wav.sampleRate()), wav.bitsPerSample(),
                channels(wav.channels()), duration(wav.durationSeconds()));
        if (!inspection.embeddable()) {
            out.println("  Capacity  none: " + wav.unsupportedReason());
            return OK;
        }
        out.println("  Capacity  " + Stego.formatBytes(inspection.textCapacityBytes()) + " of secret text");
        out.println("  Blank     " + (wav.blankCompatible() ? "text secrets open in the iOS app" : "not readable by the iOS app"));
        out.println("  Hidden    " + (inspection.hidden() == null ? "nothing found"
                : (inspection.hidden() == Stego.Kind.TEXT ? "a text message" : "a file")
                + (inspection.hiddenReadable() ? " (password protected)" : " (made by a newer version)")));
        return OK;
    }

    private void writeNew(Path target, byte[] data, boolean force) throws IOException {
        if (Files.exists(target) && !force) {
            throw new IOException(target + " already exists. Use --force to overwrite it.");
        }
        FileIO.writeAtomically(target, data);
    }

    private String readStdinText() throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        // Drop the single trailing newline that `echo` and editors add.
        if (text.endsWith("\r\n")) {
            return text.substring(0, text.length() - 2);
        }
        return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
    }

    private static boolean sameFile(Path a, Path b) {
        try {
            return Files.exists(a) && Files.exists(b) && Files.isSameFile(a, b);
        } catch (IOException e) {
            return false;
        }
    }

    private static String option(List<String> args, String name) throws UsageException {
        int at = args.indexOf(name);
        if (at < 0) {
            return null;
        }
        if (at + 1 >= args.size()) {
            throw new UsageException(name + " needs a value.");
        }
        args.remove(at);
        return args.remove(at);
    }

    private static List<String> positional(List<String> args, int count, String synopsis) throws UsageException {
        for (String a : args) {
            if (a.startsWith("--")) {
                throw new UsageException("Unknown option " + a + ".");
            }
        }
        if (args.size() != count) {
            throw new UsageException("Usage: audio-steganography " + synopsis);
        }
        return args;
    }

    private int usageError(String message) {
        err.println(message);
        err.println("Run with --help for usage.");
        return USAGE;
    }

    private static String usage() {
        return USAGE_TEXT.formatted(FileIO.appVersion(), PASSWORD_ENV);
    }

    static String hz(int rate) {
        return rate % 1000 == 0 ? rate / 1000 + " kHz" : String.format(Locale.ROOT, "%.1f kHz", rate / 1000.0);
    }

    static String channels(int n) {
        return switch (n) {
            case 1 -> "mono";
            case 2 -> "stereo";
            default -> n + " channels";
        };
    }

    static String duration(double seconds) {
        long s = Math.round(seconds);
        return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private static char[] consolePassword(String label, boolean confirm) throws IOException {
        String fromEnv = System.getenv(PASSWORD_ENV);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv.toCharArray();
        }
        Console console = System.console();
        if (console == null) {
            throw new IOException("No console to ask for a password. Set " + PASSWORD_ENV + " instead.");
        }
        char[] password = console.readPassword("%s", label);
        if (password == null || password.length == 0) {
            throw new IOException("No password entered.");
        }
        if (confirm) {
            char[] again = console.readPassword("Repeat password: ");
            boolean match = Arrays.equals(password, again);
            if (again != null) {
                Arrays.fill(again, '\0');
            }
            if (!match) {
                Arrays.fill(password, '\0');
                throw new IOException("The passwords don't match.");
            }
        }
        return password;
    }

    private static final class UsageException extends Exception {
        private static final long serialVersionUID = 1L;

        UsageException(String message) {
            super(message);
        }
    }
}
