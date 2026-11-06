package com.ars.fx.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.plugin.ContestPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How the generic contest page is arranged for one contest, in a small optional {@code layout} block of the contest definition,
 * with the operator's own file layered over it ({@code ~/.j-log/contests/<contestId>.json}, so a contest can be tailored without
 * rebuilding). Every part is optional; a contest with none gets the standard page.
 *
 * <ul>
 *   <li>{@code note} - a reminder shown above the entry (the exchange format, a rule that catches people out);</li>
 *   <li>{@code tabs} - titles of the tabs on the right, in the order wanted (tabs not listed follow);</li>
 *   <li>{@code hideTabs} - titles of tabs to leave out;</li>
 *   <li>{@code fieldOrder} - ids of the exchange fields, in the order they should appear (fields not listed follow);</li>
 *   <li>{@code defaultMode} - the mode the contest opens in;</li>
 *   <li>{@code tiles} - extra score tiles to show after the standard ones, by kind (see {@link ContestTiles});</li>
 *   <li>{@code macroSet} - {@code "contest"} for the standard contest set of F1-F8 messages (CQ, exchange, TU, ...), whose
 *       CQ text is {@code cq}; or {@code macros} - a list of {@code {label, text}}, up to eight, for a set of your own.
 *       Voice files stay on the global F-keys (J-Hub ▸ Data ▸ Macros).</li>
 * </ul>
 */
