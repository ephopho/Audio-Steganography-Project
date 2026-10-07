package com.phopho.audiosteganography.ui;

import static com.phopho.audiosteganography.ui.Layouts.fieldLabel;
import static com.phopho.audiosteganography.ui.Layouts.left;
import static com.phopho.audiosteganography.ui.Layouts.onEdit;
import static com.phopho.audiosteganography.ui.Layouts.spaced;

import com.phopho.audiosteganography.FileIO;
import com.phopho.audiosteganography.engine.Revealed;
import com.phopho.audiosteganography.engine.Stego;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** Reveal screen: 1 audio with a secret, 2 password, 3 the recovered message or file. */
final class RevealPanel extends JPanel {

    private LoadedAudio audio;
    private Revealed revealed;
    private boolean busy;

    private final CardLayout audioCards = new CardLayout();
    private final JPanel audioStack = Ui.clear(audioCards);
    private final AudioSummary summary;
    private final Notice detectNotice = new Notice();

    private final JPasswordField password = Fields.password("Password used to hide it");
    private final Button revealButton = new Button("Reveal",
            Icons.of(Icons.Glyph.UNLOCK, 17, Theme.Palette::accentText), Button.Kind.PRIMARY);
    private final Notice unlockNotice = new Notice();

    private final CardLayout secretCards = new CardLayout();
    private final JPanel secretStack = Ui.clear(secretCards);
    private final JTextArea messageView = Fields.textArea(null);
    private final FileRow fileRow = new FileRow();
    private final Button copyButton = new Button("Copy", Icons.of(Icons.Glyph.COPY, 16, Theme.Palette::text),
            Button.Kind.SECONDARY);

    RevealPanel(AudioPlayer player) {
        super(new GridLayout(1, 2, Layouts.CARD_GAP, 0));
        setOpaque(false);
        summary = new AudioSummary(player, "Play", this::browse, () -> detectNotice.show(Notice.Tone.WARNING,
                "This computer couldn't play the audio (no output device for its format)."));

        add(Layouts.column(-1, audioCard(), TipsCard.of(
                new TipsCard.Tip(Icons.Glyph.MUSIC, "Open the exact WAV you were sent. A converted, trimmed or "
                        + "re-recorded copy no longer holds the secret."),
                new TipsCard.Tip(Icons.Glyph.MESSAGE, "Messages hidden with Blank on iPhone open here too."),
                new TipsCard.Tip(Icons.Glyph.LOCK, "Passwords are case-sensitive. After a wrong guess, nothing is "
                        + "changed, so you can simply try again."))));
        add(Layouts.column(1, unlockCard(), secretCard()));
        refresh();
    }

    // ---- layout ---------------------------------------------------------------------------

    private Card audioCard() {
        Card card = new Card("1", "Audio with a secret");
        DropZone drop = new DropZone(Icons.Glyph.UPLOAD, "Drop the WAV file here", "or click to browse",
                236, this::browse, this::load);
        audioStack.add(drop, "empty");
        audioStack.add(summary, "loaded");
        summary.setTransferHandler(DropZone.fileDropHandler(this::load, active -> { }));
        card.body().add(audioStack, BorderLayout.CENTER);
        card.body().add(spaced(detectNotice, 14, 0), BorderLayout.SOUTH);
        return card;
    }

