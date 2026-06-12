package com.wm3j.jmap.ui.windows;

import com.fasterxml.jackson.databind.JsonNode;
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
    private final Label nameLabel;
    private final Label countryLabel;
    private final Label bandLabel;
    private final Label modeLabel;
    private final Label localTimeLabel;
    private final Label latLonLabel;
    private final Label zonesLabel;
    private final Label gridLabel;
    private final Label spotterLabel;

    private DxSpot currentSpot;
    // Callsign-lookup overlay: may refine lat/lon and add name/country/state
    private String  lookupName;
    private String  lookupCountry;
    private String  lookupState;
    private double  lookupLat;
    private double  lookupLon;
    private boolean hasLookup;

    private static final double CLEAR_AFTER_MINUTES = 10.0;

    public DXInfoWindow(ServiceRegistry services) {
        super("🌍  DX INFORMATION", 260);
        this.services = services;

        noSpotLabel    = styled("Click a spot on the map", "0.97em", "#6f7d8c", false);
        callsignLabel  = styled("", "1.95em", "#4fc3f7", true);
        nameLabel      = styled("", "0.97em", "#dde3ea", false);
        countryLabel   = styled("", "0.97em", "#88aacc", false);
        bandLabel      = styled("", "1.06em", "#ffd700", false);
        modeLabel      = styled("", "1.06em", "#ffffff", true);
        localTimeLabel = styled("", "1.12em", "#00cc66", false);
        latLonLabel    = styled("", "1.06em", "#dde3ea", false);
        zonesLabel     = styled("", "1.06em", "#dde3ea", false);
        gridLabel      = styled("", "1.21em", "#4fc3f7", true);
        spotterLabel   = styled("", "0.97em", "#6f7d8c", false);

        contentBox.getChildren().add(noSpotLabel);
    }

    public void showSpot(DxSpot spot) {
        this.currentSpot = spot;
        this.hasLookup     = false;
        this.lookupName    = null;
        this.lookupCountry = null;
        this.lookupState   = null;
        this.lookupLat     = 0;
        this.lookupLon     = 0;
        setVisible(true);
        update();
    }

    /**
     * Called when a CALLSIGN_RESULT arrives from j-hub.
     * Enriches the displayed spot with operator name, country, and a more precise
     * lat/lon if the cluster spot had none (lat=0, lon=0).
     */
    public void applyCallsignResult(JsonNode node) {
        if (currentSpot == null) return;
        String call = node.path("callsign").asText("").trim().toUpperCase();
        if (!call.equals(currentSpot.getDxCallsign().toUpperCase())) return;
        if (!node.path("found").asBoolean(false)) return;

        lookupName    = node.path("name").asText("").trim();
        lookupCountry = node.path("country").asText("").trim();
        lookupState   = node.path("state").asText("").trim();
        double lat = node.path("lat").asDouble(0);
        double lon = node.path("lon").asDouble(0);
        if (lat != 0 || lon != 0) {
            lookupLat = lat;
            lookupLon = lon;
            // Patch the spot if cluster didn't supply coordinates
            if (currentSpot.getDxLat() == 0 && currentSpot.getDxLon() == 0) {
                currentSpot.setDxLat(lat);
                currentSpot.setDxLon(lon);
            }
        }
        hasLookup = true;
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

        // Name from lookup
        nameLabel.setText(hasLookup && lookupName != null && !lookupName.isEmpty() ? lookupName : "");

        // Country: use state instead when the station is in the USA
        String country = hasLookup && lookupCountry != null && !lookupCountry.isEmpty()
            ? lookupCountry
            : (s.getDxccEntity() != null && !s.getDxccEntity().isEmpty() ? s.getDxccEntity() : "");
        boolean isUsa = country.equalsIgnoreCase("USA")
                     || country.equalsIgnoreCase("United States")
                     || country.equalsIgnoreCase("US");
        String locationLine;
        if (isUsa && hasLookup && lookupState != null && !lookupState.isEmpty()) {
            locationLine = lookupState + "  •  USA";
        } else {
            locationLine = country;
        }
        countryLabel.setText(locationLine);

        long hz = Math.round(s.getFrequencyKhz() * 1000.0);
        com.wm3j.jmap.service.config.Settings st = services.getSettings();
        com.jlog.bandplan.BandplanLoader.Description bp =
            com.jlog.bandplan.BandplanLoader.getInstance().describe(
                hz,
                st != null ? st.getBandplanRegion()  : null,
                st != null ? st.getBandplanCountry() : null);
        String bandTxt = String.format("%s  %.1f kHz", s.getBand(), s.getFrequencyKhz());
        if (bp != null) bandTxt += "   " + bp.activity + " — " + bp.label;
        bandLabel.setText(bandTxt);
        String mode = s.getMode();
        modeLabel.setText("⬤  " + mode);
        modeLabel.setStyle("-fx-font-size: 1.06em; -fx-font-weight: bold; -fx-text-fill: " + s.getModeColor() + ";");

        double dispLat = (hasLookup && lookupLat != 0) ? lookupLat : s.getDxLat();
        double dispLon = (hasLookup && lookupLon != 0) ? lookupLon : s.getDxLon();

        String dxLocalTime = s.getLocalTimeAtSpot();
        if (dxLocalTime == null || dxLocalTime.isEmpty()) {
            ZoneOffset dxOffset = ZoneOffset.ofTotalSeconds((int) (dispLon / 15.0 * 3600));
            dxLocalTime = ZonedDateTime.now(dxOffset).format(TIME_FMT);
        }
        localTimeLabel.setText("Local: " + dxLocalTime);

        latLonLabel.setText(String.format("%.2f°  %.2f°", dispLat, dispLon));

        ZoneInfo zi = services.zoneLookupService.lookup(dispLat, dispLon);
        zonesLabel.setText(String.format("CQ %d  ITU %d  %s", zi.cqZone(), zi.ituZone(), zi.arrlSection()));

        gridLabel.setText(services.zoneLookupService.toGridSquare(dispLat, dispLon).toUpperCase());

        long ageMin = (long) s.ageMinutes();
        spotterLabel.setText("de " + s.getSpotter() + "  " + ageMin + " min ago");

        // Build content list, omitting empty labels
        java.util.List<javafx.scene.Node> children = new java.util.ArrayList<>();
        children.add(callsignLabel);
        if (!nameLabel.getText().isEmpty())    children.add(nameLabel);
        if (!countryLabel.getText().isEmpty()) children.add(countryLabel);
        children.add(bandLabel);
        children.add(modeLabel);
        children.add(sep());
        children.add(localTimeLabel);
        children.add(latLonLabel);
        children.add(sep());
        children.add(zonesLabel);
        children.add(gridLabel);
        children.add(sep());
        children.add(spotterLabel);
        contentBox.getChildren().setAll(children);
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
        r.setStyle("-fx-border-color: #323b47; -fx-border-width: 0 0 1 0;");
        return r;
    }
}
