package com.ars.fx.data;

import com.ars.fx.mock.Mock;

import java.util.ArrayList;
import java.util.List;

/**
 * Which modules are turned on. The operator toggles them in J-Hub ▸ Modules;
 * the dock, the J-Hub nav and the dashboard count then show only enabled
 * modules. Persisted as {@code module.<id>.enabled} in {@link HubConfig};
 * default on, so a fresh install shows everything.
 */
public final class ModuleConfig {
    private ModuleConfig() {}

    public static boolean enabled(String id) { return HubConfig.getBool("module." + id + ".enabled", true); }
    public static void setEnabled(String id, boolean on) { HubConfig.setBool("module." + id + ".enabled", on); }

    /** Every module (for the management UI), in dock order. */
    public static List<Mock.Mod> all() { return Mock.MODULES; }

    /** Only the modules the operator has turned on (for the dock / nav / dashboard). */
    public static List<Mock.Mod> enabledModules() {
        List<Mock.Mod> out = new ArrayList<>();
        for (Mock.Mod m : Mock.MODULES) if (enabled(m.id())) out.add(m);
        return out;
    }

    public static int enabledCount() {
        int n = 0;
        for (Mock.Mod m : Mock.MODULES) if (enabled(m.id())) n++;
        return n;
    }

    /** One-line description shown next to each module's on/off toggle. */
    public static String blurb(String id) {
        return switch (id) {
            case "log"    -> "Logging & contests";
            case "map"    -> "Propagation & DX spots";
            case "sat"    -> "Satellite tracking";
            case "digi"   -> "Digital modes (FT8 / RTTY / PSK)";
            case "bridge" -> "WSJT-X bridge";
            case "vault"  -> "Station inventory & estate";
            case "learn"  -> "Reference & study";
            default        -> "";
        };
    }
}
