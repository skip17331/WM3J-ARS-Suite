package com.ars.fx.data;

import com.jlog.bandplan.BandplanLoader;

import java.util.Locale;

/**
 * Band lookups for the whole app, from the suite's shared band plan ({@link BandplanLoader} in j-log-common) — the one
 * place band edges live. The log entry, J-Log-Quick, the cluster feeds, the rig panel and the digital modules all ask
 * here, so a frequency has the same band everywhere. The operator's IARU region (J-Hub ▸ Station) selects the plan.
 */
public final class Bands {
    private Bands() {}

    private static BandplanLoader plan() { return BandplanLoader.getInstance(); }

    /** Point the shared plan at the region chosen in J-Hub ▸ Station ("R1" / "R2" / "R3"). Call at start-up and on change. */
    public static void applyRegion() {
        String r = HubConfig.get("station.iaru", "R2").trim().toUpperCase(Locale.ROOT);
        BandplanLoader.setActiveRegion(r.startsWith("IARU") ? r : "IARU-" + r);
    }

    /** "20m" — or "" outside every amateur band. */
    public static String of(long hz) { String b = plan().bandName(hz); return b == null ? "" : b; }

    /** Same, for a frequency typed in MHz ("14.074" or "14,074"); null if it cannot be read or is out of band. */
    public static String ofMhz(String mhz) {
        if (mhz == null) return null;
        try {
            double f = Double.parseDouble(mhz.replace(",", "").trim());
            String b = plan().bandName(Math.round(f * 1_000_000.0));
            return b;
        } catch (Exception e) { return null; }
    }

    /** "20" for "20m", "70" for "70cm" … for panels that label bands by number only; null out of band. */
    public static String number(long hz) {
        String b = of(hz);
        return b.isEmpty() ? null : b.replaceAll("[^0-9.]", "");
    }

    /** The band plan's activity at this frequency ("CW", "DATA", "PHONE"...), or "" when it is not in a band or the plan is silent. */
    public static String activityAt(long hz) {
        BandplanLoader.Description d = plan().describe(hz);
        return d == null || d.activity == null ? "" : d.activity.name();
    }

    /** What the band plan says is at this frequency, e.g. "20m · CW segment" — "" when it is not in a band. */
    public static String describe(long hz) {
        BandplanLoader.Description d = plan().describe(hz);
        if (d == null) return "";
        return d.band + " · " + (d.label == null || d.label.isBlank() ? d.activity.toString() : d.label);
    }
}
