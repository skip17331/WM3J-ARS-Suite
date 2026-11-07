package com.ars.fx.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Country names for the flat map, from Natural Earth (public domain): where to put each name and the zoom levels
 * between which it is shown (Natural Earth's own {@code MIN_LABEL} / {@code MAX_LABEL}, where the whole world at
 * 360 degrees across is about zoom 1). Sorted most important first, so a greedy pass that skips overlaps keeps the big ones.
 */
public final class MapLabels {
    private MapLabels() {}

    public record Label(String name, double lon, double lat, double minZoom, double maxZoom) {}

    private static volatile List<Label> countries;

    public static List<Label> countries() {
        if (countries == null) {
            try (InputStream in = MapLabels.class.getResourceAsStream("/com/ars/fx/map/labels50.tsv.gz")) {
                countries = in == null ? List.of() : parse(in);
            } catch (IOException e) {
                countries = List.of();
            }
        }
        return countries;
    }

    /** Parse the gzip'd tab-separated file: rank, name, lon, lat, min zoom, max zoom. */
    public static List<Label> parse(InputStream gz) throws IOException {
        List<Label> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(gz), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                String[] f = line.split("\t");
                if (f.length < 6) continue;
                out.add(new Label(f[1], Double.parseDouble(f[2]), Double.parseDouble(f[3]), Double.parseDouble(f[4]), Double.parseDouble(f[5])));
            }
        }
        return out;
    }

    /** The zoom level of a map window {@code lonSpan} degrees wide (the whole world is about 1). */
    public static double zoomOf(double lonSpan) { return Math.log(360.0 / lonSpan) / Math.log(2) + 1; }
}
