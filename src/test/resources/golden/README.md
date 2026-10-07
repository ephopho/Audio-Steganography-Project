# Golden fixtures

These WAVs were written by the **Blank iOS app's own engine** (TypeScript,
`@noble/hashes` scrypt + `@noble/ciphers` AES-256-GCM, in `blank/src/stego`), so
`BlankInteropTest` checks the Java engine against a second implementation rather
than against itself.

| File | Contents |
|---|---|
| `cover-8k-mono16.wav` | 0.5 s, 8 kHz, mono, 16-bit PCM sine. The clean cover. |
| `blank-v2-text.wav` | Cover + `hideMessage(cover, "Meet at the docks, 9pm. 🕵️ Δelta", "correct horse battery staple")` (current v2 format, scrypt N=2^14). |
| `blank-v1-text.wav` | Cover + a Blank 1.0.0 (v1) payload: no cost byte, scrypt N=2^15, fixed salt `01..10` and nonce `a0..ab`, message `legacy v1 hello`. |

The reverse direction (a WAV written by this Java engine revealed by Blank's
`revealMessage`) was checked by hand on 2026-10-07 and passed.
