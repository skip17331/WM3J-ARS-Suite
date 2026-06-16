package com.ars.fx.data;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Uploads the J-Log database to the online log services configured in
 * {@link UploadConfig}: LoTW (signed via the TrustedQSL <code>tqsl</code> binary),
 * eQSL.cc and Club Log (multipart ADIF), and QRZ Logbook (per-record API). All
 * calls block on network / a child process and must run off the UI thread.
 */
public final class LogUpload {
    private LogUpload() {}

    public record Result(boolean ok, int count, String message) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    // ---- LoTW via TrustedQSL ----------------------------------------------
    public static Result lotw() {
        List<JLogDb.QsoExport> rows = JLogDb.allForExport();
        if (rows.isEmpty()) return new Result(false, 0, "No QSOs to upload.");
        String tqsl = resolveTqsl();
        if (tqsl == null)
            return new Result(false, 0, "TrustedQSL (tqsl) not found. Install it from arrl.org/lotw and set the path in Data ▸ LoTW.");
        try {
            Path adi = Files.createTempFile("jlog-lotw-", ".adi");
            Files.writeString(adi, LogExport.toAdif(rows), StandardCharsets.UTF_8);
            List<String> cmd = new ArrayList<>(List.of(tqsl, "-x", "-d", "-u", "-a", "all"));
            String loc = UploadConfig.field("lotw", "location");
            if (!loc.isBlank()) { cmd.add("-l"); cmd.add(loc); }
            String pw = UploadConfig.field("lotw", "password");
            if (!pw.isBlank()) { cmd.add("-p"); cmd.add(pw); }
            cmd.add(adi.toString());
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean done = p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            Files.deleteIfExists(adi);
            if (!done) { p.destroyForcibly(); return new Result(false, 0, "tqsl timed out."); }
            int code = p.exitValue();
            String tail = tail(out);
            return new Result(code == 0, code == 0 ? rows.size() : 0,
                    code == 0 ? "Signed & uploaded " + rows.size() + " QSOs to LoTW." : "tqsl exit " + code + ": " + tail);
        } catch (Exception e) {
            return new Result(false, 0, "LoTW upload error: " + e.getMessage());
        }
    }

