package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * What a station usually sends: for each callsign, its name, state or province, ARRL / RAC section, CQ and ITU zone, grid
 * square, and the Sweepstakes precedence and check. Kept in {@code ~/.j-log/callhistory.db} across contests.
 *
 * <p>Two uses. Typing a call fills the blank exchange fields, and the cluster can tag a spot NEW MULT before any exchange is
 * heard, even where the multiplier lives in the exchange (Sweepstakes sections, state QSO parties, ARRL DX states...).
 *
 * <p>It learns from the operator's own QSOs (contest logs, as they are made or on a rebuild, and the normal log), and can be
 * seeded from a call history file in the common CSV form (N1MM+'s {@code !!Order!!,Call,Name,...} files, or any CSV with a
 * Call column). Values the operator logged win over imported ones. Everything is a hint: a value is only used where the
 * contest accepts it (see {@code JLogContest}), and the operator checks it before logging.
 */
public final class CallHistory {
    private CallHistory() {}

    /** The things remembered, under the key names used in the database. */
    public static final List<String> KEYS = List.of("name", "state", "section", "cqz", "ituz", "grid", "prec", "check", "age");

    private static final int SRC_IMPORTED = 0, SRC_LOGGED = 1;
    /** Prefix of the key a QSO party county is kept under: {@code county:FL}. A county code means nothing in another state's party. */
    public static final String COUNTY = "county:";

    private static Path dirOverride;
    static void setDirForTest(Path p) { dirOverride = p; cache.clear(); }
    static Path file() {
        Path dir = dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-log");
        return dir.resolve("callhistory.db");
    }

    private static final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();
    private static final Map<String, String> NONE = Map.of();

