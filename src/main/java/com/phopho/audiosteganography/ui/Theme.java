package com.phopho.audiosteganography.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.swing.JComponent;

/**
 * Colours and fonts. The palette is the Blank iOS app's Windows 11 (Fluent)
 * palette, so the two apps look like one product. Components read colours at
 * paint time from {@link #p()}, and standard Swing components register a
 * styler with {@link #bind} so a theme switch restyles them too.
 */
final class Theme {

    record Palette(
            boolean dark,
            Color bg, Color surface, Color surfaceAlt, Color input,
            Color border, Color borderStrong,
            Color text, Color textSecondary, Color textTertiary,
            Color accent, Color accentHover, Color accentPressed, Color accentText, Color accentSubtle,
            Color danger, Color dangerSubtle, Color success, Color successSubtle,
            Color warning, Color warningSubtle, Color selection) {
    }

    static final Palette LIGHT = new Palette(false,
            hex(0xF3F3F3), hex(0xFBFBFB), hex(0xF1F4F8), hex(0xFFFFFF),
            hex(0xE4E4E4), hex(0xCFCFCF),
            hex(0x1A1A1A), hex(0x5C5C5C), hex(0x8A8A8A),
            hex(0x0067C0), hex(0x1975C5), hex(0x3183CA), hex(0xFFFFFF), hex(0xEAF2FB),
            hex(0xC42B1C), hex(0xFDE7E9), hex(0x0F7B0F), hex(0xDFF6DD),
            hex(0x9D5D00), hex(0xFFF4CE), hex(0xCCE0F5));

    static final Palette DARK = new Palette(true,
            hex(0x1F1F1F), hex(0x2B2B2B), hex(0x2F343A), hex(0x1C1C1C),
            hex(0x3A3A3A), hex(0x4C4C4C),
            hex(0xF4F4F4), hex(0xB2B2B2), hex(0x888888),
            hex(0x3B9EF0), hex(0x56ACF2), hex(0x2F8AD6), hex(0xFFFFFF), hex(0x1E3A52),
            hex(0xFF99A4), hex(0x442726), hex(0x6CCB6C), hex(0x293B29),
            hex(0xE8B05C), hex(0x433519), hex(0x1E4A70));

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static Palette current = LIGHT;

    private static String regular = Font.SANS_SERIF;
    private static String semibold;

    private Theme() {
    }

    static Palette p() {
        return current;
    }

    /** Pick fonts and the starting palette (saved choice, else the OS setting). */
    static void init() {
        Set<String> families = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[] {"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Ubuntu", "Cantarell", "Noto Sans"}) {
            if (families.contains(f)) {
                regular = f;
                break;
            }
        }
        semibold = families.contains(regular + " Semibold") ? regular + " Semibold" : null;

        String saved = PREFS.get("theme", "");
        current = switch (saved) {
            case "dark" -> DARK;
            case "light" -> LIGHT;
            default -> systemPrefersDark() ? DARK : LIGHT;
        };
    }

    static void toggle() {
        current = current.dark() ? LIGHT : DARK;
        PREFS.put("theme", current.dark() ? "dark" : "light");
        LISTENERS.forEach(Runnable::run);
    }

    /** Run {@code styler} now and again after every theme switch. */
    static <T extends JComponent> T bind(T component, Consumer<T> styler) {
        styler.accept(component);
        LISTENERS.add(() -> styler.accept(component));
        return component;
    }

    static void onChange(Runnable listener) {
        LISTENERS.add(listener);
    }

    static Font font(float size) {
        return new Font(regular, Font.PLAIN, 1).deriveFont(size);
    }

    static Font semibold(float size) {
        return semibold != null
                ? new Font(semibold, Font.PLAIN, 1).deriveFont(size)
                : new Font(regular, Font.BOLD, 1).deriveFont(size);
    }

    /**
     * Font for text the user typed or revealed. A logical font, because only
     * those fall back to other fonts for scripts and symbols the main font lacks.
     */
    static Font content(float size) {
        return new Font(Font.DIALOG, Font.PLAIN, 1).deriveFont(size);
    }

    private static boolean systemPrefersDark() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                String out = run("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme");
                return out.contains("0x0");
            }
            if (os.contains("mac")) {
                return run("defaults", "read", "-g", "AppleInterfaceStyle").toLowerCase(Locale.ROOT).contains("dark");
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (InputStream in = process.getInputStream()) {
            String out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor(2, TimeUnit.SECONDS);
            return out;
        } finally {
            process.destroy();
        }
    }

    static Color hex(int rgb) {
        return new Color(rgb);
    }

    static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
