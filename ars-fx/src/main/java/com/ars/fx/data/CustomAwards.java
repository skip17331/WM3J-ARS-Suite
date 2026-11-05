package com.ars.fx.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Awards the operator defines for themselves - a special event series (the 13 Colonies), a list of counties, the
 * lighthouses they chase... An award is a named checklist: each item says what in a QSO makes it count (a callsign,
 * a state, a grid, an island...). The log is read, never changed; progress is worked out on demand.
 *
 * <p>One small JSON file per award in {@code ~/.j-log/awards/}, so a sheet can be copied, shared or edited by hand.
 */
public final class CustomAwards {
    private CustomAwards() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dirOverride;                       // tests

    /** What on a QSO an item is matched against. */
    public static final String[] FIELDS = {"callsign", "state", "county", "grid", "iota", "sota", "pota", "country", "name", "notes"};
    /** How the item text is compared with that field. */
    public static final String[] HOWS = {"equals", "starts with", "contains"};

    public static final class Item {
        public String label = "";
        public String match = "";          // comma separated alternatives; empty = the label
        public Item() {}
        public Item(String label, String match) { this.label = label; this.match = match; }
    }

    public static final class Def {
        public String id = "";
        public String name = "";
        public String description = "";
        public String field = "callsign";
        public String how = "equals";
        public List<String> bands = new ArrayList<>();     // empty = any band
        public List<String> modes = new ArrayList<>();     // empty = any mode (SSB also takes USB / LSB)
        public String from = "", to = "";                  // yyyy-mm-dd, inclusive; empty = no limit
        public boolean confirmedOnly;                      // only confirmed QSOs count
        public List<Item> items = new ArrayList<>();
        @Override public String toString() { return name == null || name.isBlank() ? id : name; }
    }

    public record Hit(String date, String call, String band, String mode, boolean confirmed) {}

    public static final class Row {
        public final Item item;
        public int qsos;
        public boolean worked, confirmed;
        public Hit first;
        Row(Item item) { this.item = item; }
    }

    public static final class Result {
        public final Def def;
        public final List<Row> rows = new ArrayList<>();
        public int worked, confirmed;
        Result(Def def) { this.def = def; }
        public int total() { return rows.size(); }
    }

    // ---- storage ---------------------------------------------------------------------------------
    static void setDirForTest(Path p) { dirOverride = p; }
    public static Path dir() {
        return dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-log", "awards");
    }

