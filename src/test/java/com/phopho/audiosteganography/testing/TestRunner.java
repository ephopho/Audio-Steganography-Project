package com.phopho.audiosteganography.testing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Minimal test runner (keeps the project free of dependencies): finds every
 * class named {@code *Test} on the test class path and runs its {@link Test}
 * methods, each on a fresh instance. Exits non-zero if anything fails.
 */
public final class TestRunner {

    private TestRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(TestRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> classNames;
        try (Stream<Path> files = Files.walk(root)) {
            classNames = files
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .filter(p -> p.endsWith("Test.class") && !p.contains("$"))
                    .map(p -> p.substring(0, p.length() - ".class".length()).replace('/', '.'))
                    .sorted()
                    .toList();
        }

        int passed = 0;
        List<String> failures = new ArrayList<>();
        long started = System.nanoTime();
        for (String className : classNames) {
            Class<?> type = Class.forName(className);
            List<Method> tests = Stream.of(type.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Test.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .toList();
            for (Method test : tests) {
                String name = type.getSimpleName() + "." + test.getName();
                try {
                    test.invoke(type.getDeclaredConstructor().newInstance());
                    passed++;
                    System.out.println("  ok    " + name);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    failures.add(name + ": " + cause);
                    System.out.println("  FAIL  " + name + "\n        " + cause);
                    if (!(cause instanceof AssertionError)) {
                        cause.printStackTrace(System.out);
                    }
                }
            }
        }

        long ms = (System.nanoTime() - started) / 1_000_000;
        System.out.printf("%n%d passed, %d failed (%d ms)%n", passed, failures.size(), ms);
        if (!failures.isEmpty()) {
            failures.forEach(f -> System.out.println("  FAILED " + f));
            System.exit(1);
        }
        if (passed == 0) {
            System.out.println("No tests found.");
            System.exit(1);
        }
    }
}
