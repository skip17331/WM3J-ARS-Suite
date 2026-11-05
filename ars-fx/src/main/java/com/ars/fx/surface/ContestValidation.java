package com.ars.fx.surface;

import com.ars.fx.data.ContestValidators;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checks what the operator types into the contest entry against what the contest definition allows: the callsign's shape,
 * a field's own format (check, zone, grid, serial...), the fixed list a field takes (Sweepstakes section and precedence,
 * Field Day section, CQ zones...), and - more loosely - state / province / county in the QSO parties.
 *
 * <p>Three outcomes besides "fine": {@link Level#PARTIAL} (not valid yet, but still on its way to a valid value while typing),
 * {@link Level#HARD} (can never be valid - the QSO is not logged) and {@link Level#SOFT} (not recognised, but the list is not
 * definitive - the operator may log it anyway).
 */
final class ContestValidation {
    private ContestValidation() {}

    enum Level { OK, PARTIAL, SOFT, HARD }
    record Result(Level level, String message) {
        static final Result OK = new Result(Level.OK, "");
        static Result partial() { return new Result(Level.PARTIAL, ""); }
        static Result soft(String m) { return new Result(Level.SOFT, m); }
        static Result hard(String m) { return new Result(Level.HARD, m); }
        boolean blocks() { return level == Level.HARD; }
    }

    private static final Set<String> US = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME",
            "MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT",
            "VA","WA","WV","WI","WY","PR","VI","GU","AS","MP");
    private static final Set<String> CA = Set.of("AB","BC","MB","NB","NL","NF","LB","NS","NT","NU","ON","PE","PEI","QC","SK","YT","MAR","GTA","GH","TER",
            "ONE","ONN","ONS");

    /**
     * @param complete false while the operator is still typing (a value that is a prefix of a valid one is fine), true on
     *                 leaving the field or when logging.
     */
    static Result check(ContestPlugin plugin, FieldDef fd, String raw, boolean complete) {
        String id = fd.getId();
        if ("band".equals(id) || "mode".equals(id) || fd.getEntryRow() == 1) return Result.OK;
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) return Result.OK;
        String label = fd.getLabel() == null || fd.getLabel().isBlank() ? id : fd.getLabel();

        if ("callsign".equals(id)) return callsign(v);

        // the field's own format
        if (fd.getValidator() != null && !fd.getValidator().isBlank()) {
            if (complete) {
                if (!ContestValidators.valid(fd.getValidator(), v)) return Result.hard("'" + v + "' is not a valid " + label + describe(fd.getValidator()));
            } else if (!typingOk(fd.getValidator(), v)) {
                return Result.hard("'" + v + "' is not a valid " + label + describe(fd.getValidator()));
            }
        }

        // zones are a fixed range
        if ("cq_zone".equals(id) && v.matches("\\d+")) {
            int n = Integer.parseInt(v);
            if (n > 40 || (complete && n < 1)) return Result.hard("CQ zone must be 1 to 40, not " + v);
        }

        // a field with its own list of values (Sweepstakes section and precedence, Field Day section, ...)
        List<String> list = fd.getOptions() != null && !fd.getOptions().isEmpty() ? fd.getOptions() : multiplierValues(plugin, id);
        if (list != null && !list.isEmpty()) {
            Result r = inList(v, list, label, complete);
            if (r.level() != Level.OK) return r;
        } else if (isStateField(plugin, id)) {
            Result r = stateLike(plugin, v, label, complete);
            if (r.level() != Level.OK) return r;
        }

        // a report: loose, because digital and phone contests use different ones
        if (complete && (id.equals("rst_rcvd") || id.equals("rst_received"))
                && !v.matches("[1-5][1-9][1-9]?|[+-]?\\d{1,2}")) return Result.soft("'" + v + "' is not a usual report (59 / 599)");
        return Result.OK;
    }

    // ---- callsign -----------------------------------------------------------------
    private static Result callsign(String v) {
        if (!v.matches("[A-Z0-9/]+")) return Result.hard("'" + v + "' is not a valid callsign (letters, digits and / only)");
        if (v.length() >= 3 && !(v.matches(".*\\d.*") && v.matches(".*[A-Z].*"))) return Result.hard("'" + v + "' is not a valid callsign (needs a letter and a digit)");
        if (v.length() > 14) return Result.hard("'" + v + "' is too long for a callsign");
        return Result.OK;
    }

    // ---- fixed lists --------------------------------------------------------------
    private static Result inList(String v, List<String> list, String label, boolean complete) {
        String key = norm(v);
        for (String o : list) if (norm(o).equals(key)) return Result.OK;
        if (!complete) for (String o : list) if (norm(o).startsWith(key)) return Result.partial();
        String hint = list.size() <= 12 ? "  Valid: " + String.join(", ", list)
                : closest(v, list);
        return Result.hard("'" + v + "' is not a valid " + label + "." + hint);
    }
    private static String closest(String v, List<String> list) {
        List<String> near = new ArrayList<>();
        for (String o : list) if (!v.isEmpty() && o.toUpperCase(Locale.ROOT).charAt(0) == v.charAt(0) && near.size() < 8) near.add(o);
        return near.isEmpty() ? "" : "  Did you mean " + String.join(", ", near) + "?";
    }
    /** "05" and "5" are the same zone. */
    private static String norm(String s) {
        String t = s.trim().toUpperCase(Locale.ROOT);
        return t.matches("\\d+") ? String.valueOf(Integer.parseInt(t)) : t;
    }
    /** The contest's fixed set of values for its multiplier field, when the field is that field. */
    private static List<String> multiplierValues(ContestPlugin p, String id) {
        var mm = p.getMultiplierModel();
        if (mm == null || mm.getField() == null || !mm.getField().equals(id)) return null;
        return mm.getValidValues();
    }

    // ---- state / province / county (QSO parties): not definitive, so a miss only warns ----
    private static boolean isStateField(ContestPlugin p, String id) {
        if (!id.equals("state_prov_rcvd") && !id.equals("state_rcvd")) return false;
        String t = p.getScoringRules() == null ? null : p.getScoringRules().getMultiplierType();
        return "qso_party".equals(t) || "states".equals(t) || "state_prov_country".equals(t);
    }
    private static Result stateLike(ContestPlugin p, String v, String label, boolean complete) {
        Set<String> known = new HashSet<>(US); known.addAll(CA); known.add("DX");
        List<String> extra = com.ars.fx.surface.contest.TrackerFactory.multiplierUniverse(p);
        if (extra != null) for (String e : extra) known.add(e.toUpperCase(Locale.ROOT));
        if (known.contains(v)) return Result.OK;
        if (!complete) for (String k : known) if (k.startsWith(v)) return Result.partial();
        return Result.soft("'" + v + "' is not a known county, state or province for this contest");
    }

    // ---- formats -------------------------------------------------------------------
    /** While typing, only reject what can never become valid. */
    private static boolean typingOk(String validator, String v) {
        return switch (validator) {
            case "numeric" -> v.matches("\\d*");
            case "ss_check" -> v.matches("\\d{0,2}");
            case "maidenhead", "maidenhead6" -> v.matches("[A-R]{0,2}\\d{0,2}([A-X]{0,2})?");
            default -> true;
        };
    }
    private static String describe(String validator) {
        return switch (validator) {
            case "numeric" -> " (digits only)";
            case "ss_check" -> " (two digits, e.g. 77)";
            case "fd_class" -> " (e.g. 2A)";
            case "maidenhead", "maidenhead6" -> " (grid square, e.g. FN31)";
            default -> "";
        };
    }
}
