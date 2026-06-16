package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Callsign lookup across multiple callbooks — QRZ.com, HamCall (online or a local
 * CD/CSV callbook), Callook.info, HamQTH and QRZCQ.com. Providers configured in
 * {@link LookupConfig} are tried in order; the first hit wins. All network calls
 * use the JDK HttpClient (no extra deps) and must be invoked off the UI thread.
 */
public final class CallsignLookup {
    private CallsignLookup() {}

    public record Result(String call, String name, String city, String state, String county,
                         String country, String grid, String source) {
        public boolean found() { return nb(name) || nb(grid) || nb(city); }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();

    // cached session keys for the credentialed providers (re-auth on miss)
    private static volatile String qrzKey, hamqthKey, qrzcqKey;

    /** Try each enabled provider in order; return the first hit, or null. */
    public static Result lookup(String call) {
        if (call == null || call.isBlank()) return null;
        String c = call.trim().toUpperCase();
        for (LookupConfig.Prov p : LookupConfig.PROVIDERS) {
            LookupConfig.Setting s = LookupConfig.get(p.id());
            if (!s.enabled()) continue;
            try {
                Result r = switch (p.id()) {
                    case "callook"    -> callook(c);
                    case "hamqth"     -> hamqth(c, s);
                    case "qrz"        -> qrz(c, s);
                    case "qrzcq"      -> qrzcq(c, s);
                    case "hamcall"    -> hamcall(c, s);
                    case "hamcall_cd" -> hamcallCd(c, s);
                    default -> null;
                };
                if (r != null && r.found()) return r;
            } catch (Exception ignored) { /* try next provider */ }
        }
        return null;
    }

    // ---- Callook.info (free, US) — verified JSON shape ---------------------
    static Result callook(String c) throws Exception {
        String body = httpGet("https://callook.info/" + enc(c) + "/json");
        if (body == null) return null;
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        if (!"VALID".equalsIgnoreCase(jstr(o, "status"))) return null;
        String name = jstr(o, "name"), grid = null, city = null, state = null;
        if (o.has("location") && o.get("location").isJsonObject()) grid = jstr(o.getAsJsonObject("location"), "gridsquare");
        if (o.has("address") && o.get("address").isJsonObject()) {
            String line2 = jstr(o.getAsJsonObject("address"), "line2");   // "CITY, ST ZIP"
            if (line2 != null) {
                int comma = line2.lastIndexOf(',');
                if (comma > 0) {
                    city = line2.substring(0, comma).trim();
                    String[] tok = line2.substring(comma + 1).trim().split("\\s+");
                    if (tok.length > 0) state = tok[0];
                }
            }
        }
        return new Result(c, name, city, state, null, "United States", grid, "Callook");
    }

    // ---- HamQTH (free, worldwide) -----------------------------------------
    static Result hamqth(String c, LookupConfig.Setting s) throws Exception {
        if (blank(s.user()) || blank(s.secret())) return null;
        if (hamqthKey == null) hamqthKey = hamqthSession(s);
        String body = hamqthLookup(c);
        if (body != null && body.contains("Session does not exist")) { hamqthKey = hamqthSession(s); body = hamqthLookup(c); }
        if (body == null) return null;
        return new Result(c, tag(body, "nick"), coalesce(tag(body, "adr_city"), tag(body, "qth")),
                tag(body, "us_state"), tag(body, "us_county"), tag(body, "country"), tag(body, "grid"), "HamQTH");
    }
    private static String hamqthSession(LookupConfig.Setting s) throws Exception {
        String body = httpGet("https://www.hamqth.com/xml.php?u=" + enc(s.user()) + "&p=" + enc(s.secret()));
        return tag(body, "session_id");
    }
    private static String hamqthLookup(String c) throws Exception {
        if (hamqthKey == null) return null;
        return httpGet("https://www.hamqth.com/xml.php?id=" + enc(hamqthKey) + "&callsign=" + enc(c) + "&prg=ARS-Suite");
    }

    // ---- QRZ.com XML (subscription) ---------------------------------------
    static Result qrz(String c, LookupConfig.Setting s) throws Exception {
        if (blank(s.user()) || blank(s.secret())) return null;
        if (qrzKey == null) qrzKey = qrzSession(s);
        String body = qrzLookup(c);
        if (body != null && body.toLowerCase().contains("session timeout")) { qrzKey = qrzSession(s); body = qrzLookup(c); }
        if (body == null) return null;
        String name = join(tag(body, "fname"), tag(body, "name"));
        return new Result(c, name, tag(body, "addr2"), tag(body, "state"), tag(body, "county"),
                tag(body, "country"), tag(body, "grid"), "QRZ");
    }
    private static String qrzSession(LookupConfig.Setting s) throws Exception {
        String body = httpGet("https://xmldata.qrz.com/xml/current/?username=" + enc(s.user()) + ";password=" + enc(s.secret()));
        return tag(body, "Key");
    }
    private static String qrzLookup(String c) throws Exception {
        if (qrzKey == null) return null;
        return httpGet("https://xmldata.qrz.com/xml/current/?s=" + enc(qrzKey) + ";callsign=" + enc(c));
    }

    // ---- QRZCQ.com --------------------------------------------------------
    static Result qrzcq(String c, LookupConfig.Setting s) throws Exception {
        if (blank(s.user()) || blank(s.secret())) return null;
        if (qrzcqKey == null) qrzcqKey = qrzcqSession(s);
        String body = qrzcqLookup(c);
        if (body != null && body.toLowerCase().contains("invalid session")) { qrzcqKey = qrzcqSession(s); body = qrzcqLookup(c); }
        if (body == null) return null;
        String name = join(tag(body, "firstname"), coalesce(tag(body, "lastname"), tag(body, "name")));
        return new Result(c, name, tag(body, "city"), tag(body, "state"), tag(body, "county"),
                tag(body, "country"), coalesce(tag(body, "locator"), tag(body, "grid")), "QRZCQ");
    }
    private static String qrzcqSession(LookupConfig.Setting s) throws Exception {
        String body = httpGet("https://ssl.qrzcq.com/api?username=" + enc(s.user()) + "&password=" + enc(s.secret()));
        return coalesce(tag(body, "Key"), tag(body, "session_id"));
    }
    private static String qrzcqLookup(String c) throws Exception {
        if (qrzcqKey == null) return null;
        return httpGet("https://ssl.qrzcq.com/api?s=" + enc(qrzcqKey) + "&callsign=" + enc(c));
    }

    // ---- HamCall online (Buckmaster) --------------------------------------
    static Result hamcall(String c, LookupConfig.Setting s) throws Exception {
        if (blank(s.user()) || blank(s.secret())) return null;
        String body = httpGet("https://hamcall.net/call?username=" + enc(s.user()) + "&password=" + enc(s.secret())
                + "&rawxml=1&callsign=" + enc(c));
        if (body == null) return null;
        String name = join(tag(body, "fname"), tag(body, "name"));
        return new Result(c, name, tag(body, "city"), tag(body, "state"), tag(body, "county"),
                tag(body, "country"), coalesce(tag(body, "grid"), tag(body, "lat")), "HamCall");
    }

    // ---- HamCall CD / local callbook (CSV) --------------------------------
    // A local callbook file: CSV with a header row naming columns
    // (call, name, city, state, county, country, grid). First matching call wins.
    static Result hamcallCd(String c, LookupConfig.Setting s) throws Exception {
        if (blank(s.extra())) return null;
        Path p = Paths.get(s.extra().startsWith("~") ? System.getProperty("user.home") + s.extra().substring(1) : s.extra());
        if (!Files.isReadable(p)) return null;
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return null;
        String[] hdr = splitCsv(lines.get(0));
        int iCall = col(hdr, "call"), iName = col(hdr, "name"), iCity = col(hdr, "city"),
                iState = col(hdr, "state"), iCounty = col(hdr, "county"), iCountry = col(hdr, "country"), iGrid = col(hdr, "grid");
        if (iCall < 0) return null;
        for (int i = 1; i < lines.size(); i++) {
            String[] f = splitCsv(lines.get(i));
            if (iCall < f.length && f[iCall].trim().equalsIgnoreCase(c))
                return new Result(c, at(f, iName), at(f, iCity), at(f, iState), at(f, iCounty), at(f, iCountry), at(f, iGrid), "HamCall CD");
        }
        return null;
    }

    // ---- HTTP + parsing helpers -------------------------------------------
    private static String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(9))
                .header("User-Agent", "ARS-Suite-JLog").GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 ? resp.body() : null;
    }
    /** Extract the text of the first <name>…</name> element (namespace/attrs tolerant). */
    static String tag(String xml, String name) {
        if (xml == null) return null;
        Matcher m = Pattern.compile("<(?:\\w+:)?" + name + "(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?" + name + ">",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(xml);
        if (!m.find()) return null;
        String v = m.group(1).trim();
        if (v.startsWith("<![CDATA[") && v.endsWith("]]>")) v = v.substring(9, v.length() - 3).trim();
        v = v.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
        return v.isEmpty() ? null : v;
    }
    private static String jstr(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? emptyToNull(o.get(k).getAsString()) : null; }
    private static String join(String a, String b) {
        if (nb(a) && nb(b)) return a.trim() + " " + b.trim();
        return nb(a) ? a.trim() : (nb(b) ? b.trim() : null);
    }
    private static String coalesce(String a, String b) { return nb(a) ? a : b; }
    private static String[] splitCsv(String line) {
        String[] raw = line.split(",", -1);
        for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim().replaceAll("^\"|\"$", "");
        return raw;
    }
    private static int col(String[] hdr, String name) {
        for (int i = 0; i < hdr.length; i++) if (hdr[i].trim().equalsIgnoreCase(name)) return i;
        return -1;
    }
    private static String at(String[] f, int i) { return (i >= 0 && i < f.length) ? emptyToNull(f[i]) : null; }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static boolean nb(String s) { return s != null && !s.isBlank(); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
