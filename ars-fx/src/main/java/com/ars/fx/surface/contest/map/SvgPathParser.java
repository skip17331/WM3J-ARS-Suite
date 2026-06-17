package com.ars.fx.surface.contest.map;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal parser for the absolute {@code M x,y L x,y … Z} subpaths used by the
 * contest map JSON. Returns one flat {@code [x0,y0,x1,y1,…]} polygon per subpath
 * (a region may have several — islands/insets).
 */
public final class SvgPathParser {
    private SvgPathParser() {}

    private static final Pattern TOKEN = Pattern.compile("([MLZ])|(-?[\\d.]+)[ ,]+(-?[\\d.]+)");

    public static List<double[]> parse(String d) {
        List<double[]> polys = new ArrayList<>();
        List<Double> cur = new ArrayList<>();
        Matcher m = TOKEN.matcher(d == null ? "" : d);
        while (m.find()) {
            if (m.group(1) != null) {
                char c = m.group(1).charAt(0);
                if ((c == 'M' || c == 'Z') && !cur.isEmpty()) { polys.add(toArr(cur)); cur = new ArrayList<>(); }
            } else {
                cur.add(Double.parseDouble(m.group(2)));
                cur.add(Double.parseDouble(m.group(3)));
            }
        }
        if (!cur.isEmpty()) polys.add(toArr(cur));
        return polys;
    }

    private static double[] toArr(List<Double> l) {
        double[] a = new double[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    /** Even-odd point-in-polygon test on a flat [x0,y0,…] ring. */
    public static boolean contains(double[] poly, double px, double py) {
        boolean in = false;
        int n = poly.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly[2 * i], yi = poly[2 * i + 1], xj = poly[2 * j], yj = poly[2 * j + 1];
            if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) in = !in;
        }
        return in;
    }
}
