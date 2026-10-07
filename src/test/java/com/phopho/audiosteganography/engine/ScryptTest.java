package com.phopho.audiosteganography.engine;

import static com.phopho.audiosteganography.testing.Assert.hexEqual;

import com.phopho.audiosteganography.testing.Test;
import java.nio.charset.StandardCharsets;

/** RFC 7914 test vectors (cross-checked against OpenSSL via Python's hashlib). */
public class ScryptTest {

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    public void pbkdf2Sha256Vector() {
        hexEqual("55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
                        + "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
                Scrypt.pbkdf2Sha256(ascii("passwd"), ascii("salt"), 1, 64));
    }

    @Test
    public void emptyPasswordAndSalt() {
        hexEqual("77d6576238657b203b19ca42c18a0497f16b4844e3074ae8dfdffa3fede21442"
                        + "fcd0069ded0948f8326a753a0fc81f17e8d3e0fb2e0d3628cf35e20c38d18906",
                Scrypt.derive(new byte[0], new byte[0], 16, 1, 1, 64));
    }

    @Test
    public void passwordNaClWithParallelism() {
        hexEqual("fdbabe1c9d3472007856e7190d01e9fe7c6ad7cbc8237830e77376634b373162"
                        + "2eaf30d92e22a3886ff109279d9830dac727afb94a83ee6d8360cbdfa2cc0640",
                Scrypt.derive(ascii("password"), ascii("NaCl"), 1024, 8, 16, 64));
    }

    @Test
    public void sodiumChlorideAtAppCost() {
        // N = 2^14, r = 8, p = 1: exactly the cost the app uses.
        hexEqual("7023bdcb3afd7348461c06cd81fd38ebfda8fbba904f8e3ea9b543f6545da1f2"
                        + "d5432955613f0fcf62d49705242a9af9e61e85dc0d651e40dfcf017b45575887",
                Scrypt.derive(ascii("pleaseletmein"), ascii("SodiumChloride"), 16384, 8, 1, 64));
    }
}
