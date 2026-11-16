package com.ars.fx.data;

import java.util.Locale;

/**
 * Frequencies on contest QSOs. Cabrillo wants kilohertz ("14025"), so a contest QSO stores its frequency in kHz; the normal log stores MHz.
 * Before this the contest page stored none, and the Cabrillo export wrote 14000 on every line.
 */
public final class ContestFreq {
    private ContestFreq() {}

    /**
     * The dial frequency in kHz for a QSO on {@code band}, or null when it should not be stored: the rig is not connected, gave no
     * reading, or is on a different band from the one on the form (the operator typed the band, so they win).
     */
    public static String khz(long hz, boolean connected, String band) {
        if (!connected || hz <= 0) return null;
        String rigBand = JLogDb.bandFor(String.format(Locale.US, "%.3f", hz / 1e6));
        if (rigBand == null || !rigBand.equalsIgnoreCase(normBand(band))) return null;
        return String.valueOf(Math.round(hz / 1000.0));
    }

    /** {@link #khz} for the rig connected now. */
    public static String rigKhz(String band) {
        RigClient.State rs = RigClient.getInstance().last();
        return rs == null ? null : khz(rs.freqHz(), rs.connected(), band);
    }

    /** A stored contest frequency as MHz for a label: "14025" becomes "14.025"; a value already in MHz ("14.074") is left alone; blank stays blank. */
    public static String toMhz(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        try {
            double v = Double.parseDouble(stored.trim());
            return v >= 1000 ? String.format(Locale.US, "%.3f", v / 1000.0) : stored.trim();
        } catch (NumberFormatException e) { return stored.trim(); }
    }

    /** "20" and "20m" are both 20 metres. */
    private static String normBand(String b) {
        if (b == null) return "";
        b = b.trim().toLowerCase(Locale.ROOT);
        return b.matches("\\d+") ? b + "m" : b;
    }
}
