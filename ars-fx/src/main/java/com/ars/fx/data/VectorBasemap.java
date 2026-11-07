package com.ars.fx.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Vector world basemap from Natural Earth (public domain), 1:50 m: land polygons and country border lines.
 * Stored as gzip'd big-endian ints and shorts: ring count, then per ring a point count and (lon, lat) in
 * hundredths of a degree. No JavaFX here, so it can be tested. A ring is {@code {lon, lat, lon, lat, ...}}
 * in degrees, plus its bounding box for culling.
 */
public final class VectorBasemap {
    public record Ring(double[] ll, double minLon, double maxLon, double minLat, double maxLat) {
        public int points() { return ll.length / 2; }
    }

    private static volatile VectorBasemap land, borders;

    private final Ring[] rings;
    private VectorBasemap(Ring[] r) { rings = r; }
    public Ring[] rings() { return rings; }

    /** Land polygons (outer rings and lake / hole rings; fill with the even-odd rule). */
    public static VectorBasemap land() {
        if (land == null) land = load("/com/ars/fx/map/land50.bin.gz");
        return land;
    }
    /** Country border lines (land borders only). */
    public static VectorBasemap borders() {
        if (borders == null) borders = load("/com/ars/fx/map/borders50.bin.gz");
        return borders;
    }

    private static VectorBasemap load(String res) {
        try (InputStream in = VectorBasemap.class.getResourceAsStream(res)) {
            if (in == null) return new VectorBasemap(new Ring[0]);
            return parse(in);
        } catch (IOException e) {
            return new VectorBasemap(new Ring[0]);
        }
    }

    /** Parse the gzip'd format described above. */
    public static VectorBasemap parse(InputStream gz) throws IOException {
        DataInputStream d = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(gz), 1 << 16));
        int n = d.readInt();
        Ring[] out = new Ring[n];
        for (int i = 0; i < n; i++) {
            int m = d.readInt();
            double[] ll = new double[m * 2];
            double x0 = 1e9, x1 = -1e9, y0 = 1e9, y1 = -1e9;
            for (int j = 0; j < m; j++) {
                double lo = d.readShort() / 100.0, la = d.readShort() / 100.0;
                ll[j * 2] = lo; ll[j * 2 + 1] = la;
                if (lo < x0) x0 = lo; if (lo > x1) x1 = lo; if (la < y0) y0 = la; if (la > y1) y1 = la;
            }
            out[i] = new Ring(ll, x0, x1, y0, y1);
        }
        return new VectorBasemap(out);
    }
}
