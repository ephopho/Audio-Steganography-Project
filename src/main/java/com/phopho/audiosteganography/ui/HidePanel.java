package com.phopho.audiosteganography.ui;

import static com.phopho.audiosteganography.ui.Layouts.fieldLabel;
import static com.phopho.audiosteganography.ui.Layouts.left;
import static com.phopho.audiosteganography.ui.Layouts.onEdit;
import static com.phopho.audiosteganography.ui.Layouts.spaced;

import com.phopho.audiosteganography.FileIO;
import com.phopho.audiosteganography.engine.Secret;
import com.phopho.audiosteganography.engine.Stego;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;

/** Hide screen: 1 cover audio, 2 secret (message or file), 3 password, 4 the saved result. */
final class HidePanel extends JPanel {

    private LoadedAudio cover;
    private Secret fileSecret;
    private boolean busy;

    private final CardLayout coverCards = new CardLayout();
    private final JPanel coverStack = Ui.clear(coverCards);
    private final AudioSummary coverSummary;
    private final Notice coverNotice = new Notice();

    private final Segmented mode = new Segmented(false, "Message", "File");
    private final CardLayout secretCards = new CardLayout();
    private final JPanel secretStack = Ui.clear(secretCards);
    private final JTextArea message = Fields.textArea("Type the message to hide…");
    private final JPanel fileHolder = Ui.clear(new BorderLayout());
    private DropZone fileDrop;
    private final FileRow fileRow = new FileRow();
    private final CapacityMeter meter = new CapacityMeter();

    private final JPasswordField password = Fields.password("Choose a password");
    private final JPasswordField confirm = Fields.password("Type it again");
    private final Notice protectNotice = new Notice();
    private final Button hideButton = new Button("Hide and save…",
            Icons.of(Icons.Glyph.LOCK, 17, Theme.Palette::accentText), Button.Kind.PRIMARY);
    private final JLabel hint = new JLabel(" ");
    private boolean hintIsProblem;

    private final Card resultCard = new Card("4", "Done");
    private final Card tips = TipsCard.of(
            new TipsCard.Tip(Icons.Glyph.MUSIC, "Any uncompressed WAV works: 8, 16, 24 or 32-bit. Longer clips hold more."),
            new TipsCard.Tip(Icons.Glyph.ALERT, "Send the saved WAV as a file. Converting it to MP3, trimming it or "
                    + "sending it as a voice note destroys the secret."),
            new TipsCard.Tip(Icons.Glyph.LOCK, "The secret is encrypted (AES-256) before it is hidden. "
                    + "Only the password opens it."));
    private final Notice resultNotice = new Notice();
    private final PlayControl playResult;
    private Path lastOutput;

    HidePanel(AudioPlayer player) {
        super(new GridLayout(1, 2, Layouts.CARD_GAP, 0));
        setOpaque(false);
        coverSummary = new AudioSummary(player, "Play", this::browse, this::playbackFailed);
        playResult = new PlayControl(player, "Play result", this::playbackFailed);

        add(Layouts.column(-1, coverCard(), resultCard(), tips));
        add(Layouts.column(0, secretCard(), protectCard()));
        refresh();
    }

    // ---- layout ---------------------------------------------------------------------------

    private Card coverCard() {
        Card card = new Card("1", "Cover audio");
        DropZone drop = new DropZone(Icons.Glyph.UPLOAD, "Drop a WAV file here", "or click to browse",
                236, this::browse, this::load);
        coverStack.add(drop, "empty");
        coverStack.add(coverSummary, "loaded");
        coverSummary.setTransferHandler(DropZone.fileDropHandler(this::load, active -> { }));
        card.body().add(coverStack, BorderLayout.CENTER);
        card.body().add(spaced(coverNotice, 14, 0), BorderLayout.SOUTH);
        return card;
    }

    private Card secretCard() {
        Card card = new Card("2", "Secret");
        card.setTrailing(mode);
        mode.onChange(i -> {
            secretCards.show(secretStack, i == 0 ? "text" : "file");
            refresh();
        });

        secretStack.add(new Fields.Box(Fields.scroll(message), null, 0), "text");

        fileDrop = new DropZone(Icons.Glyph.FILE, "Drop a file to hide",
                "or click to choose one · any type", 150, this::browseSecretFile, this::loadSecretFile);
        Button remove = new Button("", Icons.of(Icons.Glyph.X, 16, Theme.Palette::textSecondary), Button.Kind.GHOST);
        remove.setToolTipText("Remove this file");
        remove.getAccessibleContext().setAccessibleName("Remove this file");
        remove.addActionListener(e -> {
            fileSecret = null;
            showInFileHolder(fileDrop);
            refresh();
        });
        fileRow.addAction(remove);
        showInFileHolder(fileDrop);
        secretStack.add(fileHolder, "file");

        message.getDocument().addDocumentListener(onEdit(this::refresh));
        card.body().add(secretStack, BorderLayout.CENTER);
        card.body().add(spaced(meter, 16, 0), BorderLayout.SOUTH);
        return card;
    }

