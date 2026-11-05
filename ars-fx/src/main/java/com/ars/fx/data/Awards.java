package com.ars.fx.data;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Award progress worked out from the J-Log normal log: DXCC (by band and by mode group), WAS, WAC, grid squares (VUCC),
 * IOTA, POTA, SOTA and the CQ / ITU zones. Every award counts QSOs <b>worked</b> and QSOs <b>confirmed</b> — a QSO is
 * confirmed when a paper QSL, LoTW or eQSL has come back for it (see {@link JLogDb.Refs}).
 *
 * <p>Entities come from the engine's DXCC prefix data through {@link DxccInfo} (the same data the contest scorer uses).
 * Zones are not worked out from the callsign: they count only where the log holds a CQ / ITU zone (ADIF import).
 * IOTA, POTA and SOTA count the references entered on the QSO or imported — a reference is for the <i>other</i> station
 * (hunter credit).
 */
public final class Awards {
    private Awards() {}

    /** WAS — the 50 states. */
    public static final String[] STATES = {
        "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD",
        "MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC",
        "SD","TN","TX","UT","VT","VA","WA","WV","WI","WY" };

    public static final String[] CONTINENTS = {"NA","SA","EU","AF","AS","OC"};

    /** The bands shown in the DXCC matrix, longest wavelength first. */
    public static final String[] BANDS = {"160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m","70cm"};

    /** Mode groups DXCC is counted in. */
    public static final String[] MODE_GROUPS = {"CW", "Phone", "Digital"};

    private static final Pattern GRID4 = Pattern.compile("^[A-R]{2}[0-9]{2}");

    /** One award's totals. {@code all} lists every possible item when the award has a fixed list (states, continents, zones). */
    public record Progress(String code, String name, int worked, int confirmed, int target,
                           Set<String> workedSet, Set<String> confirmedSet, List<String> all) {
        public double fraction() { return target > 0 ? Math.min(1.0, (double) worked / target) : 0; }
    }

    /** A DXCC entity and where it has been worked. */
    public static final class Entity {
        public final String key, name, continent; public final int number;
        public String firstDate = "";
        public final Set<String> bandsWorked = new HashSet<>(), bandsConfirmed = new HashSet<>();
        public final Set<String> modesWorked = new HashSet<>(), modesConfirmed = new HashSet<>();
        public boolean confirmed; public int qsos;
        Entity(String key, String name, String continent, int number) { this.key = key; this.name = name; this.continent = continent; this.number = number; }
    }

    /** One IOTA / POTA / SOTA reference or grid square. */
    public static final class RefStat {
        public final String key; public String firstDate = "", lastCall = ""; public int qsos; public boolean confirmed;
        public final Set<String> bands = new TreeSet<>(), bandsConfirmed = new TreeSet<>();
        RefStat(String key) { this.key = key; }
    }

    /** Everything computed from one pass over the log. */
    public static final class Snapshot {
        public int qsoCount, unresolved;
        public Progress dxcc, was, wac, grids, iota, pota, sota, cqz, ituz;
        public final Map<String, Entity> entities = new TreeMap<>();
        public final Map<String, RefStat> gridStats = new TreeMap<>(), iotaStats = new TreeMap<>(), potaStats = new TreeMap<>(), sotaStats = new TreeMap<>();
        /** DXCC worked / confirmed per band ("20m" → n) and per mode group. */
        public final Map<String, int[]> dxccByBand = new LinkedHashMap<>(), dxccByMode = new LinkedHashMap<>();
        public final Set<String> workedStates = new TreeSet<>(), confirmedStates = new TreeSet<>();
        /** States worked / confirmed on each band ("20m" to the states), for 5-Band WAS. */
        public final Map<String, Set<String>> statesByBand = new LinkedHashMap<>(), statesConfirmedByBand = new LinkedHashMap<>();
        public List<Progress> all() { return List.of(dxcc, was, wac, grids, iota, pota, sota, cqz, ituz); }
    }

    /** Computes every award from the whole normal log. Blocking (reads the database); call off the FX thread for big logs. */
    public static Snapshot compute() { return compute(JLogDb.allForExport()); }

