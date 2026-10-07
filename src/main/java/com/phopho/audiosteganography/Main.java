package com.phopho.audiosteganography;

import com.phopho.audiosteganography.cli.Cli;
import com.phopho.audiosteganography.ui.App;
import java.awt.GraphicsEnvironment;

/** Entry point: no arguments opens the desktop app; arguments run the command line. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {
            App.launch();
            return;
        }
        System.exit(Cli.system().run(args));
    }
}
