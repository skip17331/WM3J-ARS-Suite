package com.ars.fx.data;


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

    /** A module: its id, display name, colour key and what it is. */
    public record Mod(String id, String name, String hue, String sub) {}

    /** Every module, in dock order. */
    public static final List<Mod> MODULES = List.of(
        new Mod("log",    "J-Log",    "log",    "Logging & contests"),
        new Mod("map",    "J-Map",    "map",    "Propagation & DX spots"),
        new Mod("bridge", "J-Bridge", "bridge", "WSJT-X bridge"),
        new Mod("digi",   "J-Digi",   "digi",   "Digital modes"),
        new Mod("sat",    "J-Sat",    "sat",    "Satellite tracking"),
        new Mod("vault",  "J-Vault",  "vault",  "Inventory"),
        new Mod("learn",  "J-Learn",  "learn",  "Reference")
    );

    public static boolean enabled(String id) { return HubConfig.getBool("module." + id + ".enabled", true); }
    public static void setEnabled(String id, boolean on) { HubConfig.setBool("module." + id + ".enabled", on); }

    /** Every module (for the management UI), in dock order. */
    public static List<Mod> all() { return MODULES; }

    /** Only the modules the operator has turned on (for the dock / nav / dashboard). */
    public static List<Mod> enabledModules() {
        List<Mod> out = new ArrayList<>();
        for (Mod m : MODULES) if (enabled(m.id())) out.add(m);
        return out;
    }

    public static int enabledCount() {
        int n = 0;
        for (Mod m : MODULES) if (enabled(m.id())) n++;
        return n;
    }

    private static final java.util.Set<String> OPENED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Note that a module's screen has been opened in this session (modules with no background engine count as running once opened). */
    public static void markOpened(String id) { if (id != null) OPENED.add(id); }

    /** Whether the module is doing something right now, for the dock dots: J-Log is always up; J-Map when its cluster feed
     *  is connected; J-Digi once audio capture has started; J-Bridge while its WSJT-X listener is up; J-Sat once its tracker
     *  has started; J-Vault and J-Learn (no background engine) once opened this session. */
    public static boolean running(String id) {
        if (id == null) return false;
        try {
            return switch (id) {
                case "log"    -> true;
                case "map"    -> ClusterClient.getInstance().isConnected();
                case "digi"   -> DigiEngine.running();
                case "bridge" -> BridgeService.running();
                case "sat"    -> SatService.getInstance().isRunning();
                default       -> OPENED.contains(id);
            };
        } catch (RuntimeException e) { return false; }
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
