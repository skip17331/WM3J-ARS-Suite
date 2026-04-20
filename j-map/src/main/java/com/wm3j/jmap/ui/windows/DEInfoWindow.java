package com.wm3j.jmap.ui.windows;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.config.Settings;
import com.wm3j.jmap.service.zones.ZoneInfo;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Floating DE (your station) info window.
 * Font sizes use em — scales with root font size setting.
 */
public class DEInfoWindow extends FloatingWindow {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss z");

    private final ServiceRegistry services;

    private final Label callsignLabel;
    private final Label localTimeLabel;
    private final Label latLonLabel;
    private final Label zonesLabel;
    private final Label gridLabel;

    public DEInfoWindow(ServiceRegistry services) {
        super("🏠  DE — YOUR STATION", 260);
        this.services = services;

        callsignLabel  = styled("", "2.10em", "#ffd700", true);
        localTimeLabel = styled("", "1.21em", "#00cc66", false);
        latLonLabel    = styled("", "1.06em", "#ccd6f6", false);
        zonesLabel     = styled("", "1.06em", "#ccd6f6", false);
        gridLabel      = styled("", "1.21em", "#2a7fff", true);

        contentBox.getChildren().addAll(
            callsignLabel, localTimeLabel, latLonLabel,
            sep(), zonesLabel, gridLabel
        );

        update();
    }

    @Override
    public void update() {
        Settings s = services.getSettings();

        callsignLabel.setText(s.getCallsign());

        ZoneId tz = safeZone(s.getTimezone());
        localTimeLabel.setText(ZonedDateTime.now(tz).format(TIME_FMT));

        latLonLabel.setText(String.format("%.4f°  %.4f°", s.getQthLat(), s.getQthLon()));

        ZoneInfo zi = services.zoneLookupService.lookup(s.getQthLat(), s.getQthLon());
        zonesLabel.setText(String.format("CQ %d  ITU %d  %s", zi.cqZone(), zi.ituZone(), zi.arrlSection()));

        String grid = s.getQthGrid().isBlank()
            ? services.zoneLookupService.toGridSquare(s.getQthLat(), s.getQthLon())
            : s.getQthGrid();
        gridLabel.setText(grid.toUpperCase());
    }

    private Label styled(String text, String em, String color, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + em + "; -fx-text-fill: " + color + ";" +
                   (bold ? " -fx-font-weight: bold;" : ""));
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefHeight(4);
        r.setStyle("-fx-border-color: #1e2d50; -fx-border-width: 0 0 1 0;");
        return r;
    }

    private static ZoneId safeZone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneId.of("UTC");
        try { return ZoneId.of(tz); }
        catch (Exception e) { return ZoneId.of("UTC"); }
    }
}
