package com.phopho.audiosteganography.engine;

import com.phopho.audiosteganography.engine.StegoException.Code;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Authenticated password-based encryption, byte-for-byte compatible with Blank.
 *
 * <p>Replaces the 2020 {@code PBEWithMD5AndDES} (56-bit DES, MD5, a hardcoded
 * salt, 20 iterations, no integrity check) with scrypt (N=2^14, r=8, p=1, random
 * 16-byte salt) and AES-256-GCM. The GCM tag is what lets a wrong password or a
 * tampered file be detected instead of yielding garbage.
 *
 * <pre>
 * blob (current): [ logN (1) | salt (16) | nonce (12) | ciphertext + tag (16) ]
 * blob (legacy) : [            salt (16) | nonce (12) | ciphertext + tag (16) ]  N = 2^15
 * </pre>
 * The legacy layout is only read, for messages made by Blank 1.0.0.
 */
final class Crypto {

    static final int SALT_LEN = 16;
    static final int NONCE_LEN = 12;
    static final int TAG_LEN = 16;
    static final int KEY_LEN = 32;

    static final int LOG_N = 14;
    static final int R = 8;
    static final int P = 1;
    private static final int LEGACY_LOG_N = 15;
    // Bounds on a cost byte read from a file, so a damaged one can't demand gigabytes.
    private static final int MIN_LOG_N = 10;
    private static final int MAX_LOG_N = 20;

    /** Bytes the current blob adds on top of the plaintext. */
    static final int OVERHEAD = 1 + SALT_LEN + NONCE_LEN + TAG_LEN;

    private static final SecureRandom RANDOM = new SecureRandom();

    private Crypto() {
    }

    static byte[] encrypt(byte[] plaintext, byte[] password) {
        byte[] salt = new byte[SALT_LEN];
        byte[] nonce = new byte[NONCE_LEN];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(nonce);
        byte[] key = Scrypt.derive(password, salt, 1 << LOG_N, R, P, KEY_LEN);
        try {
            byte[] sealed = cipher(Cipher.ENCRYPT_MODE, key, nonce).doFinal(plaintext);
            byte[] blob = new byte[1 + SALT_LEN + NONCE_LEN + sealed.length];
            blob[0] = LOG_N;
            System.arraycopy(salt, 0, blob, 1, SALT_LEN);
            System.arraycopy(nonce, 0, blob, 1 + SALT_LEN, NONCE_LEN);
            System.arraycopy(sealed, 0, blob, 1 + SALT_LEN + NONCE_LEN, sealed.length);
            return blob;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM is unavailable", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    static byte[] decrypt(byte[] blob, byte[] password, boolean legacyLayout) throws StegoException {
        int logN;
        int offset;
        if (legacyLayout) {
            logN = LEGACY_LOG_N;
            offset = 0;
        } else {
            if (blob.length < 1) {
                throw corrupt();
            }
            logN = blob[0] & 0xFF;
            if (logN < MIN_LOG_N || logN > MAX_LOG_N) {
                throw corrupt();
            }
            offset = 1;
        }
        if (blob.length < offset + SALT_LEN + NONCE_LEN + TAG_LEN) {
            throw corrupt();
        }

        byte[] salt = Arrays.copyOfRange(blob, offset, offset + SALT_LEN);
        byte[] nonce = Arrays.copyOfRange(blob, offset + SALT_LEN, offset + SALT_LEN + NONCE_LEN);
        int sealedAt = offset + SALT_LEN + NONCE_LEN;
        byte[] key = Scrypt.derive(password, salt, 1 << logN, R, P, KEY_LEN);
        try {
            return cipher(Cipher.DECRYPT_MODE, key, nonce).doFinal(blob, sealedAt, blob.length - sealedAt);
        } catch (AEADBadTagException e) {
            throw new StegoException(Code.WRONG_PASSWORD,
                    "Wrong password, or the audio was changed after the secret was hidden.");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM is unavailable", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    private static Cipher cipher(int mode, byte[] key, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LEN * 8, nonce));
        return cipher;
    }

    private static StegoException corrupt() {
        return new StegoException(Code.CORRUPT, "The hidden data is incomplete or damaged.");
    }
}
