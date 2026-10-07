package com.phopho.audiosteganography.ui;

import com.phopho.audiosteganography.engine.StegoException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

/** Turns failures into sentences for the user (never a stack trace). */
final class Messages {

    private Messages() {
    }

    static String of(Throwable error) {
        if (error instanceof StegoException e) {
            return e.getMessage();
        }
        if (error instanceof AccessDeniedException e) {
            return "Access to " + e.getFile() + " was denied. Choose another folder.";
        }
        if (error instanceof NoSuchFileException e) {
            return "File not found: " + e.getFile();
        }
        if (error instanceof IOException e) {
            return e.getMessage() == null ? "The file couldn't be read or written." : e.getMessage();
        }
        if (error instanceof OutOfMemoryError) {
            return "Not enough memory for a file this large.";
        }
        return "Something went wrong: " + error;
    }
}
