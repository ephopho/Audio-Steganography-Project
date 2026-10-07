package com.phopho.audiosteganography.engine;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * scrypt password-based key derivation (RFC 7914).
 *
 * <p>The JDK has no scrypt, and Blank (the iOS app) derives its keys with it,
 * so it is implemented here rather than adding a dependency. Verified against
 * the RFC 7914 test vectors and against files written by Blank's
 * {@code @noble/hashes} implementation (see the tests).
 */
final class Scrypt {

    private Scrypt() {
    }

    /**
     * Derive {@code dkLen} bytes from {@code password} and {@code salt}.
     *
     * @param n CPU/memory cost; a power of two greater than 1
     * @param r block size
     * @param p parallelisation
     */
    static byte[] derive(byte[] password, byte[] salt, int n, int r, int p, int dkLen) {
        if (n < 2 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("N must be a power of two greater than 1");
        }
        if (r < 1 || p < 1 || (long) r * p >= 1 << 30 || (long) n * r > Integer.MAX_VALUE / 32) {
            throw new IllegalArgumentException("scrypt parameters out of range");
        }

        int blockBytes = 128 * r;
        byte[] b = pbkdf2Sha256(password, salt, 1, p * blockBytes);
        int[] x = new int[32 * r];
        int[] y = new int[32 * r];
        int[] v = new int[32 * r * n];
        int[] t = new int[16];
        int[] scratch = new int[16];
        for (int i = 0; i < p; i++) {
            roMix(b, i * blockBytes, r, n, x, y, v, t, scratch);
        }
        byte[] key = pbkdf2Sha256(password, b, 1, dkLen);
        Arrays.fill(b, (byte) 0);
        Arrays.fill(v, 0);
        Arrays.fill(x, 0);
        return key;
    }

    /** PBKDF2 with HMAC-SHA-256 (RFC 8018), as scrypt uses it. */
    static byte[] pbkdf2Sha256(byte[] password, byte[] salt, int iterations, int dkLen) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // HMAC zero-pads short keys, so an empty key equals a single zero byte;
            // SecretKeySpec just refuses to hold an empty array.
            mac.init(new SecretKeySpec(password.length == 0 ? new byte[1] : password, "HmacSHA256"));
            byte[] out = new byte[dkLen];
            int blocks = (dkLen + 31) / 32;
            for (int block = 1; block <= blocks; block++) {
                mac.update(salt);
                mac.update(new byte[] {(byte) (block >>> 24), (byte) (block >>> 16), (byte) (block >>> 8), (byte) block});
                byte[] u = mac.doFinal();
                byte[] acc = u.clone();
                for (int i = 1; i < iterations; i++) {
                    u = mac.doFinal(u);
                    for (int k = 0; k < acc.length; k++) {
                        acc[k] ^= u[k];
                    }
                }
                int offset = (block - 1) * 32;
                System.arraycopy(acc, 0, out, offset, Math.min(32, dkLen - offset));
            }
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }

    private static void roMix(byte[] b, int off, int r, int n, int[] x, int[] y, int[] v, int[] t, int[] scratch) {
        int words = 32 * r;
        for (int k = 0; k < words; k++) {
            int at = off + k * 4;
            x[k] = (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
        }
        for (int i = 0; i < n; i++) {
            System.arraycopy(x, 0, v, i * words, words);
            blockMix(x, y, r, t, scratch);
        }
        int lastBlock = (2 * r - 1) * 16;
        for (int i = 0; i < n; i++) {
            int j = x[lastBlock] & (n - 1);
            int base = j * words;
            for (int k = 0; k < words; k++) {
                x[k] ^= v[base + k];
            }
            blockMix(x, y, r, t, scratch);
        }
        for (int k = 0; k < words; k++) {
            int at = off + k * 4;
            b[at] = (byte) x[k];
            b[at + 1] = (byte) (x[k] >>> 8);
            b[at + 2] = (byte) (x[k] >>> 16);
            b[at + 3] = (byte) (x[k] >>> 24);
        }
    }

    /** scryptBlockMix: even outputs go to the first half, odd outputs to the second. */
    private static void blockMix(int[] b, int[] y, int r, int[] t, int[] scratch) {
        System.arraycopy(b, (2 * r - 1) * 16, t, 0, 16);
        for (int i = 0; i < 2 * r; i++) {
            for (int k = 0; k < 16; k++) {
                t[k] ^= b[i * 16 + k];
            }
            salsa20x8(t, scratch);
            int dst = ((i & 1) == 0 ? i / 2 : r + i / 2) * 16;
            System.arraycopy(t, 0, y, dst, 16);
        }
        System.arraycopy(y, 0, b, 0, 32 * r);
    }

    /** The Salsa20/8 core, in place on {@code b}. */
    private static void salsa20x8(int[] b, int[] x) {
        System.arraycopy(b, 0, x, 0, 16);
        for (int i = 8; i > 0; i -= 2) {
            x[4] ^= Integer.rotateLeft(x[0] + x[12], 7);
            x[8] ^= Integer.rotateLeft(x[4] + x[0], 9);
            x[12] ^= Integer.rotateLeft(x[8] + x[4], 13);
            x[0] ^= Integer.rotateLeft(x[12] + x[8], 18);
            x[9] ^= Integer.rotateLeft(x[5] + x[1], 7);
            x[13] ^= Integer.rotateLeft(x[9] + x[5], 9);
            x[1] ^= Integer.rotateLeft(x[13] + x[9], 13);
            x[5] ^= Integer.rotateLeft(x[1] + x[13], 18);
            x[14] ^= Integer.rotateLeft(x[10] + x[6], 7);
            x[2] ^= Integer.rotateLeft(x[14] + x[10], 9);
            x[6] ^= Integer.rotateLeft(x[2] + x[14], 13);
            x[10] ^= Integer.rotateLeft(x[6] + x[2], 18);
            x[3] ^= Integer.rotateLeft(x[15] + x[11], 7);
            x[7] ^= Integer.rotateLeft(x[3] + x[15], 9);
            x[11] ^= Integer.rotateLeft(x[7] + x[3], 13);
            x[15] ^= Integer.rotateLeft(x[11] + x[7], 18);
            x[1] ^= Integer.rotateLeft(x[0] + x[3], 7);
            x[2] ^= Integer.rotateLeft(x[1] + x[0], 9);
            x[3] ^= Integer.rotateLeft(x[2] + x[1], 13);
            x[0] ^= Integer.rotateLeft(x[3] + x[2], 18);
            x[6] ^= Integer.rotateLeft(x[5] + x[4], 7);
            x[7] ^= Integer.rotateLeft(x[6] + x[5], 9);
            x[4] ^= Integer.rotateLeft(x[7] + x[6], 13);
            x[5] ^= Integer.rotateLeft(x[4] + x[7], 18);
            x[11] ^= Integer.rotateLeft(x[10] + x[9], 7);
            x[8] ^= Integer.rotateLeft(x[11] + x[10], 9);
            x[9] ^= Integer.rotateLeft(x[8] + x[11], 13);
            x[10] ^= Integer.rotateLeft(x[9] + x[8], 18);
            x[12] ^= Integer.rotateLeft(x[15] + x[14], 7);
            x[13] ^= Integer.rotateLeft(x[12] + x[15], 9);
            x[14] ^= Integer.rotateLeft(x[13] + x[12], 13);
            x[15] ^= Integer.rotateLeft(x[14] + x[13], 18);
        }
        for (int i = 0; i < 16; i++) {
            b[i] += x[i];
        }
    }
}