    public static Snapshot compute(List<JLogDb.QsoExport> rows) {
        Snapshot s = new Snapshot();
        s.qsoCount = rows.size();
        Map<String, DxccInfo.Info> infoCache = new HashMap<>();
        Set<String> contW = new TreeSet<>(), contC = new TreeSet<>(), cqW = new TreeSet<>(), cqC = new TreeSet<>(), ituW = new TreeSet<>(), ituC = new TreeSet<>();
        for (JLogDb.QsoExport q : rows) {
            JLogDb.Refs r = q.refs() == null ? JLogDb.Refs.NONE : q.refs();
            boolean conf = r.confirmed();
            String call = q.call() == null ? "" : q.call().trim().toUpperCase(Locale.ROOT);
            String band = q.band() == null ? "" : q.band().trim().toLowerCase(Locale.ROOT);
            String date = q.datetimeUtc() == null || q.datetimeUtc().length() < 10 ? "" : q.datetimeUtc().substring(0, 10);

            // DXCC entity + continent
            DxccInfo.Info info = infoCache.computeIfAbsent(call, DxccInfo::of);
            if (info == null) s.unresolved++;
            else {
                String key = info.dxcc() > 0 ? String.valueOf(info.dxcc()) : (info.country() != null ? info.country() : info.continent() + "?");
                String nm = info.country() != null ? info.country() : key;
                Entity e = s.entities.computeIfAbsent(key, k -> new Entity(k, nm, info.continent(), info.dxcc()));
                e.qsos++;
                if (e.firstDate.isEmpty() || (!date.isEmpty() && date.compareTo(e.firstDate) < 0)) e.firstDate = date;
                String mg = modeGroup(q.mode());
                if (!band.isEmpty()) { e.bandsWorked.add(band); if (conf) e.bandsConfirmed.add(band); }
                e.modesWorked.add(mg); if (conf) { e.modesConfirmed.add(mg); e.confirmed = true; }
                if (info.continent() != null && !info.continent().isBlank()) { contW.add(info.continent()); if (conf) contC.add(info.continent()); }
            }
            // WAS
            String st = q.state() == null ? "" : q.state().trim().toUpperCase(Locale.ROOT);
            if (Arrays.asList(STATES).contains(st)) {
                s.workedStates.add(st); if (conf) s.confirmedStates.add(st);
                if (!band.isEmpty()) {
                    s.statesByBand.computeIfAbsent(band, k -> new TreeSet<>()).add(st);
                    if (conf) s.statesConfirmedByBand.computeIfAbsent(band, k -> new TreeSet<>()).add(st);
                }
            }
            // grids (4-character squares)
            String g = q.grid() == null ? "" : q.grid().trim().toUpperCase(Locale.ROOT);
            if (GRID4.matcher(g).find()) ref(s.gridStats, g.substring(0, 4), call, band, date, conf);
            // references
            if (!r.iota().isBlank()) ref(s.iotaStats, r.iota().trim().toUpperCase(Locale.ROOT), call, band, date, conf);
            if (!r.pota().isBlank()) ref(s.potaStats, r.pota().trim().toUpperCase(Locale.ROOT), call, band, date, conf);
            if (!r.sota().isBlank()) ref(s.sotaStats, r.sota().trim().toUpperCase(Locale.ROOT), call, band, date, conf);
            // zones: the QSO's own when it has them, else from the callsign (the contest country files); approximate for the
            // big multi-zone countries, where only the station's location tells the zone
            int cq = r.cqz(), itu = r.ituz();
            if (cq < 1 || itu < 1) { int[] z = zonesFromCall(call); if (cq < 1) cq = z[0]; if (itu < 1) itu = z[1]; }
            if (cq >= 1 && cq <= 40) { cqW.add(String.valueOf(cq)); if (conf) cqC.add(String.valueOf(cq)); }
            if (itu >= 1 && itu <= 90) { ituW.add(String.valueOf(itu)); if (conf) ituC.add(String.valueOf(itu)); }
        }

        // DXCC totals, by band and by mode group
        Set<String> entW = s.entities.keySet(), entC = new TreeSet<>();
        for (Entity e : s.entities.values()) if (e.confirmed) entC.add(e.key);
        s.dxcc = new Progress("DXCC", "DX Century Club", entW.size(), entC.size(), 100, new TreeSet<>(entW), entC, null);
        for (String b : BANDS) {
            int w = 0, c = 0;
            for (Entity e : s.entities.values()) { if (e.bandsWorked.contains(b)) w++; if (e.bandsConfirmed.contains(b)) c++; }
            s.dxccByBand.put(b, new int[]{w, c});
        }
        for (String m : MODE_GROUPS) {
            int w = 0, c = 0;
            for (Entity e : s.entities.values()) { if (e.modesWorked.contains(m)) w++; if (e.modesConfirmed.contains(m)) c++; }
            s.dxccByMode.put(m, new int[]{w, c});
        }
        List<String> states = Arrays.asList(STATES), conts = Arrays.asList(CONTINENTS);
        s.was  = new Progress("WAS", "Worked All States", s.workedStates.size(), s.confirmedStates.size(), STATES.length, s.workedStates, s.confirmedStates, states);
        s.wac  = new Progress("WAC", "Worked All Continents", contW.size(), contC.size(), CONTINENTS.length, contW, contC, conts);
        s.grids = fromRefs("GRID", "Grid squares (4-character)", s.gridStats, 100);
        s.iota = fromRefs("IOTA", "Islands on the Air", s.iotaStats, 100);
        s.pota = fromRefs("POTA", "Parks on the Air (hunter)", s.potaStats, 0);
        s.sota = fromRefs("SOTA", "Summits on the Air (chaser)", s.sotaStats, 0);
        s.cqz  = new Progress("WAZ", "CQ zones", cqW.size(), cqC.size(), 40, cqW, cqC, numbers(40));
        s.ituz = new Progress("ITU", "ITU zones", ituW.size(), ituC.size(), 90, ituW, ituC, numbers(90));
        return s;
    }

