package com.phopho.audiosteganography.testing;

import com.phopho.audiosteganography.engine.StegoException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** The handful of assertions the tests need. */
public final class Assert {

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    private Assert() {
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void bytesEqual(byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("byte arrays differ (lengths " + expected.length + " vs " + actual.length + ")");
        }
    }

    public static void hexEqual(String expectedHex, byte[] actual) {
        equal(expectedHex, HexFormat.of().formatHex(actual));
    }

    /** Assert {@code action} fails with a {@link StegoException} carrying {@code code}. */
    public static StegoException failsWith(StegoException.Code code, Action action) {
        try {
            action.run();
        } catch (StegoException e) {
            if (e.code() != code) {
                throw new AssertionError("expected " + code + " but got " + e.code() + ": " + e.getMessage(), e);
            }
            return e;
        } catch (Exception e) {
            throw new AssertionError("expected StegoException " + code + " but got " + e, e);
        }
        throw new AssertionError("expected StegoException " + code + " but nothing was thrown");
    }
}
