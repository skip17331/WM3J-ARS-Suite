package com.ars.fx.data;

import com.jlog.util.Maidenhead;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Ham-to-ham tracks on J-Map: the great-circle path between two stations that are not this one - who heard whom, or where a path you are
 * curious about runs. Each end is a callsign, a prefix, or a Maidenhead grid. A short list is kept in memory and drawn on the map; adding a
 * track between the same two places again replaces it, and the oldest goes when there are more than {@link #MAX}.
 */
public final class MapTracks {
    private MapTracks() {}

    public static final int MAX = 8;
    private static final Pattern GRID = Pattern.compile("[A-Ra-r]{2}[0-9]{2}([A-Xa-x]{2})?");
    private static final Pattern SHAPE = Pattern.compile("[A-Za-z0-9/]{1,14}(-[A-Za-z0-9#]{1,4})?");
    private static final List<Track> TRACKS = new CopyOnWriteArrayList<>();

    /** One path: where each end was named as, and where it is. */
    public record Track(String from, String to, double lat1, double lon1, double lat2, double lon2) {
        public double km() { return Propagation.distanceKm(new Propagation.Point(lat1, lon1), new Propagation.Point(lat2, lon2)); }
        /** The compass direction to head from the first end toward the second, 0 to 360. */
        public double bearing() { return MapTracks.bearing(lat1, lon1, lat2, lon2); }
        /** "W1AW → JA1ABC · 10,850 km · 003°". */
        public String label() { return from + " → " + to + " · " + Units.distText(km()) + " · " + String.format(Locale.ROOT, "%03d°", Math.round(bearing()) % 360); }
        /** The short form for the middle of the line on the map. */
        public String shortLabel() { return Units.distText(km()) + " · " + String.format(Locale.ROOT, "%03d°", Math.round(bearing()) % 360); }
    }

    /** {lat, lon} for a grid square, a callsign or a prefix, or null when it cannot be placed. */
    public static double[] resolve(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.length() > 20) return null;
        if (GRID.matcher(t).matches()) return Maidenhead.center(t);
        if (!SHAPE.matcher(t).matches()) return null;          // the lookup is generous with anything that starts like a prefix, so only a call or prefix is passed to it
        return Geo.of(t);
    }

    /** The track between two places, or null when either cannot be placed or both are the same spot. */
    public static Track make(String from, String to) {
        double[] a = resolve(from), b = resolve(to);
        if (a == null || b == null) return null;
        if (Math.abs(a[0] - b[0]) < 0.01 && Math.abs(a[1] - b[1]) < 0.01) return null;
        return new Track(from.trim().toUpperCase(Locale.ROOT), to.trim().toUpperCase(Locale.ROOT), a[0], a[1], b[0], b[1]);
    }

    /** Adds a track to the map's list and returns it, or null if it could not be made. */
    public static Track add(String from, String to) {
        Track t = make(from, to);
        if (t == null) return null;
        TRACKS.removeIf(x -> x.from().equals(t.from()) && x.to().equals(t.to()));
        TRACKS.add(t);
        while (TRACKS.size() > MAX) TRACKS.remove(0);
        return t;
    }

    public static List<Track> all() { return new ArrayList<>(TRACKS); }
    public static void clear() { TRACKS.clear(); }

    /** Initial great-circle bearing from the first point to the second, degrees clockwise from north. */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