    private Card protectCard() {
        Card card = new Card("3", "Protect");
        JPanel body = Ui.clear(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(fieldLabel("Password"));
        body.add(Box.createVerticalStrut(6));
        body.add(left(new Fields.Box(password, Fields.revealToggle(password), 40)));
        body.add(Box.createVerticalStrut(12));
        body.add(fieldLabel("Confirm password"));
        body.add(Box.createVerticalStrut(6));
        body.add(left(new Fields.Box(confirm, Fields.revealToggle(confirm), 40)));
        body.add(Box.createVerticalStrut(16));
        body.add(spaced(protectNotice, 0, 12));
        body.add(left(hideButton));
        body.add(Box.createVerticalStrut(10));
        hint.setFont(Theme.font(12.5f));
        Theme.bind(hint, h -> h.setForeground(hintIsProblem ? Theme.p().danger() : Theme.p().textTertiary()));
        body.add(left(hint));

        password.getDocument().addDocumentListener(onEdit(this::refresh));
        confirm.getDocument().addDocumentListener(onEdit(this::refresh));
        confirm.addActionListener(e -> {
            if (hideButton.isEnabled()) {
                hideButton.doClick();
            }
        });
        hideButton.addActionListener(e -> hideSecret());
        card.body().add(body, BorderLayout.CENTER);
        return card;
    }

    private Card resultCard() {
        JPanel actions = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.add(playResult.button());
        actions.add(Box.createHorizontalStrut(8));
        Button folder = new Button("Show in folder", Icons.of(Icons.Glyph.FOLDER, 16, Theme.Palette::text),
                Button.Kind.SECONDARY);
        folder.addActionListener(e -> {
            if (lastOutput != null) {
                Ui.showInFolder(lastOutput);
            }
        });
        actions.add(folder);
        resultCard.body().add(resultNotice, BorderLayout.CENTER);
        resultCard.body().add(spaced(actions, 14, 0), BorderLayout.SOUTH);
        resultCard.setVisible(false);
        return resultCard;
    }

    // ---- behaviour ------------------------------------------------------------------------

    void browse() {
        Path path = Ui.chooseOpen(this, "Choose the cover audio (WAV)", "*.wav");
        if (path != null) {
            load(path);
        }
    }

    void load(Path path) {
        Ui.background(() -> LoadedAudio.read(path), loaded -> {
            Ui.rememberDir(path);
            cover = loaded;
            Stego.Inspection info = loaded.info();
            coverSummary.show(path, loaded.bytes(), info);
            coverSummary.setCaption(info.embeddable() ? "Room for " + Stego.formatBytes(info.textCapacityBytes()) : "");
            coverCards.show(coverStack, "loaded");
            resultCard.setVisible(false);
            tips.setVisible(true);
            protectNotice.clear();
            describeCover(info);
            refresh();
        }, error -> coverNotice.show(Notice.Tone.DANGER, Messages.of(error)));
    }

    private void describeCover(Stego.Inspection info) {
        if (!info.embeddable()) {
            coverNotice.show(Notice.Tone.DANGER, info.wav().unsupportedReason());
        } else if (info.hasSecret()) {
            coverNotice.show(Notice.Tone.WARNING,
                    "This audio already holds a hidden secret. Hiding a new one replaces it.");
        } else if (!info.wav().blankCompatible()) {
            coverNotice.show(Notice.Tone.INFO, "Messages hidden here won't open in Blank on iPhone, "
                    + "which reads 16-bit WAVs only. They will open in this app.");
        } else {
            coverNotice.clear();
        }
    }

    private void browseSecretFile() {
        Path path = Ui.chooseOpen(this, "Choose a file to hide", null);
        if (path != null) {
            loadSecretFile(path);
        }
    }

    private void loadSecretFile(Path path) {
        Ui.background(() -> Secret.file(path.getFileName().toString(), FileIO.read(path)), secret -> {
            fileSecret = secret;
            long packed = secret.embeddedSize() - Stego.HEADER_BYTES;
            String size = Stego.formatBytes(secret.originalSize());
            fileRow.show(secret.name(), packed < secret.originalSize()
                    ? size + "  ·  compresses to about " + Stego.formatBytes(packed) : size);
            showInFileHolder(fileRow);
            mode.select(1);
            refresh();
        }, error -> protectNotice.show(Notice.Tone.DANGER, Messages.of(error)));
    }

    /** Show the drop zone or the chosen file, each at its own natural height. */
    private void showInFileHolder(JComponent component) {
        fileHolder.removeAll();
        fileHolder.add(component, BorderLayout.NORTH);
        fileHolder.revalidate();
        fileHolder.repaint();
    }

    private Secret currentSecret() {
        if (mode.selected() == 1) {
            return fileSecret;
        }
        return message.getDocument().getLength() == 0 ? null : Secret.text(message.getText());
    }

    /** Re-evaluate capacity and what (if anything) blocks hiding; explain it under the button. */
    private void refresh() {
        Secret secret = currentSecret();
        boolean usable = cover != null && cover.info().embeddable();
        meter.set(secret == null ? 0 : secret.embeddedSize(), usable ? cover.info().capacityBytes() : -1);

        boolean problem = false;
        String reason;
        if (busy) {
            reason = "Encrypting and hiding…";
        } else if (cover == null) {
            reason = "Start by choosing a cover audio file.";
        } else if (!usable) {
            reason = "Choose a different cover audio file.";
        } else if (secret == null) {
            reason = mode.selected() == 0 ? "Type the message you want to hide." : "Choose the file you want to hide.";
        } else if (!meter.fits()) {
            problem = true;
            reason = "Too large for this audio. Pick a longer clip or a smaller secret.";
        } else if (password.getDocument().getLength() == 0) {
            reason = "Choose a password. Without it the secret can't be read.";
        } else if (confirm.getDocument().getLength() == 0) {
            reason = "Type the password again to confirm it.";
        } else if (!passwordsMatch()) {
            problem = true;
            reason = "The passwords don't match.";
        } else {
            reason = password.getDocument().getLength() < 8
                    ? "Tip: a longer password (8+ characters) is much harder to guess."
                    : "Forgotten passwords can't be recovered, so keep it safe.";
        }
        hintIsProblem = problem;
        hint.setText(reason);
        hint.setForeground(problem ? Theme.p().danger() : Theme.p().textTertiary());
        hideButton.setEnabled(!busy && usable && secret != null && meter.fits()
                && password.getDocument().getLength() > 0 && passwordsMatch());
    }

    private boolean passwordsMatch() {
        char[] a = password.getPassword();
        char[] b = confirm.getPassword();
        try {
            return Arrays.equals(a, b);
        } finally {
            Arrays.fill(a, '\0');
            Arrays.fill(b, '\0');
        }
    }

    private void hideSecret() {
        Secret secret = currentSecret();
        LoadedAudio source = cover;
        if (secret == null || source == null) {
            return;
        }
        String name = source.path().getFileName().toString();
        String stem = name.toLowerCase(Locale.ROOT).endsWith(".wav") ? name.substring(0, name.length() - 4) : name;
        Path target = Ui.chooseSave(this, "Save the audio with the hidden secret",
                source.path().resolveSibling(stem + "-hidden.wav"));
        if (target == null) {
            return;
        }

        char[] pw = password.getPassword();
        setBusy(true);
        protectNotice.clear();
        Ui.background(() -> {
            try {
                Stego.HideResult result = Stego.hide(source.bytes(), secret, pw);
                FileIO.writeAtomically(target, result.wav());
                return result;
            } finally {
                Arrays.fill(pw, '\0');
            }
        }, result -> {
            setBusy(false);
            password.setText("");
            confirm.setText("");
            showResult(target, result, secret, source.info());
        }, error -> {
            setBusy(false);
            protectNotice.show(Notice.Tone.DANGER, Messages.of(error));
        });
    }

    private void showResult(Path target, Stego.HideResult result, Secret secret, Stego.Inspection info) {
        lastOutput = target;
        String text = String.format(Locale.getDefault(),
                "Saved %s. %,d of %,d samples changed, each by the smallest possible step, far too little to hear.",
                target.getFileName(), result.samplesChanged(), info.wav().sampleCount());
        if (secret.kind() == Stego.Kind.TEXT && info.wav().blankCompatible()) {
            text += " The message also opens in Blank on iPhone.";
        }
        resultNotice.show(Notice.Tone.SUCCESS, text);
        playResult.setSource(result.wav(), info.wav());
        resultCard.setVisible(true);
        tips.setVisible(false);
        revalidate();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        hideButton.setText(busy ? "Hiding…" : "Hide and save…");
        refresh();
    }

    private void playbackFailed() {
        coverNotice.show(Notice.Tone.WARNING, "This computer couldn't play the audio (no output device for its format).");
    }
}
