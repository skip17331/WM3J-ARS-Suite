package com.ars.fx.surface;

import com.ars.fx.data.Bands;
import com.ars.fx.data.CallsignLookup;
import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.DxccInfo;
import com.ars.fx.data.LookupConfig;
import com.ars.fx.data.RotorClient;
import com.ars.fx.data.Units;
import com.ars.fx.shell.Tips;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * What J-Map shows about a clicked spot: a toast over the map with a "Log this" button, and the same details in the
 * "DX station info" drawer. Country, distance and bearing are immediate; the name and QTH follow if a lookup answers.
 */
final class DxInfoCard {
    private DxInfoCard() {}

    private static final double TOAST_SECONDS = 15;
    private static final VBox drawer = new VBox(6);
    private static Node toast;
    private static PauseTransition timer;
    private static String showing;       // the call on screen, so a slow lookup can't fill in a newer spot
    private static CallsignLookup.Result answer;   // the lookup for {@link #showing}, for the QSO form

    static { drawer.getChildren().setAll(lbl("Select a spot to see DXCC, beam heading & distance.", "dx-info-v")); }

    /** The drawer body. One node is kept so the last spot survives J-Map being rebuilt. */
    static Region drawerBody() { return drawer; }

    /** The lookup's answer for this call, if it has come in. */
    static CallsignLookup.Result answerFor(String call) { return call.equals(showing) ? answer : null; }

    /** A one-line reminder of who this is, for the QSO form. */
    static String summary(JMapView.MapSpot s) {
        DxccInfo.Info dxcc = DxccInfo.of(s.call());
        return join(" · ", dxcc == null ? null : dxcc.country(), beam(s));
    }

    static void show(Node source, JMapView.MapSpot s, boolean workedBefore) {
        showing = s.call(); answer = null;
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setMaxSize(520, Region.USE_PREF_SIZE); card.setMinWidth(460);
        card.setStyle("-fx-background-color:-ars-surface-1;-fx-background-radius:12;-fx-cursor:hand;"
                + "-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:12;");
        Tips.tip(card, "Click to dismiss");

        Label log = lbl("Log this", "jl-logbtn"); log.setStyle("-fx-cursor:hand;-fx-padding:6 14;");
        Tips.tip(log, "Open the QSO form, filled in from this spot");
        log.setOnMouseClicked(e -> { MapQsoCard.open(log, s); dismiss(); e.consume(); });
        Node action = MapQsoCard.isPinned() ? null : log;        // a docked form fills straight from the spot instead

        render(card, s, workedBefore, null, 2, action);
        render(drawer, s, workedBefore, null, 1, null);
        pop(source, card);
        if (MapQsoCard.isPinned()) MapQsoCard.open(source, s);

        if (!LookupConfig.anyEnabled()) return;
        Thread t = new Thread(() -> {
            CallsignLookup.Result r = CallsignLookup.lookup(s.call());
            if (r == null) return;
            Platform.runLater(() -> {
                if (!s.call().equals(showing)) return;
                answer = r;
                render(card, s, workedBefore, r, 2, action);
                render(drawer, s, workedBefore, r, 1, null);
                MapQsoCard.lookupArrived(s.call(), r);
            });
        }, "dx-info-lookup");
        t.setDaemon(true); t.start();
    }

    private static void render(VBox box, JMapView.MapSpot s, boolean workedBefore, CallsignLookup.Result r, int columns, Node action) {
        DxccInfo.Info dxcc = DxccInfo.of(s.call());
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(10, lbl(s.call(), "jhub-card-title"));
        head.setAlignment(Pos.CENTER_LEFT);
        if (dxcc != null) head.getChildren().add(lbl(join(" · ", dxcc.country(), dxcc.continent()), "dx-info-v"));
        if (action != null) head.getChildren().addAll(gap, action);

        List<String[]> rows = new ArrayList<>();
        if (r != null) rows.add(new String[]{"Station", join(" · ", r.name(), join(", ", r.city(), r.state()), r.grid())});
        rows.add(new String[]{"Freq", join(" · ", ClusterClient.fmtFreq(s.freqHz()) + " MHz", s.band(), s.mode())});
        rows.add(new String[]{"Beam", beam(s)});
        rows.add(new String[]{"Plan", Bands.describe(s.freqHz())});
        String age = ClusterClient.age(s.arrivalMs());
        rows.add(new String[]{"Spotted", join(" · ", s.spotter(), "now".equals(age) ? age : age + " ago")});
        rows.add(new String[]{"Log", workedBefore ? "Worked before" : "Not in your log"});

        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(4);
        for (int c = 0; c < columns; c++) {
            ColumnConstraints key = new ColumnConstraints(); key.setHalignment(HPos.LEFT);
            ColumnConstraints val = new ColumnConstraints(); val.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(key, val);
        }
        int cell = 0;
        for (String[] row : rows) {
            if (row[1] == null || row[1].isBlank()) continue;
            grid.add(lbl(row[0], "dx-info-k"), cell % columns * 2, cell / columns);
            grid.add(value(row[1]), cell % columns * 2 + 1, cell / columns);
            cell++;
        }
        box.getChildren().setAll(head, grid);
        if (s.comment() != null && !s.comment().isBlank()) box.getChildren().add(value(s.comment()));
    }

    private static Label value(String text) {
        Label l = lbl(text, "dx-info-v"); l.setWrapText(true);
        return l;
    }

    private static String beam(JMapView.MapSpot s) {
        double km = DxccInfo.distanceKm(SpotAction.qLat(), SpotAction.qLon(), s.lat(), s.lon());
        int deg = (int) Math.round(SpotAction.bearing(SpotAction.qLat(), SpotAction.qLon(), s.lat(), s.lon()));
        return String.format("%03d° %s · %s", deg, RotorClient.cardinal(deg), Units.distText(km));
    }

    /** The non-blank parts, joined. */
    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null && !p.isBlank()) sb.append(sb.isEmpty() ? "" : sep).append(p.trim());
        return sb.toString();
    }

    private static void pop(Node source, Region card) {
        Parent root = source.getScene() == null ? null : source.getScene().getRoot();
        if (!(root instanceof StackPane host)) return;
        dismiss();
        StackPane.setAlignment(card, Pos.TOP_RIGHT);
        StackPane.setMargin(card, new Insets(110, 350, 0, 0));   // over the map, clear of the right column
        card.setOnMouseClicked(e -> dismiss());
        host.getChildren().add(card);
        toast = card;
        timer = new PauseTransition(Duration.seconds(TOAST_SECONDS));
        timer.setOnFinished(e -> dismiss());
        timer.play();
        card.setOnMouseEntered(e -> timer.stop());               // stays up while you read it
        card.setOnMouseExited(e -> timer.playFromStart());
    }

    private static void dismiss() {
        if (timer != null) timer.stop();
        if (toast != null && toast.getParent() instanceof StackPane p) p.getChildren().remove(toast);
        toast = null;
    }
}
