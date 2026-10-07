package com.phopho.audiosteganography.engine;

/**
 * A failure the engine can report. Every failure carries a stable {@link Code}
 * so callers (CLI, desktop UI) can react without string-matching messages.
 * Codes mirror the Blank iOS engine's so both apps describe failures alike.
 */
public final class StegoException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Code {
        /** Not a WAV file, or structurally invalid. */
        BAD_WAV,
        /** A valid WAV, but not PCM audio this engine can carry data in. */
        UNSUPPORTED_FORMAT,
        /** The secret will not fit in the chosen cover audio. */
        TOO_LARGE,
        /** The file holds no hidden payload. */
        NO_MESSAGE,
        /** Wrong password, or the file was changed after the secret was hidden. */
        WRONG_PASSWORD,
        /** No password was given. */
        MISSING_PASSWORD,
        /** A payload is present but malformed, truncated, or from a newer version. */
        CORRUPT
    }

    private final Code code;

    public StegoException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
