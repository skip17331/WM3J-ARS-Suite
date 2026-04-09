package com.hamclock.ui.overlays;

import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.astronomy.NightMask;
import com.hamclock.service.config.Settings;
import com.hamclock.service.dx.DxSpot;
import com.hamclock.service.geomag.GeomagneticAlert;
import com.hamclock.service.lightning.LightningData;
import com.hamclock.service.radar.RadarOverlay;
import com.hamclock.service.satellite.SatelliteData;
import com.hamclock.service.surface.SurfaceConditions;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayInputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * JavaFX Canvas that renders the world map with all overlays.
 *
 * Rendering order (back to front):
 *  1. Base world map
 *  2. Night shade / grayline
 *  3. Surface conditions
 *  4. Radar
 *  5. Lightning
 *  6. Aurora
 *  7. Geomagnetic alert ring
 *  8. Weather / Tropo
 *  9. CQ zone grid
 * 10. ITU zone grid
 * 11. Maidenhead grid squares
 * 12. Satellite ground tracks
 * 13. DX spots
 * 14. Home QTH marker
 * 15. Subsolar point
 */
public class WorldMapCanvas extends Pane {

    private final ServiceRegistry services;
    private final Canvas canvas;
    private Image worldMapImage;

    // Cached night mask - recomputed every ~30s
    private NightMask nightMask;
    private long lastNightMaskMs = 0;
    private static final long NIGHT_MASK_INTERVAL_MS = 30_000;

    // Click callback for DX spot selection
    private Consumer<DxSpot> dxSpotClickCallback;

    // Last known spot list for hit-testing
    private List<DxSpot> lastSpots = new ArrayList<>();

    public WorldMapCanvas(ServiceRegistry services) {
        this.services = services;
        this.canvas   = new Canvas();

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        canvas.widthProperty().addListener(obs -> redraw());
        canvas.heightProperty().addListener(obs -> redraw());

        getChildren().add(canvas);

        // Click handler for DX spot selection
        canvas.setOnMouseClicked(e -> handleMapClick(e.getX(), e.getY()));

        try {
            worldMapImage = new Image(
                getClass().getResourceAsStream("/images/world_map.jpg"),
                0, 0, true, true);
        } catch (Exception ex) {
            worldMapImage = null;
        }
    }

    public void setDxSpotClickCallback(Consumer<DxSpot> callback) {
        this.dxSpotClickCallback = callback;
    }

    public void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        Settings s = services.getSettings();

        // 1. Base map
        if (s.isShowWorldMap()) drawBaseMap(gc, w, h);
        else { gc.setFill(Color.web("#0a1428")); gc.fillRect(0, 0, w, h); }

        // 2. Night shade / grayline
        if (s.isShowGrayline()) drawNightShade(gc, w, h);

        // 3. Surface conditions
        if (s.isShowSurfaceConditions()) drawSurfaceConditions(gc, w, h);

        // 4. Radar
        if (s.isShowRadarOverlay()) drawRadarOverlay(gc, w, h);

        // 5. Lightning
        if (s.isShowLightningOverlay()) drawLightningOverlay(gc, w, h);

        // 6. Aurora
        if (s.isShowAuroraOverlay()) drawAuroraOverlay(gc, w, h);

        // 7. Geomagnetic alert ring
        if (s.isShowGeomagneticAlerts()) drawGeomagneticAlerts(gc, w, h);

        // 8. Weather / Tropo (image-based overlays)
        // Weather and Tropo are PNG bytes — rendered via Image if present
        // (these providers return byte[] PNGs, so we skip grid rendering here)

        // 9. CQ zone grid
        if (s.isShowCqZones()) drawCqZoneGrid(gc, w, h);

        // 10. ITU zone grid
        if (s.isShowItuZones()) drawItuZoneGrid(gc, w, h);

        // 11. Maidenhead grid squares
        if (s.isShowGridSquares()) drawGridSquares(gc, w, h);

        // 12. Satellite ground tracks
        if (s.isShowSatelliteTracking()) drawSatellites(gc, w, h);

