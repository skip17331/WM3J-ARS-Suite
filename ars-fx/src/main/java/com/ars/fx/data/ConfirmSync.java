package com.ars.fx.data;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches your confirmations from LoTW and eQSL and ticks them on the QSOs already in the log - the same result as importing
 * their ADIF by hand. It only confirms QSOs you already have; a confirmation for a contact that is not in the log is counted
 * and left alone (use Import ADIF to add those).
 *
 * <ul>
 *   <li><b>LoTW</b> uses the documented query service ({@code lotwreport.adi}) with your LoTW website login.</li>
 *   <li><b>eQSL</b> uses the inbox download page. eQSL does not document it, so this part follows what other loggers do and
 *       is the less certain of the two.</li>
 * </ul>
 * Logins are sent over HTTPS and are never written into a cache file or a log. A service is asked at most once every 15 minutes,
 * and after the first time only for what arrived since.
 */
public final class ConfirmSync {
    private ConfirmSync() {}

    public record Result(String service, boolean ok, int received, int matched, String message) {}

    private static Function<String, String> http;                    // url -> body, for tests
    static void setHttpForTest(Function<String, String> f) { http = f; }
    private static final Duration MIN_GAP = Duration.ofMinutes(15);

    // ---- LoTW -----------------------------------------------------------------------------------------
    public static Result lotw() {
        String login = UploadConfig.field("lotw", "weblogin").trim(), pw = UploadConfig.field("lotw", "webpass");
        if (login.isBlank() || pw.isBlank()) return new Result("LoTW", false, 0, 0, "Enter your LoTW website login and password first (Setup \u25b8 Data \u25b8 Online log upload).");
        String wait = tooSoon("lotw");
        if (wait != null) return new Result("LoTW", false, 0, 0, wait);
        String since = HubConfig.get("confirm.lotw.since", "").trim();
        String url = "https://lotw.arrl.org/lotwuser/lotwreport.adi?login=" + enc(login) + "&password=" + enc(pw)
                + "&qso_query=1&qso_qsl=yes&qso_qsldetail=yes" + (since.isEmpty() ? "" : "&qso_qslsince=" + enc(since));
        String body;
        try { body = fetch(url); } catch (Exception e) { return new Result("LoTW", false, 0, 0, "Could not reach LoTW: " + safe(e, pw)); }
        return applyLotw(body);
    }

    static Result applyLotw(String body) {
        if (body == null || !body.toUpperCase(Locale.ROOT).contains("<EOH>"))
            return new Result("LoTW", false, 0, 0, "LoTW did not return a log: " + page(body) + " Check your LoTW website login.");
        List<Map<String, String>> recs = LogImport.parse(body);
        int[] n = apply(recs, true);
        Matcher m = Pattern.compile("(?i)<APP_LoTW_LASTQSL:\\d+>\\s*([0-9: \\-]+)").matcher(body);
        if (m.find()) HubConfig.set("confirm.lotw.since", m.group(1).trim());
        HubConfig.set("confirm.lotw.at", String.valueOf(System.currentTimeMillis()));
        return new Result("LoTW", true, recs.size(), n[0], summary(recs.size(), n[0]));
    }

    // ---- eQSL -----------------------------------------------------------------------------------------
    public static Result eqsl() {
        String user = UploadConfig.field("eqsl", "user").trim(), pw = UploadConfig.field("eqsl", "password");
        if (user.isBlank() || pw.isBlank()) return new Result("eQSL", false, 0, 0, "Enter your eQSL username and password first (Setup \u25b8 Data \u25b8 Online log upload).");
        String wait = tooSoon("eqsl");
        if (wait != null) return new Result("eQSL", false, 0, 0, wait);
        String since = HubConfig.get("confirm.eqsl.since", "").trim();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(Instant.now().atZone(ZoneOffset.UTC));
        String base = "https://www.eqsl.cc/qslcard/";
        String url = base + "DownloadInBox.cfm?UserName=" + enc(user) + "&Password=" + enc(pw) + (since.isEmpty() ? "" : "&RcvdSince=" + enc(since));
        try {
            String page = fetch(url);
            String adif = page;
            if (!page.toUpperCase(Locale.ROOT).contains("<EOH>")) {                     // the page links to the file it made for you
                Matcher m = Pattern.compile("(?i)href\\s*=\\s*[\"']([^\"']+\\.adi)[\"']").matcher(page);
                if (!m.find()) return new Result("eQSL", false, 0, 0, "eQSL did not return a log: " + page(page) + " Check your eQSL username and password (a # in the password can stop the download working).");
                adif = fetch(URI.create(base).resolve(m.group(1)).toString());
            }
            return applyEqsl(adif, stamp);
        } catch (Exception e) {
            return new Result("eQSL", false, 0, 0, "Could not reach eQSL: " + safe(e, pw));
        }
    }

