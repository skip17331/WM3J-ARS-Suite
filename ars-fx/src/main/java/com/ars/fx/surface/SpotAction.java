package com.ars.fx.surface;

import com.ars.fx.data.Geo;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RigClient;
import com.ars.fx.data.RotorClient;
import com.ars.fx.shell.Shell;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Single click-a-spot action, shared by every surface that shows DX spots
 * (the J-Map dots + rail list, the J-Log spotter / heard-by drawers, and the
 * J-Hub cluster table). Clicking a spot:
 * <ol>
 *   <li>prefills whichever log is in use — the live J-Log form if it is the
 *       current surface, otherwise queued so it fills in the moment J-Log
 *       opens;</li>
 *   <li>tunes the rig (frequency + mode) via {@link RigClient};</li>
 *   <li>asks — with a small on-brand confirm card — whether to swing the beam
 *       to the spot's bearing, and on "Rotate" drives {@link RotorClient} and
 *       points the J-Map beam wedge there.</li>
 * </ol>
 */
public final class SpotAction {
    private SpotAction() {}

    /** A new-QSO prefill the active log form applies. {@code freqMhz}/{@code mode} may be null/blank. */
    public record Prefill(String call, String freqMhz, String band, String mode) {}

    private static volatile Consumer<Prefill> logTarget;     // set by the live J-Log normal form
    private static volatile Consumer<Prefill> contestTarget; // set by the live J-Log contest form
    private static volatile Prefill pending;                 // queued when neither log form is on screen

    /** The J-Log form registers how to fill itself (called each time it is built). */
    public static void setLogTarget(Consumer<Prefill> t) { logTarget = t; }
    /** The J-Log contest form registers how to fill itself. */
    public static void setContestTarget(Consumer<Prefill> t) { contestTarget = t; }
    /** J-Log build() pulls (and clears) a queued prefill so a spot picked elsewhere lands in the fresh form. */
    public static Prefill takePending() { Prefill p = pending; pending = null; return p; }

    /** Spot click where coordinates aren't known up front — geo-locate from the call. */
    public static void onSpot(Node source, String call, long freqHz, String mode, String band) {
        double[] ll = Geo.of(call);
        onSpot(source, call, freqHz, mode, band, ll == null ? Double.NaN : ll[0], ll == null ? Double.NaN : ll[1]);
    }

    /** Spot click with a known location (map dots already carry lat/lon). */
    public static void onSpot(Node source, String call, long freqHz, String mode, String band, double lat, double lon) {
        if (call == null || call.isBlank()) return;
        String c = call.trim().toUpperCase();

        // 1. prefill the active log — live form if J-Log is up, else queue for its next open
        Prefill pf = new Prefill(c,
                freqHz > 0 ? String.format(Locale.US, "%.3f", freqHz / 1e6) : null,
                (band == null || band.isBlank()) ? null : band,
                logMode(mode, freqHz));
        if ("logc".equals(Shell.currentSurface) && contestTarget != null) contestTarget.accept(pf);
        else if ("log".equals(Shell.currentSurface) && logTarget != null) logTarget.accept(pf);
        else pending = pf;

        // 2. tune the radio
        RigClient rig = RigClient.getInstance();
        if (freqHz > 0) rig.setFreqHz(freqHz);
        String hm = hamlibMode(mode, freqHz);
        if (hm != null) rig.setMode(hm);

        // 3. offer to rotate the antenna toward the spot
        if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
            int brg = (int) Math.round(bearing(qLat(), qLon(), lat, lon));
            askRotate(source, c, brg);
        }
    }

    // ── rotate-confirm overlay ────────────────────────────────────────────────
    private static void askRotate(Node source, String call, int bearing) {
        Scene sc = source == null ? null : source.getScene();
        if (sc == null || !(sc.getRoot() instanceof Pane root)) return;

        Label title = lbl("Rotate antenna?", "jhub-card-title");
        Label body = lbl(call + " bears " + String.format("%03d°", bearing)
                + " " + RotorClient.cardinal(bearing) + " from " + HubConfig.grid()
                + ".  Swing the beam there?", "jl-emeta");
        body.setWrapText(true); body.setMaxWidth(320);

        Label no = lbl("Not now", "jl-clearbtn"); no.setStyle("-fx-cursor:hand;-fx-padding:7 14;");
        Label yes = lbl("Rotate  ⟳", "jl-logbtn"); yes.setStyle("-fx-cursor:hand;-fx-padding:7 16;");
        Region gap = new Region(); HBox.setHgrow(gap, javafx.scene.layout.Priority.ALWAYS);
        HBox btns = new HBox(10, gap, no, yes); btns.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, body, btns);
        card.setPadding(new Insets(20, 22, 18, 22)); card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color:-ars-surface-1;-fx-background-radius:12;"
                + "-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:12;");

        StackPane veil = new StackPane(card);
        veil.setStyle("-fx-background-color:rgba(6,9,13,0.55);");
        veil.setMinSize(0, 0);
        veil.prefWidthProperty().bind(root.widthProperty());
        veil.prefHeightProperty().bind(root.heightProperty());

        Runnable close = () -> root.getChildren().remove(veil);
        no.setOnMouseClicked(e -> close.run());
        veil.setOnMouseClicked(e -> { if (e.getTarget() == veil) close.run(); });   // click the scrim to dismiss
        yes.setOnMouseClicked(e -> {
            RotorClient.getInstance().moveTo(bearing);
            JMapView.setBeam(bearing);
            close.run();
        });
        root.getChildren().add(veil);
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    /** Spot mode normalised for the log's MODE field (phone → USB/LSB by band edge). */
    static String logMode(String mode, long hz) {
        if (mode == null || mode.isBlank()) return "";
        String m = mode.trim().toUpperCase();
        if (m.equals("SSB") || m.equals("PHONE")) return hz > 0 && hz < 10_000_000 ? "LSB" : "USB";
        return m;
    }

    /** Spot mode → a Hamlib mode token rigctld understands, or null to leave the rig's mode alone. */
    static String hamlibMode(String mode, long hz) {
        String m = logMode(mode, hz);
        switch (m) {
            case "": return null;
            case "FT8": case "FT4": case "JS8": case "PSK31": case "PSK":
            case "JT65": case "JT9": case "MFSK": case "OLIVIA": case "DATA": case "DIGI":
                return "PKTUSB";
            default: return m;   // CW / USB / LSB / RTTY / AM / FM pass straight through
        }
    }

    /** Initial great-circle bearing (deg, 0–360) from the QTH to the spot. */
    static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private static double qLat() { return dbl(HubConfig.get("station.lat", "39.7456"), 39.7456); }
    private static double qLon() { return dbl(HubConfig.get("station.lon", "-76.96"), -76.96); }
    private static double dbl(String s, double def) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } }
}
