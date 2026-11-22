package com.ars.fx.surface;

import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import static com.ars.fx.shell.Shell.lbl;
import static com.ars.fx.surface.JMapView.*;
import static com.ars.fx.surface.JMapControls.*;

/** J-Map's three map views (azimuthal, 3D globe, flat map): the canvases and everything painted on them. Split out of {@link JMapView}. */
final class JMapCanvases {
    private JMapCanvases() {}

    // ---- azimuthal map -----------------------------------------------------
    static Region aziArea() {
        Canvas cv = new Canvas();
        // top-right legend
        legendSpots = rt(legendSpotsText(liveSpots().size()));
        VBox tr = new VBox(2, legendSpots,
            rt("rings = " + (com.ars.fx.data.Units.standard() ? "3,000 mi" : "5,000 km")), rt("spokes = 30°"));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(58,18,0,0));
        StackPane area = new StackPane(cv, tr); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);

        Runnable redraw = () -> {
            double aw = area.getWidth(), ah = area.getHeight();
            if (aw <= 1 || ah <= 1) return;
            cv.setWidth(aw); cv.setHeight(ah);
            paintAzimuthal(cv.getGraphicsContext2D(), aw, ah);
        };
        area.widthProperty().addListener((o, a, b) -> redraw.run());
        area.heightProperty().addListener((o, a, b) -> redraw.run());
        area.setOnMouseClicked(e -> handleMapClick(area, e.getX(), e.getY()));
        currentMapRepaint = redraw;
        redraw.run();
        return area;
    }
    // ---- 3D globe ----------------------------------------------------------
    static GlobeView mapGlobe;                     // one instance, so the way it was turned survives rebuilds

    static Region globeArea() {
        if (mapGlobe == null) {
            mapGlobe = new GlobeView("map");
            mapGlobe.stationDot(false);                    // the painter draws the station with its call
            mapGlobe.painter(JMapCanvases::paintGlobe);
            mapGlobe.onClick((x, y) -> handleMapClick(mapGlobe, x, y));
        }
        mapGlobe.station(Q_LAT, Q_LON);
        legendSpots = rt(legendSpotsText(liveSpots().size()));
        VBox tr = new VBox(2, legendSpots, rt("rings = " + (com.ars.fx.data.Units.standard() ? "3,000 mi" : "5,000 km")));
        tr.setAlignment(Pos.TOP_RIGHT); tr.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); tr.setMouseTransparent(true);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT); StackPane.setMargin(tr, new Insets(58, 18, 0, 0));
        StackPane area = new StackPane(mapGlobe, tr); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);
        currentMapRepaint = () -> { mapGlobe.night(ovGrayline); mapGlobe.refresh(); };
        currentMapRepaint.run();
        return area;
    }

    static java.util.List<MapSpot> globeSpots = java.util.List.of();
    /** Everything J-Map draws on the flat and azimuthal maps, on the globe: range rings, the beam, spots, the sun, the station, and the track / path / beacon / heard-by layers. */
    static void paintGlobe(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double w, double h, boolean fresh) {
        loadPalette();
        if (fresh) globeSpots = liveSpots();               // a turn or zoom reuses the last lookup; a filter change or a new spot asks for a fresh one
        frameSpots = globeSpots;
        Proj gp = (lat, lon) -> { com.ars.fx.data.GlobeProjection.Pt p = pr.project(lat, lon, 0); return p.visible() ? new double[]{p.x(), p.y()} : null; };
        gcJump = Double.MAX_VALUE; gcGlobe = pr;
        try {
            if (ovRings) {
                g.setStroke(ovBasemap ? P.line() : Color.web("#28313d")); g.setLineWidth(1); g.setLineDashes(4, 5);
                for (double km : new double[]{5000, 10000, 15000}) globeLine(g, pr, com.ars.fx.data.GlobeProjection.circle(Q_LAT, Q_LON, km / 111.195, 120));
                g.setLineDashes(null);
            }
            if (ovBeam && !Double.isNaN(beamDeg())) drawGlobeBeam(g, pr, beamDeg());

            double spotA = dimSpots() ? 0.28 : 1.0;
            paintedDots.clear(); limbDots.clear();
            for (MapSpot s : globeSpots) {
                if (spotHidden(s.call())) continue;
                double[] xy = gp.at(s.lat(), s.lon());
                if (xy == null) { noteLimbSpot(pr, s, w, h); continue; }       // round the back: a burst on the globe's edge, if it is new
                paintedDots.add(new Dot(xy[0], xy[1], s));
                g.setStroke(Color.web("#0b0f14", 0.8 * spotA)); g.setLineWidth(2); g.strokeOval(xy[0] - 4.5, xy[1] - 4.5, 9, 9);
                spotDot(g, s, xy[0], xy[1], spotA);
            }
            if (ovGrayline) drawGlobeSun(g, pr);
            double[] me = gp.at(Q_LAT, Q_LON);
            if (me != null) {
                g.setFill(Color.web("#31bdda")); g.fillOval(me[0] - 5, me[1] - 5, 10, 10);
                g.setStroke(Color.web("#31bdda", 0.35)); g.setLineWidth(3); g.strokeOval(me[0] - 5, me[1] - 5, 10, 10);
                g.setStroke(Color.web("#0b0f14", 0.7)); g.setLineWidth(3); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.LEFT);
                g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND); g.strokeText(myCall, me[0] + 9, me[1] + 4); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.MITER);
                g.setFill(Color.web("#31bdda")); g.fillText(myCall, me[0] + 9, me[1] + 4);
            }
            drawSpotPaths(g, gp);
            drawHeardBy(g, gp);
            drawBeacons(g, gp);
            drawTracks(g, gp);
            drawSessionQsos(g, gp);
            if (ovCallsigns) drawSpotLabels(g, spotA, w, h, me != null ? me[0] : Double.NaN, me != null ? me[1] : 0);
            kickBursts();
        } finally { gcJump = 350; gcGlobe = null; frameSpots = null; }
    }
    /**
     * The sun out in space: on the line of constant latitude it rides all day (today's declination), at an orbital height rather than flat on the
     * surface, with a faint line down to the point it is overhead. Round the back of the globe it is hidden by it.
     */
    static void drawGlobeSun(GraphicsContext g, com.ars.fx.data.GlobeProjection pr) {
        double[] ss = com.ars.fx.data.SolarPosition.subSolar();
        double alt = pr.orbitAltKm(1.3);                  // near enough that it stays on the screen for most views of the globe
        double[][] track = new double[121][];
        for (int i = 0; i <= 120; i++) track[i] = new double[]{ss[0], -180 + 3.0 * i};
        g.setStroke(Color.web("#ffd84a", 0.45)); g.setLineWidth(1.2); g.setLineDashes(2, 5);
        globeLine(g, pr, track, alt);
        com.ars.fx.data.GlobeProjection.Pt up = pr.project(ss[0], ss[1], alt), down = pr.project(ss[0], ss[1], 0);
        if (up.visible() && down.visible()) { g.setLineDashes(3, 4); g.setStroke(Color.web("#ffd84a", 0.4)); g.strokeLine(down.x(), down.y(), up.x(), up.y()); }
        g.setLineDashes(null);
        if (up.visible()) drawSun(g, up.x(), up.y());
    }
    /** A polyline of {lat, lon} points on the globe, broken where a point is behind it. */
    static void globeLine(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double[][] pts) { globeLine(g, pr, pts, 0); }
    static void globeLine(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double[][] pts, double altKm) {
        com.ars.fx.data.GlobeProjection.Pt prev = null;
        for (double[] p : pts) {
            com.ars.fx.data.GlobeProjection.Pt cur = pr.project(p[0], p[1], altKm);
            if (prev != null && prev.visible() && cur.visible()) g.strokeLine(prev.x(), prev.y(), cur.x(), cur.y());
            prev = cur;
        }
    }
    /** The beam as a centre line and two edges 11 degrees either side, out to the same 15,000 km as the other maps; filled when all of it is on the near side. */
    static void drawGlobeBeam(GraphicsContext g, com.ars.fx.data.GlobeProjection pr, double heading) {
        int steps = 40; double reach = 15000, spread = 11;
        double[][] left = new double[steps + 1][], right = new double[steps + 1][], mid = new double[steps + 1][];
        for (int i = 0; i <= steps; i++) {
            double km = reach * i / steps;
            left[i] = dest(Q_LAT, Q_LON, heading - spread, km); right[i] = dest(Q_LAT, Q_LON, heading + spread, km); mid[i] = dest(Q_LAT, Q_LON, heading, km);
        }
        double[] xs = new double[(steps + 1) * 2], ys = new double[(steps + 1) * 2];
        boolean all = true;
        for (int i = 0; i <= steps; i++) {
            com.ars.fx.data.GlobeProjection.Pt a = pr.project(left[i][0], left[i][1], 0), b = pr.project(right[i][0], right[i][1], 0);
            xs[i] = a.x(); ys[i] = a.y(); xs[xs.length - 1 - i] = b.x(); ys[ys.length - 1 - i] = b.y();
            all &= a.visible() && b.visible();
        }
        if (all) { g.setFill(Color.web("#31bdda", 0.2)); g.fillPolygon(xs, ys, xs.length); }
        g.setStroke(Color.web("#31bdda", 0.8)); g.setLineWidth(1.4);
        globeLine(g, pr, mid);
        if (!all) { g.setStroke(Color.web("#31bdda", 0.45)); globeLine(g, pr, left); globeLine(g, pr, right); }
    }

    static Label rt(String s){ return lbl(s, "jm-legend"); }
    static Label legendSpots;                     // live first line of the azimuthal note
    static String legendSpotsText(int shown) { return shown + " spots · ≤ " + spotAgeMax + " min"; }

    static void paintAzimuthal(GraphicsContext g, double w, double h) {
        g.clearRect(0, 0, w, h);
        double cx = w/2, cy = h/2 + 4, R = Math.max(70, Math.min(w, h)/2 - 70);
        loadPalette();
        Color border = ovBasemap ? P.line() : Color.web("#28313d"), faint = Color.web("#28313d"), t3 = ovBasemap ? P.lineText() : Color.web("#6a7684"), t4 = ovBasemap ? P.lineText() : Color.web("#4d5663");

        // vector world basemap (Natural Earth), azimuthal-equidistant about the station, clipped to the disk
        if (ovBasemap) {
            g.save();
            g.beginPath(); g.arc(cx, cy, R, R, 0, 360); g.closePath(); g.clip();
            g.setFill(P.ocean()); g.fillRect(cx - R, cy - R, R * 2, R * 2);
            drawVectorAzimuthal(g, cx, cy, R);
            g.restore();
        }

        // real day/night terminator (gray line) projected into the azimuthal map
        if (ovGrayline) drawAzimuthalGrayLine(g, cx, cy, R);
        if (ovBasemap) {                                          // country names over the night shading, under everything else
            g.save();
            g.beginPath(); g.arc(cx, cy, R, R, 0, 360); g.closePath(); g.clip();
            drawAzimuthalLabels(g, cx, cy, R);
            g.restore();
        }

        // range rings
        if (ovRings) {
            double[] rr = {0.34, 0.67, 1.0}; String[] rl = com.ars.fx.data.Units.standard() ? new String[]{"3k mi","6k mi","9k mi"} : new String[]{"5k km","10k km","15k km"};
            g.setStroke(border); g.setLineWidth(1);
            for (int i=0;i<3;i++){ double r=R*rr[i]; g.strokeOval(cx-r, cy-r, r*2, r*2); }
            g.setFont(Font.font("JetBrains Mono", 9.5)); g.setFill(t4); g.setTextAlign(TextAlignment.LEFT);
            for (int i=0;i<3;i++){ g.fillText(rl[i], cx+4, cy-R*rr[i]+12); }
        }

        // spokes + compass labels
        String[] comp = {"N","30","60","E","120","150","S","210","240","W","300","330"};
        g.setLineWidth(1);
        for (int i=0;i<12;i++){
            double a = Math.toRadians(i*30 - 90);
            g.setStroke(ovBasemap ? P.line().deriveColor(0, 1, 1, 0.55) : Color.web("#28313d", 0.55));
            g.strokeLine(cx, cy, cx+Math.cos(a)*R, cy+Math.sin(a)*R);
            g.setFill(t3); g.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
            g.fillText(comp[i], cx+Math.cos(a)*(R+18), cy+Math.sin(a)*(R+18)+4);
        }

        // beam wedge toward the selected heading
        if (ovBeam && !Double.isNaN(beamDeg())) {
            double beam = Math.toRadians(beamDeg() - 90), spread = Math.toRadians(11);
            g.setFill(Color.web("#31bdda", 0.16));
            g.beginPath(); g.moveTo(cx, cy);
            g.lineTo(cx+Math.cos(beam-spread)*R, cy+Math.sin(beam-spread)*R);
            g.lineTo(cx+Math.cos(beam+spread)*R, cy+Math.sin(beam+spread)*R);
            g.closePath(); g.fill();
        }

        // live DX-cluster spots
        double spotA = dimSpots() ? 0.28 : 1.0;
        paintedDots.clear(); limbDots.clear();
        for (MapSpot s : liveSpots()){
            if (spotHidden(s.call())) continue;
            double[] pp = azProject(s.lat(), s.lon(), cx, cy, R);
            if (pp[2] > 1.0) continue;                       // off the visible disk
            double x = pp[0], y = pp[1];
            paintedDots.add(new Dot(x, y, s));
            spotDot(g, s, x, y, spotA);
        }
        // the Sun — sub-solar point projected into the azimuthal map (if visible)
        if (ovGrayline) {
            double[] ss = com.ars.fx.data.SolarPosition.subSolar();
            double[] sp = azProject(ss[0], ss[1], cx, cy, R);
            if (sp[2] <= 1.0) drawSun(g, sp[0], sp[1]);
        }

        // me (center)
        g.setFill(Color.web("#31bdda")); g.fillOval(cx-5, cy-5, 10, 10);
        g.setStroke(Color.web("#31bdda", 0.35)); g.setLineWidth(3); g.strokeOval(cx-5, cy-5, 10, 10);
        g.setFill(Color.web("#31bdda")); g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.fillText(myCall, cx+9, cy+4);

        // NCDXF beacon + "who's hearing me" overlays (azimuthal-equidistant projection)
        Proj azi = (lat, lon) -> { double[] pp = azProject(lat, lon, cx, cy, R); return pp[2] <= 1.0 ? new double[]{pp[0], pp[1]} : null; };
        drawSpotPaths(g, azi);
        drawHeardBy(g, azi);
        drawBeacons(g, azi);
        drawTracks(g, azi);
        drawSessionQsos(g, azi);
        if (ovCallsigns) drawSpotLabels(g, spotA, w, h, cx, cy);
        kickBursts();
    }
    /** Azimuthal-equidistant night shading + terminator (gray line) centred on QTH. */
    // One image, not thousands of small rectangles: those stalled the render thread for about 400 ms on every repaint.
    static javafx.scene.image.WritableImage nightMask;
    static String nightMaskKey = "";

    /** The dark side of the azimuthal disk, recalculated when the size, station or minute changes. */
    static javafx.scene.image.Image nightMask(double R) {
        int size = (int) Math.round(R);                       // half the disk's pixels; scaling up softens the edge
        String key = size + "/" + Q_LAT + "/" + Q_LON + "/" + (System.currentTimeMillis() / 60_000) + "/" + P.night();
        if (key.equals(nightMaskKey)) return nightMask;

        double[] ss = com.ars.fx.data.SolarPosition.subSolar();
        double declR = Math.toRadians(ss[0]), sinDecl = Math.sin(declR), cosDecl = Math.cos(declR), ssLonR = Math.toRadians(ss[1]);
        double qLatR = Math.toRadians(Q_LAT), qLonR = Math.toRadians(Q_LON), sinQ = Math.sin(qLatR), cosQ = Math.cos(qLatR);
        int[] argb = new int[size * size];
        Color nc = ovBasemap ? P.night() : Color.web("#000000", 0.42);
        int night = ((int) Math.round(nc.getOpacity() * 255) << 24) | ((int) Math.round(nc.getRed() * 255) << 16) | ((int) Math.round(nc.getGreen() * 255) << 8) | (int) Math.round(nc.getBlue() * 255);
        double half = size / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - half, dy = y + 0.5 - half, rp = Math.sqrt(dx * dx + dy * dy);
                if (rp > half) continue;
                double delta = (rp / half) * 15000.0 / 6371.0;     // angular distance (rad)
                double brg = Math.atan2(dy, dx) + Math.PI / 2;     // bearing from north
                double sinDel = Math.sin(delta), cosDel = Math.cos(delta);
                double latR = Math.asin(sinQ * cosDel + cosQ * sinDel * Math.cos(brg));
                double lonR = qLonR + Math.atan2(Math.sin(brg) * sinDel * cosQ, cosDel - sinQ * Math.sin(latR));
                if (Math.sin(latR) * sinDecl + Math.cos(latR) * cosDecl * Math.cos(lonR - ssLonR) < 0) argb[y * size + x] = night;
            }
        }
        if (nightMask == null || (int) nightMask.getWidth() != size) nightMask = new javafx.scene.image.WritableImage(size, size);
        nightMask.getPixelWriter().setPixels(0, 0, size, size,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), argb, 0, size);
        nightMaskKey = key;
        return nightMask;
    }

    static void drawAzimuthalGrayLine(GraphicsContext g, double cx, double cy, double R) {
        double[] ss = com.ars.fx.data.SolarPosition.subSolar(); double decl = ss[0], ssLon = ss[1];

        g.drawImage(nightMask(R), cx - R, cy - R, R * 2, R * 2);

        // terminator line — the great circle 90° from the sub-solar point, clipped to the disk
        g.setStroke(Color.web("#eebb58", 0.85)); g.setLineWidth(1.6); g.setLineDashes(6, 5);
        double prevX = 0, prevY = 0; boolean have = false;
        for (int d = 0; d <= 360; d += 3) {
            double[] t = dest(decl, ssLon, d, 90.0 * 111.195);
            double[] p = azProject(t[0], t[1], cx, cy, R);
            if (p[2] <= 1.0) { if (have) g.strokeLine(prevX, prevY, p[0], p[1]); prevX = p[0]; prevY = p[1]; have = true; }
            else have = false;
        }
        g.setLineDashes(null);
    }
    /** The map colours for the colour scheme in force; reloaded at the start of every paint so the theme button takes effect at once. */
    static MapPalette P = MapPalette.forScheme("dark");
    static void loadPalette() { P = MapPalette.forScheme(com.ars.fx.Themes.effectiveKey("map")); }
    /** Called when the theme button is pressed: repaint the map in the new colours. */
    static void themeChanged() { if (currentMapRepaint != null) currentMapRepaint.run(); }
    /** Antarctica is drawn as ice, everything else (Greenland too) as land. */
    static boolean isIce(com.ars.fx.data.VectorBasemap.Ring r) {
        return r.maxLat() < -60;
    }

    /** One shape on the azimuthal map, projected in degrees of arc from the station (see {@link com.ars.fx.data.AzimuthalPath}). */
    record AzShape(java.util.List<double[]> paths, int colour, boolean ice) {}
    static java.util.List<AzShape> azShapes = java.util.List.of();
    static java.util.List<double[]> azBorders = java.util.List.of();
    static String azKey = "";
    static final double AZ_HOLD = 140.0;                                  // degrees; the disk itself ends at about 135

    /** Project the land (or the countries) and borders for this station once; redone only when the station or the switch changes. */
    static void azPrepare() {
        String key = Q_LAT + "/" + Q_LON + "/" + ovCountries;
        if (key.equals(azKey)) return;
        java.util.List<AzShape> shapes = new java.util.ArrayList<>();
        if (ovCountries) {
            for (com.ars.fx.data.CountryShapes.Country c : com.ars.fx.data.CountryShapes.all())
                for (com.ars.fx.data.CountryShapes.Polygon poly : c.polygons()) {
                    java.util.List<double[]> rings = new java.util.ArrayList<>();
                    for (com.ars.fx.data.VectorBasemap.Ring r : poly.rings()) rings.add(r.ll());
                    shapes.add(new AzShape(com.ars.fx.data.AzimuthalPath.polygon(rings, Q_LAT, Q_LON, AZ_HOLD,
                            com.ars.fx.data.CountryShapes.contains(poly, Q_LON, Q_LAT)), c.colour(), c.ice()));
                }
        } else {
            for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.land().rings())
                shapes.add(new AzShape(com.ars.fx.data.AzimuthalPath.polygon(java.util.List.of(r.ll()), Q_LAT, Q_LON, AZ_HOLD,
                        com.ars.fx.data.VectorBasemap.contains(r, Q_LON, Q_LAT)), 0, isIce(r)));
        }
        java.util.List<double[]> borders = new java.util.ArrayList<>();
        for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.borders().rings())
            borders.add(com.ars.fx.data.AzimuthalPath.line(r.ll(), Q_LAT, Q_LON, AZ_HOLD));
        azShapes = shapes; azBorders = borders; azKey = key;
    }

    /** Land (or countries, each in its colour) and borders in the azimuthal projection, with no wedge at the antipode. */
    static void drawVectorAzimuthal(GraphicsContext g, double cx, double cy, double R) {
        azPrepare();
        double k = R * 111.195 / 15000.0;                                  // pixels per degree of arc
        g.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD); g.setLineWidth(0.8);
        for (AzShape sh : azShapes) {
            g.setFill(sh.ice() ? P.ice() : sh.colour() > 0 ? P.country(sh.colour()) : P.land());
            g.setStroke(sh.ice() ? P.iceCoast() : P.coast());
            g.beginPath();
            for (double[] xy : sh.paths()) azAppend(g, xy, cx, cy, k, true);
            g.fill(); g.stroke();
        }
        g.setFillRule(javafx.scene.shape.FillRule.NON_ZERO);
        g.setStroke(P.border()); g.setLineWidth(0.7);
        for (double[] xy : azBorders) { g.beginPath(); azAppend(g, xy, cx, cy, k, false); g.stroke(); }
    }
    static void azAppend(GraphicsContext g, double[] xy, double cx, double cy, double k, boolean close) {
        int n = xy.length / 2;
        if (n == 0) return;
        double lx = 0, ly = 0;
        for (int i = 0; i < n; i++) {
            double x = cx + xy[i * 2] * k, y = cy + xy[i * 2 + 1] * k;
            if (i > 0 && i < n - 1 && Math.abs(x - lx) < 0.6 && Math.abs(y - ly) < 0.6) continue;   // under a pixel apart
            if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
            lx = x; ly = y;
        }
        if (close) g.closePath();
    }

    /** Country names on the azimuthal map: the larger countries, more of them where the map stretches, none past 120 degrees. */
    static void drawAzimuthalLabels(GraphicsContext g, double cx, double cy, double R) {
        double k = R * 111.195 / 15000.0;
        g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.setLineWidth(3); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        java.util.List<double[]> placed = new java.util.ArrayList<>();
        for (com.ars.fx.data.MapLabels.Label l : com.ars.fx.data.MapLabels.countries()) {
            double[] p = com.ars.fx.data.AzimuthalPath.point(l.lat(), l.lon(), Q_LAT, Q_LON);
            if (p[2] > 120 || p[2] < 1) continue;
            double rad = Math.toRadians(p[2]), stretch = rad / Math.sin(rad);              // east-west stretch of this projection
            if (l.minZoom() > azLabelZoom(R) + Math.log(stretch) / Math.log(2)) continue;
            double x = cx + p[0] * k, y = cy + p[1] * k, hw = l.name().length() * 3.3 + 3, hh = 8;
            boolean clash = Math.hypot(x - cx, y - cy) < 26;                           // keep the station's own call clear
            for (double[] r : placed) if (Math.abs(x - r[0]) < hw + r[2] && Math.abs(y - r[1]) < hh + r[3]) { clash = true; break; }
            if (clash) continue;
            placed.add(new double[]{x, y, hw, hh});
            g.setStroke(P.halo()); g.strokeText(l.name(), x, y);
            g.setFill(P.text().deriveColor(0, 1, 1, 0.85)); g.fillText(l.name(), x, y);
        }
        g.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
    /** Which of Natural Earth's label levels the azimuthal map shows near the station: 3 (about 100 of the larger countries) on a
     *  disk 400 pixels across, more on a bigger one. Overlapping names are skipped, most important kept. */
    static double azLabelZoom(double R) { return 3.0 + Math.log(Math.max(100, R) / 400.0) / Math.log(2); }
    /** Project a geographic point into the azimuthal-equidistant map (centred on QTH). Returns {x,y,distNorm}. */
    static double[] azProject(double lat, double lon, double cx, double cy, double R) {
        double qLatR = Math.toRadians(Q_LAT), latR = Math.toRadians(lat), dLon = Math.toRadians(lon - Q_LON);
        double cosD = Math.sin(qLatR) * Math.sin(latR) + Math.cos(qLatR) * Math.cos(latR) * Math.cos(dLon);
        double delta = Math.acos(Math.max(-1, Math.min(1, cosD)));
        double distNorm = (Math.toDegrees(delta) * 111.195) / 15000.0;
        double brg = Math.atan2(Math.sin(dLon) * Math.cos(latR),
                Math.cos(qLatR) * Math.sin(latR) - Math.sin(qLatR) * Math.cos(latR) * Math.cos(dLon));
        double a = brg - Math.PI / 2, r = distNorm * R;
        return new double[]{ cx + Math.cos(a) * r, cy + Math.sin(a) * r, distNorm };
    }

    // ---- rectangular (equirectangular) blue-marble map ---------------------
    /** Band buttons on the left panel: {band number, colour}. */
    static final String[][] MAP_BANDS = {
        {"160","#ec5b57"},{"80","#ec5b57"},{"40","#eebb58"},{"30","#67d283"},{"20","#26c0cf"},
        {"17","#26c0cf"},{"15","#b191ea"},{"12","#b191ea"},{"10","#ef7d83"},
    };
    /** Overlay toggle labels, in the order of {@code overlayFlag(i)}. */
    static final Object[][] OVERLAYS = {
        {"Gray line / sun"},{"Basemap"},{"Beam heading"},{"Range rings"},{"Callsigns"},{"Needed only"},{"Starburst on new spots"},
        {"Fade old callsigns", "A callsign fades from the map after a while - sooner when the map is busy (about 75 seconds with 80 or more spots showing), later when it is quiet (up to 8 minutes with 10 or fewer). The dot stays, and the selected spot always keeps its name."},
        {"Country colours", "Each country in its own shade of the land colour, so borders are easy to see; neighbours never share one. Off: all land in one colour."},
        {"Sweepstakes sections", "The 85 ARRL and RAC Sweepstakes sections, each in its own colour with its name once you zoom in (flat map); a section you have worked in the open contest turns hunter green. Draws over the state colours if both are on."},
        {"States / provinces", "The states and provinces of the US and Canada, each in its own colour with its postal code once you zoom in (flat map)."},
    };
    static double Q_LAT = 0, Q_LON = 0;  // station QTH (from HubConfig)
    static String myCall = "";
    static double qdbl(String s, double def) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } }

    // Fixed-zoom regions for the blue marble: {minLat, maxLat, minLon, maxLon}.
    // Index 0 is the whole world; the rest snap to a continent-sized window.
    static final String[] REGION_NAMES = {"World","NA","SA","EU","AF","W.As","E.As","Aus","Ant"};
    static final double[][] REGIONS = {
        {-90,  90, -180, 180},   // World
        {  7,  72, -168, -52},   // North America
        {-56,  13,  -82, -34},   // South America
        { 34,  71,  -12,  40},   // Europe
        {-36,  38,  -18,  52},   // Africa
        { 12,  50,   26,  82},   // West Asia
        {  5,  55,  100, 150},   // East Asia
        {-47,  -8,  112, 179},   // Australia / Oceania
        {-90, -58, -180, 180},   // Antarctica
    };
    static int mapRegion = Math.max(0, Math.min(REGION_NAMES.length - 1, com.ars.fx.data.HubConfig.getInt("map.region", 0)));
    // Live equirectangular viewport the rect projection + overlays map into.
    static double vMinLon = -180, vMaxLon = 180, vMinLat = -90, vMaxLat = 90;
    static int appliedRegion = -1;
    static double lastRMX, lastRMY, lastRW = 1, lastRH = 1;     // the map rectangle of the last paint, for zoom and pan
    static Region rectMap() {
        Canvas cv = new Canvas();
        StackPane area = new StackPane(cv); area.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(area, Priority.ALWAYS);
        area.setMinSize(0, 0);

        // The map window is {vMinLat..vMaxLat} x {vMinLon..vMaxLon}: a region chip sets it, the wheel zooms about the
        // pointer, a drag pans, a double click goes back to the region. Equirectangular has equal px/degree on both axes,
        // so the rectangle's aspect is the window's lon-span : lat-span.
        Runnable redraw = () -> {
            double aw = area.getWidth(), ah = area.getHeight();
            if (aw <= 1 || ah <= 1) return;
            if (appliedRegion != mapRegion) {
                double[] rg = REGIONS[mapRegion];
                vMinLat = rg[0]; vMaxLat = rg[1]; vMinLon = rg[2]; vMaxLon = rg[3];
                appliedRegion = mapRegion;
            }
            double aspect = (vMaxLon - vMinLon) / (vMaxLat - vMinLat);
            double imgW = Math.min(aw, ah * aspect), imgH = imgW / aspect;
            cv.setWidth(aw); cv.setHeight(ah);
            paintRect(cv.getGraphicsContext2D(), aw, ah, imgW, imgH);
        };
        area.widthProperty().addListener((o, a, b) -> redraw.run());
        area.heightProperty().addListener((o, a, b) -> redraw.run());

        double[] press = new double[3];                          // x, y, dragged
        area.setOnMousePressed(e -> { press[0] = e.getX(); press[1] = e.getY(); press[2] = 0; });
        area.setOnMouseDragged(e -> {
            double dx = e.getX() - press[0], dy = e.getY() - press[1];
            if (press[2] == 0 && Math.hypot(dx, dy) < 4) return;
            press[2] = 1; press[0] = e.getX(); press[1] = e.getY();
            double lonSpan = vMaxLon - vMinLon, latSpan = vMaxLat - vMinLat;
            setWindow(vMinLon - dx / lastRW * lonSpan, vMinLat + dy / lastRH * latSpan, lonSpan, latSpan);
            currentMapRepaint.run();
        });
        // Wheel zoom glides: each notch moves a target, and every frame the view eases toward it about the point under the pointer.
        // tz = { target lon span, target lat span, anchor lon, anchor lat, anchor fx, anchor fy, last frame time }
        double[] tz = new double[7];
        boolean[] zooming = { false };
        javafx.animation.AnimationTimer zoomTimer = new javafx.animation.AnimationTimer() {
            @Override public void handle(long now) {
                double lonSpan = vMaxLon - vMinLon, latSpan = vMaxLat - vMinLat;
                double dt = tz[6] == 0 ? 0.016 : Math.min(0.05, (now - tz[6]) / 1e9);
                tz[6] = now;
                double k = 1 - Math.exp(-dt / 0.07);             // ~70 ms time constant, independent of the frame rate
                boolean done = Math.abs(tz[0] / lonSpan - 1) < 0.002;
                double nLon = done ? tz[0] : lonSpan + (tz[0] - lonSpan) * k, nLat = done ? tz[1] : latSpan + (tz[1] - latSpan) * k;
                setWindow(tz[2] - tz[4] * nLon, tz[3] - (1 - tz[5]) * nLat, nLon, nLat);
                currentMapRepaint.run();
                if (done) { stop(); zooming[0] = false; tz[6] = 0; }
            }
        };
        area.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            double lonSpan = zooming[0] ? tz[0] : vMaxLon - vMinLon, latSpan = zooming[0] ? tz[1] : vMaxLat - vMinLat;
            double cLon = vMaxLon - vMinLon, cLat = vMaxLat - vMinLat;
            double fx = Math.max(0, Math.min(1, (e.getX() - lastRMX) / lastRW)), fy = Math.max(0, Math.min(1, (e.getY() - lastRMY) / lastRH));
            tz[2] = vMinLon + fx * cLon; tz[3] = vMaxLat - fy * cLat; tz[4] = fx; tz[5] = fy;   // the point under the pointer stays put
            double f = Math.exp(e.getDeltaY() / 300.0);           // wheel up zooms in
            double ns = Math.max(Math.min(1.0 / f, Math.min(360 / lonSpan, 180 / latSpan)), 2.0 / latSpan);   // spans never exceed the world nor go under 2 degrees
            tz[0] = lonSpan * ns; tz[1] = latSpan * ns;
            if (!zooming[0]) { zooming[0] = true; tz[6] = 0; zoomTimer.start(); }
        });
        area.setOnMouseClicked(e -> {
            if (press[2] == 1) { press[2] = 0; return; }          // the end of a drag is not a click
            if (e.getClickCount() == 2) { appliedRegion = -1; currentMapRepaint.run(); return; }
            handleMapClick(area, e.getX(), e.getY());
        });
        currentMapRepaint = redraw;
        redraw.run();
        return area;
    }
    /** Set the map window from its south-west corner and spans, kept inside the world. */
    static void setWindow(double minLon, double minLat, double lonSpan, double latSpan) {
        minLon = Math.max(-180, Math.min(180 - lonSpan, minLon));
        minLat = Math.max(-90, Math.min(90 - latSpan, minLat));
        vMinLon = minLon; vMaxLon = minLon + lonSpan; vMinLat = minLat; vMaxLat = minLat + latSpan;
    }

    /** Land, coast and country borders on the flat map. Rings off the window are skipped, and points under a pixel apart are dropped. */
    static void drawVectorRect(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        g.setFill(P.ocean()); g.fillRect(rmx, rmy, rw, rh);
        double pxPerDeg = rw / (vMaxLon - vMinLon);
        g.setFill(P.land()); g.setStroke(P.coast()); g.setLineWidth(0.8);
        if (ovCountries) {                                        // each country in its colour (neighbours always differ)
            g.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
            for (com.ars.fx.data.CountryShapes.Country c : com.ars.fx.data.CountryShapes.all()) {
                g.setFill(c.ice() ? P.ice() : P.country(c.colour())); g.setStroke(c.ice() ? P.iceCoast() : P.coast());
                for (com.ars.fx.data.CountryShapes.Polygon poly : c.polygons()) {
                    if (poly.maxLon() < vMinLon || poly.minLon() > vMaxLon || poly.maxLat() < vMinLat || poly.minLat() > vMaxLat) continue;
                    g.beginPath();
                    for (com.ars.fx.data.VectorBasemap.Ring r : poly.rings()) rectAppend(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, true);
                    g.fill(); g.stroke();
                }
            }
            g.setFillRule(javafx.scene.shape.FillRule.NON_ZERO);
        }
        for (com.ars.fx.data.VectorBasemap.Ring r : ovCountries ? new com.ars.fx.data.VectorBasemap.Ring[0] : com.ars.fx.data.VectorBasemap.land().rings()) {
            if (!ringVisible(r)) continue;
            boolean ice = isIce(r);
            g.setFill(ice ? P.ice() : P.land()); g.setStroke(ice ? P.iceCoast() : P.coast());
            rectPath(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, true);
            g.fill(); g.stroke();
        }
        if (ovStates) drawRegions(g, com.ars.fx.data.RegionLayers.states(), rmx, rmy, rw, rh, pxPerDeg);
        if (ovSections) drawRegions(g, com.ars.fx.data.RegionLayers.sections(), rmx, rmy, rw, rh, pxPerDeg);
        g.setStroke(P.border()); g.setLineWidth(0.7);
        for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.borders().rings()) {
            if (!ringVisible(r)) continue;
            rectPath(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, false);
            g.stroke();
        }
        if (!ovStates && !ovSections && vMaxLon - vMinLon <= 100) {                           // state and province lines only once zoomed in
            g.setStroke(P.state()); g.setLineWidth(0.6); g.setLineDashes(4, 3);
            for (com.ars.fx.data.VectorBasemap.Ring r : com.ars.fx.data.VectorBasemap.states().rings()) {
                if (!ringVisible(r)) continue;
                rectPath(g, r.ll(), rmx, rmy, rw, rh, pxPerDeg, false);
                g.stroke();
            }
            g.setLineDashes(null);
        }
    }
    static final javafx.scene.paint.Color HUNTER_GREEN = Color.web("#355E3B");
    /** A plugin that logs the whole of one of these lights every section on the right (ON = all four Ontario sections ...). */
    static final java.util.Map<String, java.util.List<String>> SECTION_ALIASES = java.util.Map.of(
        "ON", java.util.List.of("ONN", "ONE", "GH", "ONS"), "GTA", java.util.List.of("GH"), "HI", java.util.List.of("PAC"),
        "NT", java.util.List.of("TER"), "NU", java.util.List.of("TER"), "YT", java.util.List.of("TER"),
        "FL", java.util.List.of("NFL", "SFL", "WCF"));
    static java.util.Set<String> workedSections = java.util.Set.of();
    static long workedSectionsAt = 0, soloScoreAt = 0;
    /** Sections worked in the open contest, aliases resolved; re-read at most every 2 seconds because the repaint is frequent. */
    static java.util.Set<String> workedSections() {
        long now = System.currentTimeMillis();
        if (now - workedSectionsAt > 2000) {
            // J-Map in its own window is a separate process: nothing there ever opened the contest, so the worked set was always empty.
            // Load the active contest, and re-score every few seconds because the contest page's change counter is in the other process.
            if (com.ars.fx.data.ContestState.plugin() == null) com.ars.fx.data.ContestState.ensureLoaded();
            if (com.ars.fx.shell.Shell.solo && com.ars.fx.data.ContestState.plugin() != null && now - soloScoreAt > 5000) {
                soloScoreAt = now; com.ars.fx.data.ContestState.recompute();
            }
            java.util.Set<String> w = new java.util.HashSet<>();
            for (String m : com.ars.fx.data.ContestState.workedMults()) {
                String u = m.trim().toUpperCase();
                w.add(u);
                w.addAll(SECTION_ALIASES.getOrDefault(u, java.util.List.of()));
            }
            workedSections = w; workedSectionsAt = now;
        }
        return workedSections;
    }
    /** A region layer (Sweepstakes sections, states and provinces): each region filled in its colour with an outline. Worked sections are hunter green. */
    static void drawRegions(GraphicsContext g, java.util.List<com.ars.fx.data.RegionLayers.Region> layer, double rmx, double rmy, double rw, double rh, double pxPerDeg) {
        g.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        g.setStroke(P.coast()); g.setLineWidth(0.8);
        java.util.Set<String> worked = layer == com.ars.fx.data.RegionLayers.sections() ? workedSections() : java.util.Set.of();
        for (com.ars.fx.data.RegionLayers.Region r : layer) {
            g.setFill(worked.contains(r.code()) ? HUNTER_GREEN : P.country(r.shape().colour()));
            for (com.ars.fx.data.CountryShapes.Polygon poly : r.shape().polygons()) {
                if (poly.maxLon() < vMinLon || poly.minLon() > vMaxLon || poly.maxLat() < vMinLat || poly.minLat() > vMaxLat) continue;
                g.beginPath();
                for (com.ars.fx.data.VectorBasemap.Ring ring : poly.rings()) rectAppend(g, ring.ll(), rmx, rmy, rw, rh, pxPerDeg, true);
                g.fill(); g.stroke();
            }
        }
        g.setFillRule(javafx.scene.shape.FillRule.NON_ZERO);
    }
    /** Names of the region layers, biggest region first; one that would overlap a name already placed, or does not fit in its region, is skipped. */
    static void drawRegionLabels(GraphicsContext g, java.util.List<com.ars.fx.data.RegionLayers.Region> layer, double rmx, double rmy, double rw, double rh, double pxPerDeg) {
        g.setFont(Font.font("IBM Plex Sans", FontWeight.BOLD, 11)); g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.setLineWidth(3); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        java.util.List<double[]> placed = new java.util.ArrayList<>();
        java.util.List<com.ars.fx.data.RegionLayers.Region> order = new java.util.ArrayList<>(layer);
        order.sort((a, b) -> Double.compare(b.size(), a.size()));
        for (com.ars.fx.data.RegionLayers.Region r : order) {
            if (r.labelLon() < vMinLon || r.labelLon() > vMaxLon || r.labelLat() < vMinLat || r.labelLat() > vMaxLat) continue;
            String name = r.code();
            double x = lon2x(r.labelLon(), rmx, rw), y = lat2y(r.labelLat(), rmy, rh), hw = name.length() * 3.8 + 3, hh = 8;
            if (Math.sqrt(r.size()) * pxPerDeg < hw * 1.4) continue;            // the region is too small on screen to carry its name
            boolean clash = false;
            for (double[] p : placed) if (Math.abs(x - p[0]) < hw + p[2] && Math.abs(y - p[1]) < hh + p[3]) { clash = true; break; }
            if (clash) continue;
            placed.add(new double[]{x, y, hw, hh});
            g.setStroke(P.halo()); g.strokeText(name, x, y);
            g.setFill(P.text()); g.fillText(name, x, y);
        }
        g.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
    /** Country names, most important first; a name that would overlap one already placed is skipped. */
    static void drawCountryLabels(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        double z = com.ars.fx.data.MapLabels.zoomOf(vMaxLon - vMinLon);
        g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 11)); g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.setLineWidth(3); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        java.util.List<double[]> placed = new java.util.ArrayList<>();
        for (com.ars.fx.data.MapLabels.Label l : com.ars.fx.data.MapLabels.countries()) {
            if (l.minZoom() > z + 0.3) continue;
            if (l.lon() < vMinLon || l.lon() > vMaxLon || l.lat() < vMinLat || l.lat() > vMaxLat) continue;
            double x = lon2x(l.lon(), rmx, rw), y = lat2y(l.lat(), rmy, rh), hw = l.name().length() * 3.3 + 3, hh = 8;
            boolean clash = false;
            for (double[] r : placed) if (Math.abs(x - r[0]) < hw + r[2] && Math.abs(y - r[1]) < hh + r[3]) { clash = true; break; }
            if (clash) continue;
            placed.add(new double[]{x, y, hw, hh});
            g.setStroke(P.halo()); g.strokeText(l.name(), x, y);
            g.setFill(P.text().deriveColor(0, 1, 1, 0.85)); g.fillText(l.name(), x, y);
        }
        g.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
    static boolean ringVisible(com.ars.fx.data.VectorBasemap.Ring r) {
        return r.maxLon() >= vMinLon && r.minLon() <= vMaxLon && r.maxLat() >= vMinLat && r.minLat() <= vMaxLat;
    }
    static void rectPath(GraphicsContext g, double[] ll, double rmx, double rmy, double rw, double rh, double pxPerDeg, boolean close) {
        g.beginPath();
        rectAppend(g, ll, rmx, rmy, rw, rh, pxPerDeg, close);
    }
    /** One ring added to the current path (several make a polygon with holes). */
    static void rectAppend(GraphicsContext g, double[] ll, double rmx, double rmy, double rw, double rh, double pxPerDeg, boolean close) {
        double minStep = 0.6 / pxPerDeg, lx = 0, ly = 0;
        int n = ll.length / 2;
        for (int i = 0; i < n; i++) {
            double lo = ll[i * 2], la = ll[i * 2 + 1];
            if (i > 0 && i < n - 1 && Math.abs(lo - lx) < minStep && Math.abs(la - ly) < minStep) continue;
            double x = lon2x(lo, rmx, rw), y = lat2y(la, rmy, rh);
            if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
            lx = lo; ly = la;
        }
        if (close) g.closePath();
    }

    /** The beam as great-circle paths out to the azimuthal map's 15,000 km edge, so the two views agree. */
    static void drawRectBeam(GraphicsContext g, double rmx, double rmy, double rw, double rh, double heading) {
        double reach = 15000, spread = 11;
        int steps = 40;
        double[] left = rectPath(heading - spread, reach, steps), right = rectPath(heading + spread, reach, steps), mid = rectPath(heading, reach, steps);
        int n = steps + 1;
        double[] xs = new double[n * 2], ys = new double[n * 2];
        for (int i = 0; i < n; i++) {
            xs[i] = lon2x(left[i * 2 + 1], rmx, rw); ys[i] = lat2y(left[i * 2], rmy, rh);
            int back = n * 2 - 1 - i;
            xs[back] = lon2x(right[i * 2 + 1], rmx, rw); ys[back] = lat2y(right[i * 2], rmy, rh);
        }
        double[] mx = new double[n], my = new double[n];
        for (int i = 0; i < n; i++) { mx[i] = lon2x(mid[i * 2 + 1], rmx, rw); my[i] = lat2y(mid[i * 2], rmy, rh); }
        double world = rw * 360 / (vMaxLon - vMinLon);        // the map may be zoomed to a region
        for (double wrap : new double[]{-world, 0, world}) {  // a path past the date line carries on from the other edge
            double[] sx = new double[xs.length], sm = new double[n];
            for (int i = 0; i < sx.length; i++) sx[i] = xs[i] + wrap;
            for (int i = 0; i < n; i++) sm[i] = mx[i] + wrap;
            g.setFill(Color.web("#31bdda", 0.22)); g.fillPolygon(sx, ys, sx.length);
            g.setStroke(Color.web("#31bdda", 0.8)); g.setLineWidth(1.4); g.strokePolyline(sm, my, n);
        }
    }
    /** {lat, lon, lat, lon, ...} along a bearing from the station, longitude unbroken across the date line. */
    static double[] rectPath(double bearing, double km, int steps) {
        double[] out = new double[(steps + 1) * 2];
        double prev = Q_LON;
        for (int i = 0; i <= steps; i++) {
            double[] p = dest(Q_LAT, Q_LON, bearing, km * i / steps);
            prev += (((p[1] - prev) % 360 + 540) % 360) - 180;
            out[i * 2] = p[0]; out[i * 2 + 1] = prev;
        }
        return out;
    }

    /** Draw the equirectangular overlay (graticule, terminator, spots, QTH) for the given area + centred image rect. */
    static void paintRect(GraphicsContext g, double aw, double ah, double imgW, double imgH) {
        double RMX = (aw - imgW) / 2.0, RMY = (ah - imgH) / 2.0, RW = imgW, RH = imgH;
        loadPalette();
        lastRMX = RMX; lastRMY = RMY; lastRW = RW; lastRH = RH;
        g.clearRect(0, 0, aw, ah);
        g.save();
        g.beginPath(); g.rect(RMX, RMY, RW, RH); g.closePath(); g.clip();
        if (ovBasemap) drawVectorRect(g, RMX, RMY, RW, RH);
        g.restore();
        g.setStroke(Color.web("#28313d")); g.setLineWidth(1); g.strokeRect(RMX, RMY, RW, RH);

        // clip every overlay to the map rect so a zoomed region doesn't spill markers/labels
        g.save();
        g.beginPath(); g.rect(RMX, RMY, RW, RH); g.closePath(); g.clip();

        for (int lon = -150; lon <= 150; lon += 30) {
            double x = lon2x(lon, RMX, RW); boolean prime = lon == 0;
            g.setStroke(ovBasemap ? P.line().deriveColor(0, 1, 1, prime ? 1.2 : 0.6) : Color.web("#28313d", prime ? 0.9 : 0.45)); g.setLineWidth(prime ? 1.1 : 0.6);
            g.strokeLine(x, RMY, x, RMY + RH);
        }
        for (int lat = -60; lat <= 60; lat += 30) {
            double y = lat2y(lat, RMY, RH); boolean eq = lat == 0;
            g.setStroke(ovBasemap ? P.line().deriveColor(0, 1, 1, eq ? 1.2 : 0.6) : Color.web("#28313d", eq ? 0.9 : 0.45)); g.setLineWidth(eq ? 1.1 : 0.6);
            g.strokeLine(RMX, y, RMX + RW, y);
        }
        if (ovGrayline) drawTerminator(g, RMX, RMY, RW, RH);
        if (ovBeam && !Double.isNaN(beamDeg())) drawRectBeam(g, RMX, RMY, RW, RH, beamDeg());

        if (vMaxLon - vMinLon > 200) {                          // the whole world: the continents' names; zoomed in, the countries' names
            g.setFill(ovBasemap ? P.text().deriveColor(0, 1, 1, 0.65) : Color.web("#cdd6e0", 0.65)); g.setFont(Font.font("IBM Plex Sans", FontWeight.NORMAL, 12)); g.setTextAlign(TextAlignment.CENTER);
            double[][] regions = {{52,13},{39,-98},{-14,-52},{8,20},{55,40},{36,138},{-25,134}};
            String[] rn = {"EUROPE","N. AMERICA","S. AMERICA","AFRICA","RUSSIA","ASIA","OCEANIA"};
            for (int i = 0; i < rn.length; i++) g.fillText(rn[i], lon2x(regions[i][1], RMX, RW), lat2y(regions[i][0], RMY, RH));
        } else if (ovSections || ovStates) drawRegionLabels(g, ovSections ? com.ars.fx.data.RegionLayers.sections() : com.ars.fx.data.RegionLayers.states(), RMX, RMY, RW, RH, RW / (vMaxLon - vMinLon));
        else if (ovBasemap) drawCountryLabels(g, RMX, RMY, RW, RH);

        double spotA = dimSpots() ? 0.28 : 1.0;
        paintedDots.clear(); limbDots.clear();
        for (MapSpot s : liveSpots()) {
            if (spotHidden(s.call())) continue;
            double x = lon2x(s.lon(), RMX, RW), y = lat2y(s.lat(), RMY, RH);
            paintedDots.add(new Dot(x, y, s));
            g.setStroke(Color.web("#0b0f14", 0.8 * spotA)); g.setLineWidth(2); g.strokeOval(x - 4.5, y - 4.5, 9, 9);
            spotDot(g, s, x, y, spotA);
        }

        double qx = lon2x(Q_LON, RMX, RW), qy = lat2y(Q_LAT, RMY, RH);
        g.setStroke(Color.web("#31bdda", 0.5)); g.setLineWidth(1.3); g.strokeOval(qx - 8, qy - 8, 16, 16);
        g.setFill(Color.web("#31bdda")); g.fillOval(qx - 4.5, qy - 4.5, 9, 9);
        g.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12)); g.setTextAlign(TextAlignment.CENTER);
        g.fillText(myCall, qx, qy - 13);

        // NCDXF beacon + "who's hearing me" overlays (equirectangular)
        Proj rect = (lat, lon) -> new double[]{lon2x(lon, RMX, RW), lat2y(lat, RMY, RH)};
        drawSpotPaths(g, rect);
        drawHeardBy(g, rect);
        drawBeacons(g, rect);
        drawTracks(g, rect);
        drawSessionQsos(g, rect);
        if (ovCallsigns) drawSpotLabels(g, spotA, aw, ah, qx, qy);
        kickBursts();
        g.restore();
    }
    static double lon2x(double lon, double rmx, double rw) { return rmx + ((lon - vMinLon) / (vMaxLon - vMinLon)) * rw; }
    static double lat2y(double lat, double rmy, double rh) { return rmy + ((vMaxLat - lat) / (vMaxLat - vMinLat)) * rh; }
    /** Great-circle destination point from (lat1,lon1) along bearing for distKm. */
    static double[] dest(double lat1, double lon1, double brgDeg, double distKm) {
        double R = 6371.0, d = distKm / R;
        double b = Math.toRadians(brgDeg), p1 = Math.toRadians(lat1), l1 = Math.toRadians(lon1);
        double p2 = Math.asin(Math.sin(p1) * Math.cos(d) + Math.cos(p1) * Math.sin(d) * Math.cos(b));
        double l2 = l1 + Math.atan2(Math.sin(b) * Math.sin(d) * Math.cos(p1), Math.cos(d) - Math.sin(p1) * Math.sin(p2));
        double lon = ((Math.toDegrees(l2) + 540) % 360) - 180;
        return new double[]{Math.toDegrees(p2), lon};
    }
    static void drawTerminator(GraphicsContext g, double rmx, double rmy, double rw, double rh) {
        double[] ss = com.ars.fx.data.SolarPosition.subSolar(); double decl = ss[0], ssLon = ss[1];
        double td = Math.tan(Math.toRadians(decl == 0 ? 0.001 : decl));

        // terminator polyline points (y clamped to the map rect)
        int n = 0; int N = (360 / 3) + 1;
        double[] xs = new double[N], ys = new double[N];
        for (int lon = -180; lon <= 180; lon += 3) {
            double termLat = Math.toDegrees(Math.atan(-Math.cos(Math.toRadians(lon - ssLon)) / td));
            xs[n] = lon2x(lon, rmx, rw);
            ys[n] = Math.max(rmy, Math.min(rmy + rh, lat2y(termLat, rmy, rh)));
            n++;
        }
        // Darken the NIGHT side (inside the gray line). Sun north (decl≥0) → night is
        // the southern hemisphere (below the curve); else the northern.
        boolean nightBelow = decl >= 0;
        g.setFill(ovBasemap ? P.night() : Color.web("#04060a", 0.5));
        g.beginPath(); g.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) g.lineTo(xs[i], ys[i]);
        if (nightBelow) { g.lineTo(rmx + rw, rmy + rh); g.lineTo(rmx, rmy + rh); }
        else            { g.lineTo(rmx + rw, rmy);      g.lineTo(rmx, rmy); }
        g.closePath(); g.fill();

        // the gray line itself
        g.setStroke(Color.web("#eebb58", 0.85)); g.setLineWidth(1.6); g.setLineDashes(6, 5);
        g.beginPath(); g.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) g.lineTo(xs[i], ys[i]);
        g.stroke(); g.setLineDashes(null);

        // the Sun — its sub-solar point (directly overhead: lat = declination, lon = sub-solar)
        double sx = lon2x(ssLon, rmx, rw), sy = lat2y(decl, rmy, rh);
        drawSun(g, sx, sy);
    }
    /** A small sun glyph (glow + disk + rays) at (x,y). */
    static void drawSun(GraphicsContext g, double x, double y) {
        g.setFill(Color.web("#ffd33d", 0.16)); g.fillOval(x - 17, y - 17, 34, 34);
        g.setFill(Color.web("#ffe27a", 0.28)); g.fillOval(x - 10, y - 10, 20, 20);
        g.setStroke(Color.web("#ffd84a", 0.9)); g.setLineWidth(1.4);
        for (int a = 0; a < 8; a++) {
            double ang = Math.toRadians(a * 45);
            g.strokeLine(x + Math.cos(ang) * 8.5, y + Math.sin(ang) * 8.5, x + Math.cos(ang) * 12.5, y + Math.sin(ang) * 12.5);
        }
        g.setFill(Color.web("#fff3b0")); g.fillOval(x - 6, y - 6, 12, 12);
        g.setStroke(Color.web("#ffcf3a")); g.setLineWidth(1.3); g.strokeOval(x - 6, y - 6, 12, 12);
    }

}
