package com.ars.fx.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Country outlines for J-Map's country colours (punch list 5-7): Natural Earth 1:50 m admin-0 countries, public domain, built by
 * {@code tools/basemap/build_countries.py}. Each country carries Natural Earth's {@code MAPCOLOR7}, a number from 1 to 7 chosen so
 * that no two neighbours share one. No JavaFX here, so it can be tested.
 */
public final class CountryShapes {
    private CountryShapes() {}

    /** One polygon: the outline first, then any holes. Fill with the even-odd rule. */
    public record Polygon(VectorBasemap.Ring[] rings, double minLon, double maxLon, double minLat, double maxLat) {}
    /** {@code code} is Natural Earth's ADM0_A3 (USA, DEU, ATA ...); {@code colour} 1-7. */
    public record Country(String code, int colour, Polygon[] polygons) {
        /** Only Antarctica is drawn as ice; Greenland is a country like the others and takes its colour. */
        public boolean ice() { return "ATA".equals(code); }
    }

    private static volatile List<Country> all;

    public static List<Country> all() {
        if (all == null) {
            try (InputStream in = CountryShapes.class.getResourceAsStream("/com/ars/fx/map/countries50.bin.gz")) {
                all = in == null ? List.of() : parse(in);
            } catch (IOException e) {
                all = List.of();
            }
        }
        return all;
    }

    /** Parse the format {@code build_countries.py} writes. */
    public static List<Country> parse(InputStream gz) throws IOException {
        DataInputStream d = new DataInputStream(new BufferedInputStream(new GZIPInputStream(gz), 1 << 16));
        int n = d.readInt();
        Country[] out = new Country[n];
        byte[] code = new byte[3];
        for (int c = 0; c < n; c++) {
            d.readFully(code);
            int colour = d.readByte();
            Polygon[] polys = new Polygon[d.readInt()];
            for (int p = 0; p < polys.length; p++) {
                VectorBasemap.Ring[] rings = new VectorBasemap.Ring[d.readInt()];
                for (int r = 0; r < rings.length; r++) rings[r] = VectorBasemap.readRing(d);
                VectorBasemap.Ring o = rings[0];
                polys[p] = new Polygon(rings, o.minLon(), o.maxLon(), o.minLat(), o.maxLat());
            }
            out[c] = new Country(new String(code, StandardCharsets.US_ASCII), colour, polys);
        }
        return List.of(out);
    }

    /** The country at a point, or null at sea. */
    public static Country at(double lat, double lon) {
        for (Country c : all()) for (Polygon p : c.polygons()) if (contains(p, lon, lat)) return c;
        return null;
    }

    /** Whether the point is inside the polygon (inside the outline and not in a hole), in plain longitude / latitude. */
    public static boolean contains(Polygon p, double lon, double lat) {
        if (lon < p.minLon() || lon > p.maxLon() || lat < p.minLat() || lat > p.maxLat()) return false;
        boolean in = false;
        for (VectorBasemap.Ring r : p.rings()) if (VectorBasemap.contains(r, lon, lat)) in = !in;
        return in;
    }
}
