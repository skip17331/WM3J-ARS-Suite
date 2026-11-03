package com.ars.fx;

import java.io.InputStream;
import java.util.Properties;

/**
 * The running build's version (stamped into {@code version.properties} by Maven) and
 * a tolerant comparison used by the update check. Versions look like {@code 1.11.0};
 * a leading {@code v} (git tag form) and a trailing {@code -suffix} are ignored.
 */
public final class AppVersion {
    private AppVersion() {}

    /** Shown when the stamp is missing or unfiltered (e.g. running from an IDE that skipped resource filtering). */
    public static final String UNKNOWN = "0.0.0";

    private static final String CURRENT = load();

    /** This build's version, e.g. {@code "1.11.0"}; {@link #UNKNOWN} if it can't be determined. */
    public static String current() { return CURRENT; }

    private static String load() {
        try (InputStream in = AppVersion.class.getResourceAsStream("/com/ars/fx/version.properties")) {
            if (in == null) return UNKNOWN;
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version", UNKNOWN).trim();
            return v.isEmpty() || v.startsWith("$") ? UNKNOWN : v;
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    /** True when {@code candidate} is a strictly newer version than {@code installed}. Unparseable input is never "newer". */
    public static boolean isNewer(String candidate, String installed) {
        int[] c = parse(candidate), i = parse(installed);
        if (c == null || i == null) return false;
        for (int k = 0; k < Math.max(c.length, i.length); k++) {
            int a = k < c.length ? c[k] : 0, b = k < i.length ? i[k] : 0;
            if (a != b) return a > b;
        }
        return false;
    }

    /** {@code "v1.11.0-rc1"} → {@code [1,11,0]}; null if it doesn't start with a number. */
    static int[] parse(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int dash = s.indexOf('-');
        if (dash >= 0) s = s.substring(0, dash);
        if (s.isEmpty()) return null;
        String[] parts = s.split("\\.");
        int[] out = new int[parts.length];
        try {
            for (int k = 0; k < parts.length; k++) out[k] = Integer.parseInt(parts[k]);
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }
}
