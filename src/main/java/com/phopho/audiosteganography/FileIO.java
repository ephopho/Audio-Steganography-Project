package com.phopho.audiosteganography;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** File reading and writing shared by the CLI and the desktop app. */
public final class FileIO {

    /** Largest file held in memory (Java arrays top out just under 2 GB). */
    public static final long MAX_FILE_BYTES = Integer.MAX_VALUE - 64;

    private FileIO() {
    }

    public static byte[] read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("File not found: " + path);
        }
        long size = Files.size(path);
        if (size > MAX_FILE_BYTES) {
            throw new IOException(path.getFileName() + " is too large (over 2 GB).");
        }
        return Files.readAllBytes(path);
    }

    /**
     * Write via a temporary file in the same folder, then move it into place, so
     * a crash or full disk never leaves a half-written file behind. This also makes
     * it safe to overwrite the file the data was read from.
     */
    public static void writeAtomically(Path target, byte[] data) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, ".stego-", ".tmp");
        try {
            Files.write(temp, data);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** The version from the jar manifest, or "dev" when run from classes. */
    public static String appVersion() {
        String v = FileIO.class.getPackage().getImplementationVersion();
        return v == null ? "dev" : v;
    }
}
