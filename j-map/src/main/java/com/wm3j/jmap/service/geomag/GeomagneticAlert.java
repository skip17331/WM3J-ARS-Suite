package com.wm3j.jmap.service.geomag;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Geomagnetic alert data derived from NOAA SWPC.
 * Contains the current Kp index and any active watches/warnings/alerts.
 */
public class GeomagneticAlert {

    public enum Level { NONE, WATCH, WARNING, ALERT }

    private final double kpIndex;          // 0.0–9.0
    private final double aIndex;           // 0–400
    private final Level level;
    private final String summary;          // e.g. "G2 Minor Storm"
    private final List<String> messages;   // NOAA alert text lines
    private final Instant fetchedAt;

    // Latitude below which aurora is visible (degrees from pole)
    // Kp 5 → ~60° lat, Kp 9 → ~40° lat
    private final double auroraVisibleLatitude;

    // Upcoming 24-hour Kp forecast (3-hour intervals, up to 8 values)
    private List<Double> kpForecast = Collections.emptyList();

    public GeomagneticAlert(double kpIndex, double aIndex, Level level,
                             String summary, List<String> messages, Instant fetchedAt) {
        this.kpIndex = kpIndex;
        this.aIndex  = aIndex;
        this.level   = level;
        this.summary = summary;
        this.messages = messages;
        this.fetchedAt = fetchedAt;
        this.auroraVisibleLatitude = kpToVisibleLatitude(kpIndex);
    }

    public List<Double> getKpForecast() { return kpForecast; }
    public void setKpForecast(List<Double> kpForecast) { this.kpForecast = kpForecast; }

    /** Max forecast Kp over the next 24 hours */
    public double getMaxForecastKp() {
        return kpForecast.stream().mapToDouble(Double::doubleValue).max().orElse(kpIndex);
    }

    private static double kpToVisibleLatitude(double kp) {
        // Approximate: Kp 0 → 70°, Kp 9 → 40°
        return Math.max(40.0, 70.0 - kp * 3.3);
    }

    public double getKpIndex()              { return kpIndex; }
    public double getAIndex()               { return aIndex; }
    public Level  getLevel()                { return level; }
    public String getSummary()              { return summary; }
    public List<String> getMessages()       { return messages; }
    public Instant getFetchedAt()           { return fetchedAt; }
    public double getAuroraVisibleLatitude(){ return auroraVisibleLatitude; }

    /** Storm scale label (G0–G5) */
    public String getGScale() {
        if (kpIndex >= 9)  return "G5";
        if (kpIndex >= 8)  return "G4";
        if (kpIndex >= 7)  return "G3";
        if (kpIndex >= 6)  return "G2";
        if (kpIndex >= 5)  return "G1";
        return "G0";
    }
}
