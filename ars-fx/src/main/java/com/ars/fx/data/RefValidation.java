package com.ars.fx.data;

import com.jlog.util.Maidenhead;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks of the IOTA / SOTA / POTA references typed on a QSO, in three steps that each add something the one before cannot say:
 * <ol>
 *   <li><b>Shape</b> ({@link #shape}): the strict pattern for the programme, instant and offline. A near miss ("US0001", "W7W-LC-001")
 *       is answered with the corrected reference.</li>
 *   <li><b>Existence</b> ({@link #validate}): the reference is asked of the service that owns the list ({@link RefLookup}; answers are kept a week).
 *       "Not known" is a problem; "could not ask" (offline) is not, and is reported as such rather than as an error.</li>
 *   <li><b>Sense</b>: a park or summit whose grid is far from the grid on the QSO, or an island group in a different DXCC entity from the callsign.
 *       These are warnings, never errors: operators activate from the edge of a park, and visitors operate from islands that are not their own entity.</li>
 * </ol>
 * Nothing here saves or refuses anything: a caller shows the {@link Result} and decides.
 */
public final class RefValidation {
    private RefValidation() {}

    /** OK: checked and fine. WARN: exists but looks wrong for this QSO. BAD: wrong shape or not known to the service. UNKNOWN: the shape is fine but it could not be looked up. */
    public enum Level { OK, WARN, BAD, UNKNOWN }

    public record Result(Level level, String message, RefLookup.Ref ref) {
        static Result of(Level l, String m) { return new Result(l, m, null); }
    }

    /** What else is known about the QSO: the other station's grid square (blank if none) and DXCC entity number (0 if none). */
    public record Context(String grid, int dxcc) { public static final Context NONE = new Context("", 0); }

    /** A grid this far from the park or summit is worth a second look. */
    static final double FAR_KM = 250;

    // POTA refs are a 1-3 character entity prefix and four or five digits (K-0001, VE-0001, G-10000); SOTA is association/region-three digits (W7W/LC-001, G/LD-001);
    // IOTA is a continent code and three digits (NA-005).
    private static final Pattern POTA = Pattern.compile("^[A-Z0-9]{1,3}-\\d{4,5}$"), SOTA = Pattern.compile("^[A-Z0-9]{1,3}/[A-Z]{2}-\\d{3}$"),
            IOTA = Pattern.compile("^(AF|AN|AS|EU|NA|OC|SA)-\\d{3}$");
    private static final Pattern POTA_NEAR = Pattern.compile("^([A-Z0-9]{1,3}?)[ _.]?-?(\\d{4,5})$"),
            SOTA_NEAR = Pattern.compile("^([A-Z0-9]{1,3})[-_ ]([A-Z]{2})[-_ /]?(\\d{3})$"),
            IOTA_NEAR = Pattern.compile("^(AF|AN|AS|EU|NA|OC|SA)[ _./]?(\\d{1,3})$");

    private static String example(RefLookup.Kind k) { return switch (k) { case POTA -> "US-0001"; case SOTA -> "W7W/LC-001"; case IOTA -> "NA-005"; }; }
    private static String what(RefLookup.Kind k) { return switch (k) { case POTA -> "park"; case SOTA -> "summit"; case IOTA -> "island group"; }; }

    /** The references in a field: POTA may list several parks (a "two-fer"), the others take one. */
    public static List<String> tokens(RefLookup.Kind kind, String text) {
        String t = RefLookup.normalise(text);
        List<String> out = new ArrayList<>();
        if (t.isEmpty()) return out;
        if (kind == RefLookup.Kind.POTA) { for (String s : t.split("[,;\\s]+")) if (!s.isEmpty()) out.add(s); }
        else out.add(t);
        return out;
    }

    /**
     * The offline check: null when the field is empty (nothing to say), a BAD result when a reference has the wrong shape, else an OK result
     * (which only means the shape is right; {@link #validate} goes on to ask the service).
     */
    public static Result shape(RefLookup.Kind kind, String text) {
        List<String> refs = tokens(kind, text);
        if (refs.isEmpty()) return null;
        String bare = RefLookup.normalise(text);
        if (kind != RefLookup.Kind.POTA && bare.matches(".*[,;]\\s*\\S.*"))
            return Result.of(Level.BAD, kind + " takes one " + what(kind) + " reference per QSO (" + example(kind) + ")");
        for (String r : refs) {
            if (matches(kind, r)) continue;
            String fix = nearMiss(kind, r);
            return Result.of(Level.BAD, kind + " " + r + ": not a " + kind + " reference"
                    + (fix != null ? " - did you mean " + fix + "?" : " (it looks like " + example(kind) + ")"));
        }
        return Result.of(Level.OK, "");
    }

    private static boolean matches(RefLookup.Kind k, String r) {
        return switch (k) { case POTA -> POTA.matcher(r).matches(); case SOTA -> SOTA.matcher(r).matches(); case IOTA -> IOTA.matcher(r).matches(); };
    }

    /** A corrected reference for a typo that is easy to read, or null. */
    static String nearMiss(RefLookup.Kind k, String r) {
        Matcher m;
        switch (k) {
            case POTA -> { m = POTA_NEAR.matcher(r); if (m.matches()) { String f = m.group(1) + "-" + m.group(2); return POTA.matcher(f).matches() ? f : null; } }
            case SOTA -> { m = SOTA_NEAR.matcher(r); if (m.matches()) return m.group(1) + "/" + m.group(2) + "-" + m.group(3); }
            case IOTA -> { m = IOTA_NEAR.matcher(r); if (m.matches() && m.group(2).length() == 3) return m.group(1) + "-" + m.group(2); }
        }
        return null;
    }

    /**
     * The full check for one field: shape, then existence, then sense. Blocks on the lookup (a saved copy answers at once), so call it off the FX
     * thread. Null for an empty field.
     */
    public static Result validate(RefLookup.Kind kind, String text, Context ctx) {
        Result s = shape(kind, text);
        if (s == null || s.level() == Level.BAD) return s;
        List<String> refs = tokens(kind, text);
        Level worst = Level.OK;
        List<String> lines = new ArrayList<>();
        RefLookup.Ref first = null;
        for (int i = 0; i < refs.size(); i++) {
            String r = refs.get(i);
            RefLookup.Check c = switch (kind) { case POTA -> RefLookup.checkPota(r); case SOTA -> RefLookup.checkSota(r); case IOTA -> RefLookup.checkIota(r); };
            switch (c.found()) {
                case NOT_FOUND -> { return Result.of(Level.BAD, kind + " " + r + ": not a known " + what(kind) + " (check the number)"); }
                case UNREACHABLE -> { worst = worse(worst, Level.UNKNOWN); lines.add(r + ": shape is right; could not be looked up just now"); }
                case FOUND -> {
                    RefLookup.Ref ref = c.ref();
                    if (first == null) first = ref;
                    String line = r + ": " + ref.name() + (ref.grid().isEmpty() ? "" : "  ·  " + ref.grid()) + (ref.detail().isEmpty() ? "" : "  ·  " + ref.detail());
                    String sense = i == 0 ? sense(kind, ref, ctx) : null;           // the QSO's grid belongs to the first park of a two-fer
                    if (sense != null) { worst = worse(worst, Level.WARN); line += "  -  " + sense; }
                    lines.add(line);
                }
            }
        }
        return new Result(worst, String.join("   |   ", lines), first);
    }

    /** Level.UNKNOWN is less serious than WARN (it is not a finding about the QSO). */
    private static Level worse(Level a, Level b) {
        if (a == Level.WARN || b == Level.WARN) return Level.WARN;
        if (a == Level.UNKNOWN || b == Level.UNKNOWN) return Level.UNKNOWN;
        return Level.OK;
    }

    /** What does not add up for this QSO, or null. */
    static String sense(RefLookup.Kind kind, RefLookup.Ref ref, Context ctx) {
        if (kind == RefLookup.Kind.IOTA) {
            int want = RefLookup.iotaDxcc(ref.ref());
            if (want > 0 && ctx.dxcc() > 0 && want != ctx.dxcc())
                return "this group is in DXCC entity " + want + " but the callsign is entity " + ctx.dxcc() + " (fine if they are visiting)";
            return null;
        }
        String q = ctx.grid() == null ? "" : ctx.grid().trim().toUpperCase(Locale.ROOT), g = ref.grid() == null ? "" : ref.grid().trim().toUpperCase(Locale.ROOT);
        if (!Maidenhead.isValid(q) || !Maidenhead.isValid(g)) return null;
        double km = Maidenhead.distanceKm(q, g);
        return km > FAR_KM ? String.format(Locale.ROOT, "the QSO grid %s is about %.0f km from it (%s)", q, km, g) : null;
    }
}
