package com.hamradio.jhub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CredentialStore — encrypted at-rest credential storage.
 *
 * Used by upload services (eQSL, Club Log, QRZ Logbook, HRDLog) and
 * WebDAV cloud backup. Each service stores its own JSON blob keyed by
 * a stable service id (e.g. "eqsl", "clublog", "webdav").
 *
 * <h3>Threat model</h3>
 * The store protects against casual file inspection — a user opening
 * <code>credentials.enc</code> in a text editor sees ciphertext, not their
 * eQSL password. It does NOT protect against an attacker who has both
 * disk access and the ability to read the J-Hub source. The encryption
 * key is derived from machine-stable identifiers (hostname / OS user /
 * /etc/machine-id when available) so the file does not decrypt on
 * another machine, but a determined local attacker with root could
 * reconstruct the key.
 *
 * <p>This is the right tradeoff for a single-user desktop ham radio app.
 * Credentials we're protecting (eQSL, Club Log, QRZ, HRDLog) are for
 * services where the user already trusts their browser/OS keychain;
 * the impact of compromise is annoyance, not financial loss.
 *
 * <h3>File format</h3>
 * <pre>
 *   ~/.j-hub/credentials.enc
 *
 *   12-byte IV  ‖  AES-GCM ciphertext (tag included)
 *
 *   Plaintext is a JSON object: {"eqsl":{"user":"…","pass":"…"}, "clublog":{…}, …}
 * </pre>
 */
public class CredentialStore {

    private static final Logger log = LoggerFactory.getLogger(CredentialStore.class);

    private static final CredentialStore INSTANCE = new CredentialStore();
    public static CredentialStore getInstance() { return INSTANCE; }

    /**
     * Resolved each time so tests can redirect the store via the
     * {@code j-hub.credentials.path} system property without reflection.
     */
    private static Path storePath() {
        String override = System.getProperty("j-hub.credentials.path");
        if (override != null && !override.isBlank()) return Path.of(override);
        return Path.of(System.getProperty("user.home"), ".j-hub", "credentials.enc");
    }

    // Hard-coded application salt — combined with machine identity to derive
    // the AES key. Changing this invalidates every existing credentials file.
    private static final byte[] APP_SALT =
        "j-hub.credentialstore.v1".getBytes(StandardCharsets.UTF_8);

    private static final int GCM_IV_BYTES  = 12;
    private static final int GCM_TAG_BITS  = 128;

    private final Gson gson = new Gson();
    private final ConcurrentHashMap<String, JsonObject> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    private CredentialStore() {}

    // -----------------------------------------------------------
    // Public API
    // -----------------------------------------------------------

    /** Returns stored credentials for a service, or an empty object if none. */
    public synchronized JsonObject get(String serviceId) {
        ensureLoaded();
        JsonObject o = cache.get(serviceId);
        return o == null ? new JsonObject() : o.deepCopy();
    }

    /** Replaces credentials for a service and persists immediately. */
    public synchronized void put(String serviceId, JsonObject creds) {
        ensureLoaded();
        if (creds == null || creds.entrySet().isEmpty()) {
            cache.remove(serviceId);
        } else {
            cache.put(serviceId, creds);
        }
        save();
    }

    /** Removes credentials for a service. */
    public synchronized void remove(String serviceId) {
        ensureLoaded();
        cache.remove(serviceId);
        save();
    }

    /** True if credentials exist for a service (any non-empty entry). */
    public boolean has(String serviceId) {
        ensureLoaded();
        JsonObject o = cache.get(serviceId);
        return o != null && !o.entrySet().isEmpty();
    }

