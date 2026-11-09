package com.ars.fx.data;

/**
 * How long a spot's callsign stays on the map. A busy map crowds fast, so the busier it is the sooner names go: with {@value #FEW} spots or fewer
 * a callsign stays for {@value #CALM_SEC} seconds, with {@value #MANY} or more only {@value #BUSY_SEC}, and in between the lifetime slides smoothly
 * (evenly on a log scale of the spot count, so doubling the traffic shortens it by the same fraction at any level). A name is fully visible for the first
 * half of its lifetime, then fades out; the dot stays. Kept free of JavaFX so it can be tested.
 */
public final class LabelFade {
    private LabelFade() {}

    public static final double CALM_SEC = 480, BUSY_SEC = 75;
    public static final int FEW = 10, MANY = 80;

    /** Test hook only: {@code -Dars.labelFadeScale=0.1} makes every lifetime a tenth as long, so the fade can be watched in a minute. */
    private static final double SCALE = scale();
    private static double scale() {
        try { double s = Double.parseDouble(System.getProperty("ars.labelFadeScale", "1")); return s > 0 ? s : 1; }
        catch (NumberFormatException e) { return 1; }
    }

    /** Seconds a callsign lasts with {@code spotsOnMap} spots showing; never longer than a spot itself stays on the map ({@code spotAgeMaxMinutes}). */
    public static double lifeSec(int spotsOnMap, int spotAgeMaxMinutes) {
        double f = (Math.log(Math.max(spotsOnMap, 1)) - Math.log(FEW)) / (Math.log(MANY) - Math.log(FEW));
        f = Math.max(0, Math.min(1, f));
        double life = CALM_SEC * Math.pow(BUSY_SEC / CALM_SEC, f);
        return Math.min(life, spotAgeMaxMinutes * 60.0) * SCALE;
    }

    /** Opacity of a callsign {@code ageSec} after the spot arrived: 1 for the first half of {@code lifeSec}, then down to 0 at the end of it. */
    public static double alpha(double ageSec, double lifeSec) {
        double hold = lifeSec * 0.5;
        if (ageSec <= hold) return 1.0;
        if (ageSec >= lifeSec) return 0.0;
        return 1.0 - (ageSec - hold) / (lifeSec - hold);
    }
}
