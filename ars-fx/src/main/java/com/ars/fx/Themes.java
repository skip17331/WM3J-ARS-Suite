package com.ars.fx;

import com.ars.fx.data.HubConfig;
import javafx.scene.Node;

import java.util.List;
import java.util.Set;

/**
 * Colour-scheme registry + scoping for the theme button.
 *
 * <p>Schemes are token-only CSS classes on the scene root (Dark is the base, no
 * class). The button cycles through them. Scope rules:
 * <ul>
 *   <li><b>Dashboard</b> ({@link #cycleGlobal}) sets the suite-wide scheme and
 *       clears every per-module override — a J-Hub change is for the whole suite.</li>
 *   <li><b>A module</b> ({@link #cycleModule}) sets only that module's scheme,
 *       remembered until it — or the dashboard — changes it.</li>
 * </ul>
 * Persisted in {@link HubConfig}: {@code theme.scheme} (global) and
 * {@code theme.module.<id>} (per-module overrides).
 */
public final class Themes {
    private Themes() {}

    public record Scheme(String key, String label, String cssClass) {}

    /** Cycle order. Dark = base palette (empty class); the rest add a token-override class. */
    public static final List<Scheme> SCHEMES = List.of(
            new Scheme("dark",   "Dark",   ""),
            new Scheme("light",  "Light",  "ars-light"),
            new Scheme("ocean",  "Ocean",  "ars-ocean"),
            new Scheme("forest", "Forest", "ars-forest"),
            new Scheme("amber",  "Amber",  "ars-amber"),
            new Scheme("rose",   "Rose",   "ars-rose"),
            new Scheme("nord",   "Nord",   "ars-nord"));

    /** Surfaces that keep their own scheme. J-Hub pages (dashboard/config) always follow the global. */
    private static final Set<String> MODULES = Set.of("log", "map", "sat", "digi", "bridge", "vault", "learn");

    public static Scheme byKey(String key) {
        for (Scheme s : SCHEMES) if (s.key().equals(key)) return s;
        return SCHEMES.get(0);
    }
    public static String global() { return HubConfig.get("theme.scheme", "dark"); }
    public static String moduleOverride(String id) { return HubConfig.get("theme.module." + id, ""); }
    public static boolean isModule(String surfaceId) { return surfaceId != null && MODULES.contains(surfaceId); }

    /** Effective scheme key for a surface: a module's own override if set, otherwise the global. */
    public static String effectiveKey(String surfaceId) {
        if (isModule(surfaceId)) {
            String ov = moduleOverride(surfaceId);
            if (ov != null && !ov.isBlank()) return ov;
        }
        return global();
    }
    public static Scheme effective(String surfaceId) { return byKey(effectiveKey(surfaceId)); }

    /** Apply the effective scheme for {@code surfaceId} to the root (swaps the single scheme class live). */
    public static void applyTo(Node root, String surfaceId) {
        if (root == null) return;
        for (Scheme s : SCHEMES) if (!s.cssClass().isEmpty()) root.getStyleClass().remove(s.cssClass());
        String cls = effective(surfaceId).cssClass();
        if (!cls.isEmpty() && !root.getStyleClass().contains(cls)) root.getStyleClass().add(cls);
    }

    private static String nextKey(String cur) {
        int i = 0;
        for (int k = 0; k < SCHEMES.size(); k++) if (SCHEMES.get(k).key().equals(cur)) { i = k; break; }
        return SCHEMES.get((i + 1) % SCHEMES.size()).key();
    }

    /** Module theme button: advance THIS module's own scheme (persisted as an override). Returns the new scheme. */
    public static Scheme cycleModule(String surfaceId) {
        if (!isModule(surfaceId)) return cycleGlobal();   // safety: non-module surfaces drive the global
        String next = nextKey(effectiveKey(surfaceId));
        HubConfig.set("theme.module." + surfaceId, next);
        return byKey(next);
    }

    /** Dashboard theme button: advance the suite-wide scheme + clear all per-module overrides. */
    public static Scheme cycleGlobal() {
        String next = nextKey(global());
        HubConfig.set("theme.scheme", next);
        for (String m : MODULES) HubConfig.set("theme.module." + m, "");   // a dashboard change overrides modules
        return byKey(next);
    }
}
