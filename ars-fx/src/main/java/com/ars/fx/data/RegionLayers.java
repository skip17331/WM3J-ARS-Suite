package com.ars.fx.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A region layer for J-Map, drawn like the country colours: the states and provinces of the US and Canada. Built by
 * {@code tools/basemap/build_regions.py}; the file is in the format {@link CountryShapes} reads, so {@code code} is the
 * postal code (WA, ON ...).
 * No JavaFX here, so it can be tested.
 */
public final class RegionLayers {
    private RegionLayers() {}

    /** A region with the point its name is drawn at. */
    public record Region(CountryShapes.Country shape, double labelLon, double labelLat, double size) {
        public String code() { return shape.code().trim(); }
    }

    private static volatile List<Region> states;

    public static List<Region> states() {
        if (states == null) states = load("/com/ars/fx/map/na-states.bin.gz");
        return states;
    }

    static List<Region> load(String res) {
        try (InputStream in = RegionLayers.class.getResourceAsStream(res)) {
            if (in == null) return List.of();
            return CountryShapes.parse(in).stream().map(RegionLayers::region).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** The region at a point, or null. */
    public static Region at(List<Region> layer, double lat, double lon) {
        for (Region r : layer) for (CountryShapes.Polygon p : r.shape().polygons()) if (CountryShapes.contains(p, lon, lat)) return r;
        return null;
    }

    /** The label goes in the largest polygon: at the middle of its box if that is inside it, otherwise at the inside point nearest the middle. */
    static Region region(CountryShapes.Country c) {
        CountryShapes.Polygon big = c.polygons()[0];
        double bigArea = -1;
        for (CountryShapes.Polygon p : c.polygons()) {
            double a = (p.maxLon() - p.minLon()) * (p.maxLat() - p.minLat());
            if (a > bigArea) { bigArea = a; big = p; }
        }
        double cx = (big.minLon() + big.maxLon()) / 2, cy = (big.minLat() + big.maxLat()) / 2;
        double bx = cx, by = cy;
        if (!CountryShapes.contains(big, cx, cy)) {
            double best = Double.MAX_VALUE;
            int n = 24;
            for (int i = 0; i <= n; i++) for (int j = 0; j <= n; j++) {
                double x = big.minLon() + (big.maxLon() - big.minLon()) * i / n, y = big.minLat() + (big.maxLat() - big.minLat()) * j / n;
                double d = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                if (d < best && CountryShapes.contains(big, x, y)) { best = d; bx = x; by = y; }
            }
        }
        return new Region(c, bx, by, bigArea);
    }
}
