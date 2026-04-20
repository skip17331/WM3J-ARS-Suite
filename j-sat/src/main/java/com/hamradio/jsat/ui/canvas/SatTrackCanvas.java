package com.hamradio.jsat.ui.canvas;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.model.SatelliteDefinition;
import com.hamradio.jsat.model.SatelliteState;
import com.hamradio.jsat.service.config.JsatSettings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * World map canvas rendering satellite ground tracks, footprints,
 * sub-satellite points, and the day/night terminator.
 * Uses equirectangular (plate carrée) projection.
 */
public class SatTrackCanvas extends Pane {

    private final ServiceRegistry services;
    private final Canvas canvas;
    private final Image  mapImage;

    // Satellite colors by index
    private static final Color[] SAT_COLORS = {
        Color.web("#00e5ff"), Color.web("#ff4081"), Color.web("#69f0ae"),
        Color.web("#ffea00"), Color.web("#ff6d00"), Color.web("#d500f9"),
        Color.web("#76ff03"), Color.web("#f50057")
    };

    public SatTrackCanvas(ServiceRegistry services) {
        this.services = services;
        this.canvas   = new Canvas();
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        Image img = null;
        try (InputStream in = getClass().getResourceAsStream("/data/world-map.jpg")) {
            if (in != null) img = new Image(in);
        } catch (Exception ignored) {}
        this.mapImage = (img != null && !img.isError()) ? img : null;
    }

    /** Called from animation loop — renders one frame. */
    public void render() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        JsatSettings s = services.getSettings();

        // ── Base map ──────────────────────────────────────────────────────────
        if (mapImage != null) {
            gc.drawImage(mapImage, 0, 0, w, h);
        } else {
            gc.setFill(Color.web("#0a1628"));
            gc.fillRect(0, 0, w, h);
            drawContinents(gc, w, h);
            drawGraticule(gc, w, h);
        }

        // ── Day/night terminator ──────────────────────────────────────────────
        drawTerminator(gc, w, h);

        // ── Satellite states ──────────────────────────────────────────────────
        Map<String, SatelliteState> states = services.tracker.getCurrentStates();
        int colorIdx = 0;
        for (Map.Entry<String, SatelliteState> entry : states.entrySet()) {
            Color color = SAT_COLORS[colorIdx % SAT_COLORS.length];
            colorIdx++;
            drawSatellite(gc, w, h, entry.getValue(), color, s, entry.getKey());
        }

