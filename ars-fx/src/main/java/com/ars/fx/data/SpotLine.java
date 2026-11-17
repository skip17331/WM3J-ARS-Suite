package com.ars.fx.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One "DX de" line from a DX cluster or the Reverse Beacon Network, as the cluster and RBN clients both read it:
 * {@code DX de <spotter>: <kHz> <call> [<comment>] <hhmm>Z [<spotter's grid / state / section>]}.
 *
 * <p>Cluster software varies: DXSpider can ring the bell (\u0007) at the end of a spot, CC Cluster and AR-Cluster can add the
 * spotter's locator or state after the time, and a bare spot has no comment at all. The earlier pattern wanted the time to end
 * the line and a comment to be there, so those spots were silently dropped. The trailing token must start with a letter, so
 * a number at the end of a comment is not mistaken for it.
 */
public final class SpotLine {
    private SpotLine() {}

    public record Parsed(String spotter, long freqHz, String call, String comment) {}

    private static final Pattern P = Pattern.compile(
            "DX de\\s+([A-Z0-9/#\\-]+)[:\\s]+([\\d.]+)\\s+([A-Z0-9/\\-]+)(?:\\s+(.*?))?\\s+(\\d{4})Z?(?:\\s+[A-Z][A-Z0-9]{0,5})?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** The spot on this line, or null when it is not a spot line or the frequency is unreadable. */
    public static Parsed parse(String line) {
        if (line == null) return null;
        String l = line.replaceAll("[\\p{Cntrl}&&[^\\t]]", "");          // the bell and other control characters some clusters send
        Matcher m = P.matcher(l);
        if (!m.find()) return null;
        long hz;
        try { hz = Math.round(Double.parseDouble(m.group(2)) * 1000); } catch (NumberFormatException e) { return null; }
        if (hz <= 0) return null;
        String comment = m.group(4) == null ? "" : m.group(4).trim();
        return new Parsed(m.group(1).toUpperCase(java.util.Locale.ROOT), hz, m.group(3).toUpperCase(java.util.Locale.ROOT), comment);
    }
}
