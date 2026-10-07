import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.tools.JavaCompiler;

/**
 * Zero-dependency build script. Needs only a JDK (21 or newer):
 *
 * <pre>
 *   java Build.java            compile, run the tests, build dist/audio-steganography.jar
 *   java Build.java test       compile and run the tests
 *   java Build.java package    the above, plus a native app (jpackage) in dist/app
 *   java Build.java clean      delete build/ and dist/
 * </pre>
 */
public class Build {

    static final String VERSION = "2.0.0";
    static final String APP_NAME = "Audio Steganography";
    static final String MAIN_CLASS = "com.phopho.audiosteganography.Main";
    static final String JAR_NAME = "audio-steganography.jar";
    static final String RELEASE = "21";

    static final Path ROOT = Path.of("").toAbsolutePath();
    static final Path BUILD = ROOT.resolve("build");
    static final Path MAIN_OUT = BUILD.resolve("classes/main");
    static final Path TEST_OUT = BUILD.resolve("classes/test");
    static final Path DIST = ROOT.resolve("dist");

    public static void main(String[] args) throws Exception {
        String target = args.length == 0 ? "jar" : args[0];
        switch (target) {
            case "clean" -> clean();
            case "test" -> {
                compile();
                test();
            }
            case "jar" -> {
                compile();
                test();
                jar();
            }
            case "package" -> {
                compile();
                test();
                jar();
                nativeApp();
            }
            default -> {
                System.err.println("Unknown target '" + target + "'. Use: jar (default), test, package, clean");
                System.exit(2);
            }
        }
    }

    static void clean() throws IOException {
        delete(BUILD);
        delete(DIST);
        System.out.println("Cleaned build/ and dist/");
    }

    static void compile() throws IOException {
        delete(BUILD.resolve("classes"));
        javac(sources("src/main/java"), MAIN_OUT, null);
        copyResources(ROOT.resolve("src/main/resources"), MAIN_OUT);
        javac(sources("src/test/java"), TEST_OUT, MAIN_OUT.toString());
        copyResources(ROOT.resolve("src/test/resources"), TEST_OUT);
    }

    static void javac(List<String> sources, Path out, String classPath) throws IOException {
        Files.createDirectories(out);
        JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            fail("No Java compiler found. Run this with a JDK, not a JRE.");
        }
        List<String> options = new ArrayList<>(List.of(
                "--release", RELEASE, "-encoding", "UTF-8", "-Xlint:all", "-Werror", "-d", out.toString()));
        if (classPath != null) {
            options.addAll(List.of("-cp", classPath));
        }
        options.addAll(sources);
        if (compiler.run(null, null, null, options.toArray(String[]::new)) != 0) {
            fail("Compilation failed.");
        }
    }

    /** Run the tests in a separate JVM so they get a clean, headless environment. */
    static void test() throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        String cp = MAIN_OUT + File.pathSeparator + TEST_OUT;
        Process p = new ProcessBuilder(java, "-Djava.awt.headless=true", "-cp", cp,
                "com.phopho.audiosteganography.testing.TestRunner")
                .inheritIO().start();
        if (p.waitFor() != 0) {
            fail("Tests failed.");
        }
    }

    static void jar() throws IOException {
        Files.createDirectories(DIST);
        Path jar = DIST.resolve(JAR_NAME);
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(Attributes.Name.MAIN_CLASS, MAIN_CLASS);
        attrs.put(Attributes.Name.IMPLEMENTATION_TITLE, APP_NAME);
        attrs.put(Attributes.Name.IMPLEMENTATION_VERSION, VERSION);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest);
             Stream<Path> files = Files.walk(MAIN_OUT)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                out.putNextEntry(new JarEntry(MAIN_OUT.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, out);
                out.closeEntry();
            }
        }
        System.out.println("Built " + ROOT.relativize(jar) + " (" + Files.size(jar) / 1024 + " KB)");
    }

    /** A self-contained app folder with its own trimmed Java runtime (no install needed). */
    static void nativeApp() throws Exception {
        ToolProvider jpackage = ToolProvider.findFirst("jpackage")
                .orElseThrow(() -> new IllegalStateException("jpackage not found in this JDK"));
        Path input = BUILD.resolve("package-input");
        delete(input);
        Files.createDirectories(input);
        Files.copy(DIST.resolve(JAR_NAME), input.resolve(JAR_NAME));
        Path dest = DIST.resolve("app");
        delete(dest);

        List<String> args = new ArrayList<>(List.of(
                "--type", "app-image",
                "--name", APP_NAME,
                "--app-version", VERSION,
                "--vendor", "Phopho",
                "--description", "Hide encrypted messages and files inside WAV audio",
                "--input", input.toString(),
                "--main-jar", JAR_NAME,
                "--main-class", MAIN_CLASS,
                "--add-modules", "java.desktop,java.prefs",
                "--jlink-options", "--strip-debug --no-man-pages --no-header-files",
                "--dest", dest.toString()));
        Path icon = writeIcon();
        if (icon != null) {
            args.addAll(List.of("--icon", icon.toString()));
        }
        int code = jpackage.run(System.out, System.err, args.toArray(String[]::new));
        if (code != 0) {
            fail("jpackage failed.");
        }
        System.out.println("Built " + ROOT.relativize(dest) + " (run the '" + APP_NAME + "' launcher inside)");
    }

    /** Render the app icon (drawn in code by AppIcon) into the format jpackage wants on this OS. */
    static Path writeIcon() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {MAIN_OUT.toUri().toURL()})) {
            Method render = loader.loadClass("com.phopho.audiosteganography.ui.AppIcon")
                    .getMethod("render", int.class);
            if (os.contains("win")) {
                Path ico = BUILD.resolve("icon.ico");
                int[] sizes = {16, 24, 32, 48, 64, 128, 256};
                List<byte[]> pngs = new ArrayList<>();
                for (int size : sizes) {
                    pngs.add(png((BufferedImage) render.invoke(null, size)));
                }
                Files.write(ico, ico(sizes, pngs));
                return ico;
            }
            if (os.contains("linux")) {
                Path png = BUILD.resolve("icon.png");
                Files.write(png, png((BufferedImage) render.invoke(null, 256)));
                return png;
            }
            return null; // macOS wants .icns; fall back to the default icon there.
        }
    }

    static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** A Windows .ico holding PNG images (supported since Windows Vista). */
    static byte[] ico(int[] sizes, List<byte[]> pngs) {
        int headerSize = 6 + 16 * sizes.length;
        int total = headerSize + pngs.stream().mapToInt(b -> b.length).sum();
        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        int offset = headerSize;
        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i] >= 256 ? 0 : sizes[i];
            buf.put((byte) s).put((byte) s).put((byte) 0).put((byte) 0);
            buf.putShort((short) 1).putShort((short) 32);
            buf.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        pngs.forEach(buf::put);
        return buf.array();
    }

    static List<String> sources(String dir) throws IOException {
        Path root = ROOT.resolve(dir);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(f -> f.toString().endsWith(".java")).map(Path::toString).sorted().toList();
        }
    }

    static void copyResources(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) {
            return;
        }
        try (Stream<Path> files = Files.walk(from)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path target = to.resolve(from.relativize(file).toString());
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static void delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> files = Files.walk(path)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    static void fail(String message) {
        System.err.println("BUILD FAILED: " + message);
        System.exit(1);
    }
}