    private Card unlockCard() {
        Card card = new Card("2", "Unlock");
        JPanel body = Ui.clear(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(fieldLabel("Password"));
        body.add(Box.createVerticalStrut(6));
        body.add(left(new Fields.Box(password, Fields.revealToggle(password), 40)));
        body.add(Box.createVerticalStrut(16));
        body.add(spaced(unlockNotice, 0, 12));
        body.add(left(revealButton));

        password.getDocument().addDocumentListener(onEdit(() -> {
            unlockNotice.clear();
            refresh();
        }));
        password.addActionListener(e -> {
            if (revealButton.isEnabled()) {
                revealButton.doClick();
            }
        });
        revealButton.addActionListener(e -> reveal());
        card.body().add(body, BorderLayout.CENTER);
        return card;
    }

    private Card secretCard() {
        Card card = new Card("3", "Secret");

        JLabel empty = Ui.label("The hidden message or file appears here once it's unlocked.",
                Theme.font(13), Theme.Palette::textTertiary);
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        secretStack.add(empty, "empty");

        messageView.setEditable(false);
        JPanel textPane = Ui.clear(new BorderLayout(0, 14));
        textPane.add(new Fields.Box(Fields.scroll(messageView), null, 0, Theme.Palette::surfaceAlt), BorderLayout.CENTER);
        JPanel textActions = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        copyButton.addActionListener(e -> copy());
        textActions.add(copyButton);
        textActions.add(Box.createHorizontalStrut(8));
        Button saveText = new Button("Save as…", Icons.of(Icons.Glyph.DOWNLOAD, 16, Theme.Palette::text),
                Button.Kind.SECONDARY);
        saveText.addActionListener(e -> saveRevealed());
        textActions.add(saveText);
        textActions.add(Box.createHorizontalStrut(8));
        textActions.add(clearButton());
        textPane.add(textActions, BorderLayout.SOUTH);
        secretStack.add(textPane, "text");

        Button saveFile = new Button("Save file…", Icons.of(Icons.Glyph.DOWNLOAD, 16, Theme.Palette::accentText),
                Button.Kind.PRIMARY);
        saveFile.addActionListener(e -> saveRevealed());
        JPanel fileActions = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fileActions.add(saveFile);
        fileActions.add(Box.createHorizontalStrut(8));
        fileActions.add(clearButton());
        JPanel filePane = Ui.clear(new BorderLayout(0, 14));
        filePane.add(fileRow, BorderLayout.NORTH);
        filePane.add(fileActions, BorderLayout.CENTER);
        JPanel fileWrap = Ui.clear(new BorderLayout());
        fileWrap.add(filePane, BorderLayout.NORTH);
        secretStack.add(fileWrap, "file");

        card.body().add(secretStack, BorderLayout.CENTER);
        return card;
    }

    private Button clearButton() {
        Button clear = new Button("Clear", Icons.of(Icons.Glyph.X, 16, Theme.Palette::text), Button.Kind.GHOST);
        clear.setToolTipText("Remove the revealed secret from the screen");
        clear.addActionListener(e -> clearSecret());
        return clear;
    }

    // ---- behaviour ------------------------------------------------------------------------

    void browse() {
        Path path = Ui.chooseOpen(this, "Choose the audio that holds a secret (WAV)", "*.wav");
        if (path != null) {
            load(path);
        }
    }

    void load(Path path) {
        Ui.background(() -> LoadedAudio.read(path), loaded -> {
            Ui.rememberDir(path);
            audio = loaded;
            summary.show(path, loaded.bytes(), loaded.info());
            audioCards.show(audioStack, "loaded");
            clearSecret();
            unlockNotice.clear();
            describe(loaded.info());
            refresh();
            if (canReveal()) {
                password.requestFocusInWindow();
            }
        }, error -> detectNotice.show(Notice.Tone.DANGER, Messages.of(error)));
    }

    private void describe(Stego.Inspection info) {
        if (!info.embeddable()) {
            detectNotice.show(Notice.Tone.DANGER, info.wav().unsupportedReason());
        } else if (!info.hasSecret()) {
            detectNotice.show(Notice.Tone.NEUTRAL, "No hidden secret was found in this audio.");
        } else if (!info.hiddenReadable()) {
            detectNotice.show(Notice.Tone.WARNING,
                    "This audio holds a secret made by a newer version of the app. Update to open it.");
        } else {
            detectNotice.show(Notice.Tone.SUCCESS, info.hidden() == Stego.Kind.TEXT
                    ? "A hidden message was found. Enter the password to read it."
                    : "A hidden file was found. Enter the password to unlock it.");
        }
    }

    private boolean canReveal() {
        return audio != null && audio.info().hasSecret() && audio.info().hiddenReadable();
    }

    private void refresh() {
        revealButton.setEnabled(!busy && canReveal() && password.getDocument().getLength() > 0);
    }

    private void reveal() {
        LoadedAudio source = audio;
        if (source == null) {
            return;
        }
        char[] pw = password.getPassword();
        busy = true;
        revealButton.setText("Unlocking…");
        refresh();
        Ui.background(() -> {
            try {
                return Stego.reveal(source.bytes(), pw);
            } finally {
                Arrays.fill(pw, '\0');
            }
        }, result -> {
            done();
            password.setText("");
            detectNotice.show(Notice.Tone.SUCCESS, result instanceof Revealed.Text
                    ? "Unlocked. The hidden message is shown on the right."
                    : "Unlocked. Save the hidden file from the right.");
            show(result);
        }, error -> {
            done();
            unlockNotice.show(Notice.Tone.DANGER, Messages.of(error));
            password.selectAll();
            password.requestFocusInWindow();
        });
    }

    private void done() {
        busy = false;
        revealButton.setText("Reveal");
        refresh();
    }

    private void show(Revealed result) {
        revealed = result;
        switch (result) {
            case Revealed.Text text -> {
                messageView.setText(text.message());
                messageView.setCaretPosition(0);
                secretCards.show(secretStack, "text");
            }
            case Revealed.HiddenFile file -> {
                fileRow.show(file.name(), Stego.formatBytes(file.data().length));
                secretCards.show(secretStack, "file");
            }
        }
    }

    private void clearSecret() {
        revealed = null;
        messageView.setText("");
        secretCards.show(secretStack, "empty");
    }

    private void copy() {
        if (revealed instanceof Revealed.Text text) {
            Ui.copyToClipboard(text.message());
            copyButton.setText("Copied");
            Timer reset = new Timer(1500, e -> copyButton.setText("Copy"));
            reset.setRepeats(false);
            reset.start();
        }
    }

    private void saveRevealed() {
        Revealed current = revealed;
        if (current == null) {
            return;
        }
        String name = current instanceof Revealed.HiddenFile file ? file.name() : "hidden-message.txt";
        Path target = Ui.chooseSave(this, "Save the revealed secret", Path.of(Ui.lastDir(), name));
        if (target == null) {
            return;
        }
        byte[] data = switch (current) {
            case Revealed.Text text -> text.message().getBytes(StandardCharsets.UTF_8);
            case Revealed.HiddenFile file -> file.data();
        };
        Ui.background(() -> {
            FileIO.writeAtomically(target, data);
            return target;
        }, saved -> unlockNotice.show(Notice.Tone.SUCCESS, "Saved " + saved.getFileName() + "."),
                error -> unlockNotice.show(Notice.Tone.DANGER, Messages.of(error)));
    }
}
