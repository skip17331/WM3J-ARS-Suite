package com.ars.fx.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Contest Country Files ({@code cty.dat}, by Jim Reisert AD1C, country-files.com, MIT licence): every DXCC-style entity
 * with its prefixes and, per prefix, the CQ and ITU zone and continent. Used to fill what the engine's prefix table does
 * not resolve and to supply CQ / ITU zones from a callsign. A copy ships in the jar (see COPYRIGHT.txt next to it).
 *
 * <p>Format: a header line {@code Name: CQ: ITU: Cont: Lat: Lon: TZ: Primary:} then the alias prefixes, comma separated and
 * ending in a semicolon. An alias may start with {@code =} (that exact callsign only) and carry overrides: {@code (n)} CQ zone,
 * {@code [n]} ITU zone, {@code <lat/lon>} position, {@code {xx}} continent, {@code ~tz~} time zone. Longitude is positive WEST in the file.
 */
public final class CtyData {

    public record Entity(String name, String primary, String continent, int cq, int itu, double lat, double lonEast) {}
    /** What a callsign resolves to: its entity, with the zone / continent after any per-prefix override. */
    public record Match(Entity entity, int cq, int itu, String continent) {}

    private final Map<String, Match> exact = new HashMap<>();
    private final Map<String, Match> prefixes = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private final Map<Entity, List<String>> aliases = new HashMap<>();
    private int maxPrefix = 1;

    private static volatile CtyData bundled;

    /** The copy built into the app. */
    public static CtyData bundled() {
        CtyData c = bundled;
        if (c == null) {
            try (InputStream in = CtyData.class.getResourceAsStream("/com/ars/fx/cty/cty.dat")) {
                c = in == null ? new CtyData() : parse(new String(in.readAllBytes(), StandardCharsets.ISO_8859_1));
            } catch (IOException e) { c = new CtyData(); }
            bundled = c;
        }
        return c;
    }

    public List<Entity> entities() { return entities; }
    public List<String> aliasPrefixes(Entity e) { return aliases.getOrDefault(e, List.of()); }
    public int size() { return entities.size(); }

    // ---- parsing ----------------------------------------------------------------------------------------
    private static final Pattern CQ = Pattern.compile("\\((\\d+)\\)"), ITU = Pattern.compile("\\[(\\d+)\\]"),
            POS = Pattern.compile("<[^>]*>"), CONT = Pattern.compile("\\{([A-Za-z]+)\\}"), TZ = Pattern.compile("~[^~]*~");

    public static CtyData parse(String text) {
        CtyData d = new CtyData();
        Entity cur = null;
        StringBuilder body = new StringBuilder();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.strip();
            if (line.isEmpty()) continue;
            if (cur == null) {                                   // a header line
                String[] f = line.split(":");
                if (f.length < 8) continue;
                try {
                    cur = new Entity(f[0].strip(), f[7].strip().replace("*", ""), f[3].strip(), Integer.parseInt(f[1].strip()),
                            Integer.parseInt(f[2].strip()), Double.parseDouble(f[4].strip()), -Double.parseDouble(f[5].strip()));
                } catch (NumberFormatException e) { cur = null; continue; }
                body.setLength(0);
                d.entities.add(cur);
            } else {                                            // alias lines, until the semicolon
                body.append(line);
                if (line.endsWith(";")) { d.addAliases(cur, body.toString()); cur = null; }
            }
        }
        return d;
    }

    private void addAliases(Entity e, String body) {
        List<String> list = aliases.computeIfAbsent(e, k -> new ArrayList<>());
        for (String tok : body.replace(";", "").split(",")) {
            String t = tok.strip();
            if (t.isEmpty()) continue;
            boolean isExact = t.startsWith("=");
            if (isExact) t = t.substring(1);
            int cq = e.cq(), itu = e.itu(); String cont = e.continent();
            Matcher m = CQ.matcher(t); if (m.find()) cq = Integer.parseInt(m.group(1));
            m = ITU.matcher(t); if (m.find()) itu = Integer.parseInt(m.group(1));
            m = CONT.matcher(t); if (m.find()) cont = m.group(1).toUpperCase(Locale.ROOT);
            String base = TZ.matcher(POS.matcher(CONT.matcher(ITU.matcher(CQ.matcher(t).replaceAll("")).replaceAll("")).replaceAll("")).replaceAll("")).replaceAll("").strip().toUpperCase(Locale.ROOT);
            if (base.isEmpty()) continue;
            Match match = new Match(e, cq, itu, cont);
            if (isExact) exact.put(base, match);
            else { prefixes.put(base, match); maxPrefix = Math.max(maxPrefix, base.length()); list.add(base); }
        }
    }

    // ---- lookup -----------------------------------------------------------------------------------------
    private static final java.util.Set<String> SUFFIXES = java.util.Set.of("P", "M", "MM", "AM", "QRP", "QRPP", "LH", "A", "B", "R", "T", "J");

    /** The entity for a callsign: an exact-call entry first, else the longest matching prefix. Null when nothing fits. */
    public Match lookup(String call) {
        if (call == null) return null;
        String c = call.strip().toUpperCase(Locale.ROOT);
        if (c.isEmpty()) return null;
        Match e = exact.get(c);
        if (e != null) return e;
        int slash = c.indexOf('/');
        if (slash < 0) return byPrefix(c);
        String a = c.substring(0, slash), b = c.substring(slash + 1);
        int s2 = b.indexOf('/');
        if (s2 >= 0) b = b.substring(0, s2);
        if (b.equals("MM") || b.equals("AM")) return null;                // maritime / aeronautical mobile: no entity
        if (b.length() == 1 && Character.isDigit(b.charAt(0))) {         // W1AW/4: the call area changes, the entity does not
            Match m = byPrefix(a);
            return m;
        }
        if (SUFFIXES.contains(b)) return byPrefix(a);
        // VE3/W1AW (prefix first) or W1AW/VE3 (prefix last): the shorter part that is a prefix names the entity
        Match ma = a.length() <= 4 ? byPrefix(a) : null, mb = b.length() <= 4 ? byPrefix(b) : null;
        if (a.length() <= b.length() && ma != null) return ma;
        if (mb != null && b.length() < a.length()) return mb;
        return ma != null ? ma : (mb != null ? mb : byPrefix(a.length() >= b.length() ? a : b));
    }

    private Match byPrefix(String c) {
        for (int n = Math.min(c.length(), maxPrefix); n >= 1; n--) {
            Match m = prefixes.get(c.substring(0, n));
            if (m != null) return m;
        }
        return null;
    }
}