    // -----------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            load();
            loaded = true;
        }
    }

    private void load() {
        if (!Files.exists(storePath())) return;
        try {
            byte[] file = Files.readAllBytes(storePath());
            if (file.length < GCM_IV_BYTES + 16) {
                log.warn("credentials.enc too short — ignoring");
                return;
            }
            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ct = new byte[file.length - GCM_IV_BYTES];
            System.arraycopy(file, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(file, GCM_IV_BYTES, ct, 0, ct.length);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = c.doFinal(ct);
            String json = new String(pt, StandardCharsets.UTF_8);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root != null) {
                for (Map.Entry<String, com.google.gson.JsonElement> e : root.entrySet()) {
                    if (e.getValue().isJsonObject()) {
                        cache.put(e.getKey(), e.getValue().getAsJsonObject());
                    }
                }
                log.info("CredentialStore loaded ({} services)", cache.size());
            }
        } catch (Exception e) {
            log.warn("CredentialStore load failed (machine id may have changed): {}", e.getMessage());
            // Don't propagate — start with an empty store. The user can
            // re-enter their credentials, which we'll then be able to
            // re-encrypt with the current machine's key.
        }
    }

    private void save() {
        try {
            Files.createDirectories(storePath().getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<String, JsonObject> e : cache.entrySet()) {
                root.add(e.getKey(), e.getValue());
            }
            byte[] pt = root.toString().getBytes(StandardCharsets.UTF_8);

            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = c.doFinal(pt);

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            Files.write(storePath(), out);
            try {
                // Best-effort tighten file perms on POSIX. Failure is non-fatal —
                // Windows callers fall through to the default ACL.
                Files.setPosixFilePermissions(storePath(),
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (Exception ignored) {}
            log.debug("CredentialStore saved ({} bytes)", out.length);
        } catch (Exception e) {
            log.error("CredentialStore save failed: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------
    // Key derivation
    // -----------------------------------------------------------

    /**
     * Derive an AES-256 key from machine-stable identifiers + APP_SALT.
     * Stable across reboots, unique per (machine, OS user). Resistant to
     * "copy the file off this box and decrypt it elsewhere."
     */
    private SecretKey deriveKey() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(APP_SALT);
        md.update(System.getProperty("user.name", "anon").getBytes(StandardCharsets.UTF_8));
        md.update(System.getProperty("user.home", "anon").getBytes(StandardCharsets.UTF_8));
        md.update(machineId().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(md.digest(), "AES");
    }

    /** Best-effort cross-platform machine identifier. */
    private static String machineId() {
        // Linux: /etc/machine-id (systemd) or /var/lib/dbus/machine-id
        for (String p : new String[]{"/etc/machine-id", "/var/lib/dbus/machine-id"}) {
            try {
                Path mid = Path.of(p);
                if (Files.isReadable(mid)) {
                    String s = new String(Files.readAllBytes(mid), StandardCharsets.UTF_8).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (IOException ignored) {}
        }
        // macOS: ioreg IOPlatformUUID — read by spawning a process; skip the
        // fork on systems where /etc/machine-id was sufficient.
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            try {
                Process p = new ProcessBuilder("ioreg", "-rd1", "-c", "IOPlatformExpertDevice").start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int i = out.indexOf("IOPlatformUUID");
                if (i >= 0) {
                    int q1 = out.indexOf('"', i + 14) + 1;
                    int q2 = out.indexOf('"', q1 + 1);
                    int q3 = out.indexOf('"', q2 + 1);
                    int q4 = out.indexOf('"', q3 + 1);
                    if (q4 > q3) return out.substring(q3 + 1, q4);
                }
            } catch (Exception ignored) {}
        }
        // Windows: registry MachineGuid
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Process p = new ProcessBuilder("reg", "query",
                    "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid").start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                for (String line : out.split("\\R")) {
                    line = line.trim();
                    if (line.startsWith("MachineGuid")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) return parts[parts.length - 1];
                    }
                }
            } catch (Exception ignored) {}
        }
        // Last-resort fallback: hostname. Stable across reboots, unique per host.
        try { return java.net.InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "unknown-host"; }
    }

    /** Visible for the REST endpoint — returns service ids that have any credentials. */
    public java.util.Set<String> listServices() {
        ensureLoaded();
        return new java.util.HashSet<>(cache.keySet());
    }

    /** For diagnostics / integration tests only. */
    static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
}