public record ContestLayout(String note, List<String> tabs, List<String> hideTabs, List<String> fieldOrder, String defaultMode,
                            List<String> tiles, String macroSet, String cq, List<MacroConfig.Macro> macros) {

    public static final ContestLayout NONE = new ContestLayout("", List.of(), List.of(), List.of(), "");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static Path dirOverride;                                     // tests
    static void setDirForTest(Path p) { dirOverride = p; }

    /** The layout as it was before tiles and macros: those parts are left empty. */
    public ContestLayout(String note, List<String> tabs, List<String> hideTabs, List<String> fieldOrder, String defaultMode) {
        this(note, tabs, hideTabs, fieldOrder, defaultMode, List.of(), "", "", List.of());
    }

    public static Path userDir() { return dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-log", "contests"); }
    public static Path userFile(String contestId) { return userDir().resolve(contestId + ".json"); }

    // ---- reading -------------------------------------------------------------------------------------
    /** From a free-form map (the definition's {@code layout}); wrong types and unknown keys are ignored. */
    public static ContestLayout of(Map<String, ?> m) {
        if (m == null) return NONE;
        return new ContestLayout(str(m.get("note")), strings(m.get("tabs")), strings(m.get("hideTabs")), strings(m.get("fieldOrder")), str(m.get("defaultMode")),
                strings(m.get("tiles")), str(m.get("macroSet")), str(m.get("cq")), macroList(m.get("macros")));
    }
    private static String str(Object o) { return o instanceof String s ? s.trim() : ""; }
    private static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) if (x instanceof String s && !s.isBlank()) out.add(s.trim());
        return out;
    }
    private static List<MacroConfig.Macro> macroList(Object o) {
        List<MacroConfig.Macro> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) {
            if (out.size() >= MacroConfig.count()) break;
            if (x instanceof Map<?, ?> mm && mm.get("text") instanceof String t && !t.isBlank())
                out.add(new MacroConfig.Macro(mm.get("label") instanceof String lb ? lb.trim() : "", t.trim(), ""));
        }
        return out;
    }

    /** This layout with every part that {@code over} sets replacing the one here. */
    public ContestLayout over(ContestLayout o) {
        return new ContestLayout(o.note.isEmpty() ? note : o.note, o.tabs.isEmpty() ? tabs : o.tabs,
                o.hideTabs.isEmpty() ? hideTabs : o.hideTabs, o.fieldOrder.isEmpty() ? fieldOrder : o.fieldOrder,
                o.defaultMode.isEmpty() ? defaultMode : o.defaultMode,
                o.tiles.isEmpty() ? tiles : o.tiles,
                o.macros.isEmpty() && o.macroSet.isEmpty() ? macroSet : o.macroSet,
                o.cq.isEmpty() ? cq : o.cq,
                o.macros.isEmpty() ? macros : o.macros);
    }

    /** The definition's own layout, with the operator's file over it. A missing or broken file changes nothing. */
    public static ContestLayout resolve(ContestPlugin plugin) {
        if (plugin == null) return NONE;
        ContestLayout built = of(plugin.getLayout());
        Path f = userFile(plugin.getContestId());
        if (!Files.isRegularFile(f)) return built;
        try {
            JsonNode root = JSON.readTree(Files.readString(f, StandardCharsets.UTF_8));
            JsonNode node = root.has("layout") ? root.get("layout") : root;
            @SuppressWarnings("unchecked") Map<String, Object> m = JSON.convertValue(node, Map.class);
            return built.over(of(m));
        } catch (Exception e) {
            return built;
        }
    }

    // ---- applying ------------------------------------------------------------------------------------
    /** Titles in the wanted order, without the hidden ones; unlisted titles keep their place after the listed ones. */
    public List<String> orderTabs(List<String> titles) { return order(titles, tabs, hideTabs); }
    /** Ids in the wanted order; unlisted ids keep their place after the listed ones. */
    public List<String> orderFields(List<String> ids) { return order(ids, fieldOrder, List.of()); }

    /** The F-key messages for this contest, or an empty list when it uses the global ones: the operator's own list, else the standard contest set. */
    public List<MacroConfig.Macro> contestMacros() {
        if (!macros.isEmpty()) return macros;
        if ("contest".equalsIgnoreCase(macroSet)) return ContestMacros.standard(cq);
        return List.of();
    }

    private static List<String> order(List<String> items, List<String> wanted, List<String> hidden) {
        List<String> rest = new ArrayList<>();
        for (String it : items) if (!containsIgnoreCase(hidden, it)) rest.add(it);
        List<String> out = new ArrayList<>();
        for (String w : wanted) {
            for (String it : new ArrayList<>(rest)) if (it.equalsIgnoreCase(w)) { out.add(it); rest.remove(it); break; }
        }
        out.addAll(rest);
        return out;
    }
    private static boolean containsIgnoreCase(List<String> l, String s) { for (String x : l) if (x.equalsIgnoreCase(s)) return true; return false; }

    // ---- the operator's file --------------------------------------------------------------------------
    /** Create the operator's file for a contest with the current layout filled in, if there is none yet. Returns the path. */
    public static Path ensureUserFile(ContestPlugin plugin, List<String> tabTitles, List<String> fieldIds) throws IOException {
        Path f = userFile(plugin.getContestId());
        if (Files.exists(f)) return f;
        Files.createDirectories(f.getParent());
        ContestLayout cur = resolve(plugin);
        ObjectNode root = JSON.createObjectNode();
        root.put("_help", "Layout of the contest page for " + plugin.getContestName() + ". Delete a line to go back to the built-in setting. "
                + "note = a reminder shown above the entry; tabs = order of the tabs on the right; hideTabs = tabs to leave out; "
                + "fieldOrder = order of the exchange fields (field ids); defaultMode = the mode the contest opens in (SSB, CW...); "
                + "tiles = extra score tiles (" + String.join(", ", ContestTiles.KINDS) + "); "
                + "macros = your own F1-F8 messages as {\"label\":..., \"text\":...} (text may use {CALL}, {MYCALL}, {EXCH} = what you send, {NR} = your serial number). "
                + "Save the file and reopen the contest page.");
        ObjectNode lay = root.putObject("layout");
        lay.put("note", cur.note);
        ArrayNode t = lay.putArray("tabs"); for (String s : cur.orderTabs(tabTitles)) t.add(s);
        lay.putArray("hideTabs");
        ArrayNode fo = lay.putArray("fieldOrder"); for (String s : cur.orderFields(fieldIds)) fo.add(s);
        lay.put("defaultMode", cur.defaultMode);
        ArrayNode ti = lay.putArray("tiles"); for (String s : cur.tiles) ti.add(s);
        ArrayNode ma = lay.putArray("macros");
        for (MacroConfig.Macro m : cur.contestMacros()) { ObjectNode o = ma.addObject(); o.put("label", m.label()); o.put("text", m.text()); }
        Files.writeString(f, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root), StandardCharsets.UTF_8);
        return f;
    }

    public String modeOrNull() { return defaultMode == null || defaultMode.isBlank() ? null : defaultMode.trim().toUpperCase(Locale.ROOT); }
}