        // 13. DX spots
        if (s.isShowDxSpots()) {
            drawDxSpots(gc, w, h, s);
            drawModeLegend(gc, w, h);
        }

        // 14. Home QTH marker
        drawQthMarker(gc, w, h, s);

        // 15. Subsolar point
        drawSubsolarPoint(gc, w, h);
    }

    // ── Base map ─────────────────────────────────────────────────────────────

    private void drawBaseMap(GraphicsContext gc, double w, double h) {
        if (worldMapImage != null && !worldMapImage.isError()) {
            gc.drawImage(worldMapImage, 0, 0, w, h);
        } else {
            gc.setFill(Color.web("#1a3a5c"));
            gc.fillRect(0, 0, w, h);
            gc.setStroke(Color.web("#2a4a6c", 0.5));
            gc.setLineWidth(0.5);
            gc.strokeLine(0, h / 2, w, h / 2);
            gc.strokeLine(w / 2, 0, w / 2, h);
        }
    }

    // ── Night shade ───────────────────────────────────────────────────────────

    private void drawNightShade(GraphicsContext gc, double w, double h) {
        long now = System.currentTimeMillis();
        if (nightMask == null || now - lastNightMaskMs > NIGHT_MASK_INTERVAL_MS) {
            nightMask = services.graylineService.computeNightMask(ZonedDateTime.now(ZoneOffset.UTC));
            lastNightMaskMs = now;
        }

        boolean[][] mask = nightMask.getMask();
        int lonSteps = nightMask.getLonSteps();
        int latSteps = nightMask.getLatSteps();
        double colW = w / lonSteps;
        double rowH = h / latSteps;

        gc.save();
        gc.setFill(Color.rgb(0, 0, 30, 0.55));
        for (int li = 0; li < lonSteps; li++) {
            for (int lj = 0; lj < latSteps; lj++) {
                if (mask[li][lj]) gc.fillRect(li * colW, lj * rowH, colW + 1, rowH + 1);
            }
        }
        gc.restore();
        drawTerminatorLine(gc, w, h);
    }

    private void drawTerminatorLine(GraphicsContext gc, double w, double h) {
        if (nightMask == null) return;
        boolean[][] mask = nightMask.getMask();
        int lonSteps = nightMask.getLonSteps();
        int latSteps = nightMask.getLatSteps();
        double colW = w / lonSteps;
        double rowH = h / latSteps;

        gc.save();
        gc.setStroke(Color.web("#ffd700", 0.7));
        gc.setLineWidth(1.5);
        for (int li = 0; li < lonSteps - 1; li++) {
            for (int lj = 0; lj < latSteps - 1; lj++) {
                boolean here  = mask[li][lj];
                boolean right = mask[li + 1][lj];
                boolean down  = mask[li][lj + 1];
                if (here != right || here != down) {
                    double px = li * colW + colW / 2;
                    double py = lj * rowH + rowH / 2;
                    gc.strokeOval(px - 0.5, py - 0.5, 1, 1);
                }
            }
        }
        gc.restore();
    }

    // ── Surface conditions ────────────────────────────────────────────────────

    private void drawSurfaceConditions(GraphicsContext gc, double w, double h) {
        SurfaceConditions data = services.surfaceConditionsProvider.getCached();
        if (data == null) return;

        double[][] grid = (data.getDisplayMode() == SurfaceConditions.DisplayMode.TEMPERATURE)
            ? data.getTemperatureGrid() : data.getPressureGrid();
        if (grid == null) return;

        int lonSteps = grid.length;
        int latSteps = grid[0].length;
        double colW = w / lonSteps;
        double rowH = h / latSteps;

        gc.save();
        for (int li = 0; li < lonSteps; li++) {
            for (int lj = 0; lj < latSteps; lj++) {
                double val = grid[li][lj];
                Color c = temperatureColor(val);
                gc.setFill(c);
                gc.fillRect(li * colW, lj * rowH, colW + 1, rowH + 1);
            }
        }
        gc.restore();
    }

    /** Map temperature (−40 to +40 °C) to blue→cyan→green→yellow→red */
    private Color temperatureColor(double celsius) {
        double norm = Math.max(0, Math.min(1, (celsius + 40) / 80.0));
        double alpha = 0.40;
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(0 + 0 * t), (int)(0 + 100 * t), (int)(180 + 75 * t), alpha);
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(0 + 0 * t), (int)(100 + 130 * t), (int)(255 - 255 * t), alpha);
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(0 + 255 * t), (int)(230 - 0 * t), 0, alpha);
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb(255, (int)(230 - 200 * t), 0, alpha);
        }
    }

    // ── Radar ─────────────────────────────────────────────────────────────────

    private void drawRadarOverlay(GraphicsContext gc, double w, double h) {
        RadarOverlay data = services.radarProvider.getCached();
        if (data == null) return;

        if (data.hasPng()) {
            try {
                Image img = new Image(new ByteArrayInputStream(data.getPngBytes()));
                gc.save();
                gc.setGlobalAlpha(0.65);
                gc.drawImage(img, 0, 0, w, h);
                gc.restore();
            } catch (Exception ignored) {}
            return;
        }

        double[][] grid = data.getIntensity();
        if (grid == null) return;

        int lonSteps = grid.length;
        int latSteps = grid[0].length;
        double colW = w / lonSteps;
        double rowH = h / latSteps;

        gc.save();
        for (int li = 0; li < lonSteps; li++) {
            for (int lj = 0; lj < latSteps; lj++) {
                double val = grid[li][lj];
                if (val > 0.05) {
                    Color c = radarColor(val);
                    gc.setFill(c);
                    gc.fillRect(li * colW, lj * rowH, colW + 1, rowH + 1);
                }
            }
        }
        gc.restore();
    }

    /** Radar: green (light) → yellow → red (heavy) → purple (extreme) */
    private Color radarColor(double val) {
        if (val < 0.25) return Color.rgb(0, 200, 0, val * 2.4);
        if (val < 0.5)  return Color.rgb(200, 200, 0, 0.6);
        if (val < 0.75) return Color.rgb(220, 80, 0, 0.7);
        return Color.rgb(150, 0, 200, 0.75);
    }

    // ── Lightning ─────────────────────────────────────────────────────────────

    private void drawLightningOverlay(GraphicsContext gc, double w, double h) {
        LightningData data = services.lightningProvider.getCached();
        if (data == null) return;

        double[][] density = data.getDensityGrid();
        if (density != null) {
            int lonSteps = density.length;
            int latSteps = density[0].length;
            double colW = w / lonSteps;
            double rowH = h / latSteps;

            gc.save();
            for (int li = 0; li < lonSteps; li++) {
                for (int lj = 0; lj < latSteps; lj++) {
                    double val = density[li][lj];
                    if (val > 0.05) {
                        gc.setFill(Color.rgb(255, 255, 0, val * 0.7));
                        gc.fillRect(li * colW, lj * rowH, colW + 1, rowH + 1);
                    }
                }
            }
            gc.restore();
        }

        // Draw individual strike dots (most recent strikes)
        List<LightningData.Strike> strikes = data.getStrikes();
        if (strikes != null && !strikes.isEmpty()) {
            gc.save();
            gc.setFill(Color.web("#ffffaa", 0.8));
            gc.setStroke(Color.web("#ffffff", 0.5));
            gc.setLineWidth(0.5);
            int limit = Math.min(200, strikes.size());
            for (int i = strikes.size() - limit; i < strikes.size(); i++) {
                LightningData.Strike s = strikes.get(i);
                double px = lonToX(s.lon(), w);
                double py = latToY(s.lat(), h);
                gc.fillOval(px - 1.5, py - 1.5, 3, 3);
            }
            gc.restore();
        }
    }

    // ── Aurora ────────────────────────────────────────────────────────────────

    private void drawAuroraOverlay(GraphicsContext gc, double w, double h) {
        var auroraData = services.auroraProvider.getCached();
        if (auroraData == null || auroraData.getIntensity() == null) return;

        double[][] intensity = auroraData.getIntensity();
        int lonSteps = intensity.length;
        if (lonSteps == 0) return;
        int latSteps = intensity[0].length;

        double colW = w / lonSteps;
        double rowH = h / latSteps;

        gc.save();
        for (int li = 0; li < lonSteps; li++) {
            for (int lj = 0; lj < latSteps; lj++) {
                double val = intensity[li][lj];
                if (val > 0.05) {
                    Color c;
                    if (val < 0.33)      c = Color.rgb(0, 200, 80, val * 0.8);
                    else if (val < 0.66) c = Color.rgb(100, 200, 0, val * 0.8);
                    else                 c = Color.rgb(200, 100, 0, val * 0.8);
                    gc.setFill(c);
                    gc.fillRect(li * colW, lj * rowH, colW + 1, rowH + 1);
                }
            }
        }
        gc.restore();
    }

    // ── Geomagnetic alert ring ────────────────────────────────────────────────

    private void drawGeomagneticAlerts(GraphicsContext gc, double w, double h) {
        GeomagneticAlert alert = services.geomagneticAlertProvider.getCached();
        if (alert == null || alert.getLevel() == GeomagneticAlert.Level.NONE) return;

        double visLat = alert.getAuroraVisibleLatitude();

        // Draw two filled arcs (N and S polar caps) at the aurora visibility latitude
        gc.save();

        Color ringColor = switch (alert.getLevel()) {
            case WATCH   -> Color.rgb(255, 200, 0, 0.35);   // yellow
            case WARNING -> Color.rgb(255, 120, 0, 0.40);   // orange
            case ALERT   -> Color.rgb(255, 40,  40, 0.45);  // red
            default      -> Color.rgb(0, 200, 80, 0.25);
        };

        gc.setStroke(ringColor.deriveColor(0, 1, 1, 2.0));
        gc.setLineWidth(2.5);

        // North polar ring at +visLat
        double northY = latToY(visLat, h);
        gc.setLineDashes(6, 4);
        gc.strokeLine(0, northY, w, northY);

        // South polar ring at -visLat
        double southY = latToY(-visLat, h);
        gc.strokeLine(0, southY, w, southY);

        gc.setLineDashes(null);

        // Shade poleward of visibility latitude
        gc.setFill(ringColor);
        gc.fillRect(0, 0, w, northY);                   // N polar cap
        gc.fillRect(0, southY, w, h - southY);          // S polar cap

        // Label
        gc.setFill(Color.web("#ffd700", 0.9));
        gc.setFont(mapFontBold(0.85));
        String label = "Kp " + String.format("%.0f", alert.getKpIndex()) +
                       "  " + alert.getGScale() + "  " + alert.getSummary();
        gc.fillText(label, 6, northY - 4);

        gc.restore();
    }

    // ── CQ Zone grid ──────────────────────────────────────────────────────────

    /**
     * Draws approximate CQ zone boundary lines.
     * Uses a simplified grid scan — draws a line wherever the CQ zone changes
     * between adjacent 5° grid cells.
     */
    private void drawCqZoneGrid(GraphicsContext gc, double w, double h) {
        gc.save();
        gc.setStroke(Color.web("#00aaff", 0.5));
        gc.setLineWidth(0.8);
        gc.setLineDashes(4, 3);

        int step = 5; // degrees
        gc.setFont(mapFont(0.69));
        gc.setFill(Color.web("#00aaff", 0.7));

        for (int lon = -180; lon < 180; lon += step) {
            for (int lat = -85; lat < 90; lat += step) {
                int here  = services.zoneLookupService.toCqZone(lat, lon);
                int right = services.zoneLookupService.toCqZone(lat, lon + step);
                int up    = services.zoneLookupService.toCqZone(lat + step, lon);

                double x1 = lonToX(lon, w);
                double y1 = latToY(lat, h);
                double x2 = lonToX(lon + step, w);
                double y2 = latToY(lat + step, h);

                if (here != right) gc.strokeLine(x2, y1, x2, y2);
                if (here != up)    gc.strokeLine(x1, y1, x2, y1);

                // Zone number label in center of cell (only draw once per zone change)
                if (here != right || here != up) {
                    gc.fillText(String.valueOf(here),
                        x1 + (x2 - x1) * 0.3,
                        y1 + (y2 - y1) * 0.6);
                }
            }
        }
        gc.setLineDashes(null);
        gc.restore();
    }

    // ── ITU Zone grid ─────────────────────────────────────────────────────────

    private void drawItuZoneGrid(GraphicsContext gc, double w, double h) {
        gc.save();
        gc.setStroke(Color.web("#ff9900", 0.45));
        gc.setLineWidth(0.8);
        gc.setLineDashes(3, 4);

        int step = 5;
        gc.setFont(mapFont(0.69));
        gc.setFill(Color.web("#ff9900", 0.65));

        for (int lon = -180; lon < 180; lon += step) {
            for (int lat = -85; lat < 90; lat += step) {
                int here  = services.zoneLookupService.toItuZone(lat, lon);
                int right = services.zoneLookupService.toItuZone(lat, lon + step);
                int up    = services.zoneLookupService.toItuZone(lat + step, lon);

                double x1 = lonToX(lon, w);
                double y1 = latToY(lat, h);
                double x2 = lonToX(lon + step, w);
                double y2 = latToY(lat + step, h);

                if (here != right) gc.strokeLine(x2, y1, x2, y2);
                if (here != up)    gc.strokeLine(x1, y1, x2, y1);

                if (here != right || here != up) {
                    gc.fillText(String.valueOf(here),
                        x1 + (x2 - x1) * 0.6,
                        y1 + (y2 - y1) * 0.8);
                }
            }
        }
        gc.setLineDashes(null);
        gc.restore();
    }

    // ── Maidenhead grid squares ───────────────────────────────────────────────

    /**
     * Draws Maidenhead 4-character field/square grid lines and labels.
     * Each field is 20°×10°. Each square within a field is 2°×1°.
     */
    private void drawGridSquares(GraphicsContext gc, double w, double h) {
        gc.save();

        // Field boundaries (20° lon × 10° lat) — brighter
        gc.setStroke(Color.web("#00ff88", 0.55));
        gc.setLineWidth(1.2);
        for (int lon = -180; lon <= 180; lon += 20) {
            gc.strokeLine(lonToX(lon, w), 0, lonToX(lon, w), h);
        }
        for (int lat = -90; lat <= 90; lat += 10) {
            gc.strokeLine(0, latToY(lat, h), w, latToY(lat, h));
        }

        // Square boundaries (2° lon × 1° lat) — dimmer
        gc.setStroke(Color.web("#00ff88", 0.20));
        gc.setLineWidth(0.4);
        for (int lon = -180; lon <= 180; lon += 2) {
            if (lon % 20 != 0) gc.strokeLine(lonToX(lon, w), 0, lonToX(lon, w), h);
        }
        for (int lat = -90; lat <= 90; lat += 1) {
            if (lat % 10 != 0) gc.strokeLine(0, latToY(lat, h), w, latToY(lat, h));
        }

        // Field labels (e.g. "FN", "EM", "JO")
        gc.setFill(Color.web("#00ff88", 0.70));
        gc.setFont(mapFontBold(0.77));
        for (int lon = -180; lon < 180; lon += 20) {
            for (int lat = -90; lat < 90; lat += 10) {
                double centerLon = lon + 10;
                double centerLat = lat + 5;
                String grid = services.zoneLookupService.toGridSquare(centerLat, centerLon);
                String label = grid.substring(0, 2).toUpperCase();
                gc.fillText(label, lonToX(lon + 1, w), latToY(lat + 8, h));
            }
        }

        gc.restore();
    }

    // ── Satellite ground tracks ───────────────────────────────────────────────

    private void drawSatellites(GraphicsContext gc, double w, double h) {
        SatelliteData data = services.satelliteProvider.getCached();
        if (data == null || data.getSatellites().isEmpty()) return;

        gc.save();

        Color[] satColors = {
            Color.web("#00ffff"),   // cyan
            Color.web("#ff88ff"),   // magenta
            Color.web("#88ff00"),   // yellow-green
            Color.web("#ff8800"),   // orange
            Color.web("#8888ff"),   // lavender
        };

        int colorIdx = 0;
        for (SatelliteData.SatPosition sat : data.getSatellites()) {
            Color c = satColors[colorIdx % satColors.length];
            colorIdx++;

            // Draw ground track
            List<double[]> track = sat.groundTrack();
            if (track != null && track.size() > 1) {
                gc.setStroke(c.deriveColor(0, 1, 1, 0.4));
                gc.setLineWidth(1.0);
                gc.setLineDashes(3, 3);

                double prevX = lonToX(track.get(0)[1], w);
                double prevY = latToY(track.get(0)[0], h);
                for (int i = 1; i < track.size(); i++) {
                    double nx = lonToX(track.get(i)[1], w);
                    double ny = latToY(track.get(i)[0], h);
                    // Don't draw across the antimeridian
                    if (Math.abs(nx - prevX) < w * 0.4) {
                        gc.strokeLine(prevX, prevY, nx, ny);
                    }
                    prevX = nx;
                    prevY = ny;
                }
                gc.setLineDashes(null);
            }

            // Current position dot
            double px = lonToX(sat.lon(), w);
            double py = latToY(sat.lat(), h);

            gc.setFill(c);
            gc.fillOval(px - 4, py - 4, 8, 8);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(0.8);
            gc.strokeOval(px - 4, py - 4, 8, 8);

            // Satellite name label
            gc.setFill(c.deriveColor(0, 1, 1, 0.9));
            gc.setFont(mapFontBold(0.69));
            gc.fillText(sat.name(), px + 5, py - 3);
        }

        gc.restore();
    }

    // ── DX spots ─────────────────────────────────────────────────────────────

    private void drawDxSpots(GraphicsContext gc, double w, double h, Settings s) {
        lastSpots = new ArrayList<>();
        List<DxSpot> providerSpots = services.dxSpotProvider.getCached();
        if (providerSpots != null) lastSpots.addAll(providerSpots);
        lastSpots.addAll(services.dxClusterClient.getClusterSpots());
        if (lastSpots.isEmpty()) return;

        gc.save();
        gc.setFont(mapFontBold(0.69));

        for (DxSpot spot : lastSpots) {
            if (spot.ageMinutes() > s.getDxMaxAgeMinutes()) continue;
            if (!s.getDxBandFilter().equals("ALL") && !s.getDxBandFilter().equals(spot.getBand())) continue;

            double lat = spot.getDxLat();
            double lon = spot.getDxLon();
            if (lat == 0 && lon == 0) {
                double[] ll = services.zoneLookupService.callsignToLatLon(spot.getDxCallsign());
                if (ll == null) continue;
                lat = ll[0]; lon = ll[1];
                // Cache resolved coords on the spot so click hit-testing works
                spot.setDxLat(lat);
                spot.setDxLon(lon);
            }

            double px = lonToX(lon, w);
            double py = latToY(lat, h);
            double age = spot.ageMinutes();
            double dotR = age < 5 ? 5 : age < 15 ? 4 : 3;

            gc.setFill(Color.web(spot.getModeColor()));
            gc.fillOval(px - dotR, py - dotR, dotR * 2, dotR * 2);
            gc.setStroke(Color.web("#ffffff", 0.5));
            gc.setLineWidth(0.5);
            gc.strokeOval(px - dotR, py - dotR, dotR * 2, dotR * 2);

            if (s.isDxShowCallsigns() && dotR >= 4) {
                gc.setFill(Color.WHITE);
                gc.fillText(spot.getDxCallsign(), px + dotR + 1, py + 3);
            }
        }
        gc.restore();
    }

    // ── Mode legend ───────────────────────────────────────────────────────────

    private static final String[][] MODE_LEGEND = {
        {"CW",   "#00ccff"},
        {"SSB",  "#ffd700"},
        {"FT8",  "#00ff88"},
        {"FT4",  "#aaff00"},
        {"RTTY", "#ff8800"},
        {"PSK",  "#ff44ff"},
        {"DIGI", "#aaaaaa"},
    };

    private void drawModeLegend(GraphicsContext gc, double w, double h) {
        gc.save();
        double fs   = services.getSettings().getFontSize() * 0.77;
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, fs));
        double dotR = fs * 0.45;
        double itemW = fs * 4.2;   // width per legend entry
        double barH  = fs + dotR * 2 + 6;
        double totalW = itemW * MODE_LEGEND.length;
        double startX = (w - totalW) / 2.0;
        double y      = h - barH;

        // Background strip
        gc.setFill(Color.web("#080c1a", 0.78));
        gc.fillRect(startX - 6, y, totalW + 12, barH);

        double cx = startX + itemW / 2.0;
        for (String[] entry : MODE_LEGEND) {
            // Coloured dot
            gc.setFill(Color.web(entry[1]));
            gc.fillOval(cx - dotR * 2.5, y + 3, dotR * 2, dotR * 2);
            // Label
            gc.setFill(Color.web("#ccd6f6"));
            gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
            gc.fillText(entry[0], cx - dotR * 0.5, y + barH - 4);
            cx += itemW;
        }
        gc.restore();
    }

    // ── QTH marker ───────────────────────────────────────────────────────────

    private void drawQthMarker(GraphicsContext gc, double w, double h, Settings s) {
        double px = lonToX(s.getQthLon(), w);
        double py = latToY(s.getQthLat(), h);

        gc.save();
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(1.5);
        gc.strokeLine(px - 6, py, px + 6, py);
        gc.strokeLine(px, py - 6, px, py + 6);
        gc.setFill(Color.GOLD);
        gc.fillOval(px - 3, py - 3, 6, 6);
        gc.setFont(mapFontBold(0.85));
        gc.fillText(s.getCallsign(), px + 7, py - 3);
        gc.restore();
    }

    // ── Subsolar point ────────────────────────────────────────────────────────

    private void drawSubsolarPoint(GraphicsContext gc, double w, double h) {
        if (nightMask == null) return;
        var sun = nightMask.getSolarPosition();
        double px = lonToX(sun.getSubsolarLon(), w);
        double py = latToY(sun.getSubsolarLat(), h);

        gc.save();
        // Glow halo
        gc.setFill(Color.web("#ffe000", 0.15));
        gc.fillOval(px - 14, py - 14, 28, 28);
        // Sun disc
        gc.setFill(Color.web("#ffe000", 0.95));
        gc.fillOval(px - 8, py - 8, 16, 16);
        // Rays
        gc.setStroke(Color.web("#ffe000", 0.7));
        gc.setLineWidth(1.5);
        for (int ray = 0; ray < 8; ray++) {
            double angle = Math.toRadians(ray * 45);
            gc.strokeLine(
                px + 11 * Math.cos(angle), py + 11 * Math.sin(angle),
                px + 17 * Math.cos(angle), py + 17 * Math.sin(angle));
        }
        gc.restore();
    }

    // ── Click handling ────────────────────────────────────────────────────────

    private void handleMapClick(double mx, double my) {
        if (dxSpotClickCallback == null || lastSpots.isEmpty()) return;
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        Settings s = services.getSettings();

        for (DxSpot spot : lastSpots) {
            if (spot.ageMinutes() > s.getDxMaxAgeMinutes()) continue;
            double px = lonToX(spot.getDxLon(), w);
            double py = latToY(spot.getDxLat(), h);
            if (Math.abs(mx - px) < 8 && Math.abs(my - py) < 8) {
                dxSpotClickCallback.accept(spot);
                return;
            }
        }
    }

    // ── Font helpers ──────────────────────────────────────────────────────────

    /** Canvas font scaled relative to the settings base font size. em=1.0 → base size. */
    private Font mapFont(double em) {
        return Font.font("Liberation Mono", services.getSettings().getFontSize() * em);
    }
    private Font mapFontBold(double em) {
        return Font.font("Liberation Mono", FontWeight.BOLD, services.getSettings().getFontSize() * em);
    }

    // ── Projection helpers ────────────────────────────────────────────────────

    public double lonToX(double lon, double w) { return (lon + 180.0) / 360.0 * w; }
    public double latToY(double lat, double h) { return (90.0 - lat) / 180.0 * h; }

    public void settingsChanged() { redraw(); }
}