    public static List<Def> list() {
        List<Def> out = new ArrayList<>();
        Path d = dir();
        if (!Files.isDirectory(d)) return out;
        try (var files = Files.list(d)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".json")).sorted()::iterator) {
                Def def = read(f);
                if (def != null) out.add(def);
            }
        } catch (IOException ignored) {}
        return out;
    }

    static Def read(Path f) {
        try {
            Def d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Def.class);
            if (d == null) return null;
            if (d.id == null || d.id.isBlank()) d.id = slug(f.getFileName().toString().replaceFirst("\\.json$", ""));
            if (d.items == null) d.items = new ArrayList<>();
            if (d.bands == null) d.bands = new ArrayList<>();
            if (d.modes == null) d.modes = new ArrayList<>();
            if (d.field == null || d.field.isBlank()) d.field = "callsign";
            if (d.how == null || d.how.isBlank()) d.how = "equals";
            return d;
        } catch (Exception e) { return null; }
    }

    /** Save (create or overwrite) an award. A new award gets a file name from its name that no other award uses. */
    public static Def save(Def d) throws IOException {
        Files.createDirectories(dir());
        if (d.id == null || d.id.isBlank()) {
            String base = slug(d.name), id = base;
            for (int n = 2; Files.exists(dir().resolve(id + ".json")); n++) id = base + "-" + n;
            d.id = id;
        }
        Files.writeString(dir().resolve(d.id + ".json"), GSON.toJson(d), StandardCharsets.UTF_8);
        return d;
    }

    public static void delete(Def d) throws IOException { Files.deleteIfExists(dir().resolve(d.id + ".json")); }

    /** Copy a shared award file in, under a name that is free. */
    public static Def importFile(Path f) throws IOException {
        Def d = read(f);
        if (d == null || d.items.isEmpty()) throw new IOException("not an award sheet: " + f.getFileName());
        d.id = "";        // a new file of our own, never over an existing one
        return save(d);
    }

    public static Path exportJson(Def d, Path folder) throws IOException {
        Files.createDirectories(folder);
        Path out = folder.resolve("award-" + d.id + ".json");
        Files.writeString(out, GSON.toJson(d), StandardCharsets.UTF_8);
        return out;
    }

    public static String slug(String name) {
        String s = (name == null ? "" : name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "award" : s;
    }

    // ---- item text: one item per line, "Label" or "Label = match1, match2" ----------------------------
    public static List<Item> parseItems(String text) {
        List<Item> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int eq = t.indexOf('=');
            if (eq < 0) out.add(new Item(t, ""));
            else out.add(new Item(t.substring(0, eq).trim(), t.substring(eq + 1).trim()));
        }
        return out;
    }
    public static String itemsText(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item i : items) {
            sb.append(i.label);
            if (i.match != null && !i.match.isBlank() && !i.match.equalsIgnoreCase(i.label)) sb.append(" = ").append(i.match);
            sb.append('\n');
        }
        return sb.toString();
    }

    // ---- progress --------------------------------------------------------------------------------
    public static Result compute(Def def, List<JLogDb.QsoExport> qsos) {
        Result res = new Result(def);
        List<List<String>> tokens = new ArrayList<>();
        for (Item it : def.items) {
            res.rows.add(new Row(it));
            List<String> t = new ArrayList<>();
            String src = it.match == null || it.match.isBlank() ? it.label : it.match;
            for (String p : src.split("[,;]")) if (!p.isBlank()) t.add(p.trim().toUpperCase(Locale.ROOT));
            tokens.add(t);
        }
        for (JLogDb.QsoExport q : qsos) {
            if (!passes(def, q)) continue;
            boolean conf = q.refs() != null && q.refs().confirmed();
            if (def.confirmedOnly && !conf) continue;
            String v = value(def.field, q);
            if (v.isEmpty()) continue;
            for (int i = 0; i < res.rows.size(); i++) {
                if (!matches(def.how, v, tokens.get(i))) continue;
                Row r = res.rows.get(i);
                r.qsos++; r.worked = true;
                if (conf) r.confirmed = true;
                if (r.first == null) r.first = new Hit(date(q), q.call() == null ? "" : q.call(), q.band() == null ? "" : q.band(), q.mode() == null ? "" : q.mode(), conf);
            }
        }
        for (Row r : res.rows) { if (r.worked) res.worked++; if (r.confirmed) res.confirmed++; }
        return res;
    }

    private static boolean matches(String how, String v, List<String> tokens) {
        for (String t : tokens) {
            boolean ok = switch (how == null ? "equals" : how) {
                case "starts with" -> v.startsWith(t);
                case "contains" -> v.contains(t);
                default -> v.equals(t);
            };
            if (ok) return true;
        }
        return false;
    }

    private static boolean passes(Def d, JLogDb.QsoExport q) {
        String date = date(q);
        if (!d.from.isBlank() && !date.isEmpty() && date.compareTo(d.from.trim()) < 0) return false;
        if (!d.to.isBlank() && !date.isEmpty() && date.compareTo(d.to.trim()) > 0) return false;
        if (!d.bands.isEmpty()) {
            String b = q.band() == null ? "" : q.band().trim().toLowerCase(Locale.ROOT);
            boolean ok = false;
            for (String x : d.bands) if (x.trim().toLowerCase(Locale.ROOT).equals(b)) ok = true;
            if (!ok) return false;
        }
        if (!d.modes.isEmpty()) {
            String m = q.mode() == null ? "" : q.mode().trim().toUpperCase(Locale.ROOT);
            boolean ok = false;
            for (String x : d.modes) {
                String w = x.trim().toUpperCase(Locale.ROOT);
                if (w.equals(m) || (w.equals("SSB") && (m.equals("USB") || m.equals("LSB")))) ok = true;
            }
            if (!ok) return false;
        }
        return true;
    }

    private static String date(JLogDb.QsoExport q) {
        return q.datetimeUtc() == null || q.datetimeUtc().length() < 10 ? "" : q.datetimeUtc().substring(0, 10);
    }

    private static String value(String field, JLogDb.QsoExport q) {
        JLogDb.Refs r = q.refs() == null ? JLogDb.Refs.NONE : q.refs();
        String v = switch (field == null ? "callsign" : field) {
            case "state" -> q.state();
            case "county" -> q.county();
            case "grid" -> q.grid();
            case "iota" -> r.iota();
            case "sota" -> r.sota();
            case "pota" -> r.pota();
            case "country" -> {
                DxccInfo.Info info = q.call() == null ? null : DxccInfo.of(q.call().trim().toUpperCase(Locale.ROOT));
                yield info == null ? null : info.country();
            }
            case "name" -> q.name();
            case "notes" -> q.notes();
            default -> q.call();
        };
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }

    // ---- sheet export ----------------------------------------------------------------------------
    public static Path exportCsv(Result res, Path folder) throws IOException {
        Files.createDirectories(folder);
        Path out = folder.resolve(res.def.id + "-sheet.csv");
        StringBuilder sb = new StringBuilder("Item,Status,First worked,Call,Band,Mode,QSOs\n");
        for (Row r : res.rows) {
            sb.append(csv(r.item.label)).append(',').append(r.confirmed ? "Confirmed" : r.worked ? "Worked" : "").append(',');
            sb.append(r.first == null ? ",,," : csv(r.first.date()) + "," + csv(r.first.call()) + "," + csv(r.first.band()) + "," + csv(r.first.mode()) + ",");
            sb.append(r.qsos).append('\n');
        }
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        return out;
    }
    private static String csv(String s) {
        String t = s == null ? "" : s;
        return t.contains(",") || t.contains("\"") || t.contains("\n") ? "\"" + t.replace("\"", "\"\"") + "\"" : t;
    }

    // ---- starting points ----------------------------------------------------------------------------
    /** Example: the 13 Colonies special event stations (K2A to K2M, and the WM3GPO bonus station). Check the list for the year you operate. */
    public static Def template13Colonies() {
        Def d = new Def();
        d.name = "13 Colonies special event";
        d.description = "Work the thirteen special event stations, K2A to K2M, plus the WM3GPO bonus station. Edit the list to match the year.";
        d.field = "callsign"; d.how = "equals";
        for (char c = 'A'; c <= 'M'; c++) d.items.add(new Item("K2" + c, ""));
        d.items.add(new Item("WM3GPO (bonus)", "WM3GPO"));
        return d;
    }
    public static Def blank() {
        Def d = new Def();
        d.name = "My award";
        d.description = "";
        return d;
    }
    /** Group helper for the editor's field picker. */
    public static Map<String, String> fieldHelp() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("callsign", "the other station's call (special event series)");
        m.put("state", "state / province");
        m.put("county", "county");
        m.put("grid", "grid square (use 'starts with' for 4 characters)");
        m.put("iota", "island reference");
        m.put("sota", "summit reference");
        m.put("pota", "park reference");
        m.put("country", "DXCC country name");
        m.put("name", "operator name");
        m.put("notes", "anything in the QSO's notes / comment");
        return m;
    }
}
