package com.ars.fx.net;

import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Keeps the QSOs of a contest network from crossing the LAN in the clear. Once a station has proved it knows the shared key ({@link NetAuth}),
 * every message after the welcome is sealed with AES-256-GCM under a key made from the shared key and that connection's one-time nonce, so each
 * connection has a key of its own and a recording of one cannot be read or replayed on another. A frame that does not open (wrong key, altered,
 * or sent in the clear after the handshake) is dropped.
 *
 * <p>What this does not do: it does not hide that two stations are talking, or the size and timing of the messages, and within one connection a
 * recorded frame could be sent again (every message is harmless to repeat except a chat line, which would show twice).
 */
public final class NetCrypt {
    private NetCrypt() {}

    private static final SecureRandom RNG = new SecureRandom();
    private static final int IV = 12, TAG_BITS = 128;

    /** The key for one connection: HMAC-SHA256 of the nonce, keyed with the shared key. */
    public static byte[] sessionKey(String sharedKey, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sharedKey.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(("enc|" + nonce).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** A message as a sealed frame: {@code {"t":"enc","d":"<base64 of iv, ciphertext and tag>"}}. */
    public static String seal(byte[] key, String text) {
        try {
            byte[] iv = new byte[IV]; RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[IV + ct.length];
            System.arraycopy(iv, 0, all, 0, IV); System.arraycopy(ct, 0, all, IV, ct.length);
            JsonObject o = new JsonObject(); o.addProperty("t", "enc"); o.addProperty("d", Base64.getEncoder().encodeToString(all));
            return o.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** The message inside a sealed frame, or null if the frame is not one, or does not open with this key. */
    public static String open(byte[] key, String frame) {
        try {
            JsonObject o = NetProto.parse(frame);
            if (o == null || !"enc".equals(NetProto.type(o))) return null;
            byte[] all = Base64.getDecoder().decode(NetProto.str(o, "d"));
            if (all.length < IV + TAG_BITS / 8) return null;
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, all, 0, IV));
            return new String(c.doFinal(all, IV, all.length - IV), StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }
}
