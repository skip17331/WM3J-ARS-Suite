package com.ars.fx.data;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Club Log's DXCC exceptions: single callsigns that count for another entity than their prefix says, each for a date range
 * (a DXpedition, a special-event call, an operation Club Log has placed by hand).
 *
 * <p>The file is Club Log's {@code cty.xml}, fetched with the operator's Club Log API key (the one the Club Log upload already
 * asks for) from {@code https://cdn.clublog.org/cty.php?api=KEY} as gzipped XML, at most once a day, and kept in
 * {@code ~/.j-hub/clublog/cty.xml.gz}. Without a key nothing is fetched and {@link #at} finds nothing, so lookups are as before.
 * A failed or implausible download keeps the copy on disk. Only {@code <exceptions>} is read: the prefixes already come from the
 * engine's table and cty.dat.
 *
 * <p>Record: {@code <exception record="N"><call/><entity/><adif/><cqz/><cont/><long/><lat/><start/><end/></exception>}, dates like
 * {@code 1991-03-30T23:59:59+00:00}; a missing start or end leaves that side open; the range is inclusive.
 */
public final class ClubLogCty {
    private ClubLogCty() {}

    public static final String URL = "https://cdn.clublog.org/cty.php?api=";
    static final long MAX_AGE_MS = 24L * 3600 * 1000;
    private static final int MAX_BYTES = 30_000_000;

    /** One exception: {@code call} belongs to entity {@code adif} from {@code start} to {@code end} (epoch s, inclusive). */
    public record CallException(String call, String entity, int adif, int cqz, String cont, double lat, double lon, long start, long end) {
        boolean covers(long t) { return t >= start && t <= end; }
    }

    private static volatile Map<String, List<CallException>> byCall = Map.of();
    private static volatile boolean loaded;

    static Path file() { return Paths.get(System.getProperty("user.home"), ".j-hub", "clublog", "cty.xml.gz"); }

    public static String apiKey() { return UploadConfig.field("clublog", "apikey").trim(); }

    /** The exception for {@code call} at {@code when}, or null. Also tries the two halves of "A/B" swapped, as Club Log files some that way. */
    public static CallException at(String call, Instant when) {
        if (call == null || call.isBlank()) return null;
        ensureLoaded();
        Map<String, List<CallException>> m = byCall;
        if (m.isEmpty()) return null;
        String c = call.strip().toUpperCase(Locale.ROOT);
        long t = (when == null ? Instant.now() : when).getEpochSecond();
        CallException e = first(m.get(c), t);
        if (e == null) {
            String[] p = c.split("/");
            if (p.length == 2) e = first(m.get(p[1] + "/" + p[0]), t);
        }
        return e;
    }

    private static CallException first(List<CallException> l, long t) {
        if (l != null) for (CallException e : l) if (e.covers(t)) return e;
        return null;
    }

    /** How many calls have an exception (0 = no file yet). */
    public static int size() { ensureLoaded(); return byCall.size(); }

    // ---- reading ----------------------------------------------------------------------------------

    /** The exceptions in a cty.xml (plain or gzipped), by call. */
    static Map<String, List<CallException>> parse(byte[] body) throws IOException {
        InputStream in = new ByteArrayInputStream(body);
        if (body.length > 2 && (body[0] & 0xff) == 0x1f && (body[1] & 0xff) == 0x8b) in = new GZIPInputStream(in);
        Map<String, List<CallException>> out = new HashMap<>();
        try {
            XMLInputFactory f = XMLInputFactory.newFactory();
            f.setProperty(XMLInputFactory.SUPPORT_DTD, false);                       // no DTDs or external entities from the network
            f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader r = f.createXMLStreamReader(in, "UTF-8");
            boolean inExceptions = false;
            Map<String, String> rec = null;
            String field = null;
            StringBuilder text = new StringBuilder();
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String n = r.getLocalName();
                    if (n.equals("exceptions")) inExceptions = true;
                    else if (inExceptions && n.equals("exception")) rec = new HashMap<>();
                    else if (rec != null) { field = n; text.setLength(0); }
                } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    if (field != null) text.append(r.getText());
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String n = r.getLocalName();
                    if (n.equals("exceptions")) break;                               // nothing else is needed from the file
                    if (rec != null && n.equals("exception")) {
                        CallException e = toException(rec);
                        if (e != null) out.computeIfAbsent(e.call(), k -> new ArrayList<>()).add(e);
                        rec = null;
                    } else if (rec != null && n.equals(field)) { rec.put(field, text.toString().trim()); field = null; }
                }
            }
            r.close();
        } catch (javax.xml.stream.XMLStreamException e) {
            throw new IOException("not a Club Log cty.xml: " + e.getMessage(), e);
        }
        return out;
    }

    private static CallException toException(Map<String, String> r) {
        String call = r.getOrDefault("call", "").toUpperCase(Locale.ROOT);
        int adif = num(r.get("adif"));
        if (call.isEmpty() || adif <= 0) return null;
        return new CallException(call, r.getOrDefault("entity", ""), adif, num(r.get("cqz")), r.getOrDefault("cont", ""),
                dbl(r.get("lat")), dbl(r.get("long")), time(r.get("start"), Long.MIN_VALUE), time(r.get("end"), Long.MAX_VALUE));
    }

    private static int num(String s) { try { return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; } }
    private static double dbl(String s) { try { return s == null || s.isBlank() ? Double.NaN : Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return Double.NaN; } }
    static long time(String s, long open) {
        if (s == null || s.isBlank()) return open;
        try { return OffsetDateTime.parse(s.trim()).toEpochSecond(); } catch (RuntimeException e) { return open; }
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = file();
            if (Files.isRegularFile(f)) byCall = Collections.unmodifiableMap(parse(Files.readAllBytes(f)));
        } catch (IOException | RuntimeException ignored) { /* no usable copy: no exceptions */ }
    }

    // ---- fetching ---------------------------------------------------------------------------------

    static boolean stale(long nowMs) {
        try { Path f = file(); return !Files.isRegularFile(f) || nowMs - Files.getLastModifiedTime(f).toMillis() > MAX_AGE_MS; }
        catch (IOException e) { return true; }
    }

    /** At start-up: refresh in the background when an API key is set and the copy is a day old. Never blocks, never throws. */
    public static void init() {
        if (apiKey().isEmpty() || !stale(System.currentTimeMillis())) return;
        Thread t = new Thread(() -> { try { refresh(); } catch (Throwable ignored) { } }, "clublog-cty");
        t.setDaemon(true); t.start();
    }

    /** Fetch now. Returns a short message for the screen; keeps the old copy on any failure. */
    public static String refresh() {
        String key = apiKey();
        if (key.isEmpty()) return "Set your Club Log API key first (Setup ▸ Data ▸ Online log upload ▸ Club Log).";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(System.getProperty("clublog.cty.url", URL) + URLEncoder.encode(key, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(60)).header("User-Agent", "ARS-Suite").GET().build();
            HttpResponse<byte[]> resp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL).build().send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 403 || resp.statusCode() == 401) return "Club Log refused the API key (HTTP " + resp.statusCode() + ").";
            if (resp.statusCode() / 100 != 2) return "Club Log answered HTTP " + resp.statusCode() + ".";
            return save(resp.body());
        } catch (IOException | InterruptedException | RuntimeException e) {
            return "Could not reach Club Log: " + e.getMessage();
        }
    }

    /** Check and keep a downloaded file. */
    static String save(byte[] body) throws IOException {
        if (body == null || body.length == 0 || body.length > MAX_BYTES) return "Club Log's answer was empty or too large.";
        Map<String, List<CallException>> m;
        try { m = parse(body); } catch (IOException e) { return "Club Log's answer was not the country file."; }
        int had = byCall.size();
        if (m.size() < 1000 || m.size() < had * 0.9) return "Club Log's file held only " + m.size() + " exceptions; kept the copy we have.";
        Path f = file();
        Files.createDirectories(f.getParent());
        Path tmp = f.resolveSibling("cty.xml.gz.part");
        Files.write(tmp, body);
        try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
        byCall = Collections.unmodifiableMap(m);
        loaded = true;
        return "Club Log exceptions updated: " + m.size() + " calls.";
    }

    /** For tests: replace the exceptions without touching disk. */
    static void setForTest(Map<String, List<CallException>> m) { byCall = m; loaded = true; }
}
