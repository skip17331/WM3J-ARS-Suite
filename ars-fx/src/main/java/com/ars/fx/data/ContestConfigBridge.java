package com.ars.fx.data;

import com.jlog.db.DatabaseManager;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;

/**
 * Boots the (pure, no-FXML) j-log-engine for contest mode and bridges ars-fx's
 * station config into it.
 *
 * <p>The engine keeps station identity + Cabrillo categories + Sweepstakes
 * constants in its own {@code ~/.j-log/config.db} (read by {@code AppConfig} /
 * {@code CabrilloExporter}). ars-fx keeps the same facts in {@link HubConfig}
 * ({@code ~/.j-hub}). {@link #syncStationToEngine()} copies HubConfig → the
 * engine's config keys so the engine exporter/scoring see the operator.
 */
public final class ContestConfigBridge {
    private ContestConfigBridge() {}

    private static volatile boolean booted = false;

    /** Open the engine DBs (~/.j-log) + load the bundled contest plugins. Idempotent. */
    public static synchronized void bootstrap() {
        if (booted) return;
        try {
            Class.forName("org.sqlite.JDBC");
            DatabaseManager.getInstance().initAll();   // opens/creates contest.db + config.db (WAL); must precede the rest
            AppConfig.getInstance().load();
            PluginLoader.getInstance().init();          // needs DatabaseManager.getDataDir() for user plugins
            booted = true;
            syncStationToEngine();
        } catch (Throwable e) {
            System.err.println("[contest] engine bootstrap failed: " + e.getMessage());
        }
    }

    public static boolean ready() { return booted; }

    /** Copy ars-fx HubConfig → engine config.db (station identity + Cabrillo categories + SS constants). */
    public static void syncStationToEngine() {
        if (!booted) return;
        DatabaseManager db = DatabaseManager.getInstance();
        put(db, "station.callsign", HubConfig.call());
        put(db, "station.grid",     HubConfig.grid());
        put(db, "station.operator", HubConfig.get("station.name", ""));
        put(db, "station.qth",      HubConfig.get("station.qth", ""));
        // Cabrillo header categories
        put(db, "cab.operator",    HubConfig.get("contest.cab.operator",    "SINGLE-OP"));
        put(db, "cab.band",        HubConfig.get("contest.cab.band",        "ALL"));
        put(db, "cab.mode",        HubConfig.get("contest.cab.mode",        "MIXED"));
        put(db, "cab.power",       HubConfig.get("contest.cab.power",       "HIGH"));
        put(db, "cab.assisted",    HubConfig.get("contest.cab.assisted",    "NON-ASSISTED"));
        put(db, "cab.transmitter", HubConfig.get("contest.cab.transmitter", "ONE"));
        put(db, "cab.station",     HubConfig.get("contest.cab.station",     "FIXED"));
        put(db, "cab.email",       HubConfig.get("contest.cab.email",       ""));
        put(db, "cab.club",        HubConfig.get("contest.cab.club",        ""));
        // ARRL Sweepstakes operator constants (precedence / check / section)
        put(db, "ss.precedence",   HubConfig.get("contest.ss.precedence", ""));
        put(db, "ss.check",        HubConfig.get("contest.ss.check", ""));
        put(db, "ss.section",      HubConfig.get("contest.ss.section", ""));
    }

    private static void put(DatabaseManager db, String key, String val) {
        try { db.setConfig(key, val == null ? "" : val); } catch (Exception ignored) {}
    }
}