        // ── Observer crosshair ───────────────────────────────────────────────
        drawObserver(gc, w, h, s.qthLat, s.qthLon);
    }

    // ── Drawing helpers ────────────────────────────────────────────────────────

    private void drawSatellite(GraphicsContext gc, double w, double h,
                               SatelliteState state, Color color,
                               JsatSettings s, String name) {
        double px = lonToX(state.lonDeg, w);
        double py = latToY(state.latDeg, h);

        // Footprint circle
        if (s.showFootprint && state.footprintRadiusDeg > 0) {
            double fpW = state.footprintRadiusDeg / 180.0 * w * 2;
            double fpH = state.footprintRadiusDeg / 90.0  * h;
            gc.setStroke(color.deriveColor(0, 1, 1, 0.35));
            gc.setLineWidth(1.0);
            gc.strokeOval(px - fpW / 2, py - fpH / 2, fpW, fpH);
            gc.setFill(color.deriveColor(0, 1, 1, 0.08));
            gc.fillOval(px - fpW / 2, py - fpH / 2, fpW, fpH);
        }

        // Ground track
        if (s.showGroundTrack && state.groundTrack != null) {
            gc.setStroke(color.deriveColor(0, 1, 1, 0.60));
            gc.setLineWidth(1.2);
            double prevX = -999, prevY = -999;
            for (double[] pt : state.groundTrack) {
                if (pt[0] == 0 && pt[1] == 0) { prevX = -999; continue; }
                double tx = lonToX(pt[1], w);
                double ty = latToY(pt[0], h);
                if (prevX > -999 && Math.abs(tx - prevX) < w * 0.4) {
                    gc.strokeLine(prevX, prevY, tx, ty);
                }
                prevX = tx; prevY = ty;
            }
        }

        // Satellite dot
        boolean selected = name.equals(services.tracker.getSelectedSatellite());
        double dotR = selected ? 7 : 5;
        gc.setFill(color);
        gc.fillOval(px - dotR, py - dotR, dotR * 2, dotR * 2);

        if (selected) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(px - dotR - 2, py - dotR - 2, (dotR + 2) * 2, (dotR + 2) * 2);
        }

        // Label
        gc.setFill(color);
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 11));
        gc.fillText(state.name, px + dotR + 3, py + 4);

        // Elevation badge if visible
        if (state.elevationDeg > 0) {
            String elStr = String.format("%.0f°", state.elevationDeg);
            gc.setFill(Color.web("#ffffff", 0.85));
            gc.setFont(Font.font("Liberation Mono", 10));
            gc.fillText(elStr, px + dotR + 3, py + 15);
        }
    }

    private void drawObserver(GraphicsContext gc, double w, double h, double lat, double lon) {
        double px = lonToX(lon, w);
        double py = latToY(lat, h);
        double r  = 6;
        gc.setFill(Color.web("#ffdd00"));
        gc.fillOval(px - r, py - r, r * 2, r * 2);
        gc.setStroke(Color.web("#ff8800"));
        gc.setLineWidth(2);
        gc.strokeOval(px - r, py - r, r * 2, r * 2);
        // Crosshairs
        gc.setStroke(Color.web("#ffdd00", 0.6));
        gc.setLineWidth(1);
        gc.strokeLine(px - 14, py, px - r - 2, py);
        gc.strokeLine(px + r + 2, py, px + 14, py);
        gc.strokeLine(px, py - 14, px, py - r - 2);
        gc.strokeLine(px, py + r + 2, px, py + 14);
    }

    private void drawTerminator(GraphicsContext gc, double w, double h) {
        Instant now = Instant.now();
        // Sun declination and GHA
        double[] sunGeoPos = sunPosition(now);
        double sunLat = sunGeoPos[0];
        double sunLon = sunGeoPos[1];

        // Render terminator as a semi-transparent night overlay
        // We shade pixels where solar elevation < 0
        int W = (int) w, H = (int) h;
        gc.setGlobalAlpha(0.35);
        gc.setFill(Color.web("#000020"));

        for (int px = 0; px < W; px += 3) {
            double lon = (px / w) * 360.0 - 180.0;
            for (int py = 0; py < H; py += 3) {
                double lat = 90.0 - (py / h) * 180.0;
                double elev = solarElevation(lat, lon, sunLat, sunLon);
                if (elev < -6.0) { // astronomical twilight
                    gc.fillRect(px, py, 3, 3);
                }
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawContinents(GraphicsContext gc, double w, double h) {
        // Filled landmass polygons
        gc.setFill(Color.web("#1a3a1a", 0.85));
        gc.setStroke(Color.web("#2d5a2d", 0.9));
        gc.setLineWidth(0.8);
        for (double[] poly : WorldOutlines.ALL) {
            if (poly.length < 4) continue;
            gc.beginPath();
            for (int i = 0; i < poly.length - 1; i += 2) {
                double px = lonToX(poly[i],     w);
                double py = latToY(poly[i + 1], h);
                if (i == 0) gc.moveTo(px, py);
                else        gc.lineTo(px, py);
            }
            gc.closePath();
            gc.fill();
            gc.stroke();
        }
    }

    private void drawGraticule(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web("#1a3050", 0.4));
        gc.setLineWidth(0.5);
        for (int lon = -180; lon <= 180; lon += 30) {
            double x = lonToX(lon, w);
            gc.strokeLine(x, 0, x, h);
        }
        for (int lat = -90; lat <= 90; lat += 30) {
            double y = latToY(lat, h);
            gc.strokeLine(0, y, w, y);
        }
    }

    // ── Coordinate helpers ─────────────────────────────────────────────────────

    private static double lonToX(double lon, double w) { return (lon + 180.0) / 360.0 * w; }
    private static double latToY(double lat, double h) { return (90.0 - lat)  / 180.0 * h; }

    // ── Astronomy helpers ──────────────────────────────────────────────────────

    /** Returns [sunDecDeg, sunGhaDeg]. */
    private static double[] sunPosition(Instant when) {
        double jd = when.getEpochSecond() / 86400.0 + 2440587.5;
        double T  = (jd - 2451545.0) / 36525.0;
        double L  = 280.46646 + 36000.76983 * T;
        double M  = Math.toRadians(357.52911 + 35999.05029 * T);
        double C  = (1.914602 - 0.004817 * T) * Math.sin(M) + 0.019993 * Math.sin(2 * M);
        double slon = Math.toRadians(L + C);
        double eps  = Math.toRadians(23.439 - 0.0000004 * T);
        double dec  = Math.toDegrees(Math.asin(Math.sin(eps) * Math.sin(slon)));
        double gha  = (280.46061837 + 360.98564736629 * (jd - 2451545.0)) % 360.0
                      - Math.toDegrees(Math.atan2(Math.cos(eps) * Math.sin(slon), Math.cos(slon)));
        return new double[]{ dec, gha };
    }

    private static double solarElevation(double lat, double lon, double sunLat, double sunLon) {
        double latR = Math.toRadians(lat);
        double lonR = Math.toRadians(lon);
        double sLR  = Math.toRadians(sunLat);
        double sLoR = Math.toRadians(sunLon);
        return Math.toDegrees(Math.asin(
            Math.sin(latR) * Math.sin(sLR) +
            Math.cos(latR) * Math.cos(sLR) * Math.cos(lonR - sLoR)));
    }
}
