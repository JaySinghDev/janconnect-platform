package com.janconnect.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * AES-128/ECB encoder whose output is binary-compatible with MySQL's
 * AES_ENCRYPT() / AES_DECRYPT() built-in functions.
 *
 * MySQL key derivation: 16-byte zero buffer, each key byte XOR'd in cyclically.
 * Cipher: AES/ECB/PKCS5Padding (same padding MySQL uses internally).
 * Storage: upper-case hex string so the column stays a plain VARCHAR.
 *
 * Decrypt in MySQL:
 *   SELECT CAST(AES_DECRYPT(UNHEX(password), 'Throttle Key') AS CHAR)
 *   FROM jc_users_master;
 */
public class AesPasswordEncoder implements PasswordEncoder {

    private final SecretKeySpec secretKey;

    public AesPasswordEncoder(String key) {
        this.secretKey = new SecretKeySpec(deriveKey(key), "AES");
    }

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(
                    rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("AES encrypt failed", ex);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) return false;
        try {
            return encode(rawPassword).equalsIgnoreCase(encodedPassword);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Replicates MySQL's internal key-padding:
     * allocate a 16-byte zero buffer, XOR each key byte in mod-16 position.
     */
    private static byte[] deriveKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] derived  = new byte[16];
        for (int i = 0; i < keyBytes.length; i++) {
            derived[i % 16] ^= keyBytes[i];
        }
        return derived;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
