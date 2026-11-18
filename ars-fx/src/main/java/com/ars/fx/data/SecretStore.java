package com.ars.fx.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Keeps passwords, API keys and tokens in the operating system's own store instead of a settings file: the Secret Service (GNOME Keyring,
 * KWallet) through {@code secret-tool} on Linux, the login Keychain through {@code security} on macOS, and Credential Manager's password vault
 * through PowerShell on Windows. No native libraries: each is the tool the system already has.
 *
 * <p>When no store can be reached (no {@code secret-tool}, no Secret Service running, a locked keyring nobody answers, {@code -Dars.keyring=false}),
 * {@link #available()} is false and the callers keep the secret in their file as before. Nothing here throws.
 */
public final class SecretStore {
    private SecretStore() {}

    static final String SERVICE = "ars-suite";

    /** One operating system's store. Implementations report failure by returning false / null, never by throwing. */
    interface Backend {
        /** True when the store can be used right now. */
        boolean available();
        /** The secret, or null when there is none (or it could not be read). */
        String get(String account);
        boolean put(String account, String value);
        boolean delete(String account);
    }

    private static Backend backend;
    private static Boolean usable;
    private static final Map<String, String> cache = new HashMap<>();       // account -> secret ("" = known to be absent)

    // ---- public API -------------------------------------------------------------------------------------------------
    /** Whether secrets can be kept in the system store on this computer. Decided once. */
    public static synchronized boolean available() {
        if (usable == null) usable = !"false".equalsIgnoreCase(System.getProperty("ars.keyring")) && backend().available();
        return usable;
    }

    /** The secret kept for {@code account}, or "" when there is none or no store. */
    public static synchronized String get(String account) {
        if (!available()) return "";
        String c = cache.get(account);
        if (c != null) return c;
        String v = backend().get(account);
        cache.put(account, v == null ? "" : v);
        return v == null ? "" : v;
    }

    /** Keep {@code value} for {@code account}; a blank value removes it. True when the store now holds exactly that. */
    public static synchronized boolean put(String account, String value) {
        if (!available()) return false;
        if (value == null || value.isBlank()) return delete(account);
        if (!backend().put(account, value)) return false;
        cache.put(account, value);
        return true;
    }

    public static synchronized boolean delete(String account) {
        if (!available()) return false;
        if ("".equals(cache.get(account))) return true;                       // already known to be absent: no need to ask the system again
        boolean ok = backend().delete(account);
        cache.put(account, "");
        return ok;
    }

    // ---- selection / test hooks -------------------------------------------------------------------------------------
    private static Backend backend() {
        if (backend == null) backend = forThisOs();
        return backend;
    }

    static Backend forThisOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return new CliBackend(Kind.WINDOWS);
        if (os.contains("mac")) return new CliBackend(Kind.MAC);
        return new CliBackend(Kind.LINUX);
    }

    /** Test hook: use {@code b} (null: pick by operating system again) and forget what was decided and cached. */
    static synchronized void useBackend(Backend b) { backend = b; usable = null; cache.clear(); }

    // ---- the command-line backends ----------------------------------------------------------------------------------
    enum Kind { LINUX, MAC, WINDOWS }

    static final class CliBackend implements Backend {
        private static final long TIMEOUT_S = 10;        // a locked keyring may be waiting for a password prompt; do not hang the app on it
        private final Kind kind;
        CliBackend(Kind kind) { this.kind = kind; }

        @Override public boolean available() {
            try {
                return switch (kind) {
                    case LINUX -> {
                        // "not found" is exit 1 with nothing on stderr; a missing Secret Service says why on stderr
                        Result r = run(null, "secret-tool", "lookup", "service", SERVICE, "account", "__probe__");
                        yield r != null && r.exit == 1 && r.err.isBlank() || r != null && r.exit == 0;
                    }
                    case MAC -> {
                        Result r = run(null, "security", "find-generic-password", "-s", SERVICE, "-a", "__probe__");
                        yield r != null && (r.exit == 0 || r.exit == 44);          // 44: item not found
                    }
                    case WINDOWS -> {
                        Result r = run(null, "powershell", "-NoProfile", "-NonInteractive", "-Command", WIN_PRELUDE + "'ok'");
                        yield r != null && r.exit == 0 && r.out.trim().equals("ok");
                    }
                };
            } catch (RuntimeException e) { return false; }
        }

        @Override public String get(String account) {
            Result r = switch (kind) {
                case LINUX -> run(null, "secret-tool", "lookup", "service", SERVICE, "account", account);
                case MAC -> run(null, "security", "find-generic-password", "-s", SERVICE, "-a", account, "-w");
                case WINDOWS -> run(null, "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        WIN_PRELUDE + "try { $c = $v.Retrieve('" + SERVICE + "', '" + psq(account) + "'); $c.RetrievePassword(); [Console]::Out.Write($c.Password) } catch { }");
            };
            if (r == null || r.exit != 0) return null;
            String out = kind == Kind.MAC ? r.out.replaceAll("\\r?\\n$", "") : r.out;
            return out.isEmpty() ? null : out;
        }

        @Override public boolean put(String account, String value) {
            Result r = switch (kind) {
                case LINUX -> run(value, "secret-tool", "store", "--label=ARS Suite: " + account, "service", SERVICE, "account", account);
                // "security -i" reads its commands from stdin, so the secret never appears in the process list
                case MAC -> run("add-generic-password -U -s " + SERVICE + " -a " + macq(account) + " -w " + macq(value) + "\n",
                        "security", "-i");
                case WINDOWS -> run(value, "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        WIN_PRELUDE + "$p = [Console]::In.ReadToEnd(); try { $v.Remove($v.Retrieve('" + SERVICE + "', '" + psq(account) + "')) } catch { } "
                                + "$v.Add((New-Object Windows.Security.Credentials.PasswordCredential('" + SERVICE + "', '" + psq(account) + "', $p)))");
            };
            return r != null && r.exit == 0 && !(kind == Kind.MAC && r.err.toLowerCase(Locale.ROOT).contains("error"));
        }

        @Override public boolean delete(String account) {
            Result r = switch (kind) {
                case LINUX -> run(null, "secret-tool", "clear", "service", SERVICE, "account", account);
                case MAC -> run(null, "security", "delete-generic-password", "-s", SERVICE, "-a", account);
                case WINDOWS -> run(null, "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        WIN_PRELUDE + "try { $v.Remove($v.Retrieve('" + SERVICE + "', '" + psq(account) + "')) } catch { }");
            };
            return r != null && (r.exit == 0 || kind == Kind.MAC && r.exit == 44);
        }

        private static final String WIN_PRELUDE =
                "[void][Windows.Security.Credentials.PasswordVault, Windows.Security.Credentials, ContentType=WindowsRuntime]; "
                        + "$v = New-Object Windows.Security.Credentials.PasswordVault; ";

        /** A string for inside single quotes in PowerShell. */
        static String psq(String s) { return s.replace("'", "''"); }
        /** A string as one double-quoted word for {@code security -i}. */
        static String macq(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

        private record Result(int exit, String out, String err) {}

        private static Result run(String stdin, String... cmd) {
            try {
                Process p = new ProcessBuilder(List.of(cmd)).start();
                if (stdin != null) { p.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8)); }
                p.getOutputStream().close();
                // read both streams on helper threads so a full pipe cannot stall the tool
                StringBuilder out = new StringBuilder(), err = new StringBuilder();
                Thread to = drain(p.getInputStream(), out), te = drain(p.getErrorStream(), err);
                if (!p.waitFor(TIMEOUT_S, TimeUnit.SECONDS)) { p.destroyForcibly(); return null; }
                to.join(1000); te.join(1000);
                return new Result(p.exitValue(), out.toString(), err.toString());
            } catch (IOException e) {
                return null;                                              // the tool is not installed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        private static Thread drain(java.io.InputStream in, StringBuilder sink) {
            Thread t = new Thread(() -> {
                try { sink.append(new String(in.readAllBytes(), StandardCharsets.UTF_8)); } catch (IOException ignored) { }
            }, "secret-store-io");
            t.setDaemon(true); t.start();
            return t;
        }
    }
}
