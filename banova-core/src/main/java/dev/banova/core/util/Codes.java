package dev.banova.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Random identifiers and hashing helpers.
 */
public final class Codes {

    /** No 0/O, 1/I/L to avoid confusion when people type codes. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Codes() {
    }

    /** Human friendly code such as {@code K7M2-QX9P}. */
    public static String humanCode() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /** Normalizes user typed codes: upper case, no spaces, dash in the middle. */
    public static String normalizeHumanCode(String raw) {
        if (raw == null) {
            return "";
        }
        String clean = raw.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (clean.length() != 8) {
            return clean;
        }
        return clean.substring(0, 4) + "-" + clean.substring(4);
    }

    /** URL safe random token with the given number of random bytes. */
    public static String token(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant time comparison for secrets. */
    public static boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
