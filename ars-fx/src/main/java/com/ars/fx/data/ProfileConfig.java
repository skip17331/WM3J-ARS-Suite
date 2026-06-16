package com.ars.fx.data;

import java.util.List;
import java.util.Map;

/**
 * Operating profiles — named bundles of station settings the operator switches
 * between (Contest Day / Casual DX / Digital / Satellite Pass). Activating a
 * profile writes its settings into {@link HubConfig} (so every consumer picks
 * them up) and pushes the wired ones (rig mode / band) to the live rig. The
 * active profile is remembered in {@code profile.active}.
 */
public final class ProfileConfig {
    private ProfileConfig() {}

    public record Profile(String name, String color, String rigMode, String rigBand, Map<String, String> config) {}

    private static final List<Profile> PROFILES = List.of(
            new Profile("Contest Day", "accent", "CW", "20",
                    Map.of("data.clusterMode", "CW", "rig.autoBandFollow", "true")),
            new Profile("Casual DX", "map", "USB", "20",
                    Map.of("data.clusterMode", "All", "rig.autoBandFollow", "false")),
            new Profile("Digital", "digi", "USB", "20",
                    Map.of("data.clusterMode", "Digi", "rig.autoBandFollow", "true")),
            new Profile("Satellite Pass", "sat", "USB", null,   // VHF/UHF — freq comes from J-Sat Doppler
                    Map.of("rotor.autoTrack", "true")));

    public static List<Profile> profiles() { return PROFILES; }
    public static String active() { return HubConfig.get("profile.active", ""); }
    public static Profile byName(String name) { for (Profile p : PROFILES) if (p.name().equals(name)) return p; return null; }

    /** Apply a profile: persist its settings to HubConfig + push rig mode/band to the live rig. */
    public static void activate(String name) {
        Profile p = byName(name);
        if (p == null) return;
        for (Map.Entry<String, String> e : p.config().entrySet()) HubConfig.set(e.getKey(), e.getValue());
        HubConfig.set("profile.active", name);
        if (p.rigMode() != null) RigClient.getInstance().setMode(p.rigMode());
        if (p.rigBand() != null) RigClient.getInstance().setBand(p.rigBand());
    }
}
