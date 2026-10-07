package com.phopho.audiosteganography.engine;

/** What {@link Stego#reveal} recovered: a text message or a named file. */
public sealed interface Revealed {

    /** A hidden text message. */
    record Text(String message) implements Revealed {
    }

    /** A hidden file; {@code name} has already been made safe to save. */
    record HiddenFile(String name, byte[] data) implements Revealed {
    }
}