    private static Connection open() throws Exception {
        Path p = file();
        Files.createDirectories(p.getParent());
        Connection c = Sqlite.open(p, 5000);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("CREATE TABLE IF NOT EXISTS call_history(call TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, "
                    + "src INTEGER NOT NULL DEFAULT 0, updated TEXT NOT NULL, PRIMARY KEY(call, key))");
        }
        return c;
    }

    // ---- what a contest field means --------------------------------------------------------------------

    /** The history key a contest entry field holds, or null when the field is not something a station keeps (reports, serials, power, class...). */
    public static String keyForField(String fieldId) {
        if (fieldId == null) return null;
        return switch (fieldId) {
            case "state_prov_rcvd", "state_rcvd" -> "state";
            case "section", "sect_rcvd" -> "section";
            case "cq_zone" -> "cqz";
            case "zone_hq_rcvd" -> "ituz";
            case "grid_rcvd" -> "grid";
            case "name_rcvd" -> "name";
            case "precedence" -> "prec";
            case "check" -> "check";
            case "age_rcvd" -> "age";
            default -> null;
        };
    }

    private static final Set<String> STATES = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME",
            "MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT",
            "VA","WA","WV","WI","WY","PR","VI","GU","AS","MP","AB","BC","MB","NB","NL","NS","NT","NU","ON","PE","QC","SK","YT");
    private static final Pattern SECTION = Pattern.compile("[A-Z]{2,4}"), GRID = Pattern.compile("[A-R]{2}\\d{2}([A-X]{2})?"),
            NAME = Pattern.compile("[A-Z][A-Z' .-]{1,14}"), DIGITS2 = Pattern.compile("\\d{2}");

    /** The value tidied for storage, or null when it is not a believable value for this key. */
    static String clean(String key, String raw) {
        if (raw == null) return null;
        String v = raw.strip().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) return null;
        if (key.startsWith(COUNTY)) return v.matches("[A-Z0-9]{2,6}") ? v : null;      // a county code of one QSO party's state
        switch (key) {
            case "state":   return STATES.contains(v) ? v : null;              // a county, a DX station's power... is not a state
            case "section": return SECTION.matcher(v).matches() ? v : null;
            case "cqz":     return inRange(v, 1, 40);
            case "ituz":    return inRange(v, 1, 90);
            case "grid":    return GRID.matcher(v).matches() ? v : null;
            case "name":    return NAME.matcher(v).matches() ? v : null;
            case "prec":    return v.matches("[QABUMS]") ? v : null;
            case "check":
            case "age":     return DIGITS2.matcher(v).matches() ? v : null;
            default:        return null;
        }
    }
    private static String inRange(String v, int lo, int hi) {
        try { int n = Integer.parseInt(v); return n >= lo && n <= hi ? String.valueOf(n) : null; } catch (NumberFormatException e) { return null; }
    }

    private static final Set<String> SUFFIX = Set.of("P", "M", "MM", "AM", "QRP", "QRPP", "LH", "A", "B", "R", "T", "J");

    /** The call a history entry is kept under: portable and other suffixes dropped (W1AW/4 and W1AW/P are W1AW; VE3/W1AW is W1AW). */
    public static String baseCall(String call) {
        if (call == null) return "";
        String c = call.strip().toUpperCase(Locale.ROOT);
        if (c.indexOf('/') < 0) return c;
        String best = "";
        for (String part : c.split("/")) {
            if (part.isEmpty() || SUFFIX.contains(part) || (part.length() == 1 && Character.isDigit(part.charAt(0)))) continue;
            if (part.length() > best.length()) best = part;
        }
        return best.isEmpty() ? c.split("/")[0] : best;
    }

    // ---- reading ---------------------------------------------------------------------------------------

    /** What is known about a call (empty when nothing). Safe to call from any thread; remembers answers until the history changes. */
    public static Map<String, String> get(String call) {
        String c = baseCall(call);
        if (c.length() < 3) return NONE;
        Map<String, String> hit = cache.get(c);
        if (hit != null) return hit;
        Map<String, String> m = new LinkedHashMap<>();
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement("SELECT key, value FROM call_history WHERE call=?")) {
            ps.setString(1, c);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) m.put(rs.getString(1), rs.getString(2)); }
        } catch (Exception e) { return NONE; }
        Map<String, String> out = m.isEmpty() ? NONE : Map.copyOf(m);
        cache.put(c, out);
        return out;
    }

    /** How many calls are in the history. */
    public static int count() {
        try (Connection con = open(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT call) FROM call_history")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    // ---- writing ---------------------------------------------------------------------------------------

    /** Write many {@code call -> values} at once. A logged value replaces anything; an imported value never replaces a logged one. */
    private static int write(Map<String, Map<String, String>> byCall, int src) {
        int touched = 0;
        if (byCall.isEmpty()) return 0;
        try (Connection con = open()) {
            con.setAutoCommit(false);
            String now = java.time.Instant.now().toString();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO call_history(call,key,value,src,updated) VALUES(?,?,?,?,?) "
                    + "ON CONFLICT(call,key) DO UPDATE SET value=excluded.value, src=excluded.src, updated=excluded.updated "
                    + "WHERE excluded.src >= call_history.src")) {
                for (var e : byCall.entrySet()) {
                    String call = baseCall(e.getKey());
                    if (call.length() < 3) continue;
                    boolean any = false;
                    for (var kv : e.getValue().entrySet()) {
                        String v = clean(kv.getKey(), kv.getValue());
                        if (v == null) continue;
                        ps.setString(1, call); ps.setString(2, kv.getKey()); ps.setString(3, v); ps.setInt(4, src); ps.setString(5, now);
                        ps.addBatch(); any = true;
                    }
                    if (any) touched++;
                    cache.remove(call);
                }
                ps.executeBatch();
            }
            con.commit();
        } catch (Exception e) { System.err.println("[callhistory] write failed: " + e.getMessage()); }
        return touched;
    }

    /** The history values a contest QSO carries, by key (through the contest's own field layout). */
    public static Map<String, String> fromContestQso(ContestPlugin plugin, QsoRecord q) {
        Map<String, String> m = new HashMap<>();
        if (plugin == null || q == null || plugin.getEntryFields() == null) return m;
        for (FieldDef fd : plugin.getEntryFields()) {
            if (fd.isConstant()) continue;
            String key = keyForField(fd.getId());
            String slot = key == null ? null : plugin.fieldSlotColumn(fd.getId());
            if (slot == null) continue;
            String v = switch (slot) {
                case "field1" -> q.getContestField1();
                case "field2" -> q.getContestField2();
                case "field3" -> q.getContestField3();
                case "field4" -> q.getContestField4();
                case "field5" -> q.getContestField5();
                default -> null;
            };
            if (v == null || v.isBlank()) continue;
            if (key.equals("state") && clean("state", v) == null && QsoPartySide.isParty(plugin)
                    && QsoPartySide.counties(plugin).contains(v.strip().toUpperCase(Locale.ROOT))) {
                m.put(COUNTY + QsoPartySide.partyState(plugin), v);                       // the station is in the party's state: remember its county
                continue;
            }
            m.put(key, v);
        }
        return m;
    }

    /** The history keys to try, best first, for a contest field: in a QSO party a station's county before its state. */
    public static List<String> keysForField(ContestPlugin plugin, String fieldId) {
        String key = keyForField(fieldId);
        if (key == null) return List.of();
        if (key.equals("state") && QsoPartySide.isParty(plugin)) return List.of(COUNTY + QsoPartySide.partyState(plugin), "state");
        return List.of(key);
    }

    /** Remember what a logged contest QSO showed. Cheap; call it after each QSO is saved. */
    public static void learn(ContestPlugin plugin, QsoRecord q) {
        if (q == null || q.getCallsign() == null) return;
        write(Map.of(q.getCallsign(), fromContestQso(plugin, q)), SRC_LOGGED);
    }

    private static final java.util.concurrent.ExecutorService LEARNER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "call-history-learn"); t.setDaemon(true); return t;
    });
    /** As {@link #learn}, but on a background thread of its own (for QSOs that arrive in bulk, such as the contest network catching up). */
    public static void learnAsync(ContestPlugin plugin, QsoRecord q) { LEARNER.execute(() -> learn(plugin, q)); }

    /** Learn from QSOs just imported from an ADIF file: the other station's name, state, grid and zones. */
    public static int learnFromAdif(List<Map<String, String>> records) {
        Map<String, Map<String, String>> all = new HashMap<>();
        for (Map<String, String> r : records) {
            String call = r.get("CALL");
            if (call == null || call.isBlank()) continue;
            Map<String, String> m = all.computeIfAbsent(baseCall(call), k -> new HashMap<>());
            if (r.get("NAME") != null) m.put("name", r.get("NAME"));
            if (r.get("STATE") != null) m.put("state", r.get("STATE"));
            if (r.get("GRIDSQUARE") != null) m.put("grid", r.get("GRIDSQUARE"));
            if (r.get("CQZ") != null) m.put("cqz", r.get("CQZ"));
            if (r.get("ITUZ") != null) m.put("ituz", r.get("ITUZ"));
        }
        return write(all, SRC_LOGGED);
    }

    /** Result of a rebuild or import: calls touched, and a message when something went wrong. */
    public record Result(int calls, String error) { public boolean ok() { return error == null; } }

    /** Learn from every contest log and the normal log on disk. */
    public static Result rebuildFromLogs() {
        Map<String, Map<String, String>> all = new HashMap<>();
        try {
            for (ContestPlugin p : com.jlog.plugin.PluginLoader.getInstance().getAvailablePlugins()) {
                for (QsoRecord q : com.jlog.db.ContestQsoDao.getInstance().fetchByContest(p.getContestId())) {
                    if (q.getCallsign() == null) continue;
                    all.computeIfAbsent(baseCall(q.getCallsign()), k -> new HashMap<>()).putAll(fromContestQso(p, q));
                }
            }
        } catch (Exception e) { /* a contest DB that is absent just adds nothing */ }
        for (JLogDb.QsoExport q : JLogDb.allForExport()) {
            if (q.call() == null) continue;
            Map<String, String> m = all.computeIfAbsent(baseCall(q.call()), k -> new HashMap<>());
            if (q.name() != null) m.put("name", q.name());
            if (q.state() != null) m.put("state", q.state());
            if (q.grid() != null) m.put("grid", q.grid());
            if (q.refs() != null && q.refs().cqz() > 0) m.put("cqz", String.valueOf(q.refs().cqz()));
            if (q.refs() != null && q.refs().ituz() > 0) m.put("ituz", String.valueOf(q.refs().ituz()));
        }
        return new Result(write(all, SRC_LOGGED), null);
    }

    // ---- importing a call history file -----------------------------------------------------------------

    private static final Map<String, String> HEADER = new HashMap<>();
    static {
        for (String s : new String[]{"name", "firstname", "opname"}) HEADER.put(s, "name");
        for (String s : new String[]{"state", "st", "stateprov", "province"}) HEADER.put(s, "state");
        for (String s : new String[]{"sect", "section", "arrlsection"}) HEADER.put(s, "section");
        for (String s : new String[]{"cq", "cqz", "cqzone", "cqzn"}) HEADER.put(s, "cqz");
        for (String s : new String[]{"itu", "ituz", "ituzone"}) HEADER.put(s, "ituz");
        for (String s : new String[]{"grid", "gridsquare", "locator"}) HEADER.put(s, "grid");
        for (String s : new String[]{"prec", "precedence"}) HEADER.put(s, "prec");
        for (String s : new String[]{"check", "ck"}) HEADER.put(s, "check");
        HEADER.put("age", "age");
    }

    /** Read a call history CSV: a header row naming the columns (one must be Call), then one station per row. Other columns are ignored. */
    public static Result importFile(Path file) {
        List<String> lines;
        try { lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1); }
        catch (IOException e) { return new Result(0, "Could not read the file: " + e.getMessage()); }
        int callCol = -1;
        Map<Integer, String> cols = new LinkedHashMap<>();
        Map<String, Map<String, String>> all = new HashMap<>();
        for (String line : lines) {
            String l = line.strip();
            if (l.isEmpty() || (l.startsWith("#") && callCol < 0)) continue;
            String[] f = l.split(",", -1);
            if (callCol < 0) {                                                   // the header
                for (int i = 0; i < f.length; i++) {
                    String h = f[i].strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                    if (h.equals("call") || h.equals("callsign")) callCol = i;
                    else if (HEADER.containsKey(h)) cols.put(i, HEADER.get(h));
                }
                if (callCol < 0) return new Result(0, "No Call column in the first row (expected a header such as !!Order!!,Call,Name,Sect)");
                continue;
            }
            if (callCol >= f.length) continue;
            String call = f[callCol].strip().toUpperCase(Locale.ROOT);
            if (call.isEmpty()) continue;
            Map<String, String> m = new HashMap<>();
            for (var c : cols.entrySet()) if (c.getKey() < f.length) m.put(c.getValue(), f[c.getKey()]);
            all.computeIfAbsent(baseCall(call), k -> new HashMap<>()).putAll(m);
        }
        if (callCol < 0) return new Result(0, "The file is empty");
        return new Result(write(all, SRC_IMPORTED), null);
    }

    /** Forget everything. */
    public static void clear() {
        try (Connection con = open(); Statement st = con.createStatement()) { st.execute("DELETE FROM call_history"); } catch (Exception ignored) { }
        cache.clear();
    }

    /** For callers that need the keys a contest can use: the history key of each non-constant entry field, in field order. */
    public static List<String[]> usableFields(ContestPlugin plugin) {
        List<String[]> out = new ArrayList<>();
        if (plugin == null || plugin.getEntryFields() == null) return out;
        for (FieldDef fd : plugin.getEntryFields()) {
            String key = fd.isConstant() ? null : keyForField(fd.getId());
            if (key != null) out.add(new String[]{fd.getId(), key});
        }
        return out;
    }
}
