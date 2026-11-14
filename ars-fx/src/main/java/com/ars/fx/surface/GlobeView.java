package com.ars.fx.surface;

import com.ars.fx.data.GlobeProjection;
import com.ars.fx.data.GlobeProjection.Pt;
import com.ars.fx.data.VectorBasemap;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A 3D globe: a textured sphere mesh for the Earth (land, borders and a graticule drawn from the Natural Earth data, in the colour
 * scheme's colours) and a second, slightly larger sphere carrying the night side, redrawn each minute. Everything else - markers, orbit lines,
 * footprints, the labels - is drawn on a 2D canvas laid over it, placed by {@link GlobeProjection}, so lines are as crisp as on the flat map,
 * labels are ordinary text, and a click is an ordinary hit test. Drag to turn it, wheel to zoom, double-click to centre.
 * <p>
 * One instance is kept by its owner and handed to each new layout, so the view you have turned to survives J-Sat's two-second rebuilds.
 */
final class GlobeView extends Pane {
    /** A labelled satellite, {@code altKm} above the surface; {@code ahead} is a point a little further along its track (NaN if unknown), so the icon can face its way. {@code dim} fades it; {@code ring} puts a light ring round it (visible now, or selected). */
    record Marker(String name, double lat, double lon, double altKm, Color color, boolean focus, boolean ring, boolean dim, double aheadLat, double aheadLon) {}
    /** A line along the sphere: {lat, lon} points at one height; {@code glow} is the extra width of a soft halo under it (0 for none), {@code arrows} puts direction chevrons along it. */
    record Poly(double[][] points, double altKm, Color color, double width, boolean dashed, double glow, boolean arrows) {}
    /** A line between two points that bows out between them (lat, lon, alt each end), such as a drop line or the link to a pass. */
    record Link(double lat1, double lon1, double alt1, double lat2, double lon2, double alt2, Color color, boolean dashed) {}

    /** Draws an owner's own layer over the globe, after the built-in ones. {@code fresh} is true when the owner asked for a refresh (its data may have changed) and false for a plain turn or zoom. */
    interface Painter { void paint(GraphicsContext g, GlobeProjection pr, double w, double h, boolean fresh); }

    static boolean supported() { return Platform.isSupported(ConditionalFeature.SCENE3D); }

    private static final int TEX_W = 2048, TEX_H = 1024;
    private final MeshView earth = new MeshView(sphereMesh(90, 180)), night = new MeshView(sphereMesh(60, 120));   // 2 degree rings: the edge is within a pixel of round at normal zoom, at a quarter of the triangles
    private final PhongMaterial earthMat = new PhongMaterial(Color.WHITE), nightMat = new PhongMaterial(Color.WHITE);
    private final Group world = new Group(earth, night);
    private final SubScene sub;
    private final Canvas overlay = new Canvas();
    private final Translate shift = new Translate();
    private final PointLight sunlight = new PointLight(Color.gray(0.62));   // fixed to the view, upper left, so the globe is lit from one side whichever way it is turned
    private final Rotate tilt = new Rotate(0, Rotate.X_AXIS), turn = new Rotate(0, Rotate.Y_AXIS);

    private double lat0 = 30, lon0 = 0, zoom = 1;
    private double qthLat = Double.NaN, qthLon = Double.NaN;
    private List<Marker> markers = List.of();
    private List<Poly> polys = List.of();
    private List<Link> links = List.of();
    private double[][] rings = new double[0][];           // {lat, lon, angDeg, r, g, b, strokeAlpha, fillAlpha, lineWidth}
    private Consumer<String> onPick = n -> {};
    private Runnable onMiss = () -> {};
    private final String surface;                           // whose colour scheme the globe follows: "sat", "map"
    private Painter painter;
    private BiConsumer<Double, Double> clickHook;
    private boolean stationDot = true, fresh = true;
    private String textureKey = "", nightKey = "";
    private Runnable released = () -> {};
    private double dragX, dragY; private boolean moved;
    private final List<double[]> hits = new ArrayList<>(); private final List<String> hitNames = new ArrayList<>();
    private MapPalette pal = MapPalette.forScheme("dark");
    private boolean centred;

    GlobeView(String surface) {
        this.surface = surface;
        setMinSize(0, 0); setPrefSize(0, 0);
        setStyle("-fx-background-color:-ars-bg;");
        // Shaded, so it reads as a ball: a dim fill plus one light from the upper left, on the Earth only. The night layer is a plain tint
        // (its own white ambient light) so its colour is unchanged.
        AmbientLight fill = new AmbientLight(Color.gray(0.42)), flat = new AmbientLight(Color.WHITE);
        fill.getScope().add(earth); sunlight.getScope().add(earth); flat.getScope().add(night);
        earthMat.setSpecularColor(Color.gray(0.16)); earthMat.setSpecularPower(38);   // a faint sheen on the lit side
        earth.setMaterial(earthMat);
        nightMat.setSpecularColor(null); night.setMaterial(nightMat);
        earth.setCullFace(CullFace.NONE); night.setCullFace(CullFace.NONE);        // the far side is hidden by the depth buffer, so winding does not matter
        world.getTransforms().setAll(shift, tilt, turn);                      // applied turn first, then tilt, then shift to the middle
        Group root = new Group(world, fill, sunlight, flat);
        sub = new SubScene(root, 10, 10, true, SceneAntialiasing.DISABLED);
        sub.setCamera(new PerspectiveCamera(false));
        sub.setFill(Color.TRANSPARENT);
        sub.setMouseTransparent(true);                                         // the pane takes the mouse; the 3D picking is not used
        overlay.setMouseTransparent(true);
        getChildren().addAll(sub, overlay);
        setOnMousePressed(e -> { dragX = e.getX(); dragY = e.getY(); moved = false; });
        setOnMouseDragged(e -> {
            double k = Math.toDegrees(1) / radius();                           // the surface under the pointer follows it
            double dx = e.getX() - dragX, dy = e.getY() - dragY;
            if (Math.abs(dx) + Math.abs(dy) > 2) moved = true;
            dragX = e.getX(); dragY = e.getY();
            lon0 = wrap(lon0 - dx * k); lat0 = Math.max(-88, Math.min(88, lat0 + dy * k));
            applyView(); interact();
        });
        setOnMouseReleased(e -> released.run());
        setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            zoom = Math.max(0.55, Math.min(8, zoom * Math.pow(1.0015, e.getDeltaY())));
            applyView(); interact();
        });
        setOnMouseClicked(e -> {
            if (moved) return;
            String hit = hit(e.getX(), e.getY());
            if (e.getClickCount() >= 2) {
                if (hit != null) centreOn(hit); else if (!Double.isNaN(qthLat)) { lat0 = qthLat; lon0 = qthLon; applyView(); redraw(); }
            } else if (clickHook != null) clickHook.accept(e.getX(), e.getY());
            else if (hit != null) onPick.accept(hit);
            else onMiss.run();
        });
    }

    /** True while the pointer is down on the globe, so the owner can hold off rebuilding the layout. */
    boolean busy() { return isPressed(); }
    /** Called when the pointer is let go, so a rebuild held off during the drag can run. */
    void onRelease(Runnable r) { released = r; }
    void onPick(Consumer<String> c) { onPick = c; }
    /** Called for a plain click that lands on no marker (and is not a drag or a double-click). */
    void onMiss(Runnable r) { onMiss = r; }
    /** Take a plain click (not a drag, not a double-click) at pane coordinates instead of the built-in marker pick. */
    void onClick(BiConsumer<Double, Double> c) { clickHook = c; }
    /** Draw the owner's own layer. */
    void painter(Painter p) { painter = p; }
    /** Whether to draw the white dot for the station (an owner that draws its own turns it off). */
    void stationDot(boolean on) { stationDot = on; }
    /** Show or hide the night side. */
    void night(boolean on) { night.setVisible(on); }
    /** The owner's data changed: check the textures and repaint, telling the painter it is a fresh paint. */
    void refresh() { refreshTextures(); fresh = true; redraw(); }
    void station(double lat, double lon) {
        if (lat == 0 && lon == 0) return;                         // the station is not known yet: wait for the real one
        qthLat = lat; qthLon = lon;
        if (!centred) { lat0 = Math.max(-60, Math.min(70, lat)); lon0 = lon; centred = true;
            applyView(); }
    }
    /** Replace what is drawn over the globe and repaint. */
    void show(List<Marker> m, List<Poly> p, List<Link> l, double[][] footprints) {
        markers = m; polys = p; links = l; rings = footprints;
        refreshTextures();
        redraw();
    }

    private static double wrap(double lon) { return ((lon + 540) % 360) - 180; }
    private double radius() { return Math.max(20, Math.min(0.42 * Math.min(getWidth(), getHeight()) * zoom, GlobeProjection.maxRadius(getHeight()))); }

    private void centreOn(String name) {
        for (Marker m : markers) if (m.name().equals(name)) { lat0 = Math.max(-88, Math.min(88, m.lat())); lon0 = m.lon(); applyView(); redraw(); return; }
    }
    private String hit(double x, double y) {
        double best = 18; String pick = null;
        for (int i = 0; i < hits.size(); i++) {
            double d = Math.hypot(x - hits.get(i)[0], y - hits.get(i)[1]);
            if (d < best) { best = d; pick = hitNames.get(i); }
        }
        return pick;
    }

    @Override protected void layoutChildren() {
        double w = getWidth(), h = getHeight();
        if (w < 2 || h < 2) return;
        sub.setWidth(w); sub.setHeight(h); overlay.setWidth(w); overlay.setHeight(h);
        applyView(); redraw();
    }

    /** Size and turn the spheres. The camera is the default one, so the plane through the globe's centre is 1:1 with pixels. */
    private void applyView() {
        double r = radius();
        earth.setScaleX(r); earth.setScaleY(r); earth.setScaleZ(r);
        double rn = r * 1.004; night.setScaleX(rn); night.setScaleY(rn); night.setScaleZ(rn);
        shift.setX(getWidth() / 2); shift.setY(getHeight() / 2);
        tilt.setAngle(lat0); turn.setAngle(lon0);
        double e = GlobeProjection.eyeDistance(getHeight());                      // the light sits in front of the globe, up and to the left
        sunlight.setTranslateX(getWidth() / 2 - 1.8 * r); sunlight.setTranslateY(getHeight() / 2 - 1.5 * r); sunlight.setTranslateZ(-1.1 * e);
    }

    /**
     * A unit sphere with latitude rings spaced evenly in angle and a plain equirectangular texture (u from longitude, v from latitude), positioned as
     * JavaFX's own {@code Sphere} is: longitude 0 faces a camera looking along +z, north is -y. JavaFX's {@code Sphere} spaces its rings evenly in
     * height instead, which leaves a handful of texture rows near each pole and smears the Arctic and Antarctic into streaks.
     */
    private static TriangleMesh sphereMesh(int rings, int segs) {
        TriangleMesh m = new TriangleMesh();
        float[] pts = new float[(rings + 1) * (segs + 1) * 3], tex = new float[(rings + 1) * (segs + 1) * 2];
        int p = 0, t = 0;
        for (int i = 0; i <= rings; i++) {
            double la = Math.toRadians(90 - 180.0 * i / rings);
            for (int j = 0; j <= segs; j++) {                                      // one extra column so the seam has its own texture coordinate
                double lo = Math.toRadians(-180 + 360.0 * j / segs);
                pts[p++] = (float) (Math.cos(la) * Math.sin(lo)); pts[p++] = (float) -Math.sin(la); pts[p++] = (float) (-Math.cos(la) * Math.cos(lo));
                tex[t++] = (float) j / segs; tex[t++] = (float) i / rings;
            }
        }
        int[] faces = new int[rings * segs * 2 * 6];
        int f = 0;
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                int a = i * (segs + 1) + j, b = a + 1, c = a + segs + 1, d = c + 1;
                faces[f++] = a; faces[f++] = a; faces[f++] = c; faces[f++] = c; faces[f++] = b; faces[f++] = b;
                faces[f++] = b; faces[f++] = b; faces[f++] = c; faces[f++] = c; faces[f++] = d; faces[f++] = d;
            }
        }
        m.getPoints().setAll(pts); m.getTexCoords().setAll(tex); m.getFaces().setAll(faces);
        return m;
    }

    // ── textures ──────────────────────────────────────────────────────────────
    private void refreshTextures() {
        String key = com.ars.fx.Themes.effectiveKey(surface);
        if (!key.equals(textureKey)) { pal = MapPalette.forScheme(key); earthMat.setDiffuseMap(landTexture(pal)); textureKey = key; nightKey = ""; }
        String nk = key + "/" + (System.currentTimeMillis() / 60_000);
        if (!nk.equals(nightKey)) { nightMat.setDiffuseMap(nightTexture(pal)); nightKey = nk; }
    }

    /** y in the texture for a latitude (plain equirectangular, which {@link #sphereMesh} maps onto the sphere). */
    private static double ty(double lat) { return (90 - lat) / 180 * TEX_H; }
    private static double tx(double lon) { return (lon + 180) / 360 * TEX_W; }

    private static Image landTexture(MapPalette p) {
        Canvas c = new Canvas(TEX_W, TEX_H);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(p.ocean()); g.fillRect(0, 0, TEX_W, TEX_H);
        // graticule every 30 degrees, bent to the sphere's row spacing
        g.setStroke(p.line()); g.setLineWidth(1);
        for (int lat = -60; lat <= 60; lat += 30) { double y = ty(lat); g.strokeLine(0, y, TEX_W, y); }
        for (int lon = -180; lon <= 180; lon += 30) { double x = tx(lon); g.strokeLine(x, 0, x, TEX_H); }
        g.setLineWidth(1.4);
        g.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        for (boolean ice : new boolean[]{false, true}) {
            g.setFill(ice ? p.ice() : p.land()); g.setStroke(ice ? p.iceCoast() : p.coast());
            g.beginPath();
            for (VectorBasemap.Ring ring : VectorBasemap.land().rings()) {
                if (isIce(ring) != ice) continue;
                double[] ll = ring.ll();
                for (int i = 0; i < ll.length; i += 2) {
                    double x = tx(ll[i]), y = ty(ll[i + 1]);
                    if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
                }
                g.closePath();
            }
            g.fill(); g.stroke();
        }
        g.setStroke(p.border()); g.setLineWidth(1.1);
        for (VectorBasemap.Ring ring : VectorBasemap.borders().rings()) {
            double[] ll = ring.ll();
            g.beginPath();
            for (int i = 0; i < ll.length; i += 2) { double x = tx(ll[i]), y = ty(ll[i + 1]); if (i == 0) g.moveTo(x, y); else g.lineTo(x, y); }
            g.stroke();
        }
        SnapshotParameters sp = new SnapshotParameters(); sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }
    private static boolean isIce(VectorBasemap.Ring r) {
        return r.maxLat() < -60 || (r.minLon() > -75 && r.maxLon() < -10 && r.minLat() > 58 && r.points() > 200);
    }

    /** The dark side as a see-through picture: full colour well after sunset, none by day, a soft edge at the terminator. */
    private static Image nightTexture(MapPalette p) {
        int w = 1024, h = 512;
        double[] ss = com.ars.fx.data.SolarPosition.subSolar();
        Color nc = p.night();
        double maxA = Math.min(0.78, nc.getOpacity() * 1.3);
        int rgb = ((int) Math.round(nc.getRed() * 255) << 16) | ((int) Math.round(nc.getGreen() * 255) << 8) | (int) Math.round(nc.getBlue() * 255);
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        int[] row = new int[w];
        double sinD = Math.sin(Math.toRadians(ss[0])), cosD = Math.cos(Math.toRadians(ss[0]));
        double[] cosLon = new double[w];                                            // per column, so the loop below is a multiply and an add
        for (int x = 0; x < w; x++) cosLon[x] = Math.cos(Math.toRadians((x + 0.5) / w * 360 - 180 - ss[1]));
        for (int y = 0; y < h; y++) {
            double lat = Math.toRadians(90 - (y + 0.5) / h * 180);
            double a0 = Math.sin(lat) * sinD, a1 = Math.cos(lat) * cosD;
            for (int x = 0; x < w; x++) {
                double sun = a0 + a1 * cosLon[x];                                   // sine of the sun's elevation; above 0 is day
                double a = Math.max(0, Math.min(1, (0.04 - sun) / 0.16)) * maxA;
                row[x] = ((int) Math.round(a * 255) << 24) | rgb;
            }
            pw.setPixels(0, y, w, 1, javafx.scene.image.PixelFormat.getIntArgbInstance(), row, 0, w);
        }
        return img;
    }

    // ── the overlay ───────────────────────────────────────────────────────────
    /** Redraw the overlay once per screen frame however many mouse events arrived since the last one (a drag sends well over a hundred a second). */
    private final javafx.animation.AnimationTimer redrawTimer = new javafx.animation.AnimationTimer() {
        @Override public void handle(long now) { stop(); redraw(); }
    };
    private void requestRedraw() { redrawTimer.start(); }

    /** True while the globe is being dragged or zoomed: the overlay is then drawn without its soft extras (halos, footprint tints, label outlines),
     *  which are the costly part to rasterise, and drawn in full again a moment after the last movement. */
    private boolean interacting;
    private final javafx.animation.PauseTransition settle = new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));
    { settle.setOnFinished(e -> { interacting = false; requestRedraw(); }); }
    private void interact() { interacting = true; settle.playFromStart(); requestRedraw(); }

    /** The glow around the globe, drawn once to an image: its colour stops are fractions of its radius, so the one image serves every zoom
     *  (scaled when drawn) and a frame no longer has to rasterise a gradient across most of the window. */
    private static javafx.scene.image.Image glowImage;
    private static boolean glowLight;
    private static javafx.scene.image.Image glow(boolean light) {
        if (glowImage != null && glowLight == light) return glowImage;
        int n = 512; double sil = n / 2 / 1.16;
        Color base = light ? Color.web("#4a86c5") : Color.web("#5fb0ff");
        Color none = new Color(base.getRed(), base.getGreen(), base.getBlue(), 0);
        Color peak = new Color(base.getRed(), base.getGreen(), base.getBlue(), light ? 0.30 : 0.42);
        Canvas c = new Canvas(n, n);
        c.getGraphicsContext2D().setFill(new RadialGradient(0, 0, n / 2.0, n / 2.0, n / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0, none), new Stop(0.90 * sil / (n / 2.0), none), new Stop(sil / (n / 2.0), peak), new Stop(1, none)));
        c.getGraphicsContext2D().fillOval(0, 0, n, n);
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters(); sp.setFill(Color.TRANSPARENT);
        glowImage = c.snapshot(sp, null); glowLight = light;
        return glowImage;
    }

    private void redraw() {
        double w = getWidth(), h = getHeight();
        if (w < 2 || h < 2) return;
        GraphicsContext g = overlay.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        GlobeProjection pr = new GlobeProjection(lat0, lon0, radius(), w, h);
        hits.clear(); hitNames.clear();

        double sil = pr.silhouetteRadius();
        // atmosphere: a glow just outside the edge and a thin bright rim just inside it
        double outer = sil * 1.16;
        g.drawImage(glow(pal.light()), w / 2 - outer, h / 2 - outer, outer * 2, outer * 2);
        // a thin edge so the globe reads against the background
        g.setStroke(pal.light() ? Color.web("#1c2733", 0.35) : Color.web("#9fc4e8", 0.45)); g.setLineWidth(1.5);
        g.strokeOval(w / 2 - sil, h / 2 - sil, sil * 2, sil * 2);

        for (double[] r : rings) {
            double[][] circle = GlobeProjection.circle(r[0], r[1], r[2], 90);
            if (r[7] > 0 && !interacting) tint(g, pr, r[0], r[1], circle, new Color(r[3], r[4], r[5], r[7]));
            g.setStroke(new Color(r[3], r[4], r[5], r[6])); g.setLineWidth(r.length > 8 ? r[8] : 1.6); g.setLineDashes(null);
            line(g, pr, circle, 0);
        }
        for (Poly p : polys) {
            if (p.glow() > 0 && !interacting) {                                    // a soft halo first, so the line reads as lit
                g.setStroke(new Color(p.color().getRed(), p.color().getGreen(), p.color().getBlue(), p.color().getOpacity() * 0.22));
                g.setLineWidth(p.width() + p.glow()); g.setLineDashes(null); g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                line(g, pr, p.points(), p.altKm());
                g.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            }
            g.setStroke(p.color()); g.setLineWidth(p.width());
            if (p.dashed()) g.setLineDashes(5, 5); else g.setLineDashes(null);
            line(g, pr, p.points(), p.altKm());
            if (p.arrows()) chevrons(g, pr, p);
        }
        g.setLineDashes(null);
        for (Link l : links) {
            g.setStroke(l.color()); g.setLineWidth(1.4);
            if (l.dashed()) g.setLineDashes(4, 4); else g.setLineDashes(null);
            int n = 24; Pt prev = null;
            double[] a = unit(l.lat1(), l.lon1(), l.alt1()), b = unit(l.lat2(), l.lon2(), l.alt2());
            for (int i = 0; i <= n; i++) {                                         // straight in space, so the projection bends it correctly
                double t = i / (double) n;
                double x = a[0] + (b[0] - a[0]) * t, y = a[1] + (b[1] - a[1]) * t, z = a[2] + (b[2] - a[2]) * t;
                double len = Math.sqrt(x * x + y * y + z * z);
                double lat = Math.toDegrees(Math.asin(z / len)), lon = Math.toDegrees(Math.atan2(y, x));
                Pt cur = pr.project(lat, lon, (len - 1) * GlobeProjection.EARTH_KM);
                if (prev != null && prev.visible() && cur.visible()) g.strokeLine(prev.x(), prev.y(), cur.x(), cur.y());
                prev = cur;
            }
        }
        g.setLineDashes(null);

        if (stationDot && !Double.isNaN(qthLat)) {
            Pt q = pr.project(qthLat, qthLon, 0);
            if (q.visible()) {
                g.setFill(Color.WHITE); g.fillOval(q.x() - 4, q.y() - 4, 8, 8);
                g.setStroke(Color.web("#ffffff", 0.55)); g.setLineWidth(1.2); g.strokeOval(q.x() - 7, q.y() - 7, 14, 14);
            }
        }
        for (Marker m : markers) {
            Pt p = pr.project(m.lat(), m.lon(), m.altKm());
            if (!p.visible()) continue;
            double a = m.focus() ? 1.0 : (m.dim() ? 0.3 : 0.95);
            if (m.focus()) { g.setStroke(Color.web("#ffffff", 0.55)); g.setLineWidth(2.5); g.strokeOval(p.x() - 13, p.y() - 13, 26, 26); }
            else if (m.ring()) { g.setStroke(Color.web("#e8eef4", a * 0.8)); g.setLineWidth(1.3); g.strokeOval(p.x() - 11, p.y() - 11, 22, 22); }
            double heading = Double.NaN;
            if (!Double.isNaN(m.aheadLat())) {
                Pt q = pr.project(m.aheadLat(), m.aheadLon(), m.altKm());
                if (q.visible()) heading = Math.atan2(q.y() - p.y(), q.x() - p.x());
            }
            SatIcon.draw(g, p.x(), p.y(), m.color(), m.focus() ? 22 : 17, a, heading);
            g.setFont(Font.font("JetBrains Mono", m.focus() ? FontWeight.BOLD : FontWeight.NORMAL, m.focus() ? 12 : 10)); g.setTextAlign(TextAlignment.LEFT);
            if (!interacting) { g.setStroke(Color.web("#0b0f14", 0.7 * a)); g.setLineWidth(3); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND); g.strokeText(m.name(), p.x() + 14, p.y() + 4); g.setLineJoin(javafx.scene.shape.StrokeLineJoin.MITER); }
            g.setFill(Color.web("#e8eef4", a)); g.fillText(m.name(), p.x() + 14, p.y() + 4);
            hits.add(new double[]{p.x(), p.y()}); hitNames.add(m.name());
        }
        if (painter != null) { painter.paint(g, pr, w, h, fresh); fresh = false; }
        g.setFill(Color.web("#9aa7b4", 0.8)); g.setFont(Font.font("JetBrains Mono", 10)); g.setTextAlign(TextAlignment.LEFT);
        g.fillText("drag to turn · wheel to zoom · double-click to centre", 12, h - 10);
    }

    private static double[] unit(double lat, double lon, double altKm) {
        double r = 1 + altKm / GlobeProjection.EARTH_KM, la = Math.toRadians(lat), lo = Math.toRadians(lon);
        return new double[]{r * Math.cos(la) * Math.cos(lo), r * Math.cos(la) * Math.sin(lo), r * Math.sin(la)};
    }

    /**
     * A wash of colour over the part of a footprint on the near side: a fan of triangles from its centre to each pair of neighbouring edge points that
     * can be seen, filled in one go so the pieces do not overlap and double up. Skipped when the centre is behind the globe.
     */
    private static void tint(GraphicsContext g, GlobeProjection pr, double lat, double lon, double[][] circle, Color c) {
        Pt mid = pr.project(lat, lon, 0);
        if (!mid.visible()) return;
        g.setFill(c); g.beginPath();
        Pt prev = pr.project(circle[0][0], circle[0][1], 0);
        for (int i = 1; i < circle.length; i++) {
            Pt cur = pr.project(circle[i][0], circle[i][1], 0);
            if (prev.visible() && cur.visible()) { g.moveTo(mid.x(), mid.y()); g.lineTo(prev.x(), prev.y()); g.lineTo(cur.x(), cur.y()); g.closePath(); }
            prev = cur;
        }
        g.fill();
    }

    /** Small arrowheads along a track, pointing the way the satellite is going, every few points. */
    private static void chevrons(GraphicsContext g, GlobeProjection pr, Poly p) {
        double[][] pts = p.points();
        g.setLineDashes(null); g.setLineWidth(1.6); g.setStroke(new Color(p.color().getRed(), p.color().getGreen(), p.color().getBlue(), Math.min(1, p.color().getOpacity() + 0.05)));
        for (int i = 6; i < pts.length - 1; i += 10) {
            Pt a = pr.project(pts[i][0], pts[i][1], p.altKm()), b = pr.project(pts[i + 1][0], pts[i + 1][1], p.altKm());
            if (!a.visible() || !b.visible()) continue;
            double ang = Math.atan2(b.y() - a.y(), b.x() - a.x()), cx = (a.x() + b.x()) / 2, cy = (a.y() + b.y()) / 2, L = 5;
            g.strokeLine(cx - Math.cos(ang - 0.6) * L, cy - Math.sin(ang - 0.6) * L, cx, cy);
            g.strokeLine(cx - Math.cos(ang + 0.6) * L, cy - Math.sin(ang + 0.6) * L, cx, cy);
        }
    }

    /** A polyline of {lat, lon} points, broken wherever a point is behind the globe. */
    private static void line(GraphicsContext g, GlobeProjection pr, double[][] pts, double altKm) {
        if (pts == null) return;
        boolean open = false;
        for (double[] p : pts) {
            Pt s = pr.project(p[0], p[1], altKm);
            if (!s.visible()) { if (open) { g.stroke(); open = false; } continue; }
            if (!open) { g.beginPath(); g.moveTo(s.x(), s.y()); open = true; } else g.lineTo(s.x(), s.y());
        }
        if (open) g.stroke();
    }
}
