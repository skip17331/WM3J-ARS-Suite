package com.ars.fx.mock;

import java.util.List;

/** Mock data for the UI-first build. */
public final class Mock {
    private Mock() {}

    public record Mod(String id, String name, String hue, String sub, boolean running) {}

    /** Module list for the dock + J-Hub OPERATE group (order per the screenshots). */
    public static final List<Mod> MODULES = List.of(
        new Mod("log",    "J-Log",    "log",    "running",   true),
        new Mod("map",    "J-Map",    "map",    "running",   true),
        new Mod("bridge", "J-Bridge", "bridge", "running",   true),
        new Mod("digi",   "J-Digi",   "digi",   "Digital",   false),
        new Mod("sat",    "J-Sat",    "sat",    "Satellite", false),
        new Mod("vault",  "J-Vault",  "vault",  "Inventory", false),
        new Mod("learn",  "J-Learn",  "learn",  "Reference", false)
    );

    /** Standard solar tiles (key, value) for the Space-weather drawer. */
    public static final String[][] SOLAR = {
        {"SFI","168"},{"SN","142"},{"A","7"},{"K","2"},{"X-ray","B1"},{"304Å","171"},{"Aur","1.5"},{"MUF","28"}
    };
    /** Band conditions (band, day, night). */
    public static final String[][] BANDCOND = {
        {"80–40m","good","good"},{"30–20m","good","fair"},{"17–15m","fair","poor"},{"12–10m","fair","poor"}
    };
    public static final Object[][] ROTOR_PRESETS = {
        {"EU",50},{"AF",95},{"SA",155},{"Carib",135},{"AS",330},{"OC",250}
    };
}
