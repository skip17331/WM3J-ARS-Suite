package com.ars.fx.net;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * The shared-key handshake of the contest network. The host sends a one-time nonce; a station proves it knows the key by sending an HMAC of
 * the nonce and its own name, so the key itself never crosses the network and a recorded proof cannot be replayed on a later connection.
 * This keeps stray devices on the LAN out of the log; it does not encrypt the QSOs that follow, which cross the LAN in the clear.
 */
public final class NetAuth {
    private NetAuth() {}

    public static final int MIN_KEY = 4;
    private static final SecureRandom RNG = new SecureRandom();

    public static boolean keyOk(String key) { return key != null && key.trim().length() >= MIN_KEY; }

    /** A fresh one-time value, 32 hex characters. */
    public static String nonce() {
        byte[] b = new byte[16]; RNG.nextBytes(b);
        return hex(b);
    }

    /** What a station sends to prove it knows {@code key}: HMAC-SHA256 over the nonce and its name, in hex. */
    public static String proof(String key, String nonce, String station) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal((nonce + "|" + station).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** True if {@code proof} is right for this key, nonce and station. Compared in constant time. */
    public static boolean verify(String key, String nonce, String station, String proof) {
        if (!keyOk(key) || nonce == null || station == null || proof == null) return false;
        return MessageDigest.isEqual(proof(key, nonce, station).getBytes(StandardCharsets.UTF_8), proof.getBytes(StandardCharsets.UTF_8));
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 2);
        for (byte x : b) s.append(Character.forDigit((x >> 4) & 0xf, 16)).append(Character.forDigit(x & 0xf, 16));
        return s.toString();
    }
}
