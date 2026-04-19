package com.wm3j.jmap.ui.windows;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.dx.DxSpot;
import com.wm3j.jmap.service.zones.ZoneInfo;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Floating DX info window — populated by clicking a spot on the map.
 * Font sizes use em — scales with root font size setting.
 */
public class DXInfoWindow extends FloatingWindow {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm z");

    private final ServiceRegistry services;

    private final Label noSpotLabel;
    private final Label callsignLabel;
    private final Label bandLabel;
    private final Label modeLabel;
    private final Label localTimeLabel;
    private final Label latLonLabel;
    private final Label zonesLabel;
    private final Label gridLabel;
    private final Label spotterLabel;

    private DxSpot currentSpot;
    private static final double CLEAR_AFTER_MINUTES = 10.0;

    public DXInfoWindow(ServiceRegistry services) {
        super("🌍  DX INFORMATION", 220);
        this.services = services;

        noSpotLabel    = styled("Click a spot on the map", "0.77em", "#4a5580", false);
        callsignLabel  = styled("", "1.54em", "#00ccff", true);
        bandLabel      = styled("", "0.85em", "#ffd700", false);
        modeLabel      = styled("", "0.85em", "#ffffff", true);
        localTimeLabel = styled("", "0.92em", "#00cc66", false);
        latLonLabel    = styled("", "0.85em", "#ccd6f6", false);
        zonesLabel     = styled("", "0.85em", "#ccd6f6", false);
        gridLabel      = styled("", "1em",    "#2a7fff", true);
        spotterLabel   = styled("", "0.77em", "#4a5580", false);

        contentBox.getChildren().add(noSpotLabel);
    }

    public void showSpot(DxSpot spot) {
        this.currentSpot = spot;
        setVisible(true);
        update();
    }

    @Override
    public void update() {
        if (currentSpot == null) {
            contentBox.getChildren().setAll(noSpotLabel);
            return;
        }
        // Auto-clear if the spot has aged out
        if (currentSpot.ageMinutes() > CLEAR_AFTER_MINUTES) {
            currentSpot = null;
            contentBox.getChildren().setAll(noSpotLabel);
            return;
        }

        DxSpot s = currentSpot;
        callsignLabel.setText(s.getDxCallsign());
        bandLabel.setText(s.getBand() + "  " + String.format("%.1f kHz", s.getFrequencyKhz()));
        String mode = s.getMode();
        modeLabel.setText("⬤  " + mode);
        modeLabel.setStyle("-fx-font-size: 0.85em; -fx-font-weight: bold; -fx-text-fill: " + s.getModeColor() + ";");

        String dxLocalTime = s.getLocalTimeAtSpot();
        if (dxLocalTime == null || dxLocalTime.isEmpty()) {
            ZoneOffset dxOffset = ZoneOffset.ofTotalSeconds((int) (s.getDxLon() / 15.0 * 3600));
            dxLocalTime = ZonedDateTime.now(dxOffset).format(TIME_FMT);
        }
        localTimeLabel.setText("Local: " + dxLocalTime);

        latLonLabel.setText(String.format("%.2f°  %.2f°", s.getDxLat(), s.getDxLon()));

        ZoneInfo zi = services.zoneLookupService.lookup(s.getDxLat(), s.getDxLon());
        zonesLabel.setText(String.format("CQ %d  ITU %d  %s", zi.cqZone(), zi.ituZone(), zi.arrlSection()));

        gridLabel.setText(services.zoneLookupService.toGridSquare(s.getDxLat(), s.getDxLon()).toUpperCase());

        long ageMin = (long) s.ageMinutes();
        spotterLabel.setText("de " + s.getSpotter() + "  " + ageMin + " min ago");

        contentBox.getChildren().setAll(
            callsignLabel, bandLabel, modeLabel, sep(),
            localTimeLabel, latLonLabel, sep(),
            zonesLabel, gridLabel, sep(),
            spotterLabel
        );
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
}
