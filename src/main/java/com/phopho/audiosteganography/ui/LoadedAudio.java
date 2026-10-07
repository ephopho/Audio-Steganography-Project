package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.FileIO;
import com.phopho.audiosteganography.engine.Stego;
import com.phopho.audiosteganography.engine.StegoException;
import java.io.IOException;
import java.nio.file.Path;

/** A WAV read from disk and inspected (capacity, hidden secret), ready for either screen. */
record LoadedAudio(Path path, byte[] bytes, Stego.Inspection info) {

    /** Blocking: call off the UI thread. */
    static LoadedAudio read(Path path) throws IOException, StegoException {
        byte[] bytes = FileIO.read(path);
        return new LoadedAudio(path, bytes, Stego.inspect(bytes));
    }
}