    static Result applyEqsl(String adif, String stamp) {
        List<Map<String, String>> recs = LogImport.parse(adif);
        int[] n = apply(recs, false);
        HubConfig.set("confirm.eqsl.since", stamp);
        HubConfig.set("confirm.eqsl.at", String.valueOf(System.currentTimeMillis()));
        return new Result("eQSL", true, recs.size(), n[0], summary(recs.size(), n[0]));
    }

    /** Ask every service that has a login. */
    public static List<Result> all() {
        List<Result> out = new ArrayList<>();
        out.add(lotw());
        out.add(eqsl());
        return out;
    }

    // ---- ticking the confirmations --------------------------------------------------------------------
    /** @return {matched} - how many of the records were found in the log. */
    static int[] apply(List<Map<String, String>> recs, boolean lotw) {
        int matched = 0;
        for (Map<String, String> r : recs) {
            String call = r.get("CALL"), date = r.get("QSO_DATE"), time = r.get("TIME_ON");
            if (call == null || date == null || date.length() != 8) continue;
            if (time == null || time.length() < 4) time = "0000";
            String dt = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + " "
                    + time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + (time.length() >= 6 ? time.substring(4, 6) : "00");
            String band = r.get("BAND") != null ? r.get("BAND").toLowerCase(Locale.ROOT) : Bands.ofMhz(r.get("FREQ"));
            JLogDb.Refs got = LogImport.refsOf(r);
            // everything in a LoTW download is a LoTW confirmation, everything in an eQSL inbox an eQSL one; QSL_RCVD there is not paper
            JLogDb.Refs add = new JLogDb.Refs(got.iota(), got.sota(), got.pota(), got.cqz(), got.ituz(), false, lotw, !lotw);
            boolean found = JLogDb.mergeRefs(call, dt, band, r.get("MODE"), add);
            if (!found) {                                           // the log may say USB where they say SSB: any mode at that minute and band
                String sub = r.get("SUBMODE");
                if (sub != null && !sub.isBlank()) found = JLogDb.mergeRefs(call, dt, band, sub, add);
                if (!found) found = JLogDb.mergeRefsAnyMode(call, dt, band, add);
            }
            if (found) matched++;
        }
        return new int[]{matched};
    }

    private static String summary(int received, int matched) {
        if (received == 0) return "Nothing new.";
        return received + " confirmation" + (received == 1 ? "" : "s") + " received, " + matched + " matched to QSOs in your log"
                + (matched < received ? " (" + (received - matched) + " are for contacts that are not in the log; Import ADIF adds those)" : "") + ".";
    }

    // ---- plumbing ---------------------------------------------------------------------------------------
    private static String tooSoon(String id) {
        try {
            long at = Long.parseLong(HubConfig.get("confirm." + id + ".at", "0"));
            long left = MIN_GAP.toMillis() - (System.currentTimeMillis() - at);
            if (left > 0) return "Checked a moment ago - the service asks us not to poll; try again in " + ((left + 59999) / 60000) + " min.";
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private static String fetch(String url) throws IOException, InterruptedException {
        if (http != null) return http.apply(url);
        HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120))
                .header("User-Agent", "WM3J-ARS-Suite/" + com.ars.fx.AppVersion.current() + " (amateur radio logger)").GET().build();
        HttpResponse<byte[]> rs = c.send(rq, HttpResponse.BodyHandlers.ofByteArray());
        if (rs.statusCode() != 200) throw new IOException("HTTP " + rs.statusCode());
        return new String(rs.body(), StandardCharsets.UTF_8);
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    /** A short readable line out of an HTML error page. */
    static String page(String html) {
        if (html == null) return "no answer.";
        String t = html.replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ").replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? "an empty page." : (t.length() > 160 ? t.substring(0, 160) + "\u2026" : t);
    }
    /** Never let a password into a message. */
    static String safe(Exception e, String secret) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return secret == null || secret.isEmpty() ? m : m.replace(secret, "***").replace(enc(secret), "***");
    }
}