    /** Resolve the tqsl binary: explicit override, then PATH, then common install locations. */
    public static String resolveTqsl() {
        String override = UploadConfig.tqslPath();
        if (override != null && !override.isBlank() && Files.isExecutable(Paths.get(override))) return override;
        for (String cand : new String[]{
                "/usr/bin/tqsl", "/usr/local/bin/tqsl", "/opt/homebrew/bin/tqsl",
                "/Applications/TrustedQSL/Tqsl.app/Contents/MacOS/tqsl",
                "C:\\Program Files (x86)\\TrustedQSL\\tqsl.exe", "C:\\Program Files\\TrustedQSL\\tqsl.exe"})
            if (Files.isExecutable(Paths.get(cand))) return cand;
        // PATH lookup
        try {
            boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
            Process p = new ProcessBuilder(win ? "where" : "which", "tqsl").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!out.isBlank()) { String first = out.lines().findFirst().orElse("").trim(); if (Files.isExecutable(Paths.get(first))) return first; }
        } catch (Exception ignored) { }
        return null;
    }

    // ---- eQSL.cc ----------------------------------------------------------
    public static Result eqsl() {
        List<JLogDb.QsoExport> rows = JLogDb.allForExport();
        if (rows.isEmpty()) return new Result(false, 0, "No QSOs to upload.");
        String user = UploadConfig.field("eqsl", "user"), pw = UploadConfig.field("eqsl", "password");
        if (user.isBlank() || pw.isBlank()) return new Result(false, 0, "eQSL username/password not set.");
        try {
            Multipart mp = new Multipart();
            mp.field("EQSL_USER", user); mp.field("EQSL_PSWD", pw);
            mp.file("Filename", "jlog.adi", LogExport.toAdif(rows).getBytes(StandardCharsets.UTF_8));
            String body = post(prop("eqsl.url", "https://www.eqsl.cc/qslcard/importADIF.cfm"), mp);
            boolean ok = body != null && body.toLowerCase().contains("result");
            return new Result(ok, ok ? rows.size() : 0, "eQSL: " + tail(body));
        } catch (Exception e) {
            return new Result(false, 0, "eQSL upload error: " + e.getMessage());
        }
    }

    // ---- Club Log ---------------------------------------------------------
    public static Result clublog() {
        List<JLogDb.QsoExport> rows = JLogDb.allForExport();
        if (rows.isEmpty()) return new Result(false, 0, "No QSOs to upload.");
        String call = UploadConfig.field("clublog", "callsign"), email = UploadConfig.field("clublog", "email"),
                pw = UploadConfig.field("clublog", "password"), api = UploadConfig.field("clublog", "apikey");
        if (call.isBlank() || email.isBlank() || pw.isBlank() || api.isBlank())
            return new Result(false, 0, "Club Log callsign/email/password/API key not all set.");
        try {
            Multipart mp = new Multipart();
            mp.field("email", email); mp.field("password", pw); mp.field("callsign", call); mp.field("api", api);
            mp.file("file", "jlog.adi", LogExport.toAdif(rows).getBytes(StandardCharsets.UTF_8));
            String body = post(prop("clublog.url", "https://clublog.org/putlogs.php"), mp);
            boolean ok = body != null && !body.toLowerCase().contains("error") && !body.toLowerCase().contains("invalid");
            return new Result(ok, ok ? rows.size() : 0, "Club Log: " + tail(body));
        } catch (Exception e) {
            return new Result(false, 0, "Club Log upload error: " + e.getMessage());
        }
    }

    // ---- QRZ Logbook (per-record API) -------------------------------------
    public static Result qrzLogbook() {
        List<JLogDb.QsoExport> rows = JLogDb.allForExport();
        if (rows.isEmpty()) return new Result(false, 0, "No QSOs to upload.");
        String key = UploadConfig.field("qrz_logbook", "apikey");
        if (key.isBlank()) return new Result(false, 0, "QRZ Logbook API key not set.");
        int ok = 0, dup = 0, fail = 0;
        String lastErr = "";
        for (JLogDb.QsoExport q : rows) {
            try {
                String form = "KEY=" + enc(key) + "&ACTION=INSERT&ADIF=" + enc(LogExport.toAdifRecord(q));
                String body = postForm(prop("qrz.logbook.url", "https://logbook.qrz.com/api"), form);
                String result = kv(body, "RESULT");
                if ("OK".equalsIgnoreCase(result)) ok++;
                else if (body != null && body.toLowerCase().contains("duplicate")) dup++;
                else { fail++; lastErr = kv(body, "REASON"); }
            } catch (Exception e) { fail++; lastErr = e.getMessage(); }
        }
        boolean good = ok > 0 || dup > 0;
        return new Result(good, ok, "QRZ Logbook: " + ok + " added, " + dup + " duplicate, " + fail + " failed"
                + (fail > 0 && lastErr != null ? " (" + lastErr + ")" : ""));
    }

    // ---- HTTP helpers ------------------------------------------------------
    private static String post(String url, Multipart mp) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + mp.boundary)
                .header("User-Agent", "ARS-Suite-JLog")
                .POST(HttpRequest.BodyPublishers.ofByteArray(mp.build())).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }
    private static String postForm(String url, String form) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "ARS-Suite-JLog")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }
    /** Minimal multipart/form-data body builder. */
    static final class Multipart {
        final String boundary = "----arsjlog" + Long.toHexString(System.identityHashCode(new Object())) + "x";
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        void field(String name, String value) throws Exception {
            buf.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        void file(String name, String filename, byte[] data) throws Exception {
            buf.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            buf.write(data); buf.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        byte[] build() throws Exception { buf.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8)); return buf.toByteArray(); }
    }
    private static String prop(String key, String def) { return System.getProperty(key, def); }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String kv(String body, String key) {
        if (body == null) return null;
        for (String part : body.split("[&\\n]")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).trim().equalsIgnoreCase(key)) return part.substring(eq + 1).trim();
        }
        return null;
    }
    private static String tail(String s) {
        if (s == null) return "(no response)";
        String t = s.strip().replaceAll("\\s+", " ");
        return t.length() > 160 ? t.substring(0, 160) + "…" : t;
    }
}
