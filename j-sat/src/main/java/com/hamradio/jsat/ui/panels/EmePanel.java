package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.service.config.JsatSettings;
import com.hamradio.jsat.service.orbital.LunarMath;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EmePanel — moon-bounce dashboard tile. Shows az/el, range, two-way
 * Doppler for the configured RX frequency, optical libration, and (if a
 * DX grid is configured) the next common window when both stations can
 * see the Moon.
 *
 * <p>Updated by the existing 1 Hz tick in {@link com.hamradio.jsat.ui.main.MainWindow}
 * via {@link com.hamradio.jsat.ui.main.DashboardLayout#updatePanels()}, so
 * Doppler numbers track in real time and the operator can pre-correct
 * the rig before TX.
 */
public class EmePanel extends VBox {

    private static final DateTimeFormatter HMS_UTC =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("UTC"));

    private final ServiceRegistry services;

    private final Label headerLbl;
    private final Label azElLbl;
    private final Label rangeLbl;
    private final Label dopplerLbl;
    private final Label librationLbl;
    private final Label windowLbl;

    public EmePanel(ServiceRegistry services) {
        this.services = services;
        setSpacing(2);
        setPadding(new Insets(6, 8, 6, 8));
        setStyle("-fx-background-color: #0d1018; -fx-border-color: #2a3a5a; -fx-border-width: 1; -fx-border-radius: 4;");

        JsatSettings s = services.getSettings();
        int fz = s.effective(s.rigRotorFontSize);

        headerLbl    = styled("🌒  EME",                        "#bb88ff", true,  Math.round(fz * 1.05f));
        azElLbl      = styled("Az --°  El --°",                  "#ccddee", false, fz);
        rangeLbl     = styled("Range -- km",                     "#9fbacd", false, fz);
        dopplerLbl   = styled("Doppler 2-way: -- Hz @ -- MHz",   "#ffd966", true,  fz);
        librationLbl = styled("Libration --   --",               "#9fbacd", false, fz);
        windowLbl    = styled("",                                 "#88aacc", false, Math.max(10, fz - 1));
        windowLbl.setWrapText(true);

        getChildren().addAll(headerLbl, azElLbl, rangeLbl, dopplerLbl, librationLbl, windowLbl);
    }

    /** Called by the 1 Hz animation tick. */
    public void update() {
        JsatSettings s = services.getSettings();
        Instant now = Instant.now();

        // Az/El/range/range-rate from the operator's QTH
        double[] aer = LunarMath.moonAzElRange(s.qthLat, s.qthLon, 0.0, now);
        double az = aer[0], el = aer[1], range = aer[2], rangeRate = aer[3];
        azElLbl.setText(String.format("Az %5.1f°  El %+5.1f°", az, el));
        rangeLbl.setText(String.format("Range %,7.0f km  ·  Δ %+5.3f km/s", range, rangeRate));

        // Two-way Doppler on the configured EME frequency
        double dopHz = LunarMath.dopplerShiftHz(s.emeFrequencyHz, rangeRate);
        double freqMhz = s.emeFrequencyHz / 1_000_000.0;
        dopplerLbl.setText(String.format("Doppler 2-way: %+,7.0f Hz @ %.0f MHz",
                                          dopHz, freqMhz));

        // Libration — colour-coded
        double[] lib = LunarMath.libration(now);
        double libMag = LunarMath.librationMagnitudeDeg(now);
        String libVerdict = libMag < 2.0 ? "clean"
                          : libMag < 5.0 ? "moderate"
                                         : "smeared";
        String libColour = libMag < 2.0 ? "#a6e3a1"
                         : libMag < 5.0 ? "#ffd966"
                                        : "#f38ba8";
        librationLbl.setText(String.format("Libration lon %+5.2f°  lat %+5.2f°  (%s, %.1f°)",
                                            lib[0], lib[1], libVerdict, libMag));
        librationLbl.setStyle("-fx-text-fill: " + libColour + "; -fx-font-family: 'Liberation Mono';");

        // Common-window prediction
        String dxGrid = (s.emeDxGrid == null ? "" : s.emeDxGrid.trim());
        if (dxGrid.length() >= 4 && el >= s.minPassElevationDeg) {
            double[] dxLatLon = gridToLatLon(dxGrid);
            if (dxLatLon != null) {
                LunarMath.MoonWindow w = LunarMath.nextCommonWindow(
                    s.qthLat, s.qthLon,
                    dxLatLon[0], dxLatLon[1],
                    now, Duration.ofHours(24), s.minPassElevationDeg);
                if (w != null) {
                    long mins = w.duration().toMinutes();
                    windowLbl.setText(String.format("Next window with %s:  %s – %s UTC  (%dh %02dm)",
                        dxGrid.toUpperCase(),
                        HMS_UTC.format(w.startUtc),
                        HMS_UTC.format(w.endUtc),
                        mins / 60, mins % 60));
                } else {
                    windowLbl.setText("No common window with " + dxGrid.toUpperCase() + " in next 24h");
                }
            } else {
                windowLbl.setText("");
            }
        } else if (el < s.minPassElevationDeg) {
            // Moon below the horizon — show next moonrise
            Instant rise = nextMoonriseFromNow(s.qthLat, s.qthLon, now, s.minPassElevationDeg);
            if (rise != null) {
                windowLbl.setText("Moon below horizon — next rise " + HMS_UTC.format(rise) + " UTC");
            } else {
                windowLbl.setText("Moon below horizon");
            }
        } else {
            windowLbl.setText("");
        }
    }

    /**
     * Convert a 4- or 6-character Maidenhead grid to {lat, lon} in
     * degrees (centre of the square). Returns null on invalid input.
     */
    private static double[] gridToLatLon(String g) {
        if (g == null) return null;
        String t = g.trim().toUpperCase();
        if (t.length() < 4) return null;
        char fLon = t.charAt(0), fLat = t.charAt(1);
        char sLon = t.charAt(2), sLat = t.charAt(3);
        if (fLon < 'A' || fLon > 'R' || fLat < 'A' || fLat > 'R'
            || sLon < '0' || sLon > '9' || sLat < '0' || sLat > '9') return null;

        double lon = (fLon - 'A') * 20.0 + (sLon - '0') * 2.0 - 180.0;
        double lat = (fLat - 'A') * 10.0 + (sLat - '0') * 1.0 - 90.0;

        if (t.length() >= 6) {
            char sslon = Character.toLowerCase(t.charAt(4));
            char sslat = Character.toLowerCase(t.charAt(5));
            if (sslon >= 'a' && sslon <= 'x' && sslat >= 'a' && sslat <= 'x') {
                lon += (sslon - 'a') * (2.0 / 24.0);
                lat += (sslat - 'a') * (1.0 / 24.0);
                lon += (1.0 / 24.0);
                lat += (0.5 / 24.0);
            }
        } else {
            lon += 1.0;
            lat += 0.5;
        }
        return new double[] { lat, lon };
    }

    /**
     * Coarse scan for the next time the Moon climbs above the given
     * elevation. 10-minute step is fine — moonrise takes minutes.
     */
    private static Instant nextMoonriseFromNow(double lat, double lon, Instant from,
                                                double minEl) {
        for (int min = 0; min < 24 * 60; min += 10) {
            Instant t = from.plusSeconds(min * 60L);
            if (LunarMath.moonAzElRange(lat, lon, 0, t)[1] >= minEl) return t;
        }
        return null;
    }

    private static Label styled(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