    private static void ref(Map<String, RefStat> m, String key, String call, String band, String date, boolean conf) {
        RefStat r = m.computeIfAbsent(key, RefStat::new);
        r.qsos++; r.lastCall = call; if (conf) r.confirmed = true;
        if (!band.isEmpty()) { r.bands.add(band); if (conf) r.bandsConfirmed.add(band); }
        if (r.firstDate.isEmpty() || (!date.isEmpty() && date.compareTo(r.firstDate) < 0)) r.firstDate = date;
    }

    private static Progress fromRefs(String code, String name, Map<String, RefStat> m, int target) {
        Set<String> w = new TreeSet<>(m.keySet()), c = new TreeSet<>();
        for (RefStat r : m.values()) if (r.confirmed) c.add(r.key);
        return new Progress(code, name, w.size(), c.size(), target > 0 ? target : milestone(w.size()), w, c, null);
    }

    private static List<String> numbers(int n) {
        List<String> l = new ArrayList<>(); for (int i = 1; i <= n; i++) l.add(String.valueOf(i)); return l;
    }

    /** The next round number above {@code n}, for awards with no fixed target (POTA / SOTA levels). */
    static int milestone(int n) {
        for (int m : new int[]{10, 25, 50, 100, 200, 300, 500, 1000, 2000}) if (n < m) return m;
        return ((n / 1000) + 1) * 1000;
    }

    /** CW, Phone or Digital — the groups DXCC is endorsed in. */
    /** {CQ zone, ITU zone} for a call from the contest country files, or {0, 0}. */
    public static int[] zonesFromCall(String call) {
        CtyData.Match m = CtyData.bundled().lookup(call);
        return m == null ? new int[]{0, 0} : new int[]{m.cq(), m.itu()};
    }

    /** The key an entity is counted under: its DXCC number, else its country name, else its continent. */
    public static String entityKey(DxccInfo.Info info) {
        return info.dxcc() > 0 ? String.valueOf(info.dxcc()) : (info.country() != null ? info.country() : info.continent() + "?");
    }

    public static String modeGroup(String mode) {
        String m = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (m.equals("CW")) return "CW";
        if (m.equals("SSB") || m.equals("USB") || m.equals("LSB") || m.equals("AM") || m.equals("FM") || m.contains("PHONE") || m.contains("VOICE")) return "Phone";
        return "Digital";
    }
}
