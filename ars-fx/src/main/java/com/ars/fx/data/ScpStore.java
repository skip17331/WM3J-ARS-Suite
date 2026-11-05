package com.ars.fx.data;

import java.io.IOException;
import java.net.URI;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Super Check Partial database on disk ({@code ~/.j-hub/scp/MASTER.SCP}), kept up to date from supercheckpartial.com the way its
 * Developers page asks of a logger: the file list ({@code /api/v1/files}) says whether {@code MASTER.SCP} has changed, so the file
 * itself is downloaded only when its ETag differs from the one we hold; at most one check a day; a User-Agent naming this program;
 * and the operator can switch it off (J-Hub &#9656; Data &#9656; Super Check Partial). A failed download keeps the copy we have.
 * The data is never shipped with the app.
 */
public final class ScpStore {
    private ScpStore() {}

    public static final String SITE = "https://www.supercheckpartial.com";
    public static final String FILE = "MASTER.SCP";
    public static final String KEY_AUTO = "data.scpAuto";
    public static final Duration CHECK_EVERY = Duration.ofHours(24), MANUAL_MIN = Duration.ofHours(1);
    private static final int MAX_BYTES = 5_000_000, MIN_CALLS = 1_000;

    /** A reply from the site: the status, the body (empty when there is none) and nothing else we need. */
    public record Reply(int status, byte[] body) {}
    /** The network, so tests can stand in for the site. */
    public interface Http { Reply get(String url) throws IOException, InterruptedException; }

    /** What a refresh did, in words for the screen. */
    public record Outcome(boolean changed, String message) {}

    private static Path dirOverride;
    private static Http httpOverride;
    private static volatile ScpData loaded;
    static void setDirForTest(Path p) { dirOverride = p; loaded = null; }
    static void setHttpForTest(Http h) { httpOverride = h; }
    static Path dir() { return dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-hub", "scp"); }

    public static boolean autoUpdate() { return HubConfig.getBool(KEY_AUTO, true); }

    /** The database now on disk (empty if never downloaded), read once and kept until a refresh replaces it. */
    public static ScpData current() {
        ScpData d = loaded;
        if (d != null) return d;
        try {
            Path f = dir().resolve(FILE);
            d = Files.exists(f) ? ScpData.parse(Files.readString(f, StandardCharsets.ISO_8859_1)) : ScpData.EMPTY;
        } catch (IOException e) { d = ScpData.EMPTY; }
        loaded = d;
        return d;
    }

    /**
     * Brings the file up to date if it is time: always when we have none, otherwise once a day (an operator's own refresh: once an hour).
     * Blocks on the network, so call it off the UI thread. Does nothing when automatic updates are off unless {@code manual}.
     */
    public static Outcome refresh(Instant now, boolean manual) {
        Path file = dir().resolve(FILE), etagF = dir().resolve("MASTER.etag"), stampF = dir().resolve("MASTER.stamp");
        boolean have = Files.exists(file);
        if (!manual && !autoUpdate()) return new Outcome(false, "Automatic updates are off");
        Instant last = readInstant(stampF);
        Duration gap = manual ? MANUAL_MIN : CHECK_EVERY;
        if (have && last != null && Duration.between(last, now).compareTo(gap) < 0) return new Outcome(false, "Checked recently");
        try {
            Http http = httpOverride != null ? httpOverride : ScpStore::live;
            Reply list = http.get(SITE + "/api/v1/files");
            if (list.status() != 200) throw new IOException("file list: HTTP " + list.status());
            String etag = etagOf(new String(list.body(), StandardCharsets.UTF_8), FILE);
            if (etag == null) throw new IOException(FILE + " is not in the site's file list");
            Files.createDirectories(dir());
            if (have && etag.equals(read(etagF))) {
                Files.writeString(stampF, now.toString(), StandardCharsets.UTF_8);
                return new Outcome(false, "Up to date");
            }
            Reply r = http.get(SITE + "/" + FILE);
            if (r.status() != 200) throw new IOException(FILE + ": HTTP " + r.status());
            if (r.body().length > MAX_BYTES) throw new IOException(FILE + " is larger than expected");
            ScpData fresh = ScpData.parse(new String(r.body(), StandardCharsets.ISO_8859_1));
            if (fresh.size() < MIN_CALLS) throw new IOException("the download does not look like a call list");
            Path tmp = dir().resolve(FILE + ".part");
            Files.write(tmp, r.body());
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(etagF, etag, StandardCharsets.UTF_8);
            Files.writeString(stampF, now.toString(), StandardCharsets.UTF_8);
            loaded = fresh;
            return new Outcome(true, "Updated: " + fresh.size() + " calls" + (fresh.release().isEmpty() ? "" : ", release " + fresh.release()));
        } catch (Exception e) {
            String why = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new Outcome(false, have ? "Could not update (" + why + ") - using the saved copy" : "Could not download (" + why + ")");
        }
    }

    /** The ETag the site's file list gives for {@code name}, or null. The list is a small JSON array of {name,size,etag,modified}. */
    static String etagOf(String json, String name) {
        Matcher m = Pattern.compile("\\{[^{}]*\"name\"\\s*:\\s*\"" + Pattern.quote(name) + "\"[^{}]*\\}").matcher(json);
        if (!m.find()) return null;
        Matcher e = Pattern.compile("\"etag\"\\s*:\\s*\"([^\"]+)\"").matcher(m.group());
        return e.find() ? e.group(1) : null;
    }

    private static Reply live(String url) throws IOException, InterruptedException {
        HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(40))
                .header("User-Agent", "WM3J-ARS-Suite/" + version() + " (amateur radio logger)").GET().build();
        HttpResponse<byte[]> rs = c.send(rq, HttpResponse.BodyHandlers.ofByteArray());
        return new Reply(rs.statusCode(), rs.body());
    }
    private static String version() { try { return com.ars.fx.AppVersion.current(); } catch (Throwable t) { return "dev"; } }

    private static Instant readInstant(Path p) { try { return Files.exists(p) ? Instant.parse(Files.readString(p, StandardCharsets.UTF_8).trim()) : null; } catch (Exception e) { return null; } }
    private static String read(Path p) { try { return Files.exists(p) ? Files.readString(p, StandardCharsets.UTF_8).trim() : ""; } catch (IOException e) { return ""; } }
}
